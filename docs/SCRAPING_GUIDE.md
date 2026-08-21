# Guia Técnico de Scraping e Arquitetura do Sistema 🕷️

Este documento detalha o funcionamento interno de cada componente do **`scraper-service`**, explicando os desafios técnicos de contornar proteções anti-bot, a anatomia das páginas da OLX, as estratégias de parsing e a persistência no banco de dados.

---

## 1. Visão Geral do Fluxo de Scraping

```
               [ ScrapeRequest ] (Keyword, Preço, Região, Entrega)
                       │
                       ▼
            ┌─────────────────────┐
            │    OlxUrlBuilder    │ ──> Traduz os filtros para a URL canônica da OLX
            └──────────┬──────────┘
                       │
                       ▼
            ┌─────────────────────┐
            │   SmartHttpClient   │ ──> Dispara requisição HTTP com TLS Fingerprint do Chrome 120
            └──────────┬──────────┘
                       │ (Se 403 / Desafio JS)
                       ├──────────────────────> ┌──────────────────────────┐
                       │                        │ PlaywrightBrowserFallback│ (Navegador Headless Stealth)
                       ▼                        └─────────────┬────────────┘
            ┌─────────────────────┐                           │
            │  OlxPayloadParser   │ <─────────────────────────┘
            │   (selectolax / C)  │ ──> Extrai cards da OLX, fotos, preços e flags de entrega
            └──────────┬──────────┘
                       │
                       ▼
            ┌─────────────────────┐
            │ScrapingOrchestrator │ ──> Calcula deduplicação, grava no SQLite e gera métricas
            └─────────────────────┘
```

---

## 2. Detalhamento dos Arquivos Principais

### A. Camada de Rede & Anti-Detecção

#### 1. [`src/engine/http_client.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/engine/http_client.py)
* **Objetivo:** Fazer o download do HTML da OLX em alta velocidade sem ser bloqueado pelo Cloudflare/DataDome.
* **Mecanismo:** Usa a biblioteca `curl_cffi` (escrita em C) para forçar o handshake TLS/JA3 e os cabeçalhos de rede a imitarem exatamente o **Google Chrome 120**.
* **Cabeçalhos Críticos Injetados:**
  * `User-Agent`: String de Chrome 120 em Windows 64-bit.
  * `Sec-Ch-Ua`, `Sec-Ch-Ua-Mobile`, `Sec-Ch-Ua-Platform`: Cabeçalhos Client Hints modernos do Chromium.
  * `Sec-Fetch-Dest`, `Sec-Fetch-Mode`, `Sec-Fetch-Site`, `Sec-Fetch-User`: Metadados de navegação real do usuário.
* **Detecção de Bloqueio:** Se a OLX responder `403`, `429`, `503` ou exibir a página "Just a moment..." (Cloudflare Turnstile), o cliente lança `HttpClientBlockedException`, disparando o fallback.

#### 2. [`src/engine/browser_fallback.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/engine/browser_fallback.py)
* **Objetivo:** Segunda linha de defesa. Se a OLX ativar um desafio de JavaScript complexo ou CAPTCHA interativo, este módulo abre uma instância headless do **Playwright Chromium**.
* **Técnicas Anti-Detecção:**
  * Remove a propriedade `navigator.webdriver` via script de injeção no contexto do browser.
  * Define viewport e locale brasileiros (`pt-BR`, 1920x1080).
  * Argumentos de inicialização do Chromium desativam flags de automação (`--disable-blink-features=AutomationControlled`).

---

### B. Provedor da OLX (Construção de URL & Parsing)

#### 3. [`src/providers/olx/url_builder.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/providers/olx/url_builder.py)
* **Objetivo:** Traduzir os parâmetros genéricos de busca em uma URL válida da OLX Brasil.
* **Mapeamento de Regras:**
  * **Categorias:** `informatica-e-acessorios/notebooks` -> `https://www.olx.com.br/informatica-e-acessorios/notebooks`
  * **Estados:** `sp` -> `https://www.olx.com.br/estado-sp`
  * **Regiões:** `sao-paulo-e-regiao` -> `https://www.olx.com.br/estado-sp/sao-paulo-e-regiao`
  * **Filtro de Preço Mínimo:** Parâmetro `ps` (Preço Start), ex: `ps=800`
  * **Filtro de Preço Máximo:** Parâmetro `pe` (Preço End), ex: `pe=2000`
  * **Filtro de Entrega / OLX Pay:** Parâmetro `olxpay=1`
  * **Paginação:** Parâmetro `o=2` (Offset de página)

