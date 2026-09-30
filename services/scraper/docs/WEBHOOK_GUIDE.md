# Guia Técnico da Comunicação Assíncrona via Webhook 🔔

Este documento detalha o **endpoint assíncrono** do scraper service: como enfileirar um scraping e receber o resultado via POST na URL do cliente, com retry automático.

> 📖 **Contexto:** Este é o complemento assíncrono do [`SCRAPING_GUIDE.md`](SCRAPING_GUIDE.md) e opera **sobre** a fila documentada no [`QUEUE_GUIDE.md`](QUEUE_GUIDE.md). Leia a documentação da fila primeiro para entender o ciclo de vida do `ScrapeJob`.

> 🏗️ **Boundary arquitetural:** Este serviço é o **dumb producer**. O envelope descrito aqui é o **contrato HTTP** entre este Python e o serviço externo (Java) que faz o trabalho de smart consumer — deduplicação temporal, histórico de preço, regras de negócio. Não tente fazer inteligência de domínio aqui: o producer só coleta + entrega; o consumer trata.

---

## 1. O Problema

O endpoint síncrono `POST /api/v1/scrape` força o cliente HTTP a esperar o scraping terminar (5–60s com Playwright). Para clientes que não podem manter conexão aberta — workers assíncronos, integrações serverless, apps mobile — é necessário um padrão **fire-and-forget com callback**.

A solução implementada:

- Cliente dispara e recebe `202 Accepted` em <100ms
- Sistema enfileira o job (mesma fila do endpoint síncrono)
- Quando termina, faz POST com o resultado na `webhookUrl` do cliente
- Se o cliente retornar erro, retentamos com backoff exponencial

---

## 2. Visão Geral da Arquitetura

```
                          ┌────────────────────────────────────┐
   Cliente ──POST /async─▶│       API FastAPI                  │
                          │   src/api/v1/async_scrape_routes   │
                          │                                    │
                          │  1. gera requestId (se omitido)    │
                          │  2. INSERT ScrapeJob + WebhookDel.  │
                          │  3. job fica QUEUED na fila        │
                          │  4. devolve 202 { requestId, ... }  │
                          └────────────────┬───────────────────┘
                                           │
                                           │ (worker pool, paralelo)
                                           ▼
                          ┌────────────────────────────────────┐
                          │   ScrapeWorker pool (existente)    │
                          │                                    │
                          │  - claim_next() do ScrapeJob       │
                          │  - execute_scrape()                │
                          │  - mark_success / mark_failed      │
                          │  - mark_ready(delivery) ◀── NOVO   │
                          └────────────────┬───────────────────┘
                                           │
                                           │ (dispatcher pool, paralelo)
                                           ▼
                          ┌────────────────────────────────────┐
                          │   WebhookDispatcher pool           │
                          │   (N tasks asyncio)               │
                          │                                    │
                          │  - claim_next() do WebhookDelivery │
                          │  - POST payload → webhookUrl       │
                          │  - 2xx → DELIVERED                 │
                          │  - 4xx → FAILED (sem retry)        │
                          │  - 5xx/timeout → SENDING + backoff │
                          │  - max attempts → FAILED           │
                          └────────────────┬───────────────────┘
                                           │
                                           │ (cliente do cliente)
                                           ▼
                          ┌────────────────────────────────────┐
                          │   Webhook receiver do cliente      │
                          │                                    │
                          │   POST { requestId, jobId,         │
                          │          status, response }        │
                          │   → 202 Accepted                   │
                          └────────────────────────────────────┘
```

---

## 3. Endpoint `POST /api/v1/scrape/async`

### Request

```http
POST /api/v1/scrape/async
Authorization: Basic <credenciais>
Content-Type: application/json

{
  "request": {                              // ScrapeRequest normal
    "vendor": "OLX",
    "keyword": "thinkpad t480",
    "state": "sp",
    "min_price": 500,
    "max_price": 2500
  },
  "webhookUrl": "https://meu-app.com/hooks/scraper",  // obrigatório
  "requestId": "client-abc-123"                       // opcional
}
```

| Campo | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `request` | `ScrapeRequest` | Sim | Mesma estrutura do endpoint síncrono |
| `webhookUrl` | `HttpUrl` | Sim | URL absoluta que receberá o POST com o resultado |
| `requestId` | `string` (≤255) | Não | Chave de idempotência. Se omitido, UUID v4 é gerado |

