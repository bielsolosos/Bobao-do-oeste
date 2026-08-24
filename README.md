# Marketplace Intelligence Ecosystem 🚀

Monorepo poliglota para monitoramento, scraping resiliente e análise de inteligência de preços de hardware usado.

---

## 🏛️ Estrutura do Monorepo

```
.
├── services/
│   ├── scraper/         # 🐍 Python / FastAPI (Scraping, WAF Bypass, Filas, Webhooks)
│   └── bi-engine/       # ☕ Java / Spring Boot (Analytics, Pricing Intelligence, PostgreSQL)
│
├── apps/
│   └── web/             # 🌐 TypeScript (Frontend SPA Dashboard)
│
├── .github/workflows/   # 🤖 Pipelines CI/CD independentes por serviço
└── docker-compose.yml   # 🐳 Orquestração local para desenvolvimento
```

---

## 🚀 Arquitetura de Deploy Distribuído

| Serviço | Tecnologia | Ambiente de Execução | Motivação |
| :--- | :--- | :--- | :--- |
| **`services/scraper`** | Python 3.11 (uv, FastAPI, Playwright) | **Raspberry Pi (Residencial)** | Evasão natural de WAF/Cloudflare via IP residencial |
| **`services/bi-engine`** | Java 21 (Spring Boot 3, Postgres) | **VPS Nuvem (Coolify)** | Alto throughput, processamento analítico e persistência |
| **`apps/web`** | TypeScript (SPA) | **VPS Nuvem (Coolify)** | CDN / SSR / Entrega rápida aos usuários |

---

## 🛠️ Comandos Rápidos de Desenvolvimento

### 1. Iniciar Ambiente Completo (Docker Compose)
```bash
docker compose up -d postgres scraper
```

### 2. Rodar o Scraper Localmente
```bash
cd services/scraper
uv sync
uv run pytest
uv run uvicorn src.main:app --reload --port 8001
```

---

---

## 🔐 Credenciais Padrão de Desenvolvimento

| Serviço | Tipo de Auth | Usuário | Senha Padrão | Endpoint de Login / Doc |
| :--- | :--- | :--- | :--- | :--- |
| **`services/bi-engine`** (Java) | **JWT / Bearer Token** | `admin` | `admin123` | `POST /api/v1/auth/login` \| [Swagger](http://localhost:8080/swagger-ui.html) |
| **`services/scraper`** (Python) | **HTTP Basic Auth** | `admin` | `admin` | `GET /dashboard` \| `POST /api/v1/scrape` |

---

## 📄 CI / CD (GitHub Actions)
- **`ci-scraper.yml`**: Validações de formatação (Ruff), tipagem (Pyright) e testes do Scraper Python (`services/scraper/**`).
- **`ci-bi-engine.yml`**: Compilação Maven, testes JUnit com Postgres e validação de Dockerfile do BI Engine (`services/bi-engine/**`).

