import asyncio
from datetime import datetime, timezone

import pytest
from sqlalchemy import text

from src.core.database import async_session_maker
from src.core.job_queue import JobQueueService
from src.domain.enums import JobStatusEnum, VendorEnum
from src.domain.schemas import ScrapeRequest


def _make_request(keyword: str = "test") -> ScrapeRequest:
    return ScrapeRequest(vendor=VendorEnum.OLX, keyword=keyword)


async def _cleanup_jobs() -> None:
    async with async_session_maker() as session:
        await session.execute(text("DELETE FROM scrape_jobs"))
        await session.commit()


@pytest.mark.asyncio
async def test_enqueue_creates_queued_job_with_payload():
    await _cleanup_jobs()
    async with async_session_maker() as session:
        queue = JobQueueService(session)
        request = _make_request("thinkpad t480")
        job = await queue.enqueue(request, priority=5)

        assert job.id is not None
        assert job.status == JobStatusEnum.QUEUED
        assert job.attempts == 0
        assert job.priority == 5
        assert job.vendor == VendorEnum.OLX
        assert job.request_payload["keyword"] == "thinkpad t480"
        assert job.created_at is not None
    await _cleanup_jobs()


@pytest.mark.asyncio
async def test_claim_next_returns_oldest_queued_with_priority_order():
    await _cleanup_jobs()
    async with async_session_maker() as session:
        queue = JobQueueService(session)
        low = await queue.enqueue(_make_request("low"), priority=10)
        await asyncio.sleep(0.01)
        high = await queue.enqueue(_make_request("high"), priority=0)
        await asyncio.sleep(0.01)
        mid = await queue.enqueue(_make_request("mid"), priority=5)

        claimed = await queue.claim_next("worker-test")
        assert claimed is not None
        assert claimed.id == high.id
        assert claimed.status == JobStatusEnum.RUNNING
        assert claimed.attempts == 1
        assert claimed.worker_id == "worker-test"
        assert claimed.started_at is not None

        claimed2 = await queue.claim_next("worker-test")
        assert claimed2 is not None
        assert claimed2.id == mid.id

        claimed3 = await queue.claim_next("worker-test")
        assert claimed3 is not None
        assert claimed3.id == low.id

        empty = await queue.claim_next("worker-test")
        assert empty is None
    await _cleanup_jobs()


@pytest.mark.asyncio
async def test_mark_success_stores_response_payload():
    await _cleanup_jobs()
    async with async_session_maker() as session:
        queue = JobQueueService(session)
        await queue.enqueue(_make_request())
        claimed = await queue.claim_next("worker-1")
        assert claimed is not None

        await queue.mark_success(claimed, {"success": True, "items": [], "execution": {}})

        assert claimed.status == JobStatusEnum.SUCCESS
        assert claimed.finished_at is not None
        assert claimed.error_message is None
        assert claimed.response_payload == {"success": True, "items": [], "execution": {}}
    await _cleanup_jobs()


@pytest.mark.asyncio
async def test_mark_failed_stores_error_and_payload():
    await _cleanup_jobs()
    async with async_session_maker() as session:
        queue = JobQueueService(session)
        await queue.enqueue(_make_request())
        claimed = await queue.claim_next("worker-1")
        assert claimed is not None

        await queue.mark_failed(claimed, {"success": False, "items": []}, "boom")

        assert claimed.status == JobStatusEnum.FAILED
        assert claimed.finished_at is not None
        assert claimed.error_message == "boom"
        assert claimed.response_payload == {"success": False, "items": []}
    await _cleanup_jobs()


@pytest.mark.asyncio
async def test_recover_orphaned_jobs_resets_running_to_queued():
    await _cleanup_jobs()
    async with async_session_maker() as session:
        await session.execute(
            text(
                """
                INSERT INTO scrape_jobs
                    (id, vendor, request_payload, status, priority, attempts,
                     worker_id, created_at, started_at)
                VALUES
                    ('orphan-1', 'OLX', '{}', 'RUNNING', 0, 1,
                     'old-worker', :ts, :ts)
                """
            ),
            {"ts": datetime.now(timezone.utc)},
        )
        await session.commit()

        queue = JobQueueService(session)
        recovered = await queue.recover_orphaned_jobs()
        assert recovered == 1

        result = await session.execute(
            text("SELECT status, worker_id, started_at FROM scrape_jobs WHERE id = :id"),
            {"id": "orphan-1"},
        )
        row = result.first()
        assert row is not None
        assert row[0] == "QUEUED"
        assert row[1] is None
        assert row[2] is None
    await _cleanup_jobs()


@pytest.mark.asyncio
async def test_wait_for_completion_returns_when_job_finishes():
    await _cleanup_jobs()
    async with async_session_maker() as session:
        queue = JobQueueService(session)
        job = await queue.enqueue(_make_request())

        async def finish_later() -> None:
            await asyncio.sleep(0.3)
            async with async_session_maker() as s2:
                q2 = JobQueueService(s2)
                claimed = await q2.claim_next("background-worker")
                if claimed is not None:
                    await q2.mark_success(claimed, {"success": True, "items": []})

        finisher = asyncio.create_task(finish_later())
        finished = await queue.wait_for_completion(job.id, timeout=5.0, poll_interval=0.1)
        await finisher

        assert finished.status == JobStatusEnum.SUCCESS
        assert finished.response_payload == {"success": True, "items": []}
    await _cleanup_jobs()


@pytest.mark.asyncio
async def test_wait_for_completion_times_out():
    await _cleanup_jobs()
    async with async_session_maker() as session:
        queue = JobQueueService(session)
        job = await queue.enqueue(_make_request())

        with pytest.raises(TimeoutError):
            await queue.wait_for_completion(job.id, timeout=0.5, poll_interval=0.1)
    await _cleanup_jobs()


@pytest.mark.asyncio
async def test_wait_for_completion_raises_for_missing_job():
    async with async_session_maker() as session:
        queue = JobQueueService(session)
        with pytest.raises(ValueError):
            await queue.wait_for_completion("does-not-exist", timeout=0.5, poll_interval=0.1)
