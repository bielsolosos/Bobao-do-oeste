"""
Provedor de Scraping da OLX Brasil (OlxScraperProvider).
"""

from typing import List, Tuple

from src.core.config import settings
from src.core.engine import (
    HttpClientBlockedException,
    PlaywrightBrowserFallback,
    SmartHttpClient,
)
from src.core.logger import logger
from src.domain.enums import VendorEnum
from src.domain.providers.base import BaseScraperProvider, ProviderFactory
from src.domain.providers.olx.parser import OlxPayloadParser
from src.domain.providers.olx.url_builder import OlxUrlBuilder
from src.domain.schemas import ScrapedListingDTO, ScrapeRequest


class OlxScraperProvider(BaseScraperProvider):
    """Implementação do scraper para a OLX Brasil."""

    def __init__(self):
        self.http_client = SmartHttpClient()
        self.browser_fallback = PlaywrightBrowserFallback()

    @property
    def vendor(self) -> VendorEnum:
        return VendorEnum.OLX

    def build_search_url(self, request: ScrapeRequest, page: int = 1) -> str:
        return OlxUrlBuilder.build(request, page=page)

    async def scrape(self, request: ScrapeRequest) -> Tuple[List[ScrapedListingDTO], bool]:
        all_listings: List[ScrapedListingDTO] = []
        used_fallback = False

        for page in range(1, request.max_pages + 1):
            target_url = self.build_search_url(request, page=page)
            logger.info(f"[OLX Provider] Scraping page {page}/{request.max_pages}: {target_url}")

            html_content = ""

            if request.force_browser:
                logger.info("[OLX Provider] force_browser is True, using Playwright directly.")
                html_content = await self.browser_fallback.get_page_content(target_url)
                used_fallback = True
            else:
                try:
                    html_content = await self.http_client.get(target_url)
                except HttpClientBlockedException as e:
                    logger.warning(f"[OLX Provider] Fast HTTP blocked: {e}")
                    if settings.ENABLE_PLAYWRIGHT_FALLBACK:
                        logger.info("[OLX Provider] Initiating Playwright stealth fallback...")
                        html_content = await self.browser_fallback.get_page_content(target_url)
                        used_fallback = True
                    else:
                        raise

            page_listings = OlxPayloadParser.parse_html(html_content)
            if not page_listings:
                logger.warning(f"[OLX Provider] No listings extracted from page {page}")
                break

            all_listings.extend(page_listings)

        return all_listings, used_fallback


# Registra automaticamente o provedor na Factory
ProviderFactory.register(VendorEnum.OLX, OlxScraperProvider)
