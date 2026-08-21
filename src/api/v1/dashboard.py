"""
TODO: [TEMPORARY-UI]
Este dashboard server-side com Jinja2 foi criado exclusivamente para validação
rápida e inspeção visual das tabelas no início do projeto.

Quando o Frontend oficial (SPA em Angular / Next.js) for implementado:
1. Remover este arquivo (src/api/v1/dashboard.py).
2. Remover o diretório de templates (src/views/).
3. Desinstalar a dependência 'jinja2' via: uv remove jinja2.
4. Desacoplar este router de src/api/router.py.
"""

from pathlib import Path
from typing import Any
from fastapi import APIRouter, Depends, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates
from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import select
from src.core.database import get_session
from src.core.security import verify_basic_auth
from src.domain.models import ScrapedListing, ScrapingExecution, SearchQuery

# Configure Jinja2 templates directory
templates_dir = Path(__file__).parent.parent.parent / "views"
templates = Jinja2Templates(directory=str(templates_dir))

router = APIRouter(tags=["Dashboard"])


@router.get(
    "/dashboard",
    response_class=HTMLResponse,
    summary="[TEMPORÁRIO] Interactive Web Dashboard for Scraping Telemetry & Listings",
)
@router.get(
    "/",
    response_class=HTMLResponse,
    summary="[TEMPORÁRIO] Root route redirecting to dashboard",
)
async def view_dashboard(
    request: Request,
    username: str = Depends(verify_basic_auth),
    session: AsyncSession = Depends(get_session),
) -> Any:
    # 1. Fetch Listings
    stmt_listings = select(ScrapedListing).order_by(ScrapedListing.scraped_at.desc()).limit(150)
    result_listings = await session.execute(stmt_listings)
    listings = result_listings.scalars().all()

    # 2. Fetch Executions
    stmt_execs = select(ScrapingExecution).order_by(ScrapingExecution.started_at.desc()).limit(50)
    result_execs = await session.execute(stmt_execs)
    executions = result_execs.scalars().all()

    # 3. Fetch Search Queries
    stmt_queries = select(SearchQuery).order_by(SearchQuery.created_at.desc()).limit(50)
    result_queries = await session.execute(stmt_queries)
    queries = result_queries.scalars().all()

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
