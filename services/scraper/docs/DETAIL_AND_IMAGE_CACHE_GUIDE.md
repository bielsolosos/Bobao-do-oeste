# Guia de Deep Scraping & Cache de Imagens com TTL no SQLite 🖼️💾

Este documento descreve a arquitetura, o fluxo de dados e os mecanismos de retenção/expiração da funcionalidade de **Raspagem Profunda de Anúncios Individuais (Deep Scrape)** e do **Sistema de Cache de Imagens e Detalhes com TTL no SQLite**.

---

## 1. Visão Geral e Motivação

Ao raspar a página de listagem/busca de um marketplace (como a OLX), muitas informações cruciais estão ausentes ou resumidas (descrições truncadas, poucas miniaturas de imagens, ausência da tabela de especificações detalhadas).

O módulo de **Deep Scraping & Image Cache** permite:
1. **Extração Profunda por URL:** Coleta completa da descrição, especificações técnicas (RAM, processador, SSD, condição), dados do vendedor e galeria inteira de fotos em alta resolução.
2. **Download e Cache Local de Imagens:** Baixa os bytes das fotos contornando proteções de CDN (passando headers adequados e TLS impersonation) e armazena os binários em SQLite local.
3. **Expiração Automática com TTL (Time-To-Live):** Garante que imagens e dados fiquem disponíveis para o backend/IA sem link quebrado, e depois sejam purgados automaticamente sem inflar o disco.

---

## 2. Diagrama de Arquitetura e Fluxo de Execução

```
[ POST /api/v1/scrape/detail ]
  { "url": "https://sp.olx.com.br/.../anuncio-123", "download_images": true, "ttl_hours": 12 }
                 │
                 ▼
 ┌───────────────────────────────────────────────┐
 │ 1. Checa Cache de Detalhe no SQLite           │
 │    (WHERE listing_id = :id AND expires_at > NOW)
 └───────┬───────────────────────────────┬───────┘
         │                               │
    (Cache Hit)                     (Cache Miss)
         │                               │
         ▼                               ▼
  Retorna DTO Cacheado         ┌───────────────────────────────────────┐
  + Imagens Válidas            │ 2. SmartHttpClient (curl_cffi)        │
                               │    (Fallback para Playwright Stealth) │
                               └──────────────────┬────────────────────┘
                                                  ▼
                               ┌───────────────────────────────────────┐
                               │ 3. OlxPayloadParser.parse_ad_detail   │
                               │    - Descrição completa na íntegra    │
                               │    - Tabela de specs (properties)     │
                               │    - Galeria de fotos em alta res     │
                               └──────────────────┬────────────────────┘
                                                  ▼
                               ┌───────────────────────────────────────┐
                               │ 4. ImageCacheService (Download bytes) │
                               │    - Baixa bytes com headers OLX      │
                               │    - Salva na tabela ad_image_cache   │
                               │    - Define expires_at = now + TTL    │
                               └──────────────────┬────────────────────┘
                                                  ▼
                               ┌───────────────────────────────────────┐
                               │ 5. Salva AdDetailCache com TTL        │
                               └──────────────────┬────────────────────┘
                                                  ▼
                               ┌───────────────────────────────────────┐
                               │ 6. Retorna ScrapeDetailResponse       │
                               └───────────────────────────────────────┘
```

---

## 3. Mecanismo de TTL (Como funciona a Expiração)

A expiração ocorre em **duas camadas sincronizadas**:

### A. Bloqueio Lógico na Leitura (Soft Expiration)
Todas as consultas SQL aos caches filtram estritamente registros válidos:
```sql
SELECT * FROM ad_image_cache 
WHERE id = :image_id AND expires_at > CURRENT_TIMESTAMP;
```
- Se o TTL expirou, a API retorna **`404 Not Found`** para a imagem ou aciona um novo scraping atualizado.

### B. Exclusão Física no Banco (Hard Deletion & Cron Worker)
Para evitar acúmulo de bytes no arquivo do SQLite, o sistema executa a remoção física:
```sql
DELETE FROM ad_image_cache WHERE expires_at <= CURRENT_TIMESTAMP;
DELETE FROM ad_detail_cache WHERE expires_at <= CURRENT_TIMESTAMP;
```

O **`CacheCleanupWorker`** (`src/core/workers/cache_cleanup.py`) roda como uma background task no ciclo de vida do FastAPI (`lifespan`), executando a limpeza automaticamente de hora em hora.

---

## 4. Modelos de Dados (SQLite)

