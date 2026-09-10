# Auditoria Profunda — projeto-scrap

> **Data:** 2026-09-10 · **Escopo:** monorepo completo (bi-engine, scraper, apps/web, infra)
> **Como usar este documento:** cada item tem *mecanismo* (por que quebra), *cenário em escala* (com números do seu código), *blast radius*, *design do fix*, *como validar* e *esforço*. Os file:line foram verificados em primeira mão.
> Este documento assume que você vai seguir os fixes à risca — por isso os designs são concretos, não genéricos.

---

## 0. Veredito de qualidade — a resposta honesta

### Java / bi-engine (você na mão): **arquitetura de verdade, bugs nos caminhos infelizes**

As coisas que estão certas são as **difíceis** — as que projetista júnior e LLM erram:

1. **Rotação de refresh token com lock pessimista** (`RefreshTokenRepository.java:15-17` — `PESSIMISTIC_WRITE` + `JOIN FETCH`) e `noRollbackFor` no consumo (`RefreshTokenService.java:44-53`). Isso é padrão de produção que a maioria dos tutoriais erra.
2. **O comentário do `REQUIRES_NEW` no dispatcher** (`ScrapingJobDispatcher.java:53-62`): você bateu num pitfall real do Spring (transação `readOnly` engolindo INSERTs), **resolveu e documentou**. Quem entende esse pitfall entende transação de verdade.
3. **`@TransactionalEventListener(AFTER_COMMIT)`** para desacoplar criação → dispatch. Correto.
4. Ownership via `MeService.getMe()`, `readOnly = true` nas leituras, `ddl-auto: validate`, migrations disciplinadas, `ObjectProvider` para IA opcional — tudo conforme o AGENTS.md.

Os bugs do Java estão concentrados em três categorias: **semântica entre serviços** (status FAILED→SUCCESS), **caminhos de erro** (catch engolido, estados órfãos) e **higiene de config** (secrets como default). Esse é o perfil clássico de "bom engenheiro sem teste de integração e sem cicatriz de produção". O remédio não é reescrever — é 1 teste de integração end-to-end do fluxo webhook + disciplina nos caminhos de erro.

### Python / scraper (IA escreveu, você estruturou): **formas certas, semântica furada**

O padrão é inconfundível: cada arquivo é localmente plausível, mas o sistema tem incoerência global. A prova está no `scrape.py`:

- O `except` do worker (`scrape.py:99-114`) é **bem escrito** — monta `ScrapeResponse` de falha, trunca `error_message`, chama `mark_failed` corretamente.
- E ele é **invisível**: `execute_scrape` (`scraping_service.py:79-112`) captura todas as exceções e devolve `success=False`, então `mark_success` na linha 98 roda **incondicionalmente**. O caminho de erro perfeito nunca executa.

Outros sintomas do mesmo padrão: `MAX_RETRIES` declarado (`config.py:27`) e lido em lugar nenhum; `DUPLICATE`/`PROCESSING` existem no enum Java e nunca são gravados; challenge do Cloudflare vira "SUCCESS com 0 itens". Isso **não é código ruim** — é código **não verificado**. Os testes passam porque validam a implementação, não o contrato. O seu mérito estrutural é real (docs que admitem as lacunas, CI de verdade, claim atômico da fila) — os 3 bugs críticos do Python têm fix de <20 linhas cada.

### Frontend: **instinto de stack acima da média, zero sistema imunológico**

Angular 22 + signals + control flow novo + interceptores funcionais + env-in-runtime são decisões de quem acompanha a plataforma. Mas `strict: false` deliberado (`tsconfig.json`), sem ESLint, e o único teste do repo quebrado = nenhum guard-rail. BUG-1 (loop de refresh em 403) é bug de textbook que um único teste de interceptor pegaria.

### Síntese em uma frase

> Seu desenho é de engenheiro. Seus bugs são de falta de produção. A diferença entre os dois chama-se **sistema imunológico** — testes de contrato, strict, lint, CI com semáforo verdadeiro — e é a única coisa que falta nos três serviços. O único serviço com sistema imunológico parcial é o scraper, e os testes dele validam a implementação em vez do contrato.

---

# PARTE I — ESCALA E TRANSAÇÕES (os pontos que você não tinha percebido)

## 1. A transação que segura a IA — anatomia completa do travamento

**Onde:** `WebhookScrapperService.java:47-48` (`@Async("webhookProcessorExecutor") @Transactional`), `AsyncConfig.java:27-37`, `application.yml:9-11`

### O mecanismo

`@Async` + `@Transactional` no mesmo método (`processScrappingEvent`) significa: o interceptor async submete o método para o pool de threads; **na thread do pool**, o interceptor transacional abre uma transação na entrada e comita na saída. Logo:

```
TRANSAÇÃO = vida útil do método inteiro
           = findByRequestId + analyzePayload
           = chamada Gemini COM TOOLS
           = cada tool → POST /scrape/detail no Python → Playwright carregando página da OLX (5-30s por tool)
```

Uma conexão PostgreSQL do Hikari fica **presa pela duração da análise de IA completa** — dezenas de segundos a minutos por webhook.

### A matemática (com os números do seu código)

| Recurso | Valor | Fonte |
|---|---|---|
| Hikari pool | **10 conexões** | `application.yml:10` |
| `webhookProcessorExecutor` | core **20** / max 100 / queue 2000 | `AsyncConfig.java:31-33` |
| Hikari `connectionTimeout` (default) | 30s → depois `SQLTransientConnectionException` | default Spring Boot |
| `scraperDispatcherExecutor` | core **5** / max 20 / queue 1000 | `AsyncConfig.java:19-21` |
| Concorrência do Python | **1 worker** | `.env` do scraper |

