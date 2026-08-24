# Guia Técnico da Fila de Execução de Scraping 🚦

Este documento detalha o funcionamento da **fila persistente de scraping** baseada em SQLite, os estados possíveis de um job, o ciclo de vida ponta a ponta, o modelo de concorrência e os procedimentos de recuperação.

> 📖 **Contexto:** Esta camada foi adicionada *entre* a API e o `ScrapingService` para serializar requisições e eliminar concorrência no parser HTTP / navegador headless. Para entender o scraping em si, consulte o [`SCRAPING_GUIDE.md`](SCRAPING_GUIDE.md).

---

## 1. Por que uma fila?

Sem a fila, cada `POST /api/v1/scrape` invoca o scraper imediatamente. Resultado prático: se três clientes disparam ao mesmo tempo, três sessões de `curl_cffi` (ou pior, três instâncias de Playwright Chromium) sobem em paralelo, competindo por:

- **Anti-bot da OLX/Cloudflare** — três fingerprints TLS saindo do mesmo IP em milissegundos dispara detecção
- **Recursos do host** — cada Playwright consome ~250 MB de RAM; três em paralelo podem derrubar o container
- **Race conditions no banco** — `ScrapingService` faz leitura/escrita no SQLite, e múltiplas transações simultâneas exigem `BEGIN IMMEDIATE` explícito

A fila resolve isso transformando o processamento em **trabalho serializado** (ou limitado a N workers), com **persistência no SQLite** — ou seja, jobs sobrevivem a restart do processo.

---

## 2. Visão Geral da Arquitetura

```
                              ┌─────────────────────────────────────┐
   POST /api/v1/scrape        │        API Layer (FastAPI)         │
   (pydantic ScrapeRequest)   │                                     │
            │                 │   src/api/v1/scrape_routes.py       │
            ▼                 └──────────────┬──────────────────────┘
   ┌────────────────────┐                    │ enqueue()
   │  ScrapeJob (tabela)│◀───────────────────┘
   │   status=QUEUED    │
   └─────────┬──────────┘
             │ (poll + claim atômico)
             ▼
   ┌─────────────────────────────────────────────────────────────┐
   │                  ScrapeWorker Pool (N tasks)                │
   │                                                             │
   │  ┌──────────────┐  ┌──────────────┐        ┌──────────────┐ │
   │  │ worker #0    │  │ worker #1    │  ...   │ worker #N-1  │ │
   │  │ pool-abc#0   │  │ pool-abc#1   │        │ pool-abc#N-1 │ │
   │  └──────┬───────┘  └──────┬───────┘        └──────┬───────┘ │
   │         │                 │                       │         │
   │         └─────────────────┼───────────────────────┘         │
   │                           ▼                                 │
   │              ┌─────────────────────────┐                    │
   │              │   ScrapingService       │  (executa o scrape)│
   │              │   (já existia antes)    │                    │
   │              └────────────┬────────────┘                    │
   └───────────────────────────┼─────────────────────────────────┘
                               ▼
                ┌─────────────────────────────┐
                │  ScrapeJob (status=SUCCESS) │
                │  + response_payload (JSON)  │
                │  + ScrapingExecution +      │
                │    ScrapedListing           │
                └──────────────┬──────────────┘
                               │
                               ▼
                ┌─────────────────────────────┐
                │  POST /scrape desbloqueia   │
                │  e devolve ScrapeResponse   │
                └─────────────────────────────┘
```

---

## 3. Os Dois Modelos: `ScrapeJob` vs `ScrapingExecution`

A fila introduz uma nova tabela sem remover nenhuma existente. **Os dois modelos têm propósitos diferentes** e convivem:

| Aspecto | `ScrapeJob` (novo) | `ScrapingExecution` (existente) |
|---|---|---|
| **Representa** | O *pedido* (intenção + ciclo de vida) | O *resultado* (auditoria do que foi raspado) |
| **Criado em** | `JobQueueService.enqueue()` | `ScrapingService.execute_scrape()` |
| **Status próprios** | `QUEUED`, `RUNNING`, `SUCCESS`, `FAILED` | `PENDING`, `RUNNING`, `SUCCESS`, `FAILED`, `BLOCKED_CAPTCHA`, `PARTIAL` |
| **Vive se o app reiniciar?** | ✅ Sim (tabela durável) | ✅ Sim, mas a `Execution` RUNNING fica órfã (até recovery) |
| **Contém** | `request_payload`, `response_payload`, `worker_id`, `attempts` | `total_found`, `new_items_count`, `duration_ms`, `used_fallback`, FK para `SearchQuery` |
| **Relação** | 1 `ScrapeJob` → 1 `ScrapingExecution` (criada durante o RUNNING) | 1 `ScrapingExecution` → N `ScrapedListing` |

