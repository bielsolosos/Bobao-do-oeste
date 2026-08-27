"""
Serviço de Scraping de Marketplaces (ScrapingService).
Orquestra o ciclo de vida completo de uma requisição de raspagem e persistência.
"""

import time
from datetime import datetime, timezone
from typing import List

from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import col, select

from src.core.logger import logger
from src.domain.enums import ExecutionStatusEnum
from src.domain.models import ScrapedListing, ScrapingExecution, SearchQuery
from src.domain.providers import ProviderFactory
from src.domain.schemas import ExecutionSummaryDTO, ScrapedListingDTO, ScrapeRequest, ScrapeResponse


class ScrapingService:
    """Serviço responsável por coordenar a coleta, telemetria e persistência dos dados de scraping."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def execute_scrape(self, request: ScrapeRequest) -> ScrapeResponse:
        start_time = time.perf_counter()
        started_at = datetime.now(timezone.utc)

        # 1. Localiza ou cria o registro da SearchQuery
        search_query = await self._get_or_create_search_query(request)

        # 2. Registra o início da execução
        execution = ScrapingExecution(
            search_query_id=search_query.id,
            vendor=request.vendor,
            started_at=started_at,
            status=ExecutionStatusEnum.RUNNING,
        )
        self.session.add(execution)
        await self.session.commit()
        await self.session.refresh(execution)

        try:
            # 3. Resolve o provedor e executa a coleta
            provider = ProviderFactory.get_provider(request.vendor)
            scraped_items, used_fallback = await provider.scrape(request)

            # 4. Grava/atualiza os anúncios no banco em lote
            new_items_count = await self._persist_listings(execution.id, scraped_items)

            # 5. Finaliza a execução com status SUCCESS
            finished_at = datetime.now(timezone.utc)
            duration_ms = int((time.perf_counter() - start_time) * 1000)

            execution.status = ExecutionStatusEnum.SUCCESS
            execution.finished_at = finished_at
            execution.duration_ms = duration_ms
            execution.total_found = len(scraped_items)
            execution.new_items_count = new_items_count
            execution.used_fallback = used_fallback

            self.session.add(execution)
            await self.session.commit()
            await self.session.refresh(execution)

            logger.info(
                f"Scraping execution {execution.id} finished successfully. "
                f"Found {len(scraped_items)} items ({new_items_count} new) in {duration_ms}ms"
            )

            return ScrapeResponse(
                success=True,
                execution=ExecutionSummaryDTO.model_validate(execution),
                items=scraped_items,
            )

        except Exception as e:
            await self.session.rollback()
            logger.error(f"Scraping execution {execution.id} failed: {e}", exc_info=True)
            finished_at = datetime.now(timezone.utc)
            duration_ms = int((time.perf_counter() - start_time) * 1000)

            execution.status = ExecutionStatusEnum.FAILED
            execution.finished_at = finished_at
            execution.duration_ms = duration_ms
            execution.error_message = str(e)

            self.session.add(execution)
            await self.session.commit()

            return ScrapeResponse(
                success=False,
                execution=ExecutionSummaryDTO.model_validate(execution),
                items=[],
            )

    async def get_recent_queries(self, limit: int = 50) -> List[SearchQuery]:
        """Retorna as buscas recentes registradas."""
        stmt = select(SearchQuery).order_by(col(SearchQuery.created_at).desc()).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

    async def _get_or_create_search_query(self, request: ScrapeRequest) -> SearchQuery:
        stmt = select(SearchQuery).where(
            SearchQuery.vendor == request.vendor,
            SearchQuery.keyword == request.keyword,
            SearchQuery.state_region == (request.region or request.state),
            SearchQuery.category == request.category,
            SearchQuery.min_price == request.min_price,
            SearchQuery.max_price == request.max_price,
            SearchQuery.require_delivery == request.require_delivery,
        )
        result = await self.session.execute(stmt)
        query_obj = result.scalars().first()

        if not query_obj:
            query_obj = SearchQuery(
                vendor=request.vendor,
                keyword=request.keyword,
                state_region=request.region or request.state,
                category=request.category,
                min_price=request.min_price,
                max_price=request.max_price,
                require_delivery=request.require_delivery,
            )
            self.session.add(query_obj)
            await self.session.commit()
            await self.session.refresh(query_obj)

        return query_obj

    async def _persist_listings(self, execution_id: str, items: List[ScrapedListingDTO]) -> int:
        if not items:
            return 0

        # Otimização: Busca em lote os registros de anúncios já existentes para este vendor
        vendor_ids = [item.vendor_listing_id for item in items]
        stmt = select(ScrapedListing).where(col(ScrapedListing.vendor_listing_id).in_(vendor_ids))

        result = await self.session.execute(stmt)
        existing_map = {listing.vendor_listing_id: listing for listing in result.scalars().all()}

        new_count = 0
        now = datetime.now(timezone.utc)
        seen_in_batch = set()

        for item in items:
            # Deduplica caso a mesma página da OLX contenha o mesmo card duplicado
            if item.vendor_listing_id in seen_in_batch:
                continue
            seen_in_batch.add(item.vendor_listing_id)

            if item.vendor_listing_id in existing_map:
                # Anúncio já existente no banco: atualiza preço, última execução e metadados
                existing = existing_map[item.vendor_listing_id]
                existing.execution_id = execution_id
                existing.title = item.title
                existing.price = item.price
                existing.original_price = item.original_price
                existing.url = item.url
                if item.description:
                    existing.description = item.description
                if item.state:
                    existing.state = item.state
                if item.city:
                    existing.city = item.city
                if item.neighborhood:
                    existing.neighborhood = item.neighborhood
                existing.has_delivery = item.has_delivery
                existing.delivery_type = item.delivery_type
                if item.images:
                    existing.images = item.images
                existing.scraped_at = item.scraped_at or now

                self.session.add(existing)
            else:
                # Anúncio novo: insere
                new_count += 1
                listing_model = ScrapedListing(
                    execution_id=execution_id,
                    vendor=item.vendor,
                    vendor_listing_id=item.vendor_listing_id,
                    title=item.title,
                    price=item.price,
                    original_price=item.original_price,
                    url=item.url,
                    description=item.description,
                    state=item.state,
                    city=item.city,
                    neighborhood=item.neighborhood,
                    has_delivery=item.has_delivery,
                    delivery_type=item.delivery_type,
                    images=item.images,
                    published_at=item.published_at,
                    scraped_at=item.scraped_at or now,
                )
                self.session.add(listing_model)

        await self.session.commit()
        return new_count