**Throughput sustentado de webhooks** = `pool / T_hold`. Com T_hold = 60s (uma análise modesta: 10 itens novos, 1-2 tool calls cada), a capacidade é **10 webhooks/minuto**. Cada keyword `EVERY_MINUTE` gera 1 job → 1 webhook por minuto. Ou seja:

> **O sistema satura com ~10 keywords ativas em frequência de 1 minuto — e a partir daí a API INTEIRA (login, listagens, dashboard) fica competindo por 0 conexões livres.**

### Timeline de saturação (burst real)

```
T+0s    Scheduler varre 6 monitores × 2 keywords → 12 jobs → Python (concurrency=1)
        processa em série → webhooks voltam em rajada minutos depois
T+0s    12 threads WebhookProc iniciam: 10 pegam conexões, 2 esperam (timeout 30s)
T+0s    Cada thread: findByRequestId (ms) → analyzePayload → 20 itens novos
T+2s    Gemini + tools → cada tool = POST no Python → Playwright (5-30s)
T+30s   As 2 threads sem conexão: SQLTransientConnectionException
        → cai no catch:97 → "erro inesperado" no log → NADA é gravado
T+60s   Usuário tenta logar → login precisa de conexão → espera 30s → 500
T+300s  As 10 webhooks commitam → API respira por segundos → próximo burst
```

Detalhe cruel: o executor tem queue de **2000** — os webhooks excedentes não falham, **enfileiram em memória**. A degradação é silenciosa até a heap.

### Design do fix — máquina de estados + 3 transações curtas

**Regra de ouro: nenhum IO externo (HTTP, LLM, Playwright) dentro de transação.**

O padrão correto **já existe no seu código** — o `ScrapingJobDispatcher` usa `TransactionTemplate` programático com `REQUIRES_NEW` (`ScrapingJobDispatcher.java:61-62`). Aplique o mesmo no fluxo webhook:

```
WebhookEvent:     RECEIVED → PROCESSING → PROCESCED | FAILED | DUPLICATE
ScrapingExecution: PENDING → PROCESSING → SUCCESS | FAILED (+ errorMessage)
```

```
TX1 — CLAIM (<100ms, transação curta):
  SELECT webhook_event FOR UPDATE por requestId
  se status != RECEIVED  → marca DUPLICATE e retorna   ← idempotência mora aqui (item 3)
  status → PROCESSING; execution → PROCESSING; commit
  (aqui também: 1 query IN batch para separar itens novos de existentes — mata o N+1 do item 10)

SEM TX — ANÁLISE (segundos/minutos, zero conexões presas):
  chamar Gemini + tools livremente; montar resultados em memória
  (nenhuma escrita em banco aqui — nem save, nem valueToTree persistido)

TX2 — PERSISTÊNCIA (<1s):
  saveAll(listings) em batch, salvar AiAnalysisLog, event → PROCESSED,
  execution → SUCCESS com contagens; commit

TX3 — FALHA (catch FORA de qualquer transação do fluxo feliz):
  nova transação curta: event → FAILED + errorMessage, execution → FAILED;
  emitir métrica/log de erro
```

Se a IA estiver lenta no item 3 do fix, o pior caso é N threads sem conexão nenhuma — que é exatamente o comportamento correto.

**Efeito colateral positivo:** com a análise fora da TX, o `ObjectMapper.valueToTree` do `rawPayload` e a checagem item-a-item (`WebhookScrapperService.java:141-143`) saem da transação de graça.

### Como validar

1. WireMock/stub do Gemini com delay de 30s por resposta.
2. Disparar 20 POSTs concorrentes em `/api/v1/webhooks/scraper`.
3. **Critérios de aceitação:** login manual durante o teste responde < 200ms; zero `SQLTransientConnectionException`; zero execution PENDING após o teste; segunda entrega do mesmo requestId → DUPLICATE sem chamar IA (verificar contagem de chamadas no stub).

### Esforço: **M (2-3 dias)** — reestrutura o fluxo central; os testes de `WebhookScrapperService` precisam ser reescritos junto.

---

## 2. O catch que engole o rollback — commit parcial

**Onde:** `WebhookScrapperService.java:97-102` (catch dentro do método `@Transactional`)

### O mecanismo

O Spring só faz **rollback quando a exceção cruza a fronteira do proxy transacional** (o método anotado). Se você captura a exceção *dentro* do método, o interceptor vê um retorno normal e **COMITA** tudo que já foi feito:

```
linha 77:  webhookEvent.save()  → PROCESSED já gravado      (commitado depois)
linhas 141-160: itens atualizados parcialmente             (commitado depois)
linha 165: factory.analizeScrappedItens(...) LANÇA (ex: Gemini 429/timeout)
linha 202: saveAll NUNCA roda; execution.setStatus(SUCCESS) NUNCA roda
linha 97:  catch(Exception) → log.error → método retorna "normal"
           → INTERCEPTOR COMITA → event PROCESSED + execution PENDING eterno
```

Gatilhos reais desse cenário: `price` nulo violando `NOT NULL` (o payload Python não é validado — `WebhookIncomingPayload` tem zero anotações de validação), Gemini fora do ar, constraint `uq_monitor_vendor_listing` na race do item 3.

### O fix