Em resumo: **`ScrapeJob` é o envelope da fila, `ScrapingExecution` é a auditoria do trabalho**.

---

## 4. Ciclo de Vida do Job (Máquina de Estados)

```
                       enqueue(request)
                              │
                              ▼
                       ┌─────────────┐
                       │   QUEUED    │ ◀─────────────────────┐
                       └──────┬──────┘                       │ recover_orphaned_jobs()
                              │ claim_next()                 │ (startup do worker)
                              ▼                              │
                       ┌─────────────┐                       │
                       │   RUNNING   │ ──────────────────────┘
                       └──────┬──────┘
                              │
                  ┌───────────┴────────────┐
                  ▼                        ▼
           ┌─────────────┐          ┌─────────────┐
           │   SUCCESS   │          │   FAILED    │
           │ (terminal)  │          │ (terminal)  │
           └─────────────┘          └─────────────┘
```

### Tabela de Transições

| De | Para | Gatilho | Onde |
|---|---|---|---|
| _(novo)_ | `QUEUED` | `enqueue()` | `JobQueueService.enqueue` |
| `QUEUED` | `RUNNING` | `claim_next()` | `JobQueueService.claim_next` |
| `RUNNING` | `SUCCESS` | `mark_success()` | `JobQueueService.mark_success` |
| `RUNNING` | `FAILED` | `mark_failed()` | `JobQueueService.mark_failed` |
| `RUNNING` | `QUEUED` | `recover_orphaned_jobs()` no startup | `JobQueueService.recover_orphaned_jobs` |

> **Observação:** Não há transição `QUEUED → FAILED` direta. Um job só vira `FAILED` depois de ter sido `RUNNING` (ou seja, alguém tentou executá-lo). Cancelamento manual não é suportado nesta versão.

---

## 5. Detalhamento dos Status

### `QUEUED`
- **Significado:** O job foi aceito pela API, está persistido no SQLite e aguarda um worker livre.
- **Campos populados:** `id`, `vendor`, `request_payload`, `priority`, `created_at`.
- **Quem pode ler:** Qualquer worker fazendo `claim_next()`.
- **Quem move para `RUNNING`:** Exatamente um worker, atomicamente (ver §7).

### `RUNNING`
- **Significado:** Um worker reivindicou o job e está executando o `ScrapingService.execute_scrape(request)`.
- **Campos populados adicionalmente:** `worker_id` (formato `pool-xxxxxxxx#N`), `started_at`, `attempts` (incrementado a cada claim).
- **Visibilidade:** Outros workers não pegam este job porque o `claim_next` filtra por `status=QUEUED`.
- **Risco:** Se o processo do worker crashar (ex.: OOM kill do container), o job fica preso em `RUNNING` até o próximo `recover_orphaned_jobs`.

### `SUCCESS`
- **Significado:** O scrape terminou sem exceções e o `ScrapeResponse` completo foi serializado em `response_payload`.
- **Campos populados adicionalmente:** `finished_at`, `response_payload` (dict com `success`, `items`, `execution`).
- **Estado terminal:** Não muda mais. O `wait_for_completion()` da API retorna assim que detecta este status.

### `FAILED`
- **Significado:** Uma exceção foi capturada durante o `execute_scrape` ou o worker crashou no meio.
- **Campos populados adicionalmente:** `finished_at`, `error_message` (truncado a 1000 chars), `response_payload` contendo um `ScrapeResponse` sintético com `success=False`.
- **Estado terminal:** Idem `SUCCESS`.
- **Retry:** Não há retry automático nesta versão. Para reprocessar, é preciso criar um novo `ScrapeJob` via API.

---

## 6. Schema da Tabela `scrape_jobs`

Definido em `src/domain/models.py`:

| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | TEXT (UUID) | PK |
| `vendor` | TEXT (enum) | Marketplace alvo (`OLX`, `MERCADO_LIVRE`, `ENJOEI`) |
| `request_payload` | JSON | `ScrapeRequest.model_dump(mode="json")` completo |
| `status` | TEXT (enum) | `QUEUED` \| `RUNNING` \| `SUCCESS` \| `FAILED` (indexado) |
| `priority` | INT | Default `0`. Ordem de claim é `priority ASC, created_at ASC` (indexado) |
| `attempts` | INT | Incrementado a cada claim. Indica quantas vezes o job foi pego por um worker |
| `worker_id` | TEXT NULL | Identificador do worker que está processando (`pool-xxxxxxxx#N`) |
| `created_at` | DATETIME | Quando o job foi enfileirado (indexado) |
| `started_at` | DATETIME NULL | Quando um worker pegou o job |
| `finished_at` | DATETIME NULL | Quando o job atingiu estado terminal |
| `response_payload` | JSON NULL | `ScrapeResponse.model_dump(mode="json")` final |
| `error_message` | TEXT NULL | Mensagem de erro (truncada) se `FAILED` |

