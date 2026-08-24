import os
from pathlib import Path
from typing import AsyncGenerator

from sqlalchemy import event
from sqlalchemy.ext.asyncio import AsyncSession, async_sessionmaker, create_async_engine
from sqlmodel import SQLModel

from src.core.config import settings
from src.core.logger import logger

# Ensure directory for SQLite database exists
if settings.is_sqlite:
    db_path = settings.DATABASE_URL.replace("sqlite+aiosqlite:///", "")
    db_dir = Path(db_path).parent
    os.makedirs(db_dir, exist_ok=True)

engine = create_async_engine(
    settings.DATABASE_URL,
    echo=False,
    future=True,
)


@event.listens_for(engine.sync_engine, "connect")
def _enable_sqlite_foreign_keys(dbapi_connection, connection_record):
    """Habilita enforcement de FK no SQLite (OFF por default)."""
    if settings.is_sqlite:
        cursor = dbapi_connection.cursor()
        cursor.execute("PRAGMA foreign_keys=ON")
        cursor.close()


async_session_maker = async_sessionmaker(
    bind=engine,
    class_=AsyncSession,
    expire_on_commit=False,
)


async def init_db() -> None:
    """Initialize tables in the database."""
    async with engine.begin() as conn:
        logger.info("Initializing database schema...")
        await conn.run_sync(SQLModel.metadata.create_all)
        logger.info("Database schema initialized successfully.")


async def get_session() -> AsyncGenerator[AsyncSession, None]:
    """Dependency for providing database sessions."""
    async with async_session_maker() as session:
        try:
            yield session
        except Exception:
            await session.rollback()
            raise
        finally:
            await session.close()