1. **Remover o try/catch do método transacional.** Com `@Async void`, exceção não propagada não derruba nada — o TX proxy faz rollback e o `AsyncUncaughtExceptionHandler` do Spring só loga. Rollback acontece, que é o que importa.
2. **Adicionar a compensação explícita (TX3 do item 1):** capturar no ponto certo (depois do claim, fora das transações curtas) e marcar `event → FAILED`, `execution → FAILED` com `errorMessage` — em transação nova.
3. **Saneamento do payload inbound:** `@Valid` no controller + validação no DTO (`price` com default defensivo ou rejeição do item, `vendorListingId` `@NotBlank`). Um item ruim não pode derrubar o lote inteiro — rejeitar o item, não a entrega.

### Como validar

Teste de integração: payload com 2 itens válidos + 1 com `price: null` → resultado esperado: event FAILED com errorMessage preenchido, execution FAILED, **zero** listing persistido (rollback limpo), nenhum estado PENDING/PROCESSED incoerente.

### Esforço: **P (meio dia)** — mas faça JUNTO com o item 1 (o redesign absorve este fix).

---

## 3. Idempotência — a race do at-least-once

**Onde:** `WebhookScrapperService.java:67-79` (claim sem lock, sem guard de status)

### O mecanismo

O webhook worker Python entrega com **retry + backoff** (at-least-once) — duplicata é questão de tempo, não de "se". A checagem atual (`findByRequestId` sem lock, sem olhar status) tem race clássica de TOCTOU:

```
T+0.000s  delivery #1 chega → thread A: findByRequestId → RECEIVED → prossegue
T+0.050s  delivery #2 (retry) chega → thread B: findByRequestId → RECEIVED → prossegue
T+1s      AMBAS chamam Gemini (custo 2x)
T+2s      AMBAS fazem saveAll → a segunda estoura uq_monitor_vendor_listing
          → cai no catch do item 2 → commit parcial → estado inconsistente
```

Detalhe que dói: os enums `WebhookStatus.PROCESSING` e `DUPLICATE` **já existem no schema e nunca são gravados** — o desenho previu idempotência, a implementação não executou.

### O fix

Já embutido no TX1 do item 1:

```java
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("select w from WebhookEvent w where w.requestId = :requestId")
Optional<WebhookEvent> findByRequestIdForUpdate(@Param("requestId") String requestId);

// no claim:
if (event.getStatus() != WebhookStatus.RECEIVED) {
    event.setStatus(WebhookStatus.DUPLICATE);
    return duplicate();   // IA não roda, nada é persistido
}
```

O lock serializa os dois deliveries; o perdedor vê `PROCESSING` e sai como `DUPLICATE`. Como o lock mora na mesma TX curta do claim, não há contenção.

### Como validar

Teste concorrente (2 threads processando o mesmo requestId): exatamente 1 chamada de IA (verificar via mock), 1 `PROCESSED` + 1 `DUPLICATE`, zero violações de constraint.

### Esforço: **P (meio dia)** dentro do redesign do item 1.

---

## 4. HTTP sem read timeout — o scraper pendurado que mata o dispatcher em silêncio

**Onde:** `ScraperHttpClient.java:32-35` (só `connectTimeout(10s)`, **zero read timeout**), `ScrapingJobDispatcher.java:65-77` (self-invocation), `ScrapingScheduler.java:41-42` (`lastScrapedAt` antes do dispatch)

### O mecanismo — três defeitos encadeados

**(a) Sem read timeout:** um Uvicorn que aceita a conexão TCP e não responde (GIL preso, Playwright hang, GC longo) deixa a thread Java **eternamente bloqueada** no read. `connectTimeout` não protege contra isso — ele só cobre o handshake TCP.

**(b) Self-invocation na criação de monitor:** `onMonitorCreated` (`ScrapingJobDispatcher.java:67`) chama `this.dispatchQuery(...)` na linha 72-76 — chamada pela própria instância **contorna o proxy** → o `@Async` da linha 79 não se aplica → **as keywords são despachadas em série, na mesma thread**. Um monitor com 5 keywords = 5 HTTP round-trips em série; se o Python estiver pendurado no defeito (a), a thread trava **para sempre**. (No fluxo do scheduler está correto — lá a chamada sai de outra bean, o proxy aplica e cada keyword ganha thread própria.)

**(c) A aritmética do silêncio:** `scraperDispatcherExecutor` tem core **5** / queue **1000**. Cinco dispatches pendurados = **cinco threads mortas** = todo o resto enfileira. E o `ThreadPoolTaskExecutor` do Spring só cria threads acima do core quando a queue está CHEIA — ou seja, com queue de 1000, você nunca passa de 5 threads. Novos monitores criados, `MonitorCreatedEvent` publicado, evento enfileirado, **nada acontece, nenhum erro, nenhum log**. A telemetria do item 5 ainda reporta "fila vazia".

**(d) Bônus — a janela queimada:** `ScrapingScheduler.java:41-42` grava `lastScrapedAt` **antes** do dispatch. Job que falha = ciclo cron perdido em silêncio (e o `isDue` da linha 66 usa `lastScrapedAt` como referência — a próxima execução já "andou"). Como o webhook processing grava `lastScrapedAt` de novo (`WebhookScrapperService.java:213`), o set no scheduler só serve para piorar o cenário de falha.

### O fix

