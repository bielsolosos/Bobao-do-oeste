"""
Serviço de orquestração do scraping assíncrono.

Combina `ScrapeQueueService` (cria o job) + `WebhookQueueService` (cria
a delivery) numa única transação. Este service existe para evitar que
`WebhookQueueService` precise conhecer o modelo `ScrapeJob` (que é
conceito de outra feature), respeitando SRP.
"""

import uuid
from typing import Optional, Tuple

from sqlalchemy.ext.asyncio import AsyncSession

from src.core.logger import logger
from src.core.queues.scrape import ScrapeQueueService
from src.core.queues.webhook import WebhookQueueService
from src.domain.models import ScrapeJob, WebhookDelivery
from src.domain.schemas import ScrapeRequest


class AsyncScrapeService:
    """Orquestra a criação de ScrapeJob + WebhookDelivery de forma atômica."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def enqueue_with_webhook(
        self,
        request: ScrapeRequest,
        webhook_url: str,
        request_id: Optional[str] = None,
    ) -> Tuple[ScrapeJob, WebhookDelivery]:
        """
        Cria um ScrapeJob em QUEUED + um WebhookDelivery em PENDING, atomicamente.

        Se `request_id` for fornecido, valida que é único (lança ValueError
        se já existir). Se omitido, gera um UUID v4.

        A transação engloba ambos os INSERTs — se qualquer um falhar, nada
        é persistido.
        """
        scrape_queue = ScrapeQueueService(self.session)
        webhook_queue = WebhookQueueService(self.session)

        effective_request_id = request_id or str(uuid.uuid4())

        if request_id is not None:
            existing = await webhook_queue.get_by_request_id(request_id)
            if existing is not None:
                raise ValueError(f"requestId '{request_id}' já existe (job_id={existing.scrape_job_id})")

        job = await scrape_queue.enqueue(request)
        delivery = await webhook_queue.create_pending(
            scrape_job_id=job.id,
            request_id=effective_request_id,
            webhook_url=webhook_url,
        )
        await self.session.commit()

        logger.info(f"AsyncScrape enfileirado: request_id={effective_request_id}, job_id={job.id}, url={webhook_url}")
        return job, delivery
