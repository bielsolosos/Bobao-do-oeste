import os

import pytest

os.environ["APP_ENV"] = "testing"
os.environ["DATABASE_URL"] = "sqlite+aiosqlite:///./data/test_scraper.db"

from src.core.config import settings

settings.APP_ENV = "testing"
settings.DATABASE_URL = "sqlite+aiosqlite:///./data/test_scraper.db"

from src.core.database import init_db  # noqa: E402
from src.core.workers.scrape import get_scrape_worker  # noqa: E402
from src.core.workers.webhook import get_webhook_worker  # noqa: E402


@pytest.fixture(autouse=True, scope="session")
async def initialize_test_database():
    await init_db()

    yield
    await get_scrape_worker().stop()
    await get_webhook_worker().stop()


@pytest.fixture(autouse=True)
async def stop_workers_after_test():
    await get_scrape_worker().stop()
    await get_webhook_worker().stop()
    yield
    await get_scrape_worker().stop()
    await get_webhook_worker().stop()
