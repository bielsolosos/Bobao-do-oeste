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

    # Job Queue Settings
    SCRAPE_WORKER_CONCURRENCY: int = 1
    SCRAPE_WORKER_POLL_INTERVAL: float = 1.0
    SCRAPE_JOB_TIMEOUT_SECONDS: int = 300

    # Webhook Delivery Settings (async endpoint)
    WEBHOOK_DISPATCHER_CONCURRENCY: int = 2
    WEBHOOK_DISPATCHER_POLL_INTERVAL: float = 2.0
    WEBHOOK_DELIVERY_TIMEOUT_SECONDS: int = 10
    WEBHOOK_DELIVERY_MAX_ATTEMPTS: int = 5
    WEBHOOK_DELIVERY_BASE_BACKOFF_SECONDS: float = 1.0
    WEBHOOK_DELIVERY_MAX_BACKOFF_SECONDS: float = 60.0
    WEBHOOK_STUCK_TIMEOUT_SECONDS: int = 60

    # Cache Cleanup Background Task Settings (1 hour by default)
    CACHE_CLEANUP_INTERVAL_SECONDS: int = 3600

    @property
    def is_sqlite(self) -> bool:
        return self.DATABASE_URL.startswith("sqlite")


settings = Settings()
