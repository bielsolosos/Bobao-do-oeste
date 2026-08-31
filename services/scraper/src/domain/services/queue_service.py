"""
Serviço de Domínio para Telemetria e Métricas de Filas (QueueService).
"""

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import col

from src.domain.enums import DeliveryStatusEnum, JobStatusEnum
from src.domain.models import ScrapeJob, WebhookDelivery
from src.domain.schemas import QueueStatusDTO


class QueueService:
    """Serviço responsável pela consulta de status e métricas da fila de scraping e webhooks."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def get_queue_status(self) -> QueueStatusDTO:
        """Calcula a volumetria agregada de jobs e webhooks em tempo real."""
        # 1. Contagem de jobs agrupados por status
        stmt_jobs = select(ScrapeJob.status, func.count(col(ScrapeJob.id))).group_by(ScrapeJob.status)
        res_jobs = await self.session.execute(stmt_jobs)
        job_counts = dict(res_jobs.all())

        queued = job_counts.get(JobStatusEnum.QUEUED, 0)
        running = job_counts.get(JobStatusEnum.RUNNING, 0)
        success = job_counts.get(JobStatusEnum.SUCCESS, 0)
        failed = job_counts.get(JobStatusEnum.FAILED, 0)

        # 2. Contagem de webhooks pendentes/em envio
        stmt_wh = select(func.count(col(WebhookDelivery.id))).where(
            col(WebhookDelivery.status).in_(
                [DeliveryStatusEnum.PENDING, DeliveryStatusEnum.READY, DeliveryStatusEnum.SENDING]
            )
        )
        res_wh = await self.session.execute(stmt_wh)
        pending_webhooks = res_wh.scalar_one_or_none() or 0

        return QueueStatusDTO(
            queued_jobs=queued,
            running_jobs=running,
            total_pending_jobs=queued + running,
            pending_webhooks=pending_webhooks,
            total_success_jobs=success,
            total_failed_jobs=failed,
        )
