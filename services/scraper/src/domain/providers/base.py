from abc import ABC, abstractmethod
from typing import Dict, List, Tuple, Type

from src.domain.enums import VendorEnum
from src.domain.schemas import ScrapedListingDTO, ScrapeRequest


class BaseScraperProvider(ABC):
    """Classe base abstrata para Provedores de Scraping de Marketplaces."""

    @property
    @abstractmethod
    def vendor(self) -> VendorEnum:
        """Retorna o enum do marketplace."""
        pass

    @abstractmethod
    def build_search_url(self, request: ScrapeRequest, page: int = 1) -> str:
        """Gera a URL canônica de busca para a página informada."""
        pass

    @abstractmethod
    async def scrape(self, request: ScrapeRequest) -> Tuple[List[ScrapedListingDTO], bool]:
        """
        Executa a raspagem.
        Retorna uma tupla: (Lista de anúncios extraídos, booleano indicando se usou fallback de navegador).
        """
        pass


class ProviderFactory:
    """Fábrica e Registro central de Provedores de Scraping."""

    _providers: Dict[VendorEnum, Type[BaseScraperProvider]] = {}

    @classmethod
    def register(cls, vendor: VendorEnum, provider_cls: Type[BaseScraperProvider]) -> None:
        cls._providers[vendor] = provider_cls

    @classmethod
    def get_provider(cls, vendor: VendorEnum) -> BaseScraperProvider:
        provider_cls = cls._providers.get(vendor)
        if not provider_cls:
            raise ValueError(f"Provedor não implementado para o marketplace: {vendor}")
        return provider_cls()
