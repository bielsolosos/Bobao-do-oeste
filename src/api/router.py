from fastapi import APIRouter
from src.api.v1.executions import router as executions_router
from src.api.v1.listings import router as listings_router
from src.api.v1.scrape import router as scrape_router

api_router = APIRouter(prefix="/api/v1")
api_router.include_router(scrape_router)
api_router.include_router(executions_router)
api_router.include_router(listings_router)
