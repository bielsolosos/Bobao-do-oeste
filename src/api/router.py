from fastapi import APIRouter, Depends
from src.api.v1.dashboard import router as dashboard_router
from src.api.v1.executions import router as executions_router
from src.api.v1.listings import router as listings_router
from src.api.v1.scrape import router as scrape_router
from src.core.security import verify_basic_auth

# Rotas de API v1 (Protegidas com HTTP Basic Auth)
api_router = APIRouter(prefix="/api/v1", dependencies=[Depends(verify_basic_auth)])
api_router.include_router(scrape_router)
api_router.include_router(executions_router)
api_router.include_router(listings_router)

# Router principal
main_router = APIRouter()

# TODO: [TEMPORARY-UI] Remover o include do dashboard_router quando a SPA frontend dedicada for criada
main_router.include_router(dashboard_router)
main_router.include_router(api_router)
