from typing import List, Optional

from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.core.database import get_session
from src.domain.enums import ExecutionStatusEnum, VendorEnum
from src.domain.schemas import ExecutionSummaryDTO
from src.domain.services import ExecutionService

router = APIRouter(prefix="/executions", tags=["Executions"])


@router.get(
    "",
    response_model=List[ExecutionSummaryDTO],
    summary="Lista o histórico de execuções de scraping",
)
async def list_executions(
    vendor: Optional[VendorEnum] = None,
    status_filter: Optional[ExecutionStatusEnum] = Query(default=None, alias="status"),
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    session: AsyncSession = Depends(get_session),
) -> List[ExecutionSummaryDTO]:
    service = ExecutionService(session)
    return await service.list_executions(
        vendor=vendor,
        status=status_filter,
        limit=limit,
        offset=offset,
    )


@router.get("/{execution_id}", response_model=ExecutionSummaryDTO, summary="Consulta telemetria de uma execução por ID")
async def get_execution(
    execution_id: str,
    session: AsyncSession = Depends(get_session),
) -> ExecutionSummaryDTO:
    service = ExecutionService(session)
    execution = await service.get_execution_by_id(execution_id)
    if not execution:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Execução com ID '{execution_id}' não encontrada",
        )
    return execution