1. **Read timeout no HttpClient:**
```java
HttpClient.newBuilder()
    .version(HttpClient.Version.HTTP_1_1)
    .connectTimeout(Duration.ofSeconds(10))
    .build();
// e no JdkClientHttpRequestFactory (Spring 6.1+):
factory.setReadTimeout(Duration.ofSeconds(30));
```
2. **Retry com backoff + jitter** para 5xx/timeout no `dispatchAsyncScrape` (2 tentativas, 500ms/2s). Nada de resilience4j num primeiro momento — retry manual cobre 90% do valor com 10% da dependência. Circuit breaker fica para quando houver indicador real de necessidade.
3. **Resolver o self-invocation:** mover o loop `for (query : ...) dispatchQuery(...)` do `onMonitorCreated` para outra bean (ou injetar o próprio proxy via `ObjectProvider<ScrapingJobDispatcher>`), garantindo que cada keyword ganhe thread própria — igual o scheduler já faz.
4. **Mover o `setLastScrapedAt`** para depois do dispatch bem-sucedido — ou remover do scheduler e confiar apenas no momento do processamento do webhook.
5. **Falha visível:** quando o dispatch falha após retries, marcar `ScrapingExecution → FAILED` na hora (a execution já foi criada — não deixar PENDING; ver item 6).

### Como validar

Docker: pausar o container do Python (`docker pause scraper`) → disparar criação de monitor com 3 keywords → **critérios:** cada dispatch falha em ≤ 30s (timeout), 3 executions FAILED com errorMessage, criação de monitor não trava a thread de evento, fila do dispatcher não cresce indefinidamente.

### Esforço: **M (1 dia)**

---

# PARTE II — VERDADE OPERACIONAL

## 5. Telemetria que mente — nos dois lados do contrato

**Onde:** `scrape.py:97-98`, `scraping_service.py:79-112`, `WebhookScrapperService.java:70` vs `:207`, `ScraperHttpClient.java:79-82`, `dashboard-home.component.ts:211-226`

### O inventário da mentira

| Onde | O que mente | Consequência prática |
|---|---|---|
| Python worker | `execute_scrape` engole TUDO → `mark_success` **incondicional** (`scrape.py:98`) | OLX fora do ar = `ScrapeJob.status=SUCCESS`; `total_success_jobs` conta falhas como sucesso |
| Python fallback | Cloudflare challenge não limpa após 12 tentativas → retorna HTML do desafio como página (`browser_fallback.py:75-90`) → parse vazio → `success=True, total_found=0` | Bloqueio de bot indistinguível de "busca sem resultado" |
| Java webhook | Payload `status=FAILED` → event `FAILED` (linha 70) mas o fluxo **continua** e linha 207 seta execution `SUCCESS` | A telemetria do pipeline esconde toda falha do Python |
| Java client | Scraper caído → `getQueueStatus` catch → retorna `(0,0,0,0,0,0)` (`ScraperHttpClient.java:79-82`) | Dashboard mostra "fila vazia" quando deveria gritar "scraper DOWN" |
| Frontend | `dashboard-home.component.ts:211-226`: quatro `error: () => {}` e cards inicializados em 0 | Falha = "0" exibido como métrica real — dado falso na tela |

### O princípio

Cada componente reporta sucesso porque cada um só conhece seu escopo local — **ninguém é dono da verdade do status no contrato**. Isso é "teatro de sucesso": seu monitoramento existe, mas todas as suas métricas são ficção. Você não pode confiably nem saber se o sistema funcionou ontem.

### O fix — uma regra única de propagação

Defina a regra e implemente nos dois lados:

```
status Python SUCCESS + itens  → ExecutionStatus.SUCCESS
status Python FAILED           → ExecutionStatus.FAILED (+ errorMessage do summary)
status Python SUCCESS + 0 itens → SUCCESS com totalFound=0 (e flag used_fallback se caiu no Playwright)
challenge não limpo            → lança exceção no Python (não é "0 resultados")
```

1. **Python:** `execute_scrape` **lança** exceções tipadas (`ScrapeError`) em vez de engolir → o `except` do worker (que já é bom, `scrape.py:99-114`) passa a executar → `mark_failed` correto. No `browser_fallback`, levantar `ScrapeBlockedError` se o desafio não limpar em N tentativas; no provider, distinguir `blocked` de `empty` no summary (`used_fallback` + `error_message`).
2. **Java:** no claim (TX1 do item 1), se `status != SUCCESS` → marcar execution `FAILED` com `error_message` do summary e **retornar sem chamar IA**.
3. **`getQueueStatus`:** propagar a indisponibilidade — lançar exceção → controller responde **503** com corpo `{scraperReachable: false}` → frontend mostra banner "Scraper indisponível".
4. **Frontend:** no dashboard, `error` handler seta estado de erro por card → UI mostra "—" e ícone de falha; nunca 0 como se fosse verdade.

### Como validar

Teste de contrato: payload com `status=FAILED` → execution FAILED, IA não invocada (mock verify). Python: OLX simulada retornando 500 → job FAILED com error_message preenchido. Derrubar o Python → endpoint de queue status responde 503.

### Esforço: **M (1-2 dias)** nos dois serviços somados.

---

## 6. Estados órfãos — o reaper que não existe

**Onde:** `ScrapingExecution.status=PENDING` (sem job que o recupere — `findByStatus` nunca usado), `ScrapingJobDispatcher.java:100-107` (cria execution PENDING no dispatch), Python `queues/webhook.py:243-273` (recovery só cobre `SENDING`)

### O mecanismo

Todo ponto de falha dos itens 1-4 produz estados que **nunca mais ninguém toca**:

- Execution `PENDING` para sempre: dispatch HTTP falhou após criar a execution (o catch do dispatcher — `ScrapingJobDispatcher.java:148-150` — só loga, não marca FAILED); OU webhook nunca chegou (Python morreu entre scrape e delivery).
- `WebhookEvent` `RECEIVED` eterno (stub criado no dispatch, webhook nunca chegou).
- Python: delivery `PENDING` órfã — `mark_ready` (`scrape.py:118`) roda fora do try e após o commit do job; se o processo morrer nesse intervalo, o job é terminal mas a delivery fica `PENDING`, e `recover_stuck_deliveries` só recupera `SENDING`.

O sistema não tem **nenhum mecanismo de varredura** — verificado: nada consulta executions por status.

### O fix

**Java — reaper agendado (a rede de segurança do pipeline inteiro):**

```java
@Scheduled(fixedDelay = 300_000)  // a cada 5 min
@Transactional
public void reapOrphanedExecutions() {
    // executions PENDING/PROCESSING há mais de 30 min → FAILED
    // errorMessage = "Timeout aguardando webhook do scraper"
    // event correspondente RECEIVED há > 30 min → FAILED
    // métrica incrementada (quantas executions estão sendo reapeadas —
    //   esse número É o seu indicador de saúde do pipeline)
}
```

**Python — estender o recovery:** delivery `PENDING` cujo job já está em status terminal → marcar pronta/failed no startup; mover `mark_ready` para dentro do mesmo bloco de commit do `mark_success`/`mark_failed`.

O reaper também é o que torna os itens 4 e 5 **degraváveis em produção**: você não precisa acertar 100% dos caminhos de erro — precisa garantir que nenhum erro vire estado eterno.

### Como validar

Criar execution PENDING manualmente (fixture), rodar o reaper → FAILED com errorMessage. Derrubar o Python no meio de um job (kill -9 no container) → restart → delivery reenfileirada ou failed, nunca PENDING eterna.

### Esforço: **P (meio dia)** Java + **P (meio dia)** Python.

---

# PARTE III — SEGURANÇA

## 7. Segredos em código — plano de remediação completo

**Onde:** `application.yml:27` (chave Gemini REAL), `application.yml:33` (JWT default), `application.yml:40-41` + `docker-compose.yml:39-42` (`admin/admin`), `V1__create_users_and_roles.sql:48-63` (seed `admin123`), `docker-compose.yml:12` (`postgrespassword`), `.dockerignore` raiz nunca aplicado (contexts são `services/*/` e `apps/web/`)

### A gravidade, com precisão

`application.yml:27` — o fallback do placeholder **é a chave real**: `${GEMINI_API_KEY:AIzaSyBnjz8fLgcPeOOexLPVIfXU8w5HiQaqB7Q}`. Está no HEAD e em **todo o histórico desde `73c004f`** (27/08). Repo privado reduz o blast radius, não elimina: clones, CI de terceiros, colaborador futuro, um "torna público" descuidado — e chave Gemini é **fatura direto na sua conta**. O JWT default é a mesma string base64 em 3 arquivos — quem lê o repo forja token de admin se a env não for injetada.

### Plano de remediação (nesta ordem)

1. **HOJE — Rotacionar a chave Gemini** (AI Studio → revogar a `AIza...`, gerar nova). A chave atual deve ser considerada comprometida independentemente de você remover do repo. Nova chave **só via env** — nunca em YAML.
2. **Fail-fast no startup** (o padrão anti-default): criar validador que **recusa subir** se `JWT_SECRET`, `GEMINI_API_KEY`, `SCRAPER_PASSWORD` não vierem de env (ou se vierem com os valores default conhecidos). Ex: `@ConfigurationProperties` + `@Validated` com `@NotBlank`, ou check no `ApplicationRunner` que lança. O mesmo no Python: `config.py` sem default para `BASIC_AUTH` quando `APP_ENV=production`.
3. **Histórico:** `git filter-repo` (ou BFG) para purgar `services/bi-engine/src/main/resources/application.yml` de todas as revisões → force-push → todos os clones/CI caches ficam inválidos (era o objetivo). Fazer num momento sem PRs abertos (repo tem 2 autores — combinar).
4. **`.dockerignore` por context** (o da raiz **não é aplicado** — Docker lê o do diretório do context): criar `services/bi-engine/.dockerignore` (`.env`, `target/`), `services/scraper/.dockerignore` (`.venv/`, `*.db`, `data/`, `tests/`, `.pytest_cache/`), `apps/web/project-ui/.dockerignore` (`node_modules/`, `dist/`). Hoje o build do bi-engine está enviando o **`.env` com a chave real** ao daemon.
5. **Seed admin:** não é possível editar V1 (já aplicada) → **V5**: deletar o admin seed se ainda tiver o hash público do repo (`DELETE FROM users WHERE username='admin' AND password = '$2a$10$...'`) — se o hash mudou, alguém já rotacionou e a linha fica. Remover as credenciais do README.
6. **Compose:** senha Postgres via `${POSTGRES_PASSWORD:?POSTGRES_PASSWORD obrigatório}` (sem default), portas com bind `127.0.0.1:5432:5432`.
7. **Prevenir recorrência:** gitleaks no pre-commit + GitHub Actions (teria pego o item 1 automaticamente); habilitar push protection do GitHub.

### Como validar

`docker build` do bi-engine sem `.env` no context (verificar com `docker build --no-cache` + inspecionar contexto); app sobe **sem** `JWT_SECRET` → falha com mensagem clara; `gitleaks detect` limpo.