### Response `202 Accepted`

```json
{
  "request_id": "client-abc-123",
  "job_id": "9f3c1a4e-...-...",
  "status": "queued",
  "webhook_url": "https://meu-app.com/hooks/scraper"
}
```

> **Importante:** O cliente HTTP **não espera** o scraping terminar. O 202 vem em <100ms após enfileirar.

### Possíveis erros

| Código | Causa |
|---|---|
| `401` | Credenciais inválidas (Basic Auth obrigatório) |
| `409` | `requestId` já existe no sistema |
| `422` | Body malformado (Pydantic) |

---

## 4. Endpoint `GET /api/v1/webhooks/{request_id}`

Para inspecionar o status de uma entrega (útil para debug e para clientes que perdem o webhook).

### Response `200 OK`

```json
{
  "request_id": "client-abc-123",
  "job_id": "9f3c1a4e-...-...",
  "webhook_url": "https://meu-app.com/hooks/scraper",
  "status": "DELIVERED",
  "attempts": 1,
  "max_attempts": 5,
  "last_attempt_at": "2026-08-24T13:45:01",
  "next_attempt_at": null,
  "delivered_at": "2026-08-24T13:45:01",
  "last_error": null,
  "last_response_code": 202,
  "created_at": "2026-08-24T13:44:55",
  "updated_at": "2026-08-24T13:45:01"
}
```

| Código | Causa |
|---|---|
| `200` | Delivery encontrada |
| `404` | `requestId` não existe |

---

## 5. Payload Enviado ao Cliente (POST no webhookUrl)

Quando o scraping termina, o sistema faz POST com:

```json
{
  "requestId": "client-abc-123",
  "jobId": "9f3c1a4e-...-...",
  "status": "SUCCESS",
  "response": {
    "success": true,
    "execution": {
      "id": "...",
      "vendor": "OLX",
      "status": "SUCCESS",
      "duration_ms": 12340,
      "total_found": 42,
      "new_items_count": 5,
      "used_fallback": false,
      "started_at": "...",
      "finished_at": "..."
    },
    "items": [
      { "vendor_listing_id": "...", "title": "...", "price": 1450.0, ... }
    ]
  }
}
```

- `status` (no topo) reflete sucesso/falha do **scraping** (`SUCCESS` ou `FAILED`)
- `response` é o `ScrapeResponse` completo — mesma estrutura do endpoint síncrono
- O cliente deve retornar **qualquer 2xx** (preferencialmente `202`) para confirmar o recebimento

### Exemplo de receiver (Python/FastAPI)

```python
from fastapi import FastAPI, Request

app = FastAPI()


@app.post("/hooks/scraper")
async def receive_scrape_result(request: Request):
    payload = await request.json()
    request_id = payload["requestId"]
    status = payload["status"]
    items = payload["response"]["items"]

    # Processa o resultado...
    print(f"[{request_id}] {status} - {len(items)} items")

    # Retorna 202 para o dispatcher parar de tentar
    return {"received": True}
```

---

## 6. Ciclo de Vida da Entrega (Máquina de Estados)

```
                       enqueue_with_job()
                              │
                              ▼
                       ┌─────────────┐
                       │   PENDING   │  aguardando ScrapeJob terminar
                       └──────┬──────┘
                              │ mark_ready() (chamado pelo ScrapeWorker)
                              ▼
                       ┌─────────────┐
                       │    READY    │  job pronto, ainda não enviado
                       └──────┬──────┘
                              │ claim_next() pelo dispatcher
                              ▼
                       ┌─────────────┐
                       │   SENDING   │  tentativa em andamento
                       └──────┬──────┘
                  ┌───────────┴────────────┐
                  ▼                        ▼
           ┌─────────────┐          ┌─────────────┐
           │  DELIVERED  │          │   FAILED    │
           │  (terminal) │          │  (terminal) │
           └─────────────┘          └─────────────┘

    Retry path (SENDING → SENDING):
    SENDING ──5xx/timeout──▶ SENDING (com next_attempt_at)
                                │
                                └─▶ após max_attempts: FAILED
```

### Transições

