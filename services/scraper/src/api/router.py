from fastapi import APIRouter, Depends

from src.api.v1.async_scrape_routes import router as async_scrape_router
from src.api.v1.dashboard_routes import router as dashboard_router
from src.api.v1.execution_routes import router as executions_router
from src.api.v1.listing_routes import router as listings_router
from src.api.v1.queue_routes import router as queue_router
from src.api.v1.scrape_routes import router as scrape_router
from src.api.v1.webhook_routes import router as webhook_router
from src.core.security import verify_basic_auth

# Rotas de API v1 (Protegidas com HTTP Basic Auth)
api_router = APIRouter(prefix="/api/v1", dependencies=[Depends(verify_basic_auth)])
api_router.include_router(scrape_router)
api_router.include_router(async_scrape_router)
api_router.include_router(executions_router)
api_router.include_router(listings_router)
api_router.include_router(webhook_router)
api_router.include_router(queue_router)

# Router principal
main_router = APIRouter()

# TODO: [TEMPORARY-UI] Remover o include do dashboard_router quando a SPA frontend dedicada for criada
main_router.include_router(dashboard_router)
main_router.include_router(api_router)
