from abc import ABC, abstractmethod
from typing import List, Tuple
from src.domain.enums import VendorEnum
from src.domain.schemas import ScrapeRequest, ScrapedListingDTO


class BaseScraperProvider(ABC):
    """Abstract Base Class for Marketplace Scrapers."""

    @property
    @abstractmethod
    def vendor(self) -> VendorEnum:
        """Returns the vendor/marketplace enum."""
        pass

    @abstractmethod
    def build_search_url(self, request: ScrapeRequest) -> str:
        """Constructs the canonical marketplace search URL from the request parameters."""
        pass

    @abstractmethod
    async def scrape(self, request: ScrapeRequest) -> Tuple[List[ScrapedListingDTO], bool]:
        """
        Executes the scraping process.
        Returns a tuple: (List of scraped listings, boolean flag indicating if fallback was used).
        """
        pass
