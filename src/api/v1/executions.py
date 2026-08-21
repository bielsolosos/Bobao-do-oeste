from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import select
from src.core.database import get_session
from src.domain.enums import ExecutionStatusEnum, VendorEnum
from src.domain.models import ScrapingExecution
from src.domain.schemas import ExecutionSummaryDTO

router = APIRouter(prefix="/executions", tags=["Executions"])


@router.get(
    "",
    response_model=List[ExecutionSummaryDTO],
    summary="List all scraping executions with telemetry",
)
async def list_executions(
    vendor: Optional[VendorEnum] = None,
    status_filter: Optional[ExecutionStatusEnum] = Query(default=None, alias="status"),
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    session: AsyncSession = Depends(get_session),
) -> List[ExecutionSummaryDTO]:
    stmt = select(ScrapingExecution).order_by(ScrapingExecution.started_at.desc())

    if vendor:
        stmt = stmt.where(ScrapingExecution.vendor == vendor)
    if status_filter:
        stmt = stmt.where(ScrapingExecution.status == status_filter)

    stmt = stmt.offset(offset).limit(limit)
    result = await session.execute(stmt)
    executions = result.scalars().all()

    return [
        ExecutionSummaryDTO(
            execution_id=e.id,
            vendor=e.vendor,
            status=e.status,
            duration_ms=e.duration_ms,
            total_found=e.total_found,
            new_items_count=e.new_items_count,
            used_fallback=e.used_fallback,
            error_message=e.error_message,
            started_at=e.started_at,
            finished_at=e.finished_at,
        )
        for e in executions
    ]


@router.get(
    "/{execution_id}",
    response_model=ExecutionSummaryDTO,
    summary="Get execution telemetry by ID",
)
async def get_execution(
    execution_id: str,
    session: AsyncSession = Depends(get_session),
) -> ExecutionSummaryDTO:
    stmt = select(ScrapingExecution).where(ScrapingExecution.id == execution_id)
    result = await session.execute(stmt)
    execution = result.scalars().first()

    if not execution:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Execution with ID '{execution_id}' not found",
        )

    return ExecutionSummaryDTO(
        execution_id=execution.id,
        vendor=execution.vendor,
        status=execution.status,
        duration_ms=execution.duration_ms,
        total_found=execution.total_found,
        new_items_count=execution.new_items_count,
        used_fallback=execution.used_fallback,
        error_message=execution.error_message,
        started_at=execution.started_at,
        finished_at=execution.finished_at,
    )
