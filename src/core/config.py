from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    APP_NAME: str = "Marketplace Scraper Service"
    APP_ENV: str = "development"
    PORT: int = 8001
    HOST: str = "127.0.0.1"
    LOG_LEVEL: str = "INFO"

    # Basic Authentication Credentials
    BASIC_AUTH_USERNAME: str = "admin"
    BASIC_AUTH_PASSWORD: str = "admin"

    # Database Configuration (SQLite async by default)
    DATABASE_URL: str = "sqlite+aiosqlite:///./data/scraper.db"

    # Scraping Engine Settings
    DEFAULT_TIMEOUT_SECONDS: int = 15
    MAX_RETRIES: int = 2
    ENABLE_PLAYWRIGHT_FALLBACK: bool = True
    HEADLESS: bool = True

    @property
    def is_sqlite(self) -> bool:
        return self.DATABASE_URL.startswith("sqlite")


settings = Settings()
