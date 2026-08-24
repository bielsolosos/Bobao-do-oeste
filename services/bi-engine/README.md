# BI Engine Service ☕ (Java / Spring Boot)

Serviço responsável por Business Intelligence, análise de mercado, precificação histórica e algoritmos de recomendação de hardware usado.

## 🎯 Responsabilidades
- Ingestão de Webhooks enviados pelo `services/scraper` (`POST /api/v1/webhooks/scraper`)
- Armazenamento analítico no PostgreSQL
- Cálculo de médias móveis, detecção de oportunidades de compra e scoring de preços
- Exposição de APIs REST para consumo pela SPA Frontend (`apps/web`)

## 🛠️ Tecnologias
- Java 21 / Spring Boot 3
- PostgreSQL / Flyway
- Spring Security + JJWT 0.12.6
- Springdoc OpenAPI (Swagger UI)
- Maven

---

## 🔐 Autenticação & Credenciais Padrão (Seed Flyway)

O banco é populado automaticamente via migration Flyway (`V1__create_users_and_roles.sql`) com o seguinte usuário inicial:

- **Username:** `admin`
- **Password:** `admin123`
- **Roles:** `ROLE_ADMIN`, `ROLE_USER`

### 🔑 Como Autenticar:
```bash
# 1. Obter tokens de acesso e refresh
curl -X POST http://localhost:8080/api/v1/auth/login \
  -H "Content-Type: application/json" \
  -d '{"username":"admin","password":"admin123"}'

# 2. Consultar perfil autenticado
curl http://localhost:8080/api/v1/me \
  -H "Authorization: Bearer <TOKEN>"
```

---

## 📖 Documentação Interativa (Swagger)
- **Swagger UI:** `http://localhost:8080/swagger-ui.html`
- **OpenAPI JSON:** `http://localhost:8080/api-docs`
- **Healthcheck Actuator:** `http://localhost:8080/actuator/health`

