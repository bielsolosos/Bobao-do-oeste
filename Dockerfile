# ===================================================
# Dockerfile - Marketplace Scraper Service
# Otimizado para Coolify com UV, Playwright e SQLite persistente em /data
# ===================================================

FROM python:3.11-slim-bookworm AS base

# Instalar dependências essenciais do sistema e curl para healthcheck
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl \
    ca-certificates \
    && rm -rf /var/lib/apt/lists/*

# Instalar o gerenciador UV da Astral
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Definir variáveis de ambiente padrão para produção
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PORT=8001 \
    HOST=0.0.0.0 \
    APP_ENV=production \
    DATABASE_URL=sqlite+aiosqlite:////data/scraper.db

# 1. Copiar definições de pacotes para cache de camadas do Docker
COPY pyproject.toml uv.lock ./

# 2. Instalar dependências de produção sem dev-dependencies
RUN uv sync --frozen --no-dev --no-install-project

# 3. Instalar o navegador Chromium e suas dependências de sistema necessárias para o Playwright
RUN /app/.venv/bin/playwright install --with-deps chromium

# 4. Copiar o código da aplicação
COPY src/ ./src/
COPY docs/ ./docs/
COPY README.md ./

# Volume persistente para o banco de dados SQLite no Coolify
VOLUME /data

# Porta exposta da API / Dashboard
EXPOSE 8001

# Healthcheck interno do container
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8001/health || exit 1

# Ponto de entrada utilizando o executável uvicorn do ambiente virtual UV
ENTRYPOINT ["/app/.venv/bin/uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8001"]
