"""
Testes do pool de workers. Usa mock no `ScrapingService.execute_scrape` para
evitar chamadas reais de rede e validar concorrência e isolamento de jobs.
"""

import asyncio
from datetime import datetime, timezone

import pytest
from sqlalchemy import text
from sqlmodel import col, select

from src.core.database import async_session_maker
from src.core.job_queue import JobQueueService
from src.core.worker import ScrapeWorker
from src.domain.enums import ExecutionStatusEnum, JobStatusEnum, VendorEnum
from src.domain.models import ScrapeJob
from src.domain.schemas import ExecutionSummaryDTO, ScrapeRequest, ScrapeResponse
from src.domain.services import ScrapingService


async def _cleanup_jobs() -> None:
    async with async_session_maker() as session:
        await session.execute(text("DELETE FROM scrape_jobs"))
        await session.commit()


def _make_fake_scraper():

    async def fake_execute_scrape(self, request: ScrapeRequest) -> ScrapeResponse:
        await asyncio.sleep(0.05)
        return ScrapeResponse(
            success=True,
            execution=ExecutionSummaryDTO(
                id=f"exec-{uuid_suffix()}",
                vendor=request.vendor,
                status=ExecutionStatusEnum.SUCCESS,
                duration_ms=50,
                total_found=0,
                new_items_count=0,
                started_at=datetime.now(timezone.utc),
                finished_at=datetime.now(timezone.utc),
            ),
            items=[],
        )

    return fake_execute_scrape


def uuid_suffix() -> str:
    import uuid

    return uuid.uuid4().hex[:8]


@pytest.mark.asyncio
async def test_pool_starts_and_stops_requested_number_of_tasks(monkeypatch):
    monkeypatch.setattr(ScrapingService, "execute_scrape", _make_fake_scraper())

    pool = ScrapeWorker(concurrency=3, poll_interval=0.05)
    await pool.start()
    try:
        assert len(pool._tasks) == 3
        assert all(not t.done() for t in pool._tasks)
    finally:
        await pool.stop()

    assert all(t.done() for t in getattr(pool, "_tasks_at_stop", pool._tasks) or [])


@pytest.mark.asyncio
async def test_pool_processes_multiple_jobs_with_concurrency(monkeypatch):
    """
    Enfileira 4 jobs com concurrency=2 e confirma que:
    - todos terminam com SUCCESS
    - pelo menos 2 workers diferentes registraram claim (worker_id distintos)
    """
    monkeypatch.setattr(ScrapingService, "execute_scrape", _make_fake_scraper())

    await _cleanup_jobs()
    pool = ScrapeWorker(concurrency=2, poll_interval=0.05)
    await pool.start()

    job_ids: list[str] = []
    try:
        async with async_session_maker() as session:
            queue = JobQueueService(session)
            for i in range(4):
                job = await queue.enqueue(ScrapeRequest(vendor=VendorEnum.OLX, keyword=f"kw-{i}"))
                job_ids.append(job.id)

        async with async_session_maker() as session:
            queue = JobQueueService(session)
            for job_id in job_ids:
                final = await queue.wait_for_completion(job_id, timeout=10.0, poll_interval=0.1)
                assert final.status == JobStatusEnum.SUCCESS
    finally:
        await pool.stop()

    async with async_session_maker() as session:
        stmt = select(col(ScrapeJob.worker_id)).where(col(ScrapeJob.id).in_(job_ids)).distinct()
        result = await session.execute(stmt)
        workers = {row[0] for row in result.all() if row[0]}

    assert len(workers) >= 2, f"Esperava >=2 worker_ids distintos, achei {workers}"
    assert all(w.startswith(pool.pool_id) for w in workers)
    await _cleanup_jobs()


@pytest.mark.asyncio
async def test_pool_processes_jobs_in_parallel_via_started_at(monkeypatch):
    """
    Com concurrency=3 e 3 jobs cuja execução leva 0.3s cada, o tempo total
    deve ser ~0.3s (paralelo) e não ~0.9s (serial).
    """

    async def slow_scraper(self, request: ScrapeRequest) -> ScrapeResponse:
        await asyncio.sleep(0.3)
        return ScrapeResponse(
            success=True,
            execution=ExecutionSummaryDTO(
                id=f"exec-{uuid_suffix()}",
                vendor=request.vendor,
                status=ExecutionStatusEnum.SUCCESS,
                duration_ms=300,
                total_found=0,
                new_items_count=0,
                started_at=datetime.now(timezone.utc),
                finished_at=datetime.now(timezone.utc),
            ),
            items=[],
        )

    monkeypatch.setattr(ScrapingService, "execute_scrape", slow_scraper)

    await _cleanup_jobs()
    pool = ScrapeWorker(concurrency=3, poll_interval=0.05)
    await pool.start()

    started = time_now()
    try:
        async with async_session_maker() as session:
            queue = JobQueueService(session)
            job_ids = [
                (await queue.enqueue(ScrapeRequest(vendor=VendorEnum.OLX, keyword=f"kw-{i}"))).id for i in range(3)
            ]

        async with async_session_maker() as session:
            queue = JobQueueService(session)
            for job_id in job_ids:
                final = await queue.wait_for_completion(job_id, timeout=10.0, poll_interval=0.1)
                assert final.status == JobStatusEnum.SUCCESS
    finally:
        await pool.stop()
    elapsed = time_now() - started

    # Margem generosa: serial seria 0.9s, paralelo ~0.3s. Aceita até 0.7s.
    assert elapsed < 0.7, f"Execução paralela esperada < 0.7s, levou {elapsed:.2f}s"
    await _cleanup_jobs()


def time_now() -> float:
    import time

    return time.perf_counter()
