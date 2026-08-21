# ===================================================
# Dockerfile - Marketplace Scraper Service
# Imagem oficial Microsoft Playwright + UV (Zero build overhead, rápido e estável no Coolify)
# ===================================================

FROM mcr.microsoft.com/playwright/python:v1.49.1-noble

# Instalar o gerenciador UV da Astral
COPY --from=ghcr.io/astral-sh/uv:latest /uv /uvx /bin/

WORKDIR /app

# Variáveis de ambiente de produção
ENV PYTHONUNBUFFERED=1 \
    PYTHONDONTWRITEBYTECODE=1 \
    UV_COMPILE_BYTECODE=1 \
    UV_LINK_MODE=copy \
    PORT=8001 \
    HOST=0.0.0.0 \
    APP_ENV=production \
    DATABASE_URL=sqlite+aiosqlite:////tmp/scraper.db

# 1. Copiar manifesto de dependências
COPY pyproject.toml uv.lock ./

# 2. Instalar dependências Python via UV (rápido e determinístico)
RUN uv sync --frozen --no-dev --no-install-project

# 3. Copiar código-fonte da aplicação
COPY src/ ./src/
COPY docs/ ./docs/
COPY README.md ./

# Porta exposta da API e do Dashboard
EXPOSE 8001

# Healthcheck interno do container
HEALTHCHECK --interval=30s --timeout=5s --start-period=10s --retries=3 \
    CMD curl -f http://localhost:8001/health || exit 1

# Inicialização via Uvicorn
ENTRYPOINT ["/app/.venv/bin/uvicorn", "src.main:app", "--host", "0.0.0.0", "--port", "8001"]
