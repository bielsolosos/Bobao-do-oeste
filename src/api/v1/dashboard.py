"""
TODO: [TEMPORARY-UI]
Dashboard server-side com Jinja2 para validação e inspeção visual das tabelas.
"""

from pathlib import Path
from typing import Any
from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.ext.asyncio import AsyncSession
from src.core.database import get_session
from src.core.security import verify_basic_auth
from src.domain.services import ExecutionService, ListingService, ScrapingService

templates_dir = Path(__file__).parent.parent.parent / "views"
templates = Jinja2Templates(directory=str(templates_dir))

router = APIRouter(tags=["Dashboard"])


@router.get(
    "/dashboard",
    response_class=HTMLResponse,
    summary="[TEMPORÁRIO] Dashboard Web para Visualização de Listagens e Telemetria",
)
@router.get(
    "/",
    response_class=HTMLResponse,
    summary="[TEMPORÁRIO] Rota raiz redirecionando para dashboard",
)
async def view_dashboard(
    request: Request,
    username: str = Depends(verify_basic_auth),
    session: AsyncSession = Depends(get_session),
) -> Any:
    listing_service = ListingService(session)
    execution_service = ExecutionService(session)
    scraping_service = ScrapingService(session)

    listings = await listing_service.get_recent_listings(limit=150)
    executions = await execution_service.get_recent_executions(limit=50)
    queries = await scraping_service.get_recent_queries(limit=50)

    return templates.TemplateResponse(
        request=request,
        name="dashboard.html",
        context={
            "username": username,
            "listings": listings,
            "executions": executions,
            "queries": queries,
            "hasattr": hasattr,
        },
    )
