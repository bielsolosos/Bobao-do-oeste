from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.core.config import settings
from src.core.database import get_session
from src.core.job_queue import JobQueueService
from src.core.logger import logger
from src.domain.enums import ExecutionStatusEnum
from src.domain.schemas import ExecutionSummaryDTO, ScrapeRequest, ScrapeResponse

router = APIRouter(prefix="/scrape", tags=["Scraping"])


@router.post(
    "",
    response_model=ScrapeResponse,
    status_code=status.HTTP_200_OK,
    summary="Enfileira e executa coleta no marketplace (aguarda na fila)",
)
async def trigger_scrape(
    request: ScrapeRequest,
    session: AsyncSession = Depends(get_session),
) -> ScrapeResponse:
    """
    Enfileira a requisição de scraping e bloqueia até o pool de workers processá-la.

    O contrato HTTP permanece síncrono: o cliente recebe o `ScrapeResponse`
    completo ao final. A concorrência efetiva é limitada por
    `SCRAPE_WORKER_CONCURRENCY` no pool de workers.
    """
    queue = JobQueueService(session)
    job = await queue.enqueue(request)

    final_job = await queue.wait_for_completion(job.id, timeout=float(settings.SCRAPE_JOB_TIMEOUT_SECONDS))

    if final_job.response_payload is not None:
        return ScrapeResponse.model_validate(final_job.response_payload)

    logger.error(f"Job {final_job.id} terminou sem response_payload")
    return ScrapeResponse(
        success=False,
        execution=ExecutionSummaryDTO(
            id=final_job.id,
            vendor=final_job.vendor,
            status=ExecutionStatusEnum.FAILED,
            error_message=final_job.error_message or "Worker encerrou sem payload",
            started_at=final_job.created_at,
            finished_at=final_job.finished_at,
        ),
        items=[],
    )
