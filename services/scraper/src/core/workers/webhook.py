"""
Pool assíncrono de workers que entrega webhooks de forma confiável.

Faz polling na tabela `webhook_deliveries` e envia POSTs HTTP para as
URLs dos clientes. 2xx (incluindo 202) → sucesso. 4xx → falha terminal.
5xx/timeout/network error → retry com backoff exponencial e jitter, até
`WEBHOOK_DELIVERY_MAX_ATTEMPTS`.
"""

import asyncio
import random
import uuid
from datetime import datetime, timedelta, timezone
from typing import List, Optional, Tuple

import httpx
from sqlalchemy import select
from sqlmodel import col

from src.core.config import settings
from src.core.database import async_session_maker
from src.core.logger import logger
from src.core.queues.webhook import WebhookQueueService
from src.domain.models import ScrapeJob


class WebhookWorker:
    """Pool de tasks que entrega webhooks de forma assíncrona e confiável."""

    def __init__(
        self,
        concurrency: Optional[int] = None,
        poll_interval: Optional[float] = None,
        timeout: Optional[int] = None,
    ):
        self.concurrency = max(1, concurrency or settings.WEBHOOK_DISPATCHER_CONCURRENCY)
        self.poll_interval = poll_interval if poll_interval is not None else settings.WEBHOOK_DISPATCHER_POLL_INTERVAL
        self.timeout = timeout or settings.WEBHOOK_DELIVERY_TIMEOUT_SECONDS
        self.pool_id = f"hookpool-{uuid.uuid4().hex[:8]}"
        self._tasks: List[asyncio.Task] = []
        self._stop_event = asyncio.Event()

    async def start(self) -> None:
        """Inicia o pool e recupera deliveries órfãs de crashes anteriores."""
        logger.info(
            f"Iniciando WebhookWorker pool: {self.pool_id} "
            f"(concurrency={self.concurrency}, poll_interval={self.poll_interval}s, "
            f"timeout={self.timeout}s)"
        )
        async with async_session_maker() as session:
            queue = WebhookQueueService(session)
            await queue.recover_stuck_deliveries(settings.WEBHOOK_STUCK_TIMEOUT_SECONDS)

        self._stop_event.clear()
        self._tasks = [
            asyncio.create_task(
                self._run_loop(worker_index=i),
                name=f"webhook-worker-{self.pool_id}-{i}",
            )
            for i in range(self.concurrency)
        ]

    async def stop(self) -> None:
        """Sinaliza parada e aguarda tasks (com timeout)."""
        logger.info(f"Parando WebhookWorker pool: {self.pool_id} ({len(self._tasks)} workers)")
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
        worker_id = self._worker_id(worker_index)
        logger.info(f"WebhookWorker em execução: {worker_id}")
        while not self._stop_event.is_set():
            try:
                processed = await self._process_one(worker_id)
                if not processed:
                    try:
                        await asyncio.wait_for(self._stop_event.wait(), timeout=self.poll_interval)
                    except asyncio.TimeoutError:
                        pass
            except Exception as e:
                logger.error(f"Erro no webhook worker {worker_id}: {e}", exc_info=True)
                await asyncio.sleep(2.0)
        logger.info(f"WebhookWorker encerrado: {worker_id}")

    async def _process_one(self, worker_id: str) -> bool:
        async with async_session_maker() as session:
            queue = WebhookQueueService(session)
            delivery = await queue.claim_next(worker_id)
            if delivery is None:
                return False

            logger.info(
                f"Enviando webhook: id={delivery.id}, request_id={delivery.request_id}, "
                f"attempt={delivery.attempts + 1}/{delivery.max_attempts}, "
                f"url={delivery.webhook_url}"
            )

            scrape_response = await self._load_scrape_response(delivery.scrape_job_id)
            if scrape_response is None:
                await queue.mark_failed_terminal(delivery, "ScrapeJob não encontrado ou sem response_payload")
                return True

            payload = {
                "requestId": delivery.request_id,
                "jobId": delivery.scrape_job_id,
                "status": "SUCCESS" if scrape_response.get("success") else "FAILED",
                "response": scrape_response,
            }

            outcome, response_code, error_message = await self._post_with_timeout(delivery.webhook_url, payload)

            if outcome == "success":
                await queue.mark_delivered(delivery, response_code or 0)
            elif outcome == "client_error":
                await queue.mark_failed_terminal(
                    delivery,
                    f"HTTP {response_code}: {error_message}",
                    response_code=response_code,
                )
            else:
                next_at = self._compute_next_attempt(delivery.attempts + 1)
                await queue.mark_retry(delivery, response_code, error_message, next_at)

            return True

    async def _load_scrape_response(self, scrape_job_id: str) -> Optional[dict]:
        async with async_session_maker() as session:
            stmt = select(col(ScrapeJob.response_payload)).where(col(ScrapeJob.id) == scrape_job_id)
            result = await session.execute(stmt)
            return result.scalar_one_or_none()

    async def _post_with_timeout(self, url: str, payload: dict) -> Tuple[str, Optional[int], str]:
        """Retorna (outcome, status_code, error_message). outcome ∈ {success, client_error, retryable}."""
        try:
            async with httpx.AsyncClient(timeout=self.timeout) as client:
                response = await client.post(url, json=payload)

            if 200 <= response.status_code < 300:
                return "success", response.status_code, ""
            if 400 <= response.status_code < 500:
                return "client_error", response.status_code, response.text[:300]
            return "retryable", response.status_code, f"HTTP {response.status_code}: {response.text[:300]}"

        except httpx.TimeoutException as e:
            return "retryable", None, f"Timeout após {self.timeout}s: {e!s}"[:300]
        except httpx.RequestError as e:
            return "retryable", None, f"Erro de rede: {e!s}"[:300]

    def _compute_next_attempt(self, attempts: int) -> datetime:
        """
        Backoff exponencial: delay = min(base * 2^(attempts-1), max) + jitter.

        attempts aqui é o número da tentativa que *vai* acontecer (1-indexed).
        """
        base = settings.WEBHOOK_DELIVERY_BASE_BACKOFF_SECONDS
        max_delay = settings.WEBHOOK_DELIVERY_MAX_BACKOFF_SECONDS
        exponential = base * (2 ** (attempts - 1))
        delay = min(exponential, max_delay)
        jitter = random.uniform(0, 1)
        return datetime.now(timezone.utc) + timedelta(seconds=delay + jitter)


_webhook_worker_instance: Optional[WebhookWorker] = None


def get_webhook_worker() -> WebhookWorker:
    """Retorna o singleton do pool de webhook workers (criado lazy no primeiro start)."""
    global _webhook_worker_instance
    if _webhook_worker_instance is None:
        _webhook_worker_instance = WebhookWorker()
    return _webhook_worker_instance
