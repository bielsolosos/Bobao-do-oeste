from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.core.database import get_session
from src.domain.schemas import (
    AsyncScrapeRequest,
    AsyncScrapeResponse,
)
from src.domain.services import AsyncScrapeService

router = APIRouter(prefix="/scrape", tags=["Scraping"])


@router.post(
    "/async",
    response_model=AsyncScrapeResponse,
    status_code=status.HTTP_202_ACCEPTED,
    summary="Enfileira scraping e envia resultado via webhook (não bloqueia)",
)
async def trigger_async_scrape(
    body: AsyncScrapeRequest,
    session: AsyncSession = Depends(get_session),
) -> AsyncScrapeResponse:
    """
    Aceita uma requisição de scraping e a coloca na fila. Retorna 202 imediatamente
    com um `requestId`. O resultado do scraping é enviado via POST para a
    `webhookUrl` quando o worker termina (com retry exponencial se necessário).

    - Se `requestId` for fornecido e já existir no sistema → 409 Conflict.
    - Se omitido, um UUID v4 é gerado e retornado na resposta.
    """
    service = AsyncScrapeService(session)
    try:
        job, delivery = await service.enqueue_with_webhook(
            request=body.request,
            webhook_url=str(body.webhook_url),
            request_id=body.request_id,
        )
    except ValueError as e:
        raise HTTPException(
            status_code=status.HTTP_409_CONFLICT,
            detail=str(e),
        ) from e

    return AsyncScrapeResponse(
        request_id=delivery.request_id,
        job_id=job.id,
        status="queued",
        webhook_url=delivery.webhook_url,
    )
