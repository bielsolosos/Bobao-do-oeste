# Marketplace Scraper Service 🚀

Serviço de web scraping de alta performance, desacoplado e resiliente contra bloqueios (WAF/Cloudflare), projetado para alimentar pipelines de análise inteligente de hardware usado (ThinkPads, Mini PCs, GPUs, etc.).

---

## 🛠️ Stack Tecnológica

- **Gerenciador de Pacotes & Runtime:** [uv](https://github.com/astral-sh/uv) (Python 3.11+)
- **API Framework:** FastAPI + Uvicorn (ASGI assíncrono)
- **Scraping Engine (Tier 1):** `curl_cffi` (impersonação do handshake TLS e headers do Chrome 120)
- **Scraping Engine (Tier 2 - Fallback):** Playwright Chromium Headless (modo stealth para CAPTCHA/JavaScript)
- **Parser HTML:** `selectolax` (C Modest Engine)
- **Banco de Dados & ORM:** SQLite assíncrono com `SQLModel` / `SQLAlchemy`
- **Validação de Tipos:** Pydantic v2

---

## 🏛️ Arquitetura e Atores

```
                      [ POST /api/v1/scrape ] (Java Core / User)
                                 │
                                 ▼
                    ┌───────────────────────────┐
                    │   ScrapingOrchestrator    │
                    └────────────┬──────────────┘
                                 │
         ┌───────────────────────┴───────────────────────┐
         ▼                                               ▼
┌─────────────────┐                             ┌─────────────────┐
│   SearchQuery   │                             │  OlxUrlBuilder  │
└─────────────────┘                             └────────┬────────┘
                                                         │
                                                         ▼
                                                ┌─────────────────┐
                                                │ SmartHttpClient │ (curl_cffi Chrome TLS)
                                                └────────┬────────┘
                                                         │ (Fallback Playwright se 403)
                                                         ▼
                                                ┌─────────────────┐
                                                │ OlxPayloadParser│ (selectolax)
                                                └────────┬────────┘
                                                         │
                                 ┌───────────────────────┘
                                 ▼
                     ┌───────────────────────┐
                     │  ScrapingExecution    │ (Histórico e Telemetria)
                     └───────────┬───────────┘
                                 │
                                 ▼
                     ┌───────────────────────┐
                     │    ScrapedListing     │ (Anúncios & Deduplicação)
                     └───────────────────────┘
```

---

## 🚀 Como Executar

### 1. Pré-requisitos
Ter o `uv` instalado na máquina:
```bash
# Windows
powershell -ExecutionPolicy ByPass -c "irm https://astral.sh/uv/install.ps1 | iex"
```

### 2. Instalar Dependências e Navegadores
```bash
uv sync
uv run playwright install chromium
```

### 3. Rodar os Testes Automatizados
```bash
uv run pytest
```

### 4. Iniciar o Servidor FastAPI
```bash
uv run uvicorn src.main:app --reload --port 8000
```
Swagger UI disponível em: `http://localhost:8000/docs`

---

## 📡 Endpoints da API

### 1. Disparar Coleta (`POST /api/v1/scrape`)
Exemplo de Request:
```json
{
  "vendor": "OLX",
  "keyword": "thinkpad t480",
  "state": "sp",
  "min_price": 500.0,
  "max_price": 2500.0,
  "require_delivery": true,
  "max_pages": 1
}
```

Exemplo de Response:
```json
{
  "success": true,
  "execution": {
    "execution_id": "7f3281bf-ea5f-4bce-9e4e-1a34dff72228",
    "vendor": "OLX",
    "status": "SUCCESS",
    "duration_ms": 278,
    "total_found": 50,
    "new_items_count": 50,
    "used_fallback": false,
    "started_at": "2026-08-21T18:20:28.000Z",
    "finished_at": "2026-08-21T18:20:29.000Z"
  },
  "items": [
    {
      "vendor": "OLX",
      "vendor_listing_id": "1527993289",
      "title": "Notebook ThinkPad T480",
      "price": 1500.0,
      "url": "https://sp.olx.com.br/sao-paulo-e-regiao/informatica/notebooks/notebook-thinkpad-t480-1527993289",
      "state": "SP",
      "has_delivery": true,
      "delivery_type": "OLX_PAY",
      "images": ["https://img.olx.com.br/thumbs700x500/..."],
      "scraped_at": "2026-08-21T18:20:29.000Z"
    }
  ]
}
```

### 2. Consultar Histórico de Execuções (`GET /api/v1/executions`)
Retorna telemetria, taxa de sucesso e tempos de resposta de todas as buscas realizadas.

### 3. Consultar Anúncios Salvos (`GET /api/v1/listings`)
Permite filtrar anúncios do banco por palavra-chave, faixa de preço, marketplace e opção de entrega.