---

## 7. Modelo de Concorrência

### Claim Atômico (por que funciona no SQLite)

O `claim_next` usa uma única sentença `UPDATE ... WHERE id = (SELECT ...) RETURNING`:

```sql
UPDATE scrape_jobs
SET status = 'RUNNING', worker_id = ?, started_at = ?, attempts = attempts + 1
WHERE id = (
    SELECT id FROM scrape_jobs
    WHERE status = 'QUEUED'
    ORDER BY priority ASC, created_at ASC
    LIMIT 1
)
RETURNING *;
```

O SQLite tem um único writer por vez. Combinado com o lock implícito do `UPDATE`, isso garante que dois workers concorrentes nunca pegam o mesmo job. **Não é necessário `SELECT FOR UPDATE SKIP LOCKED`** (que o SQLite nem suporta).

### Pool de Workers

- `ScrapeWorker(concurrency=N)` lança **N tasks asyncio** no mesmo event loop do FastAPI.
- Cada task é um loop `claim → process → claim` independente.
- O `worker_id` segue o padrão `<pool_id>#<index>`, permitindo rastrear qual worker processou cada job (visível no campo `worker_id` da tabela).
- `SCRAPE_WORKER_CONCURRENCY` (env) controla o N. Default = `1`.

### Ordenação

Jobs são reivindicados na ordem `priority ASC, created_at ASC`:
- **Priority menor = processado primeiro** (use `0` para scraping prioritário).
- Empate em priority desempata por FIFO (`created_at`).

### Race com a sessão da API

A API enfileira usando a sessão do request (`Depends(get_session)`), mas o `wait_for_completion` faz poll usando **sessões novas a cada iteração** (`async_session_maker()`). Isso evita o cache do *identity map* do SQLAlchemy — sem isso, a API veria sempre o `status=QUEUED` antigo e nunca detectaria a conclusão.

---

## 8. Comportamento da API

### `POST /api/v1/scrape` (síncrono, espera na fila)

```
caller ──HTTP POST──▶ FastAPI
                          │
                          ├─ enqueue() ──────▶ ScrapeJob (QUEUED)
                          │
                          ├─ wait_for_completion() ──▶ poll 500ms
                          │                              │
                          │   (worker pega, processa, marca SUCCESS/FAILED)
                          │                              │
                          ◀──────────────────────────────┘
                          │
                          ├─ ScrapeResponse.model_validate(response_payload)
                          │
                          ◀──HTTP 200 + JSON── caller
```

- **Timeout:** Configurável via `SCRAPE_JOB_TIMEOUT_SECONDS` (default 300s). Estourou → `TimeoutError` propaga para o handler, devolvendo `500`.
- **Cliente HTTP:** Não muda nada. Continua sendo `POST` + esperar o body.
- **Fila cheia / travada:** A request fica bloqueada até o timeout. Não há 503 de backpressure nesta versão.

---

## 9. Recuperação de Falhas

| Cenário | O que acontece | Recuperação |
|---|---|---|
| **App reinicia com job em `RUNNING`** | O `recover_orphaned_jobs()` no `start()` do pool reseta todos os `RUNNING` → `QUEUED`. | Automática no startup |
| **Worker crasha (OOM, signal) durante scrape** | O job fica `RUNNING` até o próximo restart do app. | Reset no startup |
| **DB file corrompido / inacessível** | `engine.begin()` levanta `OperationalError`. O app falha no startup. | Intervenção manual (backup + restore) |
| **`ScrapeService.execute_scrape` lança exceção** | Worker captura, monta `ScrapeResponse(success=False, ...)` sintético, chama `mark_failed`. | O job fica em `FAILED`. Não há retry automático. |
| **API caller desiste (timeout TCP)** | O job continua sendo processado em background. Quando terminar, fica em `SUCCESS` ou `FAILED` orfão. | Inspeção via `GET /executions` ou query direta em `scrape_jobs` |

---

## 10. Configuração (Variáveis de Ambiente)

