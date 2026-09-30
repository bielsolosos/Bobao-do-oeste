# Guia Técnico de Scraping e Arquitetura do Sistema 🕷️

Este documento detalha o funcionamento interno de cada componente do **`scraper-service`**, explicando os desafios técnicos de contornar proteções anti-bot, a anatomia das páginas da OLX, as estratégias de parsing, a camada de serviços de domínio e a persistência no banco de dados.

> 🚦 **Nova camada de fila:** As requisições `POST /api/v1/scrape` passam por uma **fila persistente SQLite** antes de chegar ao `ScrapingService`. Veja o [`QUEUE_GUIDE.md`](QUEUE_GUIDE.md).
>
> 🖼️ **Deep Scraping & Cache de Imagens:** Para raspagem de anúncio único por URL com extração completa de specs e cache de imagens com TTL no SQLite, veja o [`DETAIL_AND_IMAGE_CACHE_GUIDE.md`](DETAIL_AND_IMAGE_CACHE_GUIDE.md).

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
            │   ScrapingService   │ ──> Deduplicação em lote, grava no SQLite e gera métricas
            └─────────────────────┘
```

---

## 2. Detalhamento dos Componentes

### A. Camada de Rede & Anti-Detecção (`src/core/engine/`)

#### 1. `src/core/engine/http_client.py`
* **Objetivo:** Fazer o download do HTML da OLX em alta velocidade sem ser bloqueado pelo Cloudflare/DataDome.
* **Mecanismo:** Usa a biblioteca `curl_cffi` (escrita em C) para forçar o handshake TLS/JA3 e os cabeçalhos de rede a imitarem o **Google Chrome 120**.
* **Cabeçalhos Críticos Injetados:**
  * `User-Agent`: String de Chrome 120 em Windows 64-bit.
  * `Sec-Ch-Ua`, `Sec-Ch-Ua-Mobile`, `Sec-Ch-Ua-Platform`: Cabeçalhos Client Hints modernos do Chromium.
  * `Sec-Fetch-Dest`, `Sec-Fetch-Mode`, `Sec-Fetch-Site`, `Sec-Fetch-User`: Metadados de navegação real do usuário.
* **Detecção de Bloqueio:** Se a OLX responder `403`, `429`, `503` ou exibir a página "Just a moment..." (Cloudflare Turnstile), o cliente lança `HttpClientBlockedException`, disparando o fallback.

#### 2. `src/core/engine/browser_fallback.py`
* **Objetivo:** Segunda linha de defesa. Se a OLX ativar um desafio de JavaScript complexo ou CAPTCHA interativo, este módulo abre uma instância headless do **Playwright Chromium**.
* **Técnicas Anti-Detecção:**
  * Aplica `playwright_stealth` para mascarar WebGL, Canvas, Webdriver e permissões.
  * Define viewport e locale brasileiros (`pt-BR`, 1920x1080).
  * Argumentos de inicialização do Chromium desativam flags de automação (`--disable-blink-features=AutomationControlled`).

---

### B. Provedores de Scraping (`src/domain/providers/`)

#### 3. `src/domain/providers/olx/url_builder.py`
* **Objetivo:** Traduzir os parâmetros genéricos de busca em uma URL válida da OLX Brasil.
* **Mapeamento de Regras:**
  * **Categorias:** `informatica-e-acessorios/notebooks` -> `https://www.olx.com.br/informatica-e-acessorios/notebooks`
  * **Estados:** `sp` -> `https://www.olx.com.br/estado-sp`
  * **Regiões:** `sao-paulo-e-regiao` -> `https://www.olx.com.br/estado-sp/sao-paulo-e-regiao`
  * **Filtro de Preço Mínimo:** Parâmetro `ps` (Price Start), ex: `ps=800`
  * **Filtro de Preço Máximo:** Parâmetro `pe` (Price End), ex: `pe=2000`
  * **Filtro de Entrega / OLX Pay:** Parâmetro `olxpay=1`
  * **Paginação:** Parâmetro `o=2` (Offset de página)

#### 4. `src/domain/providers/olx/parser.py`
* **Objetivo:** Extrair e higienizar os anúncios do HTML bruto retornado pela OLX.
* **Estratégia 1 (Next.js Hydration Script):** Localiza a tag `<script id="__NEXT_DATA__">` e faz o parse direto do JSON original.
* **Estratégia 2 (Parser DOM de Alta Velocidade com Selectolax):** Localiza os cards `section.olx-adcard` e extrai IDs, títulos, preços normalizados (`_parse_price`) e flags de entrega.

#### 5. `src/domain/providers/olx/provider.py` & `src/domain/providers/base.py`
* Implementa o contrato `BaseScraperProvider` e registra-se dinamicamente na `ProviderFactory`.

---

### C. Camada de Serviços de Domínio (`src/domain/services/`)

#### 6. `src/domain/services/scraping_service.py`
* Coordena a busca/criação de `SearchQuery`, disparo da raspagem, persistência em lote e auditoria de telemetria da `ScrapingExecution`.

#### 7. `src/domain/services/listing_service.py`
* Centraliza filtros e consultas de anúncios salvos (`ScrapedListing`), isolando as consultas SQL dos controladores.

#### 8. `src/domain/services/execution_service.py`
* Gerencia o histórico e detalhamento das execuções de scraping (`ScrapingExecution`).

---

### D. Camada de Apresentação & API (`src/api/v1/`)

* **Controllers Desacoplados:** Os endpoints (`src/api/v1/scrape_routes.py`, `src/api/v1/listing_routes.py`, `src/api/v1/execution_routes.py`, `src/api/v1/dashboard_routes.py`) interagem exclusivamente através dos serviços (`ScrapingService`, `ListingService`, `ExecutionService`), sem acoplamento direto com queries SQL.
