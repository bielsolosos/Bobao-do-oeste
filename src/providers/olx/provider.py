"""
Provedor de Scraping da OLX Brasil (OlxScraperProvider).

Esta classe implementa o contrato `BaseScraperProvider` para a OLX.
Ela integra os quatro pilares do scraping:
1. `OlxUrlBuilder`: Monta a URL de busca paginada.
2. `SmartHttpClient`: Executa a requisição rápida via `curl_cffi` (Tier 1).
3. `PlaywrightBrowserFallback`: Acionado automaticamente se a requisição rápida for bloqueada (Tier 2).
4. `OlxPayloadParser`: Extrai os anúncios e os transforma em `ScrapedListingDTO`.
"""

from typing import List, Tuple
from src.core.config import settings
from src.core.logger import logger
from src.domain.enums import VendorEnum
from src.domain.schemas import ScrapeRequest, ScrapedListingDTO
from src.engine.browser_fallback import PlaywrightBrowserFallback
from src.engine.http_client import HttpClientBlockedException, SmartHttpClient
from src.providers.base import BaseScraperProvider
from src.providers.olx.parser import OlxPayloadParser
from src.providers.olx.url_builder import OlxUrlBuilder


class OlxScraperProvider(BaseScraperProvider):
    """
    Implementação concreta do scraper da OLX Brasil com suporte a fallback e paginação.
    """

    def __init__(self):
        self.http_client = SmartHttpClient()
        self.browser_fallback = PlaywrightBrowserFallback()

    @property
    def vendor(self) -> VendorEnum:
        """Identificador do marketplace."""
        return VendorEnum.OLX

    def build_search_url(self, request: ScrapeRequest, page: int = 1) -> str:
        """Gera a URL canônica para a página indicada."""
        return OlxUrlBuilder.build(request, page=page)

    async def scrape(self, request: ScrapeRequest) -> Tuple[List[ScrapedListingDTO], bool]:
        """
        Executa a raspagem de 1 ou mais páginas de resultados da OLX.

        Parâmetros:
            request (ScrapeRequest): Configuração e filtros da busca.

        Retorna:
            Tuple[List[ScrapedListingDTO], bool]:
                - Lista contendo todos os anúncios coletados.
                - Booleano indicando se o Playwright (fallback) precisou ser utilizado.
        """
        all_listings: List[ScrapedListingDTO] = []
        used_fallback = False

        for page in range(1, request.max_pages + 1):
            target_url = self.build_search_url(request, page=page)
            logger.info(f"[OLX Provider] Scraping page {page}/{request.max_pages}: {target_url}")

            html_content = ""

            # Caso o usuário solicite forçar o Playwright explicitamente
            if request.force_browser:
                logger.info("[OLX Provider] force_browser is True, using Playwright directly.")
                html_content = await self.browser_fallback.get_page_content(target_url)
                used_fallback = True
            else:
                try:
                    # Tier 1: Requisição rápida HTTP com TLS do Chrome 120
                    html_content = await self.http_client.get(target_url)
                except HttpClientBlockedException as e:
                    logger.warning(f"[OLX Provider] Fast HTTP blocked: {e}")
                    if settings.ENABLE_PLAYWRIGHT_FALLBACK:
                        logger.info("[OLX Provider] Initiating Playwright stealth fallback...")
                        html_content = await self.browser_fallback.get_page_content(target_url)
                        used_fallback = True
                    else:
                        raise

            # Extração dos anúncios do HTML
            page_listings = OlxPayloadParser.parse_html(html_content)
            if not page_listings:
                logger.warning(f"[OLX Provider] No listings extracted from page {page}")
                break

            all_listings.extend(page_listings)

        return all_listings, used_fallback
