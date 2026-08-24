from src.domain.providers.base import BaseScraperProvider, ProviderFactory
# Importa subprovedores para garantir autorregistro na ProviderFactory
import src.domain.providers.olx

__all__ = ["BaseScraperProvider", "ProviderFactory"]
