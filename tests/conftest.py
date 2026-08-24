import pytest

from src.core.database import init_db


@pytest.fixture(autouse=True, scope="session")
async def initialize_test_database():
    await init_db()
