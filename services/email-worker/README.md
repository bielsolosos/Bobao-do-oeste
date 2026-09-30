# BI Scraper - Email Worker (Cloudflare Worker)

Microserviço serverless em Cloudflare Worker para disparo de e-mails transacionais e resumos periódicos do BI Scraper.

## Recursos
- **Endpoint HTTP:** `POST /send-email` (ou `/api/v1/email/send`)
- **Autenticação:** Protegido via header `x-auth-token` ou `Authorization: Bearer <AUTH_SECRET>`
- **Transporte:** Envio seguro via SMTP Gmail (`smtp.gmail.com:465`)
- **Health Check:** `GET /health`

## Variáveis de Ambiente / Secrets (Wrangler / Cloudflare Dashboard)
- `AUTH_SECRET`: Chave secreta de autenticação entre o `bi-engine` e o Worker.
- `GMAIL_USER`: E-mail remetente (`fatiarapidaautomation@gmail.com`).
- `GMAIL_APP_PASSWORD`: Senha de aplicativo do Gmail gerada para a automação.
- `FROM_NAME`: Nome de exibição do remetente (ex: `BI Scraper Notificações`).

## Desenvolvimento Local & Deploy
```bash
# Instalar dependências
npm install

# Rodar localmente na porta 8787
npm run dev

# Deploy para Cloudflare Workers
npm run deploy
```

## Exemplo de Requisição (cURL)
```bash
curl -X POST http://localhost:8787/send-email \
  -H "Content-Type: application/json" \
  -H "x-auth-token: bi-scraper-email-secret-token" \
  -d '{
    "to": "destinatario@email.com",
    "subject": "🎯 Resumo de Anúncios - BI Scraper",
    "html": "<h1>Novos anúncios encontrados!</h1>"
  }'
```