### Esforço: **M (1 dia inteiro)** — rotação+fail-fast (2h), filter-repo+cache clean (2-3h), dockerignores (30min), V5+README (1h), gitleaks (1h).

---

## 8. O contrato webhook aberto — cadeia de ataque e design HMAC

**Onde:** `SecurityConfig.java:36` (`/api/v1/webhooks/**` → `permitAll`), `WebhookController.java` (sem `@Valid`, sem assinatura), `WebhookController.java:44-60` (`GET /events` público), Python `schemas.py:77-81` (`webhookUrl` qualquer `HttpUrl`), `schemas.py:142` (`ScrapeDetailRequest.url` `str` solto), `image_cache_service.py:98` (persiste bytes de qualquer URL)

### A cadeia completa de ataque (6 passos)

```
1. Atacante descobre a URL pública do bi-engine (o CORS do application.yml:37
   já revela que existe https://bi.bielsolosos.dev.br)
2. POST /api/v1/webhooks/scraper com payload forjado (requestId próprio, status SUCCESS,
   itens com vendorListingId/urls escolhidos) → permitAll → 200 OK
3. Itens falsos entram no monitor DE QUALQUER USUÁRIO → data poisoning do BI
4. Itens "novos" disparam análise → Gemini com tools → custo da SUA chave Gemini
5. A tool ScrappingDetailsTools faz POST /scrape/detail no Python com a URL DO ATACANTE
   → SSRF indireto: o Python busca qualquer host da rede interna
6. O image cache PERSISTE o resultado e o serve via /api/v1/scrape/images/{id}
   → proxy SSRF com cache + exfiltração
```

Bônus: `GET /api/v1/webhooks/events` é **público** — telemetria (requestId, jobId, status) de todos os usuários sem login. E spam ilimitado de `webhook_events` (DoS de disco).

### O fix — HMAC no contrato + fechamento de rotas + mitigações Python

**Assinatura (Python — worker do webhook):**

```python
body = payload_bytes_exatos_que_vao_no_post   # serializa UMA vez, assina, envia os MESMOS bytes
signature = hmac.new(settings.WEBHOOK_SECRET.encode(), body, hashlib.sha256).hexdigest()
headers = {"Content-Type": "application/json",
           "X-Signature": signature,
           "X-Timestamp": str(int(time.time()))}
```

**Verificação (Java — filter/verifier antes do controller):**

```java
byte[] raw = request.getInputStream().readAllBytes();          // ler antes do Jackson
Mac mac = Mac.getInstance("HmacSHA256");
mac.init(new SecretKeySpec(secret.getBytes(), "HmacSHA256"));
String expected = HexFormat.of().formatHex(mac.doFinal(raw));
String provided = request.getHeader("X-Signature");

if (provided == null || !MessageDigest.isEqual(expected.getBytes(), provided.getBytes()))  // constant-time
    return 401;
if (Math.abs(now - Long.parseLong(request.getHeader("X-Timestamp"))) > 300)                 // replay ±5min
    return 401;
```

- Secret compartilhado via env (`WEBHOOK_SECRET`) nos dois serviços, **sem default** (fail-fast do item 7).
- `/api/v1/webhooks/events` → rota autenticada + role `ADMIN` (não há ownership por usuário nesse recurso).

**Mitigações SSRF no Python (passe único):**

1. `ScrapeDetailRequest.url`: trocar `str` → `HttpUrl` + allowlist de domínios de marketplace (`olx.com.br` etc.) — é o alvo da tool, domínio é conhecido.
2. `webhookUrl`: resolve do host → bloquear ranges privados (loopback, RFC1918, link-local `169.254.0.0/16`) — em rede Docker, validar contra a subnet interna.
3. `image_cache_service`: mesmo allowlist de domínios antes de baixar.

### Como validar

Post no webhook sem `X-Signature` → 401; com assinatura errada → 401; com assinatura certa e timestamp de 10 min atrás → 401; com tudo certo → 200. `GET /events` sem token → 401/403. Python: `ScrapeDetailRequest` com `url=http://169.254.169.254/` → 422.

### Esforço: **M (1 dia)** — Python (2-3h) + Java filter (3-4h) + rotas (1h) + testes.

---

# PARTE IV — FRONTEND

## 9. O loop infinito de refresh e a fila que nunca acorda

**Onde:** `auth.interceptor.ts:26` (403 tratado como 401), `auth.interceptor.ts:66-72` (erro do refresh não propaga à fila), `auth.interceptor.ts:83-94` (fila pendurada), `app.html:1` + `app.routes.ts:7` (toast container fora de `/login`), `environment.ts:3` (fallback de prod hardcoded)

### O mecanismo — dois bugs encadeados no mesmo interceptor

**Loop 403 (BUG-1):** 403 = "autenticado, mas sem permissão" (é exatamente o que seu backend devolve no `validatePermission`). Refresh **nunca** resolve 403. Ciclo:

```
request → 403 → refresh (200, rotaciona RT) → retry com token novo → 403 (permissão não muda)
        → refresh → retry → 403 → ...  LOOP INFINITO
```

Cada iteração rotaciona o refresh token (`auth.interceptor.ts:54-55`) e gera tráfego contra o servidor **até o refresh falhar por idade/revogação**. Usuário vê a tela travada; o backend apanha de graça.