#### 4. [`src/providers/olx/parser.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/providers/olx/parser.py)
* **Objetivo:** Extrair e higienizar os anúncios do HTML bruto retornado pela OLX.
* **Estratégia 1 (Next.js Hydration Script):**
  * Tenta localizar a tag `<script id="__NEXT_DATA__" type="application/json">`.
  * Se existir, faz o parse direto do JSON sem depender de classes CSS.
* **Estratégia 2 (Parser DOM de Alta Velocidade com Selectolax):**
  * Localiza os cards da interface moderna da OLX: `section.olx-adcard`, `div.olx-adcard`.
  * **Extração do ID do Anúncio:** Regex no link do produto: `-(\d{8,12})(?:\?|$)` (ex: `notebook-thinkpad-1527993289` -> ID `1527993289`).
  * **Extração de Título:** Tag `<h2>` interna do card.
  * **Extração e Normalização de Preço:**
    * Vendedores e OLX formatam preços de várias formas (`R$ 1.500`, `R$ 1.500,00`, `1500`).
    * O método `_parse_price()` trata separadores de milhar (ponto no Brasil) e decimais (vírgula no Brasil), convertendo com segurança para `float`.
  * **Detecção de Entrega (OLX Pay):**
    * Busca menções a `Frete grátis`, `Entrega`, `OLX Pay` e `Garantia da OLX` no texto do card.
    * Mapeia para `has_delivery: true` e `delivery_type: "OLX_PAY"`.
  * **Extração de Imagens:**
    * Captura URLs de imagens do CDN da OLX (`img.olx.com.br`), descartando ícones e logos.
  * **Extração de Localização:**
    * Extrai estado e sub-região a partir do subdomínio da URL (ex: `https://sp.olx.com.br/grande-campinas/...` -> UF: `SP`, Região: `Grande Campinas`).

#### 5. [`src/providers/olx/provider.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/providers/olx/provider.py)
* **Objetivo:** Implementa a interface [`BaseScraperProvider`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/providers/base.py) unindo `UrlBuilder`, `SmartHttpClient`, `PlaywrightBrowserFallback` e `OlxPayloadParser`.
* Suporta scraping multipágina iterando sobre `range(1, request.max_pages + 1)`.

---

### C. Orquestração e Banco de Dados

#### 6. [`src/services/orchestrator.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/services/orchestrator.py)
* **Objetivo:** Centralizar a execução e gerenciar o ciclo de vida e estado das buscas.
* **Fluxo de Trabalho:**
  1. `_get_or_create_search_query()`: Localiza ou insere a intenção de busca no SQLite.
  2. Cria o registro `ScrapingExecution` com status `RUNNING` e timestamp `started_at`.
  3. Aciona o Provider correspondente via `ProviderFactory`.
  4. `_persist_listings()`:
     * Compara cada anúncio retornado com os já existentes no banco pelo par `(vendor, vendor_listing_id)`.
     * Contabiliza quantos anúncios são inéditos (`new_items_count`).
     * Grava todos os snapshots na tabela `scraped_listings`.
  5. Atualiza a `ScrapingExecution` para `SUCCESS` com `duration_ms` e contadores.
  6. Em caso de exceção de rede/parser, marca `status = FAILED` e persiste a mensagem de erro para auditoria.

#### 7. [`src/domain/models.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/domain/models.py)
* **Entidades Relacionais do SQLModel (SQLite):**
  * `SearchQuery`: Tabela `search_queries` (filtros e intenção de busca).
  * `ScrapingExecution`: Tabela `scraping_executions` (rodadas de execução e telemetria).
  * `ScrapedListing`: Tabela `scraped_listings` (produtos capturados e histórico).

---

### D. Camada de Apresentação (FastAPI)

#### 8. [`src/api/v1/scrape.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/api/v1/scrape.py)
* Endpoint: `POST /api/v1/scrape`
* Recebe `ScrapeRequest` validado pelo Pydantic e devolve `ScrapeResponse` estruturado com o status e a lista dos itens.

#### 9. [`src/api/v1/executions.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/api/v1/executions.py)
* Endpoints: `GET /api/v1/executions` e `GET /api/v1/executions/{id}`
* Permite auditar tempos de resposta, se houve uso de fallback e volume de anúncios capturados por execução.

#### 10. [`src/api/v1/listings.py`](file:///C:/Users/Gabriel%20Vertis/Projects/teste-scrap/src/api/v1/listings.py)
* Endpoint: `GET /api/v1/listings`
* Permite consultas avançadas aos anúncios salvos no banco local por palavras-chave, limites de preço e suporte a entrega.
