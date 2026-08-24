# Importa subprovedores para garantir autorregistro na ProviderFactory
import src.domain.providers.olx as _olx  # noqa: F401
from src.domain.providers.base import BaseScraperProvider, ProviderFactory

__all__ = ["BaseScraperProvider", "ProviderFactory"]
