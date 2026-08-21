from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.ext.asyncio import AsyncSession
from src.core.database import get_session
from src.domain.schemas import ScrapeRequest, ScrapeResponse
from src.services.orchestrator import ScrapingOrchestrator

router = APIRouter(prefix="/scrape", tags=["Scraping"])


@router.post(
    "",
    response_model=ScrapeResponse,
    status_code=status.HTTP_200_OK,
    summary="Trigger a marketplace scrape job",
)
async def trigger_scrape(
    request: ScrapeRequest,
    session: AsyncSession = Depends(get_session),
) -> ScrapeResponse:
    orchestrator = ScrapingOrchestrator(session=session)
    response = await orchestrator.execute_scrape(request)
    return response
