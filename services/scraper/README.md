# Marketplace Scraper Service 🕷️

Serviço assíncrono e resiliente em Python (FastAPI + SQLModel + Playwright) para scraping de marketplaces (OLX, etc.) com bypass anti-bot, fila persistente SQLite e entrega de resultados via Webhooks.

## 🚀 Funcionalidades
- **Scraping de Alta Performance:** Integração com `curl_cffi` (impersonate Chrome TLS/JA3) e fallback inteligente para Playwright Chromium Stealth.
- **Deep Scraping & Image Cache (TTL):** Extração de anúncios únicos por URL com tabela de especificações técnicas, download de galeria de fotos em alta resolução e armazenamento com expiração no SQLite.
- **Fila Persistente & Concorrência:** Gerenciamento em background via SQLite com pool de workers assíncronos.
- **Webhook Dispatcher:** Entrega resiliente de resultados via HTTP POST com retry e backoff exponencial.
- **Deduplicação & Upsert:** Atualização contínua de preços e metadados sem duplicar registros.
- **Cron Worker de Limpeza:** Purgamento periódico automático de imagens e caches expirados a cada 1 hora.

## 📚 Documentação Técnica
- [Guia de Deep Scraping & Image Cache com TTL](docs/DETAIL_AND_IMAGE_CACHE_GUIDE.md)
- [Guia Técnico de Scraping e Anti-Detecção](docs/SCRAPING_GUIDE.md)
- [Guia da Fila de Execução e Workers](docs/QUEUE_GUIDE.md)
- [Guia de Entrega de Webhooks](docs/WEBHOOK_GUIDE.md)

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

