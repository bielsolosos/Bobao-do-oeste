# Marketplace Scraper Service 🕷️

Serviço assíncrono e resiliente em Python (FastAPI + SQLModel + Playwright) para scraping de marketplaces (OLX, etc.) com bypass anti-bot, fila persistente SQLite e entrega de resultados via Webhooks.

## 🚀 Funcionalidades
- **Scraping de Alta Performance:** Integração com `curl_cffi` (impersonate Chrome TLS/JA3) e fallback inteligente para Playwright Chromium Stealth.
- **Fila Persistente & Concorrência:** Gerenciamento em background via SQLite com pool de workers assíncronos.
- **Webhook Dispatcher:** Entrega resiliente de resultados via HTTP POST com retry e backoff exponencial.
- **Deduplicação & Upsert:** Atualização contínua de preços e metadados sem duplicar registros.

## 📦 Execução Local
```bash
# Instalar dependências com UV
uv sync

# Iniciar o servidor
uv run uvicorn src.main:app --host 0.0.0.0 --port 8001 --reload
```

## 🧪 Testes
```bash
uv run pytest tests/ -v
```