| De | Para | Gatilho | Onde |
|---|---|---|---|
| _(novo)_ | `PENDING` | `enqueue_with_job()` | `AsyncScrapeService` |
| `PENDING` | `READY` | `mark_ready(job_id)` | `ScrapeWorker._process_one` |
| `READY` | `SENDING` | `claim_next()` | `WebhookWorker` |
| `SENDING` | `DELIVERED` | `mark_delivered()` após 2xx | `WebhookWorker` |
| `SENDING` | `SENDING` | `mark_retry()` após 5xx/timeout | `WebhookWorker` |
| `SENDING` | `FAILED` | `mark_retry()` quando attempts >= max | `WebhookQueueService` |
| `SENDING` | `FAILED` | `mark_failed_terminal()` após 4xx | `WebhookWorker` |
| `SENDING` | `READY` | `recover_stuck_deliveries()` no startup | `WebhookQueueService` |

---

## 7. Detalhamento dos 5 Status

### `PENDING`
- **Significado:** A delivery foi criada junto com o ScrapeJob, mas o scrape ainda está rodando (ou na fila).
- **Campos populados:** `id`, `request_id`, `webhook_url`, `max_attempts`, `created_at`.
- **Visibilidade:** Nenhum dispatcher lê PENDING — apenas o `ScrapeWorker` transita para READY.

### `READY`
- **Significado:** O `ScrapeWorker` terminou o job (sucesso ou falha). Delivery está pronta para ser enviada.
- **Campos populados adicionalmente:** `updated_at` (atualizado).
- **Visibilidade:** O `WebhookDispatcher.claim_next()` pega deliveries em READY primeiro.

### `SENDING`
- **Significado:** O dispatcher reivindicou a delivery e está fazendo o POST (ou está esperando o momento de retentar).
- **Campos populados:** `worker_id`, `last_attempt_at`, `attempts` (incrementado a cada falha), `next_attempt_at` (apenas em retry), `last_response_code`, `last_error`.
- **Visibilidade:** Apenas deliveries em SENDING com `next_attempt_at <= now` são re-claimadas.

### `DELIVERED`
- **Significado:** O cliente retornou 2xx e a entrega foi confirmada.
- **Campos populados:** `delivered_at`, `last_response_code`.
- **Terminal:** Não muda mais.

### `FAILED`
- **Significado:** A entrega falhou definitivamente. Pode ser por 4xx (erro do cliente) ou por max attempts esgotado (5xx persistente, timeout crônico).
- **Campos populados:** `attempts` (atingiu max), `last_error`, `last_response_code`.
- **Terminal:** Não há retry automático. Cliente pode:
  - Consultar status via `GET /api/v1/webhooks/{requestId}`
  - Re-disparar o scraping com novo `requestId`

---

## 8. Schema da Tabela `webhook_deliveries`

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | TEXT (UUID) | PK |
| `scrape_job_id` | TEXT (FK) | Aponta para `scrape_jobs.id` (indexado) |
| `request_id` | TEXT | **Único global** (idempotência). Indexado |
| `webhook_url` | TEXT | URL absoluta que receberá o POST |
| `status` | TEXT (enum) | `PENDING` \| `READY` \| `SENDING` \| `DELIVERED` \| `FAILED` (indexado) |
| `attempts` | INT | Número de tentativas falhas. Incrementado em cada retry |
| `max_attempts` | INT | Default 5 (configurável) |
| `worker_id` | TEXT NULL | ID do dispatcher que está processando (`hookpool-xxxxxxxx#N`) |
| `last_attempt_at` | DATETIME NULL | Última vez que tentamos enviar |
| `next_attempt_at` | DATETIME NULL | Quando a próxima tentativa deve acontecer (indexado) |
| `delivered_at` | DATETIME NULL | Quando o cliente confirmou com 2xx |
| `last_error` | TEXT NULL | Mensagem da última falha (truncada a 1000 chars) |
| `last_response_code` | INT NULL | HTTP status da última tentativa |
| `created_at` | DATETIME | Quando o `POST /scrape/async` foi feito |
| `updated_at` | DATETIME | Última modificação de status |

---

## 9. Política de Retry

### Backoff Exponencial com Jitter

Cada tentativa após a primeira espera `2^(attempts-1)` segundos + jitter aleatório (0–1s), limitado por `WEBHOOK_DELIVERY_MAX_BACKOFF_SECONDS` (60s default).

