"""
Serviço de fila de entregas de webhook.

Encapsula operações sobre a tabela `webhook_deliveries`:
- claim atômico para o worker pegar a próxima entrega a enviar
- marcação de sucesso/falha/retry
- recovery de deliveries órfãs no startup

A criação de novas deliveries (junto com o ScrapeJob) é responsabilidade
do `AsyncScrapeService` em `domain/services/`, que orquestra os dois.
Este service é "puro" — só conhece `WebhookDelivery`.
"""

import uuid
from datetime import datetime, timezone
from typing import Optional

from sqlalchemy import case, select, update
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import col

from src.core.config import settings
from src.core.logger import logger
from src.domain.enums import DeliveryStatusEnum
from src.domain.models import WebhookDelivery


class WebhookQueueService:
    """Serviço para transicionar e consultar entregas de webhook."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def get_by_request_id(self, request_id: str) -> Optional[WebhookDelivery]:
        """Busca uma entrega pelo request_id (idempotency key do cliente)."""
        stmt = select(WebhookDelivery).where(col(WebhookDelivery.request_id) == request_id)
        result = await self.session.execute(stmt)
        return result.scalars().first()

    async def create_pending(
        self,
        scrape_job_id: str,
        request_id: str,
        webhook_url: str,
    ) -> WebhookDelivery:
        """
        Cria uma nova delivery em PENDING. Não faz commit — o caller
        (orquestrador `AsyncScrapeService`) controla a transação
        para garantir atomicidade entre job e delivery.
        """
        if request_id is None:
            request_id = str(uuid.uuid4())

        delivery = WebhookDelivery(
            scrape_job_id=scrape_job_id,
            request_id=request_id,
            webhook_url=webhook_url,
            status=DeliveryStatusEnum.PENDING,
            max_attempts=settings.WEBHOOK_DELIVERY_MAX_ATTEMPTS,
        )
        self.session.add(delivery)
        await self.session.flush()
        logger.info(
            f"Webhook delivery criada (flushed): request_id={request_id}, job_id={scrape_job_id}, url={webhook_url}"
        )
        return delivery

    async def mark_ready(self, scrape_job_id: str) -> int:
        """
        Transiciona delivery de PENDING → READY quando o ScrapeJob termina.

        Retorna o número de deliveries atualizadas (0 se não houver delivery
        associada a esse job, ex.: scraping síncrono).
        """
        now = datetime.now(timezone.utc)
        stmt = (
            update(WebhookDelivery)
            .where(
                col(WebhookDelivery.scrape_job_id) == scrape_job_id,
                col(WebhookDelivery.status) == DeliveryStatusEnum.PENDING,
            )
            .values(status=DeliveryStatusEnum.READY, updated_at=now)
        )
        result = await self.session.execute(stmt)
        await self.session.commit()
        updated = getattr(result, "rowcount", 0) or 0
        if updated:
            logger.debug(f"Delivery pronta para envio: job_id={scrape_job_id}")
        return updated

    async def claim_next(self, worker_id: str) -> Optional[WebhookDelivery]:
        """
        Reivindica atomicamente a próxima delivery a ser enviada.

        Pega:
        - READY: primeira entrega pronta
        - SENDING: entrega em retry cujo next_attempt_at já passou

        Ordena por (next_attempt_at NULLS FIRST, created_at) — deliveries prontas
        vêm antes de retries atrasados, e FIFO dentro de cada grupo.
        """
        now = datetime.now(timezone.utc)
        status_priority = case((col(WebhookDelivery.status) == DeliveryStatusEnum.READY, 0), else_=1)
        next_id_subquery = (
            select(col(WebhookDelivery.id))
            .where(
                (col(WebhookDelivery.status) == DeliveryStatusEnum.READY)
                | (
                    (col(WebhookDelivery.status) == DeliveryStatusEnum.SENDING)
                    & (col(WebhookDelivery.next_attempt_at) <= now)
                )
            )
            .order_by(
                status_priority.asc(),
                col(WebhookDelivery.next_attempt_at).asc().nulls_first(),
                col(WebhookDelivery.created_at).asc(),
            )
            .limit(1)
            .scalar_subquery()
        )

        stmt = (
            update(WebhookDelivery)
            .where(col(WebhookDelivery.id) == next_id_subquery)
            .values(
                status=DeliveryStatusEnum.SENDING,
                worker_id=worker_id,
                last_attempt_at=now,
                updated_at=now,
            )
            .returning(col(WebhookDelivery.id))
        )
        result = await self.session.execute(stmt)
        delivery_id = result.scalar_one_or_none()
        await self.session.commit()

        if delivery_id is None:
            return None

        delivery = await self.session.get(WebhookDelivery, delivery_id)
        if delivery:
            logger.info(
                f"Delivery reivindicada: id={delivery.id}, "
                f"request_id={delivery.request_id}, attempts={delivery.attempts}, "
                f"worker={worker_id}"
            )
        return delivery

    async def mark_delivered(self, delivery: WebhookDelivery, response_code: int) -> None:
        """Marca a delivery como entregue com sucesso (cliente retornou 2xx)."""
        now = datetime.now(timezone.utc)
        stmt = (
            update(WebhookDelivery)
            .where(col(WebhookDelivery.id) == delivery.id)
            .values(
                status=DeliveryStatusEnum.DELIVERED,
                delivered_at=now,
                last_response_code=response_code,
                last_error=None,
                updated_at=now,
            )
        )
        await self.session.execute(stmt)
        await self.session.commit()
        logger.info(
            f"Delivery entregue: id={delivery.id}, request_id={delivery.request_id}, "
            f"code={response_code}, attempts={delivery.attempts}"
        )

    async def mark_retry(
        self,
        delivery: WebhookDelivery,
        response_code: Optional[int],
        error_message: str,
        next_attempt_at: datetime,
    ) -> None:
        """
        Agenda uma nova tentativa. Incrementa attempts e seta next_attempt_at.

        Se attempts + 1 >= max_attempts, marca como FAILED (sem mais retries).
        """
        now = datetime.now(timezone.utc)
        new_attempts = delivery.attempts + 1
        is_terminal = new_attempts >= delivery.max_attempts
        new_status = DeliveryStatusEnum.FAILED if is_terminal else DeliveryStatusEnum.SENDING
        new_next_attempt = None if is_terminal else next_attempt_at

        stmt = (
            update(WebhookDelivery)
            .where(col(WebhookDelivery.id) == delivery.id)
            .values(
                attempts=new_attempts,
                last_response_code=response_code,
                last_error=error_message[:1000],
                updated_at=now,
                status=new_status,
                next_attempt_at=new_next_attempt,
            )
        )
        await self.session.execute(stmt)
        await self.session.commit()

        if is_terminal:
            logger.warning(
                f"Delivery FAILED (max attempts): id={delivery.id}, "
                f"request_id={delivery.request_id}, attempts={new_attempts}, "
                f"last_error={error_message[:200]}"
            )
        else:
            logger.info(
                f"Delivery retry agendado: id={delivery.id}, "
                f"request_id={delivery.request_id}, attempts={new_attempts}, "
                f"next_attempt_at={next_attempt_at.isoformat()}"
            )

    async def mark_failed_terminal(
        self,
        delivery: WebhookDelivery,
        error_message: str,
        response_code: Optional[int] = None,
    ) -> None:
        """Marca como FAILED sem retry (ex.: 4xx do cliente)."""
        now = datetime.now(timezone.utc)
        stmt = (
            update(WebhookDelivery)
            .where(col(WebhookDelivery.id) == delivery.id)
            .values(
                status=DeliveryStatusEnum.FAILED,
                attempts=delivery.attempts + 1,
                last_error=error_message[:1000],
                next_attempt_at=None,
                updated_at=now,
                last_response_code=response_code,
            )
        )
        await self.session.execute(stmt)
        await self.session.commit()
        logger.warning(
            f"Delivery FAILED (terminal): id={delivery.id}, "
            f"request_id={delivery.request_id}, error={error_message[:200]}"
        )

    async def recover_stuck_deliveries(self, stuck_timeout_seconds: int) -> int:
        """
        Reseta deliveries em SENDING há mais de `stuck_timeout_seconds` para READY.

        Cobre o caso de um dispatcher que crashou mid-flight. Os outros
        deliveries (PENDING, READY, DELIVERED, FAILED) não são tocados.
        """
        threshold_dt = datetime.fromtimestamp(
            datetime.now(timezone.utc).timestamp() - stuck_timeout_seconds, tz=timezone.utc
        )
        stmt = (
            update(WebhookDelivery)
            .where(
                col(WebhookDelivery.status) == DeliveryStatusEnum.SENDING,
                col(WebhookDelivery.last_attempt_at) <= threshold_dt,
            )
            .values(
                status=DeliveryStatusEnum.READY,
                worker_id=None,
                next_attempt_at=None,
                updated_at=datetime.now(timezone.utc),
            )
        )
        result = await self.session.execute(stmt)
        await self.session.commit()
        recovered = getattr(result, "rowcount", 0) or 0
        if recovered:
            logger.warning(
                f"Recuperadas {recovered} deliveries SENDING travadas → READY (timeout {stuck_timeout_seconds}s)"
            )
        return recovered
