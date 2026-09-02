import base64
from datetime import datetime, timedelta, timezone
from unittest.mock import AsyncMock, patch

import pytest
from httpx import ASGITransport, AsyncClient
from sqlmodel import col, select

from src.core.config import settings
from src.core.database import async_session_maker
from src.domain.enums import DeliveryTypeEnum, VendorEnum
from src.domain.models import AdImageCache
from src.domain.providers.olx.parser import OlxPayloadParser
from src.domain.services.image_cache_service import ImageCacheService
from src.main import app

auth_str = f"{settings.BASIC_AUTH_USERNAME}:{settings.BASIC_AUTH_PASSWORD}"
auth_header = {"Authorization": f"Basic {base64.b64encode(auth_str.encode()).decode()}"}


@pytest.mark.asyncio
async def test_olx_payload_parser_ad_detail():
    mock_html = """
    <!DOCTYPE html>
    <html>
      <head>
        <script id="__NEXT_DATA__" type="application/json">
        {
          "props": {
            "pageProps": {
              "ad": {
                "listId": "1389472918",
                "subject": "Notebook Dell G15 RTX 3050 16GB",
                "body": "Notebook em excelente estado, acompanha carregador original e nota fiscal.",
                "priceValue": "R$ 3.500",
                "oldPrice": "R$ 4.000",
                "olxPay": true,
                "location": {
                  "uf": "SP",
                  "municipality": "São Paulo",
                  "neighbourhood": "Pinheiros"
                },
                "images": [
                  {"original": "https://img.olx.com.br/images/99/9912345678.jpg"},
                  {"original": "https://img.olx.com.br/images/99/9987654321.jpg"}
                ],
                "properties": [
                  {"label": "Memória RAM", "value": "16 GB"},
                  {"label": "Armazenamento", "value": "512 GB SSD"},
                  {"label": "Condição", "value": "Usado"}
                ],
                "user": {
                  "name": "Gabriel",
                  "memberSince": "2020-01-01"
                }
              }
            }
          }
        }
        </script>
      </head>
      <body></body>
    </html>
    """

    url = "https://sp.olx.com.br/sao-paulo-e-regiao/informatica-e-acessorios/notebooks/notebook-dell-g15-1389472918"
    detail = OlxPayloadParser.parse_ad_detail_html(mock_html, url)

    assert detail is not None
    assert detail.vendor_listing_id == "1389472918"
    assert detail.title == "Notebook Dell G15 RTX 3050 16GB"
    assert detail.price == 3500.0
    assert detail.original_price == 4000.0
    assert detail.has_delivery is True
    assert detail.delivery_type == DeliveryTypeEnum.OLX_PAY
    assert len(detail.images) == 2
    assert detail.properties.get("Memória RAM") == "16 GB"
    assert detail.properties.get("Condição") == "Usado"
    assert detail.seller_name == "Gabriel"


@pytest.mark.asyncio
async def test_image_cache_service_and_expiration():
    async with async_session_maker() as session:
        service = ImageCacheService(session)

        # Mock downloading image bytes
        fake_image_bytes = b"\xff\xd8\xff\xe0\x00\x10JFIF"
        with patch.object(
            service.http_client, "get_bytes", new=AsyncMock(return_value=(fake_image_bytes, "image/jpeg"))
        ):
            cached_dtos = await service.cache_images_for_listing(
                vendor=VendorEnum.OLX,
                vendor_listing_id="test_listing_123",
                image_urls=["https://img.olx.com.br/img1.jpg"],
                ttl_hours=1,
            )

            assert len(cached_dtos) == 1
            image_id = cached_dtos[0].id

            # Recupera a imagem
            retrieved = await service.get_image_by_id(image_id)
            assert retrieved is not None
            img_bytes, mime = retrieved
            assert img_bytes == fake_image_bytes
            assert mime == "image/jpeg"

            # Simula expiração alterando o expires_at para o passado
            stmt = select(AdImageCache).where(col(AdImageCache.id) == image_id)
            res = await session.execute(stmt)
            record = res.scalars().first()
            assert record is not None
            record.expires_at = datetime.now(timezone.utc) - timedelta(hours=2)
            session.add(record)
            await session.commit()

            # Tenta recuperar após expirado
            retrieved_expired = await service.get_image_by_id(image_id)
            assert retrieved_expired is None

            # Executa rotina de cleanup
            deleted = await service.cleanup_expired()
            assert deleted >= 1


@pytest.mark.asyncio
async def test_scrape_detail_api_endpoint():
    mock_html = """
    <!DOCTYPE html>
    <html>
      <head>
        <script id="__NEXT_DATA__" type="application/json">
        {
          "props": {
            "pageProps": {
              "ad": {
                "listId": "9876543210",
                "subject": "MacBook Air M1 8GB 256GB",
                "body": "MacBook impecável bateria 95%.",
                "priceValue": "4200",
                "olxPay": false,
                "location": {"uf": "RJ", "municipality": "Rio de Janeiro"},
                "images": ["https://img.olx.com.br/mac1.jpg"],
                "properties": [{"label": "Processador", "value": "Apple M1"}]
              }
            }
          }
        }
        </script>
      </head>
      <body></body>
    </html>
    """

    fake_bytes = b"FAKE_JPEG_IMAGE_BYTES"

    transport = ASGITransport(app=app)
    async with AsyncClient(transport=transport, base_url="http://test") as client:
        with (
            patch("src.core.engine.http_client.SmartHttpClient.get", new=AsyncMock(return_value=mock_html)),
            patch(
                "src.core.engine.http_client.SmartHttpClient.get_bytes",
                new=AsyncMock(return_value=(fake_bytes, "image/jpeg")),
            ),
        ):
            resp = await client.post(
                "/api/v1/scrape/detail",
                headers=auth_header,
                json={
                    "url": "https://rj.olx.com.br/rio-de-janeiro-e-regiao/macbook-air-m1-9876543210",
                    "download_images": True,
                    "ttl_hours": 6,
                },
            )

            assert resp.status_code == 200
            data = resp.json()
            assert data["success"] is True
            assert data["data"]["title"] == "MacBook Air M1 8GB 256GB"
            assert data["data"]["price"] == 4200.0
            assert len(data["data"]["cached_images"]) == 1

            image_id = data["data"]["cached_images"][0]["id"]

            # Test image retrieval endpoint (imagens cacheadas são públicas para consumo do backend/frontend)
            img_resp = await client.get(f"/api/v1/scrape/images/{image_id}", headers=auth_header)
            assert img_resp.status_code == 200
            assert img_resp.content == fake_bytes
            assert img_resp.headers["content-type"] == "image/jpeg"


@pytest.mark.asyncio
async def test_cache_cleanup_worker_lifecycle():
    from src.core.workers.cache_cleanup import CacheCleanupWorker

    worker = CacheCleanupWorker(interval_seconds=3600)
    await worker.start()
    assert worker._task is not None
    assert not worker._task.done()

    # Executa uma rodada manual de limpeza
    deleted = await worker.run_once()
    assert isinstance(deleted, int)

    await worker.stop()
    assert worker._stop_event.is_set()