| Variável | Default | Descrição |
|---|---|---|
| `SCRAPE_WORKER_CONCURRENCY` | `1` | Quantos workers rodam em paralelo. Aumentar com cautela: cada worker com Playwright ativo consome ~250 MB. |
| `SCRAPE_WORKER_POLL_INTERVAL` | `1.0` | Segundos que cada worker dorme entre polls quando a fila está vazia. |
| `SCRAPE_JOB_TIMEOUT_SECONDS` | `300` | Timeout do `POST /api/v1/scrape` esperando o job concluir. |

> **Heurística para `SCRAPE_WORKER_CONCURRENCY`:** se você pretende usar o fallback Playwright com frequência, mantenha `1` ou `2`. Para scraping só via HTTP (curl_cffi sem fallback), 3–5 costuma ser seguro no Coolify single-container.

---

## 11. Detalhamento dos Componentes (Código)

### `src/core/job_queue.py` — `JobQueueService`
Encapsula todas as operações sobre `scrape_jobs`:
- `enqueue(request, priority=0)` → cria `ScrapeJob` QUEUED
- `claim_next(worker_id)` → claim atômico do mais antigo
- `mark_success(job, response_payload)` / `mark_failed(job, response_payload, error_message)`
- `wait_for_completion(job_id, timeout, poll_interval)` → poll com sessão isolada
- `recover_orphaned_jobs()` → reseta RUNNING → QUEUED
- `get_stats()` → contadores por status (últimos 500 jobs)

### `src/core/worker.py` — `ScrapeWorker` (na verdade um pool)
- `__init__(concurrency=None, poll_interval=None)` → lê de `settings` se não fornecido
- `start()` → roda `recover_orphaned_jobs()` uma vez + lança N tasks asyncio
- `stop()` → sinaliza `_stop_event`, aguarda com timeout de 10s, cancela se necessário
- `_run_loop(worker_index)` → loop individual de cada worker
- `_process_one(worker_id)` → claim + execute + mark em uma sessão SQLite

### `src/main.py` — `lifespan`
```python
@asynccontextmanager
async def lifespan(app: FastAPI):
    await init_db()
    worker = get_worker()
    await worker.start()
    try:
        yield
    finally:
        await worker.stop()
```

### `src/api/v1/scrape_routes.py` — `POST /api/v1/scrape`
- Enfileira via `JobQueueService.enqueue`
- Aguarda via `JobQueueService.wait_for_completion`
- Retorna `ScrapeResponse` parseado de `response_payload`

---

## 12. Operações & Diagnóstico

### Ver status da fila via SQL

```sql
-- Contagem por status
SELECT status, COUNT(*) FROM scrape_jobs GROUP BY status;

-- Jobs QUEUED há mais tempo (possível travamento)
SELECT id, vendor, created_at, priority
FROM scrape_jobs
WHERE status = 'QUEUED'
ORDER BY created_at ASC
LIMIT 10;

-- Jobs que estão RUNNING há muito tempo (possível órfão)
SELECT id, worker_id, started_at, attempts
FROM scrape_jobs
WHERE status = 'RUNNING'
ORDER BY started_at ASC;

-- Histórico recente de falhas com mensagem
SELECT id, vendor, error_message, finished_at
FROM scrape_jobs
WHERE status = 'FAILED'
ORDER BY finished_at DESC
LIMIT 20;
```

### Reprocessar um job (manualmente)

```sql
-- Criar um novo job a partir de um payload antigo
INSERT INTO scrape_jobs (id, vendor, request_payload, status, priority, attempts, created_at)
SELECT
    lower(hex(randomblob(16))) AS id,
    vendor,
    request_payload,
    'QUEUED',
    priority,
    0,
    CURRENT_TIMESTAMP
FROM scrape_jobs
WHERE id = :job_id_original;
```

> O endpoint de retry automático (`POST /scrape/{job_id}/retry`) **não existe** nesta versão. Reprocesso é manual via SQL ou re-disparo da request original.

### Forçar reset de jobs RUNNING órfãos (sem reiniciar o app)

```sql
UPDATE scrape_jobs
SET status = 'QUEUED', started_at = NULL, worker_id = NULL
WHERE status = 'RUNNING';
```

Útil se o container foi reiniciado de forma "quente" (sem matar o PID 1) e o `recover_orphaned_jobs()` não rodou.

---

## 13. Próximos Passos (Roadmap)

- [ ] Retry automático com backoff exponencial em `FAILED`
- [ ] `GET /api/v1/jobs` e `GET /api/v1/jobs/{id}` para inspeção via dashboard
- [ ] Cancelamento (`QUEUED → CANCELLED`) via API
- [ ] Filtros no `claim_next` (ex.: worker só pega jobs do seu vendor)
- [ ] Migração para ARQ/Taskiq quando sair do single-container
