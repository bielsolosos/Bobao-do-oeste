# Marketplace Scraper Service 🚀

Serviço de web scraping de alta performance, desacoplado e resiliente contra bloqueios (WAF/Cloudflare), projetado para alimentar pipelines de análise inteligente de hardware usado (ThinkPads, Mini PCs, GPUs, etc.).

> 📖 **Documentação Técnica Interna Detalhada:** Consulte o [Guia de Scraping & Arquitetura](docs/SCRAPING_GUIDE.md) para detalhes aprofundados sobre a evasão de anti-bot com `curl_cffi`, anatomia de rotas da OLX e estratégias de parsing.

---

## 🖥️ Dashboard Web Interativo

O serviço inclui um Dashboard visual responsivo (Tailwind CSS + DaisyUI) protegido por **HTTP Basic Auth**:

- **URL do Dashboard:** `http://localhost:8001/dashboard` (ou na raiz `http://localhost:8001/`)
- **Credenciais Padrão:**
  - **Usuário:** `admin` (configurável via `BASIC_AUTH_USERNAME` no `.env`)
  - **Senha:** `admin` (configurável via `BASIC_AUTH_PASSWORD` no `.env`)

### Recursos do Dashboard:
- 📊 **KPIs em Tempo Real:** Total de anúncios no banco, histórico de rodadas e consultas ativas.
- 🛍️ **Visualização de Anúncios (`scraped_listings`):** Cards com fotos, preços formatados, badges de OLX Pay / Entrega e links diretos para o marketplace.
- ⚡ **Histórico de Execuções (`scraping_executions`):** Tabela de auditoria com status, duração em milissegundos, itens novos e flags de fallback.
- 🔍 **Consultas Salvas (`search_queries`):** Histórico dos filtros e parâmetros pesquisados.
- 🚀 **Disparo de Coletas em 1 Clique:** Modal interativo para rodar novas buscas contra a OLX com feedback em tempo real.

---

## 🛠️ Stack Tecnológica

- **Gerenciador de Pacotes & Runtime:** [uv](https://github.com/astral-sh/uv) (Python 3.11+)
- **API & UI Framework:** FastAPI + Uvicorn + Jinja2 (ASGI assíncrono)
- **Segurança:** HTTP Basic Authentication com `secrets.compare_digest`
- **Scraping Engine (Tier 1):** `curl_cffi` (impersonação do handshake TLS e headers do Chrome 120)
- **Scraping Engine (Tier 2 - Fallback):** Playwright Chromium Headless (modo stealth para CAPTCHA/JavaScript)
- **Parser HTML:** `selectolax` (C Modest Engine)
- **Banco de Dados & ORM:** SQLite assíncrono com `SQLModel` / `SQLAlchemy`
- **Validação de Tipos:** Pydantic v2

---

## 🚀 Como Executar

### 1. Iniciar a Aplicação (API + Dashboard)
```bash
# Windows
.\run_api.bat

# Linux / Mac
./run_api.sh
```
### 2. Rodar os Testes Automatizados
```bash
# Windows
.\run_tests.bat

# Linux / Mac
./run_tests.sh
```

### 3. Deploy no Coolify (Docker)

1. No painel do Coolify, crie um novo recurso apontando para este repositório Git.
2. Selecione o tipo de build como **Dockerfile**.
3. Em **Port Mapping / Destination Port**, configure: `8001`.
4. Em **Environment Variables**, defina (se desejar sobrescrever):
   * `BASIC_AUTH_USERNAME=admin`
   * `BASIC_AUTH_PASSWORD=sua_senha_segura`
   * `DATABASE_URL=sqlite+aiosqlite:////tmp/scraper.db`
   * `PORT=8001`
   * `HOST=0.0.0.0`
   * `APP_ENV=production`

---

## 📡 Endpoints da API (Protegidos por Basic Auth)

### 1. Disparar Coleta (`POST /api/v1/scrape`)
```bash
curl -X POST http://localhost:8001/api/v1/scrape \
  -u admin:admin \
  -H "Content-Type: application/json" \
  -d '{
    "vendor": "OLX",
    "keyword": "thinkpad t480",
    "state": "sp",
    "min_price": 500.0,
    "max_price": 2500.0,
    "require_delivery": true
  }'
```

### 2. Consultar Histórico de Execuções (`GET /api/v1/executions`)
```bash
curl -u admin:admin http://localhost:8001/api/v1/executions
```

### 3. Consultar Anúncios Salvos (`GET /api/v1/listings`)
```bash
curl -u admin:admin "http://localhost:8001/api/v1/listings?keyword=thinkpad&has_delivery=true"
```

### 4. Health Check Público (`GET /health`)
```bash
curl http://localhost:8001/health
```
