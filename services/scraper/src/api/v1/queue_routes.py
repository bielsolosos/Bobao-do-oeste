from fastapi import APIRouter, Depends
from sqlalchemy.ext.asyncio import AsyncSession

from src.core.database import get_session
from src.domain.schemas import QueueStatusDTO
from src.domain.services import QueueService

router = APIRouter(prefix="/queue", tags=["Queue"])


@router.get(
    "/status",
    response_model=QueueStatusDTO,
    summary="Retorna contagem em tempo real dos jobs e webhooks na fila",
)
async def get_queue_status(session: AsyncSession = Depends(get_session)) -> QueueStatusDTO:
    service = QueueService(session)
    return await service.get_queue_status()
