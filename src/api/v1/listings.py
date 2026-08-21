from typing import List, Optional
from fastapi import APIRouter, Depends, Query
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import select
from src.core.database import get_session
from src.domain.enums import VendorEnum
from src.domain.models import ScrapedListing
from src.domain.schemas import ScrapedListingDTO

router = APIRouter(prefix="/listings", tags=["Listings"])


@router.get(
    "",
    response_model=List[ScrapedListingDTO],
    summary="Query stored marketplace listings",
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
    stmt = select(ScrapedListing).order_by(ScrapedListing.scraped_at.desc())

    if vendor:
        stmt = stmt.where(ScrapedListing.vendor == vendor)
    if execution_id:
        stmt = stmt.where(ScrapedListing.execution_id == execution_id)
    if keyword:
        stmt = stmt.where(ScrapedListing.title.ilike(f"%{keyword}%"))
    if min_price is not None:
        stmt = stmt.where(ScrapedListing.price >= min_price)
    if max_price is not None:
        stmt = stmt.where(ScrapedListing.price <= max_price)
    if has_delivery is not None:
        stmt = stmt.where(ScrapedListing.has_delivery == has_delivery)

    stmt = stmt.offset(offset).limit(limit)
    result = await session.execute(stmt)
    listings = result.scalars().all()

    return [
        ScrapedListingDTO(
            id=l.id,
            vendor=l.vendor,
            vendor_listing_id=l.vendor_listing_id,
            title=l.title,
            price=l.price,
            original_price=l.original_price,
            url=l.url,
            description=l.description,
            state=l.state,
            city=l.city,
            neighborhood=l.neighborhood,
            has_delivery=l.has_delivery,
            delivery_type=l.delivery_type,
            images=l.images or [],
            published_at=l.published_at,
            scraped_at=l.scraped_at,
        )
        for l in listings
    ]