**Fila que nunca acorda (BUG-2):** durante o refresh, requests concorrentes de 401 ficam esperando `refreshTokenSubject` emitir token não-nulo (`filter(token => token !== null)`, linha 84). Quando o refresh **falha**, o `catchError` (linha 66-72) faz logout e navega — mas **nunca emite nada no subject**. Os assinantes enfileirados ficam presos: **spinners eternos em todas as telas** com chamadas paralelas (o padrão no seu app — o dashboard dispara 4 em `dashboard-home.component.ts:208-228`).

**Morte por mil cortes (BUG-3):** o `<app-ui-toast-container>` só existe dentro do layout (`app-layout.component.ts:12`); `/login` está fora dele (`app.routes.ts:7`) → **nenhum toast de login nunca apareceu** — e o comentário em `login.component.ts:17` ("MENSAGEM DE ERRO ESTATICA COMO BACKUP SEGURO") mostra que o sintoma foi remendado sem diagnóstico.

### O fix

```ts
// (1) 403 NUNCA passa pelo refresh:
if (error.status === 401 && !isAuthUrl(req)) return handle401Error(req, next, http, router);
if (error.status === 403) return throwError(() => error);  // toast "sem permissão" no errorInterceptor

// (2) no catchError do refresh — acordar a fila ANTES de tudo:
catchError((err) => {
  refreshTokenSubject.error(err);                          // acorda TODOS os enfileirados com erro
  refreshTokenSubject = new BehaviorSubject<string | null>(null); // subject novo p/ próximo ciclo
  isRefreshing = false;
  localStorage.removeItem('jwt_token');
  localStorage.removeItem('refresh_token');
  router.navigate(['/login']);
  return throwError(() => err);
})
```

3. **Mover `<app-ui-toast-container>` (e o confirm dialog) para o `app.html` root** — overlays não podem viver dentro de um layout que rotas fora dele não usam.
4. **Matar o fallback de produção:** `environment.ts:3` — se `window.env?.apiUrl` não existir, a app inteira aponta para o domínio de **dev** em silêncio. Validar no `entrypoint.sh`: `: "${API_URL:?API_URL obrigatório}"` — container crasha se não vier.

### Como validar

Teste: mock de 403 em `/monitors` → **uma** tentativa, sem chamadas a `/auth/refresh`, toast de permissão. Mock de refresh 500 + 2 requests 401 concorrentes → ambos terminam em erro, nenhum spinner pendurado. Fluxo de login → toast aparece.

### Esforço: **P (meio dia-1 dia)** com testes.

---

# PARTE V — ESCALA DE LEITURA E DÍVIDAS MENORES

## 10. N+1 — as três rotas e o batch

| Rota | Onde | Custo | Fix |
|---|---|---|---|
| `GET /monitors` (paginado) | `ProductMonitorMapper.java:113-117` itera `searchQueries` lazy | 1 + N queries/página | `@EntityGraph` no `findByUserId` (o `findByActiveTrue` já tem — `ProductMonitorRepository.java:21` — é copiar) |
| `GET /listings` | `ProductMonitorService.java:127-152` — `l.getProductMonitor().getName()` lazy por item | +N queries/página | Query de projeção (id, monitorName) ou `JOIN FETCH` |
| Webhook processing | `WebhookScrapperService.java:141-143` — 1 SELECT por item **dentro da TX longa** | 50 itens = 50 queries segurando conexão | 1 query `WHERE (monitor_id, vendor, vendor_listing_id) IN (...)` → mapa em memória |

O terceiro morre junto com o TX1 do item 1 (a separação novos/existentes vira parte do claim). Os dois primeiros: **P (meio dia)**.

**Nota de escala:** esses N+1 não travam a API sozinhos (páginas são 10-20 itens), mas multiplicam latência de p99 sob pool já estressado pelo item 1 — corrija depois do redesign, não antes.

## 11. Bundle de correções médias (mecanismo → risco → fix, direto)

| # | Item | Onde | Fix |
|---|---|---|---|
| 11.1 | `NOTEBOOK` aceito e **silenciosamente ignorado** — usuário configura specs, IA nunca roda, zero erro | `AnalisyFactorySelector.java:26` → fallback NONE | Rejeitar com 400 claro OU implementar factory. Decidir e comunicar via API |
| 11.2 | Model IA hardcoded `GEMINI_2_5_FLASH_LITE` — a config `application.yml:29` (`gemini-2.5-flash`) é **morta** | `AnalisysFactorySimpleImpl.java:145` | Injetar model via properties |
| 11.3 | `cleanupExpiredTokens` nunca roda — tabela `refresh_tokens` cresce sem limite | `RefreshTokenService.java:58-64` (sem `@Scheduled`, sem caller) | `@Scheduled` diário |
| 11.4 | **Bug multi-vendor latente** — `_persist_listings` busca sem filtrar `vendor`; IDs OLX/ML colidem | `scraping_service.py:155` | `.where(vendor == request.vendor)` — **UMA LINHA, antes de qualquer provider novo** |
| 11.5 | `AdDetailCache` INSERT puro → `IntegrityError` no re-scrape pós-TTL (janela garantida: purge horário vs TTL mínimo 1h); URL sem ID → `vendor_listing_id=""` colide | `scrape_detail_service.py:42-46,114-123` | UPSERT `ON CONFLICT (vendor, vendor_listing_id) DO UPDATE` |
| 11.6 | Delivery `PENDING` órfã (Python) — recovery só cobre `SENDING` | `queues/webhook.py:243-273` | Cobrir `PENDING` com job terminal (item 6) |
| 11.7 | `MAX_RETRIES` config **morta** — zero retry em todo o pipeline | `config.py:27` | Retry c/ backoff no `SmartHttpClient` (5xx/timeout) + re-tentar página antes do `break` |
| 11.8 | CORS Python `["*"]` + `allow_credentials=True` (combinação inválida/spec) | `main.py:70-76` | Allowlist via env (o Java já faz certo — seguir o próprio padrão do repo) |
| 11.9 | `price=0.0` como sentinel de "preço não achado" — polui estatísticas | `parser.py:155,407` | `None` + Java trata ausente |
| 11.10 | Frontend: `strict: false` deliberado (raiz de 22 `as any`), teste `app.spec.ts` quebrado, sem ESLint | `tsconfig.json:5-16`, `app.spec.ts:22` | `strict: true` + `strictTemplates` + ESLint (`@angular-eslint`) + consertar/deletar o spec |
| 11.11 | 401 com corpo de 400: "não autenticado" → BusinessException → HTTP 400 | `MeService.java:25` | Exceção dedicada → 401; ownership violado → 404 uniforme (anti-enumeração) |
| 11.12 | `durationMs`/`total_found`/`error_message` do summary Python descartados | `WebhookScrapperService` | Popular na persistência (era o ponto 5, junto) |

