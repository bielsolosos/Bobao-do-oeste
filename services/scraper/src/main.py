import time
from contextlib import asynccontextmanager

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware

from src.api.router import main_router
from src.core.config import settings
from src.core.database import init_db
from src.core.logger import logger
from src.core.metrics import observe_request, prometheus_response
from src.core.workers.cache_cleanup import get_cache_cleanup_worker
from src.core.workers.scrape import get_scrape_worker
from src.core.workers.webhook import get_webhook_worker


@asynccontextmanager
async def lifespan(app: FastAPI):
    logger.info(f"Starting {settings.APP_NAME} in [{settings.APP_ENV}] mode...")
    await init_db()

    scrape_worker = get_scrape_worker()
    webhook_worker = get_webhook_worker()
    cleanup_worker = get_cache_cleanup_worker()

    if settings.APP_ENV != "testing":
        await scrape_worker.start()
        await webhook_worker.start()
        await cleanup_worker.start()

    try:
        yield
    finally:
        if settings.APP_ENV != "testing":
            await cleanup_worker.stop()
            await webhook_worker.stop()
            await scrape_worker.stop()
        logger.info(f"Shutting down {settings.APP_NAME}...")


app = FastAPI(
    title=settings.APP_NAME,
    description="Stateless & Resilient Marketplace Scraper Service with SQLite Execution History and Web UI",
    version="1.0.0",
    lifespan=lifespan,
)


# Request logging middleware
@app.middleware("http")
async def log_requests(request: Request, call_next):
    start_time = time.perf_counter()
    client_ip = request.client.host if request.client else "unknown"
    method = request.method
    url_path = request.url.path

    logger.info(f"--> [REQ] {method} {url_path} (from: {client_ip})")

    try:
        response = await observe_request(request, call_next)
        duration_ms = (time.perf_counter() - start_time) * 1000
        logger.info(f"<-- [RES] {method} {url_path} | Status: {response.status_code} ({duration_ms:.2f}ms)")
        return response
    except Exception as exc:
        duration_ms = (time.perf_counter() - start_time) * 1000
        logger.error(f"<-- [ERR] {method} {url_path} | Exception: {exc} ({duration_ms:.2f}ms)")
        raise


# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Mount all application routers (Dashboard + API v1)
app.include_router(main_router)


@app.get("/health", tags=["Health"])
async def health_check():
    """Public healthcheck endpoint."""
    return {
        "status": "healthy",
        "app": settings.APP_NAME,
        "env": settings.APP_ENV,
    }


# TODO: Restringir o endpoint do Prometheus a rede interna do Alloy no Coolify.
@app.get("/metrics", include_in_schema=False)
async def metrics():
    return prometheus_response()


if __name__ == "__main__":
    import uvicorn

    uvicorn.run(
        "src.main:app",
        host=settings.HOST,
        port=settings.PORT,
        reload=(settings.APP_ENV == "development"),
    )
