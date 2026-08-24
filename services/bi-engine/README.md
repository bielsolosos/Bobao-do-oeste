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
- Maven / Gradle
