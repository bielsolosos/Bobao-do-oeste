from fastapi import APIRouter, Depends, HTTPException, Response, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.core.config import settings
from src.core.database import get_session
from src.core.logger import logger
from src.core.queues.scrape import ScrapeQueueService
from src.domain.enums import ExecutionStatusEnum
from src.domain.schemas import (
    ExecutionSummaryDTO,
    ScrapeDetailRequest,
    ScrapeDetailResponse,
    ScrapeRequest,
    ScrapeResponse,
)
from src.domain.services.image_cache_service import ImageCacheService
from src.domain.services.scrape_detail_service import ScrapeDetailService

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
    queue = ScrapeQueueService(session)
    job = await queue.enqueue(request)
    await session.commit()

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


@router.post(
    "/detail",
    response_model=ScrapeDetailResponse,
    status_code=status.HTTP_200_OK,
    summary="Extrai dados profundos e galeria de fotos de uma URL direta de anúncio",
)
async def scrape_ad_detail(
    request: ScrapeDetailRequest,
    session: AsyncSession = Depends(get_session),
) -> ScrapeDetailResponse:
    """
    Raspagem aprofundada de um anúncio específico por URL:
    - Extrai descrição completa, atributos/especificações e galeria de fotos.
    - Baixa e persiste imagens no SQLite local com TTL (expiração automática).
    - Utiliza cache inteligente para evitar downloads e requisições repetidas.
    """
    service = ScrapeDetailService(session)
    return await service.execute(request)


@router.get(
    "/images/{image_id}",
    status_code=status.HTTP_200_OK,
    summary="Recupera imagem binária cacheada no SQLite",
)
async def get_cached_image(
    image_id: str,
    session: AsyncSession = Depends(get_session),
):
    """
    Serve os bytes de uma imagem previamente baixada e salva no SQLite.
    Retorna 404 caso a imagem não exista ou seu TTL tenha expirado.
    """
    service = ImageCacheService(session)
    result = await service.get_image_by_id(image_id)
    if not result:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Imagem não encontrada ou expirada do cache.",
        )

    image_bytes, mime_type = result
    return Response(
        content=image_bytes,
        media_type=mime_type,
        headers={"Cache-Control": "public, max-age=86400"},
    )


@router.post(
    "/images/cleanup",
    status_code=status.HTTP_200_OK,
    summary="Limpa imagens expiradas do SQLite",
)
async def cleanup_expired_images(
    session: AsyncSession = Depends(get_session),
):
    """Executa a rotina de exclusão de imagens cujo TTL já expirou."""
    service = ImageCacheService(session)
    deleted_count = await service.cleanup_expired()
    return {"success": True, "deleted_images": deleted_count}