| Tentativa | Espera base | Faixa real (com jitter) |
|---|---|---|
| 1 (1ª falha) | 1s | 1–2s |
| 2 | 2s | 2–3s |
| 3 | 4s | 4–5s |
| 4 | 8s | 8–9s |
| 5 | 16s | 16–17s → **FAILED** |

> Se a 5ª tentativa (max) falhar, status vira `FAILED` e para.

### Decisão por código HTTP

| Resposta do cliente | Ação |
|---|---|
| `200`, `201`, `202`, `204` (qualquer 2xx) | ✅ `DELIVERED` (fim) |
| `400`, `404`, `410` (4xx) | ❌ `FAILED` terminal (URL/auth inválida — não adianta retentar) |
| `408`, `429`, `500`, `502`, `503`, `504` (5xx e 408/429) | 🔄 Retry com backoff |
| Timeout, connection error, DNS error | 🔄 Retry com backoff |
| 5xx após max attempts | ❌ `FAILED` terminal |

---

## 10. Configuração (Variáveis de Ambiente)

| Variável | Default | Descrição |
|---|---|---|
| `WEBHOOK_DISPATCHER_CONCURRENCY` | `2` | Quantos dispatchers rodam em paralelo |
| `WEBHOOK_DISPATCHER_POLL_INTERVAL` | `2.0` | Segundos de espera entre polls quando fila vazia |
| `WEBHOOK_DELIVERY_TIMEOUT_SECONDS` | `10` | Timeout por tentativa HTTP |
| `WEBHOOK_DELIVERY_MAX_ATTEMPTS` | `5` | Máximo de tentativas antes de `FAILED` |
| `WEBHOOK_DELIVERY_BASE_BACKOFF_SECONDS` | `1.0` | Base do backoff exponencial |
| `WEBHOOK_DELIVERY_MAX_BACKOFF_SECONDS` | `60.0` | Teto do backoff (mesmo com 2^N crescente) |
| `WEBHOOK_STUCK_TIMEOUT_SECONDS` | `60` | Tempo após o qual uma delivery SENDING é considerada órfã (recovery no startup) |

---

## 11. Recuperação de Falhas

| Cenário | O que acontece | Recuperação |
|---|---|---|
| **App reinicia com delivery SENDING** | `recover_stuck_deliveries()` reseta SENDING → READY se `last_attempt_at` > 60s atrás | Automática no startup |
| **Cliente do webhook está offline** | 5xx/timeout → retry com backoff até max | Automática |
| **Cliente retornou 4xx** | Status vai para FAILED imediatamente | Cliente deve consultar `GET /webhooks/{id}` |
| **Job de scrape FAILED** | A delivery é marcada READY mesmo assim (com `status: FAILED` no payload do webhook) | Cliente vê o status FAILED no payload |
| **URL do cliente mudou** | A delivery FAILED com `last_error` descritivo | Cliente dispara novo `POST /scrape/async` |

---

## 12. Detalhamento dos Componentes (Código)

### `src/domain/models.py` — `WebhookDelivery`
Tabela completa. 14 colunas, FK para `scrape_jobs`, `request_id` com unique constraint.

### `src/core/queues/webhook.py` — `WebhookQueueService`
Encapsula operações sobre a fila de entregas:
- `mark_ready(scrape_job_id)` — PENDING → READY (chamado pelo ScrapeWorker)
- `claim_next(worker_id)` — claim atômico do próximo (READY ou SENDING atrasado)
- `mark_delivered()`, `mark_retry()`, `mark_failed_terminal()` — UPDATE statements
- `recover_stuck_deliveries()` — reseta SENDING órfãs para READY
- `get_by_request_id()` — usado pela API de status

### `src/core/workers/webhook.py` — `WebhookWorker`
Pool de N tasks asyncio:
- `start()` / `stop()` — gerencia ciclo de vida (chamado no `lifespan`)
- `_run_loop(worker_index)` — loop individual: claim → POST → mark
- `_post_with_timeout()` — httpx com timeout e classificação de outcome
- `_compute_next_attempt()` — backoff exponencial + jitter
- Recovery de stuck deliveries no `start()`

### `src/api/v1/async_scrape_routes.py` — `POST /api/v1/scrape/async`
- Valida body com `AsyncScrapeRequest`
- Chama `AsyncScrapeService.enqueue_async_scrape()` — 409 se requestId duplicado
- Retorna `AsyncScrapeResponse` com 202

