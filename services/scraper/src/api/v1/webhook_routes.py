from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.core.database import get_session
from src.core.queues.webhook import WebhookQueueService
from src.domain.schemas import WebhookStatusResponse

router = APIRouter(prefix="/webhooks", tags=["Webhooks"])


@router.get(
    "/{request_id}",
    response_model=WebhookStatusResponse,
    summary="Consulta o status de uma entrega de webhook pelo requestId",
)
async def get_webhook_status(
    request_id: str,
    session: AsyncSession = Depends(get_session),
) -> WebhookStatusResponse:
    queue = WebhookQueueService(session)
    delivery = await queue.get_by_request_id(request_id)
    if delivery is None:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"requestId '{request_id}' não encontrado",
        )

    return WebhookStatusResponse(
        request_id=delivery.request_id,
        job_id=delivery.scrape_job_id,
        webhook_url=delivery.webhook_url,
        status=delivery.status,
        attempts=delivery.attempts,
        max_attempts=delivery.max_attempts,
        last_attempt_at=delivery.last_attempt_at,
        next_attempt_at=delivery.next_attempt_at,
        delivered_at=delivery.delivered_at,
        last_error=delivery.last_error,
        last_response_code=delivery.last_response_code,
        created_at=delivery.created_at,
        updated_at=delivery.updated_at,
    )
