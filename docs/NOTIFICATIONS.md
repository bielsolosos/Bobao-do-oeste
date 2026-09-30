# 🔔 Sistema de Notificações — BI Scraper

Este documento descreve a arquitetura, padrões de projeto, canais suportados, agendamentos periódicos e guia operacional do **Sistema de Notificações** do ecossistema BI Scraper.

---

## 1. Visão Geral da Arquitetura

O sistema de notificações adota o **Strategy Pattern**, desacoplamento por eventos Spring (`@TransactionalEventListener` assíncrono via `NotificationPublisherService`) e polimorfismo de templates de domínio (`NotificationTemplate`).

```
                              ┌────────────────────────────────────────┐
                              │           Scraping Ingestion           │
                              │     (WebhookScrapperService / AI)      │
                              └──────────────────┬─────────────────────┘
                                                 │
                                                 ▼
                              ┌────────────────────────────────────────┐
                              │      Spring NotificationEvent          │
                              └──────────────────┬─────────────────────┘
                                                 │
                                                 ▼
┌────────────────────────────────────────────────────────────────────────────────────────┐
│ NotificationPublisherService (Core Orchestrator)                                      │
│  - Resolve canais habilitados no UserConfig (DISCORD, EMAIL)                          │
│  - Delega para NotificationStrategySelector                                           │
└───────────────────────┬────────────────────────────────────────┬───────────────────────┘
                        │                                        │
                        ▼                                        ▼
      ┌───────────────────────────────────┐    ┌───────────────────────────────────┐
      │     NotificationDiscordStrategy   │    │      NotificationEmailStrategy    │
      │  (Tempo Real / Webhook Discord)   │    │      (Digest Periódico 4x/dia)    │
      └─────────────────┬─────────────────┘    └─────────────────┬─────────────────┘
                        │                                        │
                        ▼                                        ▼
             [ Discord Webhook API ]                 [ JavaMailSender (SMTP TLS) ]
                                                                 │
                                                                 ▼
                                                      [ Gmail / Destinatário ]
```

### 1.1. Componentes Principais

| Componente | Pacote | Responsabilidade |
|---|---|---|
| `NotificationEvent` | `core.utils` | Objeto de evento imutável contendo `recipient` e `template`. |
| `NotificationTemplate` | `core.utils` | Interface base com métodos polimórficos (`toDiscordPayload()`, `toHtmlEmail()`, `getMessageTemplate()`). |
| `NotificationPublisherService` | `domain.notification.service` | Ouve `NotificationEvent` de forma assíncrona e despacha para cada canal ativo do usuário. |
| `NotificationStrategySelector` | `domain.notification.strategy` | Roteador de estratégias injetadas pelo Spring (`Map<NotificationChannel, NotificationStrategy>`). |
| `NotificationDiscordStrategy` | `domain.notification.strategy.impl` | Formata e envia webhooks para o Discord com embeds ricos. |
| `NotificationEmailStrategy` | `domain.notification.strategy.impl` | Envia e-mails HTML responsivos diretamente via **SMTP** (`JavaMailSender`). |
| `MonitoringEmailDigestService` | `domain.monitoring.service` | Serviço de domínio que agrega oportunidades `HIGH` match e publica o digest. |
| `EmailDigestScheduler` | `infrastructure.scheduling` | Trigger/Cronjob agendado (4x/dia) na camada de infraestrutura que delega ao serviço de domínio. |

---

## 2. Canal Discord (Alertas em Tempo Real)

### 2.1. Funcionamento
Quando um anúncio com tier `HIGH` é identificado durante o processo de scraping e análise por IA, o `WebhookScrapperService` dispara um `NotificationEvent` com o template `MonitoringListingNotificationTemplate`.

### 2.2. Payload do Webhook
O template formata um embed rico contendo:
- **Título & Link:** Link direto para a listagem no marketplace.
- **Preço & Economia:** Valor formatado em BRL e comparação com faixa esperada.
- **Score de Match:** Porcentagem calculada pelo algoritmo/IA com badge colorido.
- **Origem & Localização:** Cidade, Estado e indicador de entrega/frete.
- **Thumbnail:** Imagem principal do produto diretamente no Discord.

### 2.3. Configuração do Usuário
Configurado em `user_configs`:
- `discord_enabled` (`BOOLEAN`): Liga/desliga notificações do Discord.
- `discord_webhook_url` (`VARCHAR`): URL do canal do Discord (ex: `https://discord.com/api/webhooks/...`).

---

## 3. Canal E-mail (Digest Periódico via SMTP)

### 3.1. Digest Periódico 4x ao Dia
O agendador `EmailDigestScheduler` (delegando ao serviço de domínio `MonitoringEmailDigestService`) executa 4 vezes ao dia nos seguintes horários de pico comercial:
- **08:00** (Manhã)
- **12:00** (Almoço)
- **16:00** (Tarde)
- **20:00** (Noite)

