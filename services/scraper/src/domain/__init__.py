from src.domain.enums import DeliveryTypeEnum, ExecutionStatusEnum, VendorEnum
from src.domain.models import ScrapedListing, ScrapingExecution, SearchQuery
from src.domain.providers import BaseScraperProvider, ProviderFactory
from src.domain.schemas import (
    ExecutionSummaryDTO,
    ScrapedListingDTO,
    ScrapeRequest,
    ScrapeResponse,
)

# Services NÃO são auto-importados aqui para evitar import circular com
# src.core.queues (usado por AsyncScrapeService). Importe explicitamente:
#   from src.domain.services import ScrapingService, AsyncScrapeService, ...

__all__ = [
    "DeliveryTypeEnum",
    "ExecutionStatusEnum",
    "VendorEnum",
    "ScrapedListing",
    "ScrapingExecution",
    "SearchQuery",
    "ExecutionSummaryDTO",
    "ScrapeRequest",
    "ScrapeResponse",
    "ScrapedListingDTO",
    "BaseScraperProvider",
    "ProviderFactory",
]
