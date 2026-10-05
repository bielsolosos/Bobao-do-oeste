# BI Engine Service ☕ (Java / Spring Boot)

Serviço responsável por Business Intelligence, análise de mercado, auditoria de IA, orquestração de coletas e algoritmos de recomendação de hardware usado.

## 🎯 Responsabilidades
- Ingestão de Webhooks enviados pelo `services/scraper` (`POST /api/v1/webhooks/scraper`)
- Armazenamento de dados analíticos no PostgreSQL com versionamento Flyway
- Análise de anúncios via Inteligência Artificial (Google Gemini e DeepSeek via Spring AI) com estratégias `SIMPLE` e especializada `NOTEBOOK`
- Integração com deep scraping via Function Calling / Tools (`ScrappingDetailsTools`)
- Notificações automáticas de novos anúncios em canais como Discord Webhook e E-mail Digest
- **Autenticação Híbrida Desacoplada:** Suporte a Senha e Código OTP por E-mail (MFA Passwordless condicional via `@ConditionalOnExpression`)
- **Sistema de Onboarding Fechado por Convites (User Invites):** Emissão administrativa, expiração de 48h, envio de link exclusivo por e-mail e auto-login
- Exposição de APIs REST completas, métricas e telemetria para consumo pela SPA Frontend (`apps/web/project-ui`)

## 🛠️ Tecnologias
- Java 21 / Spring Boot 4.1.1 / Spring AI 2.0.1
- PostgreSQL / Flyway
- Spring Security + JJWT 0.12.6
- Spring Mail (JavaMailSender)
- Springdoc OpenAPI (Swagger UI)
- Micrometer + Prometheus
- Maven

---

## 🔐 Autenticação, Convites & Credenciais Padrão (Seed Flyway)

O banco é populado automaticamente via migration Flyway (`V1__create_users_and_roles.sql`) com o seguinte usuário inicial:

- **Username:** `admin`
- **Password:** `admin123`
- **Roles:** `ROLE_ADMIN`, `ROLE_USER`

Para detalhes arquiteturais completos, consulte [docs/ARCHITECTURE_AUTH_HYBRID.md](../../docs/ARCHITECTURE_AUTH_HYBRID.md).

### 🔑 Modos de Autenticação:
1. **Tradicional:** `POST /api/v1/auth/login` (username e password).
2. **OTP por E-mail:** `POST /api/v1/auth/otp/send` e `POST /api/v1/auth/otp/verify`.
3. **Ativação por Convite:** `GET /api/v1/auth/invites/validate` e `POST /api/v1/auth/invites/accept`.

```bash
# 1. Obter tokens de acesso e refresh via senha
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
- **OpenAPI JSON:** `http://localhost:8080/docs`
- **Healthcheck Actuator:** `http://localhost:8080/actuator/health`
- **Prometheus Metrics:** `http://localhost:8080/actuator/prometheus`


