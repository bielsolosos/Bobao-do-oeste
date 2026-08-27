import asyncio
import os
import uuid
from datetime import datetime, timezone
from typing import List, Optional

from src.core.config import settings
from src.core.database import async_session_maker
from src.core.logger import logger
from src.core.queues.scrape import ScrapeQueueService
from src.core.queues.webhook import WebhookQueueService
from src.domain.enums import ExecutionStatusEnum
from src.domain.schemas import ExecutionSummaryDTO, ScrapeRequest, ScrapeResponse
from src.domain.services.scraping_service import ScrapingService


class ScrapeWorker:
    """Pool de workers que processa a fila de scraping com N tarefas concorrentes."""

    def __init__(
        self,
        concurrency: Optional[int] = None,
        poll_interval: Optional[float] = None,
    ):
        self.concurrency = max(1, concurrency or settings.SCRAPE_WORKER_CONCURRENCY)
        self.poll_interval = poll_interval if poll_interval is not None else settings.SCRAPE_WORKER_POLL_INTERVAL
        self.pool_id = f"scrapepool-{uuid.uuid4().hex[:8]}"
        self._tasks: List[asyncio.Task] = []
        self._stop_event = asyncio.Event()

    async def start(self) -> None:
        """Inicia o pool e recupera jobs órfãos de um restart anterior."""
        logger.info(
            f"Iniciando ScrapeWorker pool: {self.pool_id} "
            f"(concurrency={self.concurrency}, poll_interval={self.poll_interval}s)"
        )
        async with async_session_maker() as session:
            queue = ScrapeQueueService(session)
            await queue.recover_orphaned_jobs()

        self._stop_event.clear()
        self._tasks = [
            asyncio.create_task(self._run_loop(worker_index=i), name=f"scrape-worker-{self.pool_id}-{i}")
            for i in range(self.concurrency)
        ]

    async def stop(self) -> None:
        """Sinaliza parada para todas as tasks e aguarda com timeout."""
        logger.info(f"Parando ScrapeWorker pool: {self.pool_id} ({len(self._tasks)} workers)")
        self._stop_event.set()
        if self._tasks:
            try:
                await asyncio.wait_for(asyncio.gather(*self._tasks, return_exceptions=True), timeout=10.0)
            except asyncio.TimeoutError:
                for task in self._tasks:
                    task.cancel()
                logger.warning(f"Pool {self.pool_id}: workers cancelados por timeout na parada")
            self._tasks = []

    def _worker_id(self, index: int) -> str:
        return f"{self.pool_id}#{index}"

    async def _run_loop(self, worker_index: int) -> None:
        """Loop de uma task do pool: claim → execute → mark, até stop."""
        worker_id = self._worker_id(worker_index)
        logger.info(f"Worker em execução: {worker_id}")
        while not self._stop_event.is_set():
            try:
                processed = await self._process_one(worker_id)
                if not processed:
                    try:
                        await asyncio.wait_for(self._stop_event.wait(), timeout=self.poll_interval)
                    except asyncio.TimeoutError:
                        pass
            except Exception as e:
                logger.error(f"Erro no worker {worker_id}: {e}", exc_info=True)
                await asyncio.sleep(2.0)
        logger.info(f"Worker encerrado: {worker_id}")

    async def _process_one(self, worker_id: str) -> bool:
        """Tenta processar um job. Retorna True se processou, False se fila vazia."""
        async with async_session_maker() as session:
            queue = ScrapeQueueService(session)
            job = await queue.claim_next(worker_id)
            if job is None:
                return False

            job_id = job.id
            job_vendor = job.vendor
            job_created_at = job.created_at

            logger.info(
                f"Processando job {job_id} (worker={worker_id}, vendor={job_vendor.value}, attempt={job.attempts})"
            )
            try:
                request = ScrapeRequest.model_validate(job.request_payload)
                scraping = ScrapingService(session)
                response: ScrapeResponse = await scraping.execute_scrape(request)
                await queue.mark_success(job, response.model_dump(mode="json"))
            except Exception as e:
                await session.rollback()
                logger.error(f"Job {job_id} falhou durante execução: {e}", exc_info=True)
                failed_response = ScrapeResponse(
                    success=False,
                    execution=ExecutionSummaryDTO(
                        id=job_id,
                        vendor=job_vendor,
                        status=ExecutionStatusEnum.FAILED,
                        error_message=str(e)[:500],
                        started_at=job_created_at,
                        finished_at=datetime.now(timezone.utc),
                    ),
                    items=[],
                )
                await queue.mark_failed(job, failed_response.model_dump(mode="json"), str(e))

            # Transiciona webhook delivery PENDING → READY (se houver)
            delivery_queue = WebhookQueueService(session)
            await delivery_queue.mark_ready(job_id)
            return True


ScrapeWorkerPool = ScrapeWorker
_scrape_worker_instance: Optional[ScrapeWorker] = None


def get_scrape_worker() -> ScrapeWorker:
    """Retorna o singleton do pool de scrape workers (criado lazy no primeiro start)."""
    global _scrape_worker_instance
    if _scrape_worker_instance is None:
        _scrape_worker_instance = ScrapeWorker()
    return _scrape_worker_instance
