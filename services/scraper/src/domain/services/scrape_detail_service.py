"""
Serviço de Extração Profunda de Anúncio Único (ScrapeDetailService).
"""

import re
from datetime import datetime, timedelta, timezone
from typing import Optional

from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import select

from src.core.config import settings
from src.core.engine import HttpClientBlockedException, PlaywrightBrowserFallback, SmartHttpClient
from src.core.logger import logger
from src.domain.enums import VendorEnum
from src.domain.models import AdDetailCache
from src.domain.providers.olx.parser import OlxPayloadParser
from src.domain.schemas import ScrapeDetailRequest, ScrapeDetailResponse, ScrapedListingDetailDTO
from src.domain.services.image_cache_service import ImageCacheService


class ScrapeDetailService:
    """Orquestra o scraping detalhado de um anúncio individual e o cache de suas imagens."""

    def __init__(self, session: AsyncSession):
        self.session = session
        self.http_client = SmartHttpClient()
        self.browser_fallback = PlaywrightBrowserFallback()
        self.image_cache_service = ImageCacheService(session)

    async def execute(self, request: ScrapeDetailRequest) -> ScrapeDetailResponse:
        url = str(request.url).strip()
        vendor = request.vendor or VendorEnum.OLX
        now = datetime.now(timezone.utc)

        # 1. Extração do vendor_listing_id a partir da URL
        id_match = re.search(r"-(\d{8,12})(?:\?|$)", url)
        listing_id = id_match.group(1) if id_match else ""

        # 2. Verificação de Cache ativo (se listing_id foi identificado)
        if listing_id:
            stmt = select(AdDetailCache).where(
                AdDetailCache.vendor == vendor,
                AdDetailCache.vendor_listing_id == listing_id,
                AdDetailCache.expires_at > now,
            )
            res = await self.session.execute(stmt)
            cached_detail_record = res.scalars().first()

            if cached_detail_record:
                logger.info(f"Listing detail cache hit for {listing_id}. Retrieving cached response.")
                payload = cached_detail_record.parsed_payload
                dto = ScrapedListingDetailDTO.model_validate(payload)

                # Anexa imagens cacheadas existentes
                cached_imgs = await self.image_cache_service.get_cached_images_for_listing(vendor, listing_id)
                dto.cached_images = cached_imgs

                return ScrapeDetailResponse(
                    success=True,
                    from_cache=True,
                    used_fallback=False,
                    data=dto,
                )

        # 3. Execução de Scraping HTTP / Fallback
        used_fallback = False
        html_content = ""

        if request.force_browser:
            logger.info(f"force_browser is True, scraping ad page via Playwright: {url}")
            html_content = await self.browser_fallback.get_page_content(url)
            used_fallback = True
        else:
            try:
                html_content = await self.http_client.get(url)
            except HttpClientBlockedException as e:
                logger.warning(f"Fast HTTP blocked for ad detail: {e}")
                if settings.ENABLE_PLAYWRIGHT_FALLBACK:
                    logger.info("Initiating Playwright stealth fallback for ad detail...")
                    html_content = await self.browser_fallback.get_page_content(url)
                    used_fallback = True
                else:
                    raise

        # 4. Parseamento da página de detalhe
        detail_dto: Optional[ScrapedListingDetailDTO] = None
        if vendor == VendorEnum.OLX:
            detail_dto = OlxPayloadParser.parse_ad_detail_html(html_content, url)

        if not detail_dto:
            logger.error(f"Failed to extract ad detail from {url}")
            return ScrapeDetailResponse(
                success=False,
                from_cache=False,
                used_fallback=used_fallback,
                error_message="Não foi possível extrair os dados da página do anúncio.",
            )

        effective_listing_id = detail_dto.vendor_listing_id or listing_id

        # 5. Download e Cache de Imagens no SQLite se solicitado
        if request.download_images and detail_dto.images:
            cached_images = await self.image_cache_service.cache_images_for_listing(
                vendor=vendor,
                vendor_listing_id=effective_listing_id,
                image_urls=detail_dto.images,
                ttl_hours=request.ttl_hours,
            )
            detail_dto.cached_images = cached_images

        # 6. Salva o Cache de Detalhes no SQLite
        expires_at = now + timedelta(hours=request.ttl_hours)
        detail_cache = AdDetailCache(
            vendor=vendor,
            vendor_listing_id=effective_listing_id,
            url=url,
            parsed_payload=detail_dto.model_dump(mode="json"),
            created_at=now,
            expires_at=expires_at,
        )
        self.session.add(detail_cache)
        await self.session.commit()

        return ScrapeDetailResponse(
            success=True,
            from_cache=False,
            used_fallback=used_fallback,
            data=detail_dto,
        )
