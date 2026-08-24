from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from src.core.database import get_session
from src.domain.enums import VendorEnum
from src.domain.schemas import ScrapedListingDTO
from src.domain.services import ListingService

router = APIRouter(prefix="/listings", tags=["Listings"])


@router.get(
    "",
    response_model=List[ScrapedListingDTO],
    summary="Consulta anúncios coletados no banco de dados",
)
async def list_listings(
    vendor: Optional[VendorEnum] = None,
    execution_id: Optional[str] = None,
    keyword: Optional[str] = None,
    min_price: Optional[float] = None,
    max_price: Optional[float] = None,
    has_delivery: Optional[bool] = None,
    limit: int = Query(default=50, ge=1, le=200),
    offset: int = Query(default=0, ge=0),
    session: AsyncSession = Depends(get_session),
) -> List[ScrapedListingDTO]:
    service = ListingService(session)
    return await service.list_listings(
        vendor=vendor,
        execution_id=execution_id,
        keyword=keyword,
        min_price=min_price,
        max_price=max_price,
        has_delivery=has_delivery,
        limit=limit,
        offset=offset,
    )
