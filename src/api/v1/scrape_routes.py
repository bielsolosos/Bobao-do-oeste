from fastapi import APIRouter, Depends, status
from sqlalchemy.ext.asyncio import AsyncSession

from src.core.database import get_session
from src.domain.schemas import ScrapeRequest, ScrapeResponse
from src.domain.services import ScrapingService

router = APIRouter(prefix="/scrape", tags=["Scraping"])


@router.post("", response_model=ScrapeResponse, status_code=status.HTTP_200_OK, summary="Dispara coleta no marketplace")
async def trigger_scrape(
    request: ScrapeRequest,
    session: AsyncSession = Depends(get_session),
) -> ScrapeResponse:
    service = ScrapingService(session)
    return await service.execute_scrape(request)