**Esforço do bundle:** ~3-4 dias somados. 11.4 e 11.5 são pré-requisitos para qualquer provider novo (ML/ENJOEI) — sem eles, próximo vendor = corrupção de dados cruzados.

---

# PARTE VI — TRILHA DE PUBLICAÇÃO

## Produto viável vs projeto pra publicar — a diferença real

Seu diagnóstico está certo: o projeto está **demo-complete** (features funcionam no caminho feliz, single usuário, ambiente dev) e tem cara de produto (o conjunto monitores + análise IA + logs de custo + dashboard é um esqueleto de produto legítimo). O que falta é a camada **operationally-complete**: sobreviver a retry duplicado, scraper caído, IA lenta, carga, e ataque. O detalhe que vale ouro: **para portfólio, a camada operacional também é o que mais impressiona** — "redesenhei o fluxo webhook de transação-longa para 3 transações curtas e o p99 sob burst caiu de timeout para 200ms" vale mais que qualquer feature nova.

## Ordem de execução (dependências reais)

```
FASE 0 — HOJE (nada depende de nada, é urgência):
  7.1  Rotacionar a chave Gemini                      [1h]

FASE 1 — Segurança (bloqueia deploy público)              ~3 dias
  7.2  Fail-fast de secrets (Java + Python)
  7.3  git filter-repo + limpeza de caches
  7.4  .dockerignore por context
  7.5  V5 (desativar admin seed) + READMEs
  8    HMAC no webhook + fechar /events + SSRF allowlist
  CI   needs: verify nos 3 workflows + npm ci + teste/strict no frontend

FASE 2 — Núcleo de escala (redesenha junto, não em patche) ~1 semana
  1+2+3  Redesign webhook: 3 TXs curtas + máquina de estados
         + idempotência (FOR UPDATE) + catch fora da TX
  4      Read timeout + retry + self-invocation + lastScrapedAt
  6      Reaper (Java + recovery Python)
  5      Verdade de status end-to-end (Python raise → worker except → Java FAILED)
  →     Teste de carga de aceitação (stub Gemini 30s, 20 webhooks concorrentes)

FASE 3 — Frontend                                         ~2 dias
  9      Interceptor (403 ≠ 401, acordar fila, toast no root)
  11.10   strict + ESLint + spec quebrado

FASE 4 — Escala de leitura + bundle                       ~3-4 dias
  10      EntityGraph/JOIN FETCH/IN batch
  11.*    (11.4 e 11.5 ANTES de qualquer provider novo)

FASE 5 — Nomes e docs                                     ~1 dia
  bi-engine/bi-scraper/biscraper/"Bobão do Oeste"/teste-scrap/project-ui
  → uma identidade. READMEs (Boot 4.1.1, /docs, remover promessas inexistentes)
```

## Resumo de esforço

| Trilha | Escopo | Estimativa (dev solo) |
|---|---|---|
| **Publicar com segurança** (portfólio) | Fase 0 + Fase 1 + interceptor (9) + spec fix | **~4-5 dias** |
| **Produto operacional** | Fases 0-4 | **~2.5-3 semanas** |
| Opcional pós-produto | ShedLock, Testcontainers na CI, OpenAPI→tipos TS, signal forms, provider ML (com 11.4/11.5) | backlog |

---

## Apêndice — mapa dos gargalos para você nunca mais esquecer

```
        Python (concurrency=1)                Java bi-engine
  ┌──────────────────────────┐      ┌────────────────────────────────────┐
  │ fila SQLite (ok, atômica) │      │ scheduler → dispatcher (5 threads) │
  │ worker: mark_success ✗    │──webhook (sem HMAC, retry)──▶ │ @Async(20) @Transactional      │
  │ recovery PENDING ✗         │      │   ├─ Gemini + tools (MINUTOS)    │ ← segura conexão
  │ retry: inexistente ✗       │      │   └─ pool Hikari = 10 ✗          │   = API INTEIRA trava
  │ challenge = "0 results" ✗  │      │ catch engolido → commit ✗        │
  └──────────────────────────┘      │ execution PENDING eterna ✗       │
                                     └────────────────────────────────────┘
```

Cada ✗ é um item deste documento. A ordem de correção não é por gravidade individual — é por dependência (Fase 2 resolve 5 ✗ de uma vez porque moram no mesmo fluxo).
