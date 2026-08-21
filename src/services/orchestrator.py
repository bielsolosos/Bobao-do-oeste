from datetime import datetime
import time
from typing import Dict, Type
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import select
from src.core.logger import logger
from src.domain.enums import ExecutionStatusEnum, VendorEnum
from src.domain.models import ScrapedListing, ScrapingExecution, SearchQuery
from src.domain.schemas import (
    ExecutionSummaryDTO,
    ScrapeRequest,
    ScrapeResponse,
    ScrapedListingDTO,
)
from src.providers.base import BaseScraperProvider
from src.providers.olx.provider import OlxScraperProvider


class ProviderFactory:
    """Registry for marketplace providers."""

    _providers: Dict[VendorEnum, Type[BaseScraperProvider]] = {
        VendorEnum.OLX: OlxScraperProvider,
    }

    @classmethod
    def get_provider(cls, vendor: VendorEnum) -> BaseScraperProvider:
        provider_cls = cls._providers.get(vendor)
        if not provider_cls:
            raise ValueError(f"Provider not implemented for vendor: {vendor}")
        return provider_cls()


class ScrapingOrchestrator:
    """Orchestrates search queries, provider execution, and database persistence."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def execute_scrape(self, request: ScrapeRequest) -> ScrapeResponse:
        start_time = time.perf_counter()
        started_at = datetime.utcnow()

        # 1. Find or create SearchQuery
        search_query = await self._get_or_create_search_query(request)

        # 2. Create ScrapingExecution with RUNNING status
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
            # 3. Resolve provider and execute scraping
            provider = ProviderFactory.get_provider(request.vendor)
            scraped_items, used_fallback = await provider.scrape(request)

            # 4. Save listings and compute new items
            new_items_count = await self._persist_listings(execution.id, scraped_items)

            # 5. Mark execution as SUCCESS
            finished_at = datetime.utcnow()
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
                execution=ExecutionSummaryDTO(
                    execution_id=execution.id,
                    vendor=execution.vendor,
                    status=execution.status,
                    duration_ms=execution.duration_ms,
                    total_found=execution.total_found,
                    new_items_count=execution.new_items_count,
                    used_fallback=execution.used_fallback,
                    started_at=execution.started_at,
                    finished_at=execution.finished_at,
                ),
                items=scraped_items,
            )

        except Exception as e:
            logger.error(f"Scraping execution {execution.id} failed: {e}", exc_info=True)
            finished_at = datetime.utcnow()
            duration_ms = int((time.perf_counter() - start_time) * 1000)

            execution.status = ExecutionStatusEnum.FAILED
            execution.finished_at = finished_at
            execution.duration_ms = duration_ms
            execution.error_message = str(e)

            self.session.add(execution)
            await self.session.commit()

            return ScrapeResponse(
                success=False,
                execution=ExecutionSummaryDTO(
                    execution_id=execution.id,
                    vendor=execution.vendor,
                    status=execution.status,
                    duration_ms=execution.duration_ms,
                    total_found=0,
                    new_items_count=0,
                    error_message=str(e),
                    started_at=execution.started_at,
                    finished_at=execution.finished_at,
                ),
                items=[],
            )

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

    async def _persist_listings(
        self, execution_id: str, items: list[ScrapedListingDTO]
    ) -> int:
        if not items:
            return 0

        new_count = 0
        for item in items:
            # Check if listing ID already exists in DB
            stmt = select(ScrapedListing).where(
                ScrapedListing.vendor == item.vendor,
                ScrapedListing.vendor_listing_id == item.vendor_listing_id,
            )
            result = await self.session.execute(stmt)
            existing = result.scalars().first()

            if not existing:
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
                scraped_at=item.scraped_at or datetime.utcnow(),
            )
            self.session.add(listing_model)

        await self.session.commit()
        return new_count