### `src/api/v1/webhook_routes.py` — `GET /api/v1/webhooks/{request_id}`
- Busca por `request_id` via `WebhookQueueService.get_by_request_id` (404 se não existir)
- Retorna `WebhookStatusResponse`

### `src/main.py` — `lifespan`
```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    scrape_worker = get_scrape_worker()
    webhook_worker = get_webhook_worker()
    cleanup_worker = get_cache_cleanup_worker()
    await scrape_worker.start()
    await webhook_worker.start()
    await cleanup_worker.start()
    try:
        yield
    finally:
        await cleanup_worker.stop()
        await webhook_worker.stop()
        await scrape_worker.stop()
```

### `src/core/workers/scrape.py` — Notificação de Conclusão
Ao final de `_process_one`, chama `webhook_queue.mark_ready(job.id)` para disponibilizar a entrega para o `WebhookWorker`. É no-op se o job não tem delivery associada (caso do endpoint síncrono).

---

## 13. Operações & Diagnóstico

### Inspecionar deliveries pendentes/falhadas

```sql
-- Todas as deliveries hoje
SELECT request_id, status, attempts, max_attempts, last_response_code
FROM webhook_deliveries
WHERE created_at >= datetime('now', '-1 day')
ORDER BY created_at DESC;

-- Taxa de sucesso nas últimas 24h
SELECT status, COUNT(*) AS n
FROM webhook_deliveries
WHERE created_at >= datetime('now', '-1 day')
GROUP BY status;

-- Entregas que estão travadas em SENDING há muito tempo
SELECT request_id, worker_id, last_attempt_at, attempts
FROM webhook_deliveries
WHERE status = 'SENDING'
  AND last_attempt_at < datetime('now', '-5 minutes');

-- Top erros
SELECT last_error, COUNT(*) AS n
FROM webhook_deliveries
WHERE status = 'FAILED' AND last_error IS NOT NULL
GROUP BY last_error
ORDER BY n DESC
LIMIT 10;
```

### Reprocessar uma delivery FAILED (manualmente)

Não há endpoint de retry automático. Para reprocessar, dispare um novo `POST /scrape/async` com o mesmo `requestId`? **Não** — isso dá 409. Use um novo `requestId`.

Ou, se quiser reprocessar a mesma URL, atualize o status manualmente:

```sql
UPDATE webhook_deliveries
SET status = 'READY', attempts = 0, next_attempt_at = NULL, last_error = NULL
WHERE request_id = 'cliente-abc-123';
```

O dispatcher pegará no próximo poll.

---

## 14. Segurança

- **Autenticação:** A API (`POST /scrape/async` e `GET /webhooks/{id}`) exige **HTTP Basic Auth** (mesmo do resto do serviço). Configure no Coolify com `BASIC_AUTH_USERNAME` e `BASIC_AUTH_PASSWORD`.
- **Validação de URL:** `webhookUrl` é validado por `HttpUrl` do Pydantic. URLs sem esquema (`http://` ou `https://`) são rejeitadas.
- **SSRF:** ⚠️ **Esta versão não tem proteção contra SSRF**. Um cliente malicioso pode passar `http://localhost:8080/admin` como webhookUrl e fazer o servidor enviar requests para a própria rede interna. **Antes de expor publicamente, implemente allowlist ou blocklist de domínios/IPs privados** (`127.0.0.0/8`, `10.0.0.0/8`, `169.254.0.0/16`, etc.).
- **HTTPS:** Recomenda-se fortemente que clientes usem `https://` no webhookUrl.
- **HMAC (futuro):** Para autenticar que o POST veio realmente do scraper, podemos adicionar um header `X-Signature: sha256=<hmac>` em iteração futura.

---

## 15. Próximos Passos (Roadmap)

- [ ] Retry manual via `POST /api/v1/webhooks/{request_id}/retry`
- [ ] HMAC signature no body do webhook
- [ ] Allowlist/blocklist de domínios para prevenir SSRF
- [ ] Filtros no claim (ex.: prioridade por vendor)
- [ ] Dashboard widget mostrando deliveries recentes
- [ ] Cancelamento: `CANCELLED` status para deliveries antes do scrape terminar
- [ ] Webhook de "delivery_failed" opcional (avisar o cliente que a entrega dele falhou)
