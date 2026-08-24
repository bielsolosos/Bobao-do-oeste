"""
Fila persistente de execuções de scraping baseada em SQLite.

Serializa as requisições através de um único worker asyncio, evitando
concorrência no parser HTTP e no navegador headless. O claim é atômico
via `UPDATE ... WHERE id = (SELECT ... LIMIT 1) RETURNING`, compatível
com SQLite (single-writer garante a atomicidade).
"""

import asyncio
import time
from datetime import datetime, timezone
from typing import Any, Dict, Optional

from sqlalchemy import select, update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import col

from src.core.database import async_session_maker
from src.core.logger import logger
from src.domain.enums import JobStatusEnum
from src.domain.models import ScrapeJob
from src.domain.schemas import ScrapeRequest

TERMINAL_STATUSES = {JobStatusEnum.SUCCESS, JobStatusEnum.FAILED}


class JobQueueService:
    """Serviço de fila persistente de jobs de scraping."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def enqueue(self, request: ScrapeRequest, priority: int = 0) -> ScrapeJob:
        """Enfileira uma nova requisição de scraping."""
        job = ScrapeJob(
            vendor=request.vendor,
            request_payload=request.model_dump(mode="json"),
            priority=priority,
            status=JobStatusEnum.QUEUED,
        )
        self.session.add(job)
        await self.session.commit()
        await self.session.refresh(job)
        logger.info(f"Job enfileirado: {job.id} (vendor={job.vendor.value}, priority={priority})")
        return job

    async def claim_next(self, worker_id: str) -> Optional[ScrapeJob]:
        """
        Reivindica atomicamente o próximo job QUEUED.

        Usa `UPDATE ... WHERE id = (SELECT ... LIMIT 1) RETURNING` para
        garantir que dois workers simultâneos não peguem o mesmo job.
        Retorna None se não houver jobs na fila.
        """
        now = datetime.now(timezone.utc)
        next_id_subquery = (
            select(col(ScrapeJob.id))
            .where(col(ScrapeJob.status) == JobStatusEnum.QUEUED)
            .order_by(col(ScrapeJob.priority).asc(), col(ScrapeJob.created_at).asc())
            .limit(1)
            .scalar_subquery()
        )
        stmt = (
            update(ScrapeJob)
            .where(col(ScrapeJob.id) == next_id_subquery)
            .values(
                status=JobStatusEnum.RUNNING,
                worker_id=worker_id,
                started_at=now,
                attempts=ScrapeJob.attempts + 1,
            )
            .returning(ScrapeJob)
        )
        result = await self.session.execute(stmt)
        await self.session.commit()
        job = result.scalar_one_or_none()

        if job:
            logger.info(f"Job reivindicado: {job.id} (worker={worker_id}, attempt={job.attempts})")
        return job

    async def mark_success(self, job: ScrapeJob, response_payload: Dict[str, Any]) -> None:
        """Marca um job como concluído com sucesso e armazena o payload final."""
        job.status = JobStatusEnum.SUCCESS
        job.finished_at = datetime.now(timezone.utc)
        job.response_payload = response_payload
        job.error_message = None
        self.session.add(job)
        await self.session.commit()
        logger.info(f"Job concluído com sucesso: {job.id}")

    async def mark_failed(self, job: ScrapeJob, response_payload: Dict[str, Any], error_message: str) -> None:
        """Marca um job como falho e armazena o payload de erro."""
        job.status = JobStatusEnum.FAILED
        job.finished_at = datetime.now(timezone.utc)
        job.response_payload = response_payload
        job.error_message = error_message[:1000]
        self.session.add(job)
        await self.session.commit()
        logger.info(f"Job falhou: {job.id} - {error_message}")

    async def wait_for_completion(
        self,
        job_id: str,
        timeout: float = 300.0,
        poll_interval: float = 0.5,
    ) -> ScrapeJob:
        """
        Bloqueia (com yield ao event loop) até o job atingir um status terminal.

        Usa uma sessão fresca por poll para evitar o cache do identity-map
        da sessão de origem (enqueue). Lança TimeoutError se exceder o
        timeout. Lança ValueError se o job não existir.
        """
        start = time.perf_counter()
        while True:
            elapsed = time.perf_counter() - start
            if elapsed > timeout:
                raise TimeoutError(f"Job {job_id} não foi concluído em {timeout:.0f}s")

            async with async_session_maker() as poll_session:
                stmt = select(ScrapeJob).where(col(ScrapeJob.id) == job_id)
                result = await poll_session.execute(stmt)
                job = result.scalars().first()

            if job is None:
                raise ValueError(f"Job {job_id} não encontrado")

            if job.status in TERMINAL_STATUSES:
                return job

            await asyncio.sleep(poll_interval)

    async def recover_orphaned_jobs(self) -> int:
        """
        Reseta jobs RUNNING para QUEUED (uso no startup do worker).

        Útil para sobreviver a restarts do processo: jobs que estavam
        RUNNING quando o app caiu são reenfileirados para reprocessamento.
        """
        stmt = (
            update(ScrapeJob)
            .where(col(ScrapeJob.status) == JobStatusEnum.RUNNING)
            .values(status=JobStatusEnum.QUEUED, started_at=None, worker_id=None)
        )
        result = await self.session.execute(stmt)
        await self.session.commit()
        recovered = getattr(result, "rowcount", 0) or 0
        if recovered:
            logger.warning(f"Recuperados {recovered} jobs RUNNING órfãos → QUEUED (restart do worker)")
        return recovered

    async def get_stats(self) -> Dict[str, int]:
        """Retorna contadores por status (útil para diagnóstico)."""
        stmt = select(ScrapeJob).order_by(col(ScrapeJob.created_at).desc()).limit(500)
        result = await self.session.execute(stmt)
        rows = result.scalars().all()
        counts: Dict[str, int] = {s.value: 0 for s in JobStatusEnum}
        for job in rows:
            counts[job.status.value] += 1
        return counts