### `ad_image_cache`
| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único da imagem no cache |
| `vendor` | Enum | Marketplace de origem (ex: `OLX`) |
| `vendor_listing_id` | String (Index) | ID nativo do anúncio no marketplace |
| `image_index` | Integer | Posição ordinal da foto na galeria (0, 1, 2...) |
| `original_url` | Text | URL pública original no CDN |
| `image_bytes` | BLOB (LargeBinary) | Conteúdo binário bruto da imagem |
| `mime_type` | String | MIME type (ex: `image/jpeg`, `image/webp`) |
| `size_bytes` | Integer | Tamanho do arquivo em bytes |
| `created_at` | DateTime (UTC) | Data/hora do download |
| `expires_at` | DateTime (UTC, Index) | Data/hora exata em que o TTL expira |

### `ad_detail_cache`
| Campo | Tipo | Descrição |
|---|---|---|
| `id` | UUID (PK) | Identificador único do registro de cache |
| `vendor` | Enum | Marketplace de origem (`OLX`) |
| `vendor_listing_id` | String (Index) | ID nativo do anúncio |
| `url` | Text | URL da página do anúncio |
| `parsed_payload` | JSON | JSON completo estruturado com specs, descrição e metadados |
| `created_at` | DateTime (UTC) | Data/hora da coleta |
| `expires_at` | DateTime (UTC, Index) | Data/hora de expiração do cache de detalhe |

---

## 5. Endpoints da API

### `POST /api/v1/scrape/detail`
Executa o Deep Scrape de um anúncio individual.

**Request Body:**
```json
{
  "url": "https://sp.olx.com.br/sao-paulo-e-regiao/informatica-e-acessorios/notebooks/notebook-dell-g15-1389472918",
  "vendor": "OLX",
  "download_images": true,
  "ttl_hours": 12,
  "force_browser": false
}
```

**Response Body (`200 OK`):**
```json
{
  "success": true,
  "from_cache": false,
  "used_fallback": false,
  "data": {
    "vendor": "OLX",
    "vendor_listing_id": "1389472918",
    "url": "https://sp.olx.com.br/.../notebook-dell-g15-1389472918",
    "title": "Notebook Dell G15 RTX 3050 16GB",
    "price": 3500.0,
    "original_price": 4000.0,
    "description": "Notebook em perfeito estado, usado para trabalho e jogos...",
    "state": "SP",
    "city": "São Paulo",
    "neighborhood": "Pinheiros",
    "has_delivery": true,
    "delivery_type": "OLX_PAY",
    "properties": {
      "Memória RAM": "16 GB",
      "Armazenamento": "512 GB SSD",
      "Processador": "Intel Core i7",
      "Condição": "Usado"
    },
    "images": [
      "https://img.olx.com.br/images/99/9912345678.jpg"
    ],
    "cached_images": [
      {
        "id": "a6f8b9e2-9d32-4e89-8d7b-123456789abc",
        "image_index": 0,
        "original_url": "https://img.olx.com.br/images/99/9912345678.jpg",
        "endpoint_url": "/api/v1/scrape/images/a6f8b9e2-9d32-4e89-8d7b-123456789abc",
        "mime_type": "image/jpeg",
        "size_bytes": 154320,
        "expires_at": "2026-09-02T05:50:00Z"
      }
    ],
    "seller_name": "Gabriel",
    "seller_info": {
      "user_id": "99887766",
      "member_since": "2020-01-01",
      "verified": true
    },
    "published_at": "2026-09-01T14:30:00Z",
    "scraped_at": "2026-09-01T17:50:00Z"
  },
  "error_message": null
}
```

---

### `GET /api/v1/scrape/images/{image_id}`
Recupera o arquivo binário da imagem do cache local SQLite.

- **Headers de Resposta:** `Content-Type: image/jpeg` (ou `image/webp`), `Cache-Control: public, max-age=86400`.
- **Status `404 Not Found`:** Retornado caso o ID não exista ou já tenha expirado.

---

### `POST /api/v1/scrape/images/cleanup`
Endpoint para acionar manualmente uma rodada de limpeza de todos os registros e fotos expiradas no SQLite.

**Response (`200 OK`):**
```json
{
  "success": true,
  "deleted_images": 14
}
```

---

## 6. Configurações de Ambiente

Variáveis disponíveis no arquivo `.env` ou nas configurações (`src/core/config.py`):

| Variável | Padrão | Descrição |
|---|---|---|
| `CACHE_CLEANUP_INTERVAL_SECONDS` | `3600` | Intervalo em segundos entre cada rodada do cron worker de limpeza (1 hora) |
| `DEFAULT_TIMEOUT_SECONDS` | `15` | Timeout para download de páginas e imagens |
| `ENABLE_PLAYWRIGHT_FALLBACK` | `true` | Ativa navegador Playwright Stealth caso o HTTP rápido tome 403 |
