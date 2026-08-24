"""
Serviço de Domínio para Gestão e Consulta de Anúncios Coletados (ListingService).
"""

from typing import List, Optional
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import col, select
from src.domain.enums import VendorEnum
from src.domain.models import ScrapedListing
from src.domain.schemas import ScrapedListingDTO


class ListingService:
    """Serviço responsável pela consulta e filtragem de anúncios coletados."""

    def __init__(self, session: AsyncSession):
        self.session = session

    async def list_listings(
        self,
        vendor: Optional[VendorEnum] = None,
        execution_id: Optional[str] = None,
        keyword: Optional[str] = None,
        min_price: Optional[float] = None,
        max_price: Optional[float] = None,
        has_delivery: Optional[bool] = None,
        limit: int = 50,
        offset: int = 0,
    ) -> List[ScrapedListingDTO]:
        stmt = select(ScrapedListing).order_by(col(ScrapedListing.scraped_at).desc())

        if vendor:
            stmt = stmt.where(ScrapedListing.vendor == vendor)
        if execution_id:
            stmt = stmt.where(ScrapedListing.execution_id == execution_id)
        if keyword:
            stmt = stmt.where(col(ScrapedListing.title).ilike(f"%{keyword}%"))
        if min_price is not None:
            stmt = stmt.where(ScrapedListing.price >= min_price)
        if max_price is not None:
            stmt = stmt.where(ScrapedListing.price <= max_price)
        if has_delivery is not None:
            stmt = stmt.where(ScrapedListing.has_delivery == has_delivery)

        stmt = stmt.offset(offset).limit(limit)
        result = await self.session.execute(stmt)
        listings = result.scalars().all()

        return [ScrapedListingDTO.model_validate(listing) for listing in listings]

    async def get_recent_listings(self, limit: int = 150) -> List[ScrapedListing]:
        """Consulta anúncios recentes como entidades ORM (usado na view do dashboard)."""
        stmt = select(ScrapedListing).order_by(col(ScrapedListing.scraped_at).desc()).limit(limit)
        result = await self.session.execute(stmt)
        return list(result.scalars().all())

