import pytest

from src.core.database import async_session_maker
from src.domain.enums import ExecutionStatusEnum, VendorEnum
from src.domain.models import ScrapedListing, ScrapingExecution, SearchQuery
from src.domain.services import ExecutionService, ListingService


async def _create_search_query(session, query_id: str) -> None:
    """Cria a SearchQuery pai antes de criar a ScrapingExecution (FK)."""
    session.add(SearchQuery(id=query_id, vendor=VendorEnum.OLX, keyword="test"))
    await session.commit()


@pytest.mark.asyncio
async def test_listing_service_filtering():
    async with async_session_maker() as session:
        await _create_search_query(session, "query-test-1")
        exec_item = ScrapingExecution(
            search_query_id="query-test-1",
            vendor=VendorEnum.OLX,
            status=ExecutionStatusEnum.SUCCESS,
        )
        session.add(exec_item)
        await session.commit()
        await session.refresh(exec_item)

        listing1 = ScrapedListing(
            execution_id=exec_item.id,
            vendor=VendorEnum.OLX,
            vendor_listing_id="srv-1001",
            title="Dell Inspiron i7 16GB",
            price=2500.0,
            url="https://olx.com.br/test-1001",
            has_delivery=True,
        )
        listing2 = ScrapedListing(
            execution_id=exec_item.id,
            vendor=VendorEnum.OLX,
            vendor_listing_id="srv-1002",
            title="MacBook Air M1",
            price=4500.0,
            url="https://olx.com.br/test-1002",
            has_delivery=False,
        )
        session.add_all([listing1, listing2])
        await session.commit()

        service = ListingService(session)

        # Test filter by keyword
        results = await service.list_listings(keyword="Dell")
        assert len(results) >= 1
        assert any(r.vendor_listing_id == "srv-1001" for r in results)

        # Test filter by price range
        results = await service.list_listings(min_price=3000.0)
        assert any(r.vendor_listing_id == "srv-1002" for r in results)
        assert not any(r.vendor_listing_id == "srv-1001" for r in results)

        # Test filter by delivery
        results = await service.list_listings(has_delivery=True)
        assert any(r.vendor_listing_id == "srv-1001" for r in results)


@pytest.mark.asyncio
async def test_execution_service_crud():
    async with async_session_maker() as session:
        await _create_search_query(session, "query-test-2")
        exec_item = ScrapingExecution(
            search_query_id="query-test-2",
            vendor=VendorEnum.OLX,
            status=ExecutionStatusEnum.SUCCESS,
            total_found=10,
            new_items_count=5,
        )
        session.add(exec_item)
        await session.commit()
        await session.refresh(exec_item)

        service = ExecutionService(session)

        # Test get by ID
        dto = await service.get_execution_by_id(exec_item.id)
        assert dto is not None
        assert dto.execution_id == exec_item.id
        assert dto.total_found == 10

        # Test listing by vendor
        exec_list = await service.list_executions(vendor=VendorEnum.OLX)
        assert len(exec_list) >= 1
        assert any(e.execution_id == exec_item.id for e in exec_list)


@pytest.mark.asyncio
async def test_queue_service_metrics():
    from src.domain.enums import DeliveryStatusEnum, JobStatusEnum
    from src.domain.models import ScrapeJob, WebhookDelivery
    from src.domain.services import QueueService

    async with async_session_maker() as session:
        job1 = ScrapeJob(vendor=VendorEnum.OLX, status=JobStatusEnum.QUEUED, request_payload={})
        job2 = ScrapeJob(vendor=VendorEnum.OLX, status=JobStatusEnum.RUNNING, request_payload={})
        session.add_all([job1, job2])
        await session.commit()
        await session.refresh(job1)

        wh1 = WebhookDelivery(
            scrape_job_id=job1.id, request_id="req-wh-1", webhook_url="http://test", status=DeliveryStatusEnum.PENDING
        )
        session.add(wh1)
        await session.commit()

        service = QueueService(session)
        stats = await service.get_queue_status()

        assert stats.queued_jobs >= 1
        assert stats.running_jobs >= 1
        assert stats.total_pending_jobs >= 2
        assert stats.pending_webhooks >= 1