**Regras de Negócio do Digest:**
1. Itera sobre todos os usuários que possuem `email_enabled = true` em `user_configs` (utilizando `userConfigRepository.findAllWithUser()` para eager fetch de segurança).
2. Busca listagens no repositório com `matchTier = MatchTier.HIGH` criadas nas **últimas 4 horas** (`firstSeenAt >= now - 4h`).
3. **Zero Spam:** Se nenhuma listagem `HIGH` match for encontrada na janela de 4 horas, o envio do e-mail é automaticamente ignorado.
4. Gera um template HTML moderno e responsivo (`MonitoringEmailDigestNotificationTemplate`) com ícones SVG vetoriais, badges de monitor, limites de exibição (`maxItems = 4`) e links diretos para o painel web.
5. Publica o `NotificationEvent` que é processado de forma assíncrona pelo `NotificationPublisherService`.

### 3.2. Envio Direto via JavaMailSender (SMTP TLS)
O envio é realizado diretamente pelo container Java do `bi-engine` utilizando `spring-boot-starter-mail` conectado ao Gmail SMTP (`smtp.gmail.com:587` com STARTTLS).

---

## 4. Configuração & Variáveis de Ambiente

### 4.1. Configuração no `application.yml` (`services/bi-engine`)

```yaml
spring:
  mail:
    host: ${SPRING_MAIL_HOST:smtp.gmail.com}
    port: ${SPRING_MAIL_PORT:587}
    username: ${SPRING_MAIL_USERNAME:fatiarapidaautomation@gmail.com}
    password: ${SPRING_MAIL_PASSWORD:}
    properties:
      mail:
        smtp:
          auth: true
          starttls:
            enable: true
            required: true

biscraper:
  app-url: ${APP_FRONTEND_URL:https://bi.bielsolosos.dev.br}
  email:
    enabled: ${EMAIL_NOTIFICATIONS_ENABLED:true}
    from: ${EMAIL_FROM:BI Scraper <fatiarapidaautomation@gmail.com>}
    digest:
      cron: ${EMAIL_DIGEST_CRON:0 0 8,12,16,20 * * *}
      window-hours: ${EMAIL_DIGEST_WINDOW_HOURS:4}
      max-items: ${EMAIL_DIGEST_MAX_ITEMS:4}
```

### 4.2. Tabela de Variáveis de Ambiente

| Variável | Padrão | Descrição |
|---|---|---|
| `SPRING_MAIL_HOST` | `smtp.gmail.com` | Host SMTP para envio direto. |
| `SPRING_MAIL_PORT` | `587` | Porta SMTP com STARTTLS. |
| `SPRING_MAIL_USERNAME` | `fatiarapidaautomation@gmail.com` | Usuário/E-mail do remetente Gmail. |
| `SPRING_MAIL_PASSWORD` | *(vazio)* | Senha de App do Gmail (16 dígitos). |
| `EMAIL_NOTIFICATIONS_ENABLED` | `true` | Ativação global do subsistema de e-mails. |
| `EMAIL_FROM` | `BI Scraper <fatiarapidaautomation@gmail.com>` | Cabeçalho "From" dos e-mails enviados. |
| `EMAIL_DIGEST_CRON` | `0 0 8,12,16,20 * * *` | Expressão cron para disparo do digest 4x/dia. |
| `EMAIL_DIGEST_WINDOW_HOURS` | `4` | Janela retroativa em horas para buscar listagens `HIGH`. |
| `EMAIL_DIGEST_MAX_ITEMS` | `4` | Quantidade máxima de anúncios exibidos no corpo do e-mail. |
| `APP_FRONTEND_URL` | `https://bi.bielsolosos.dev.br` | URL base da aplicação para links nos e-mails. |

---

## 5. Banco de Dados & Histórico de Logs

### 5.1. Tabela `user_configs`
Contém as preferências e credenciais de notificação de cada usuário:
```sql
ALTER TABLE user_configs
    ADD COLUMN discord_webhook_url VARCHAR(500),
    ADD COLUMN discord_enabled     BOOLEAN NOT NULL DEFAULT TRUE,
    ADD COLUMN email_enabled       BOOLEAN NOT NULL DEFAULT FALSE;
```

### 5.2. Tabela `notification_logs`
Registra a rastreabilidade e auditoria de cada notificação disparada:
```sql
CREATE TABLE notification_logs (
    id          BIGSERIAL PRIMARY KEY,
    recipient_id UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    channel     VARCHAR(25) NOT NULL,
    description TEXT,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT NOW()
);
```

---

## 6. Como Testar

### 6.1. Executar Testes Unitários e de Integração
Para rodar toda a suíte de testes de notificações no backend:
```bash
cd services/bi-engine
./mvnw test -Dtest=*Notification*,*Email*,*Discord*
```

