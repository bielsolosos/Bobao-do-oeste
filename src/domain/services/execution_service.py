"""
Serviço de Domínio para Gestão e Telemetria de Execuções (ExecutionService).
"""

from typing import List, Optional

from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import col, select

from src.domain.enums import ExecutionStatusEnum, VendorEnum
from src.domain.models import ScrapingExecution
from src.domain.schemas import ExecutionSummaryDTO


class ExecutionService:
    """Serviço responsável pela consulta e auditoria do histórico de execuções."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def list_executions(
        self,
        vendor: Optional[VendorEnum] = None,
        status: Optional[ExecutionStatusEnum] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> List[ExecutionSummaryDTO]:
        stmt = select(ScrapingExecution).order_by(col(ScrapingExecution.started_at).desc())

        if vendor:
            stmt = stmt.where(ScrapingExecution.vendor == vendor)
        if status:
            stmt = stmt.where(ScrapingExecution.status == status)

        stmt = stmt.offset(offset).limit(limit)
        result = await self.session.execute(stmt)
        executions = result.scalars().all()

        return [ExecutionSummaryDTO.model_validate(execution) for execution in executions]

    async def get_execution_by_id(self, execution_id: str) -> Optional[ExecutionSummaryDTO]:
        stmt = select(ScrapingExecution).where(ScrapingExecution.id == execution_id)
        result = await self.session.execute(stmt)
        execution = result.scalars().first()

        if not execution:
            return None

        return ExecutionSummaryDTO.model_validate(execution)

    async def get_recent_executions(self, limit: int = 50) -> List[ScrapingExecution]:
        """Consulta execuções recentes como entidades ORM (usado na view do dashboard)."""
        stmt = select(ScrapingExecution).order_by(col(ScrapingExecution.started_at).desc()).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())
