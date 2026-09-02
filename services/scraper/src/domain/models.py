import uuid
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from sqlalchemy import LargeBinary, Text
from sqlmodel import JSON, Column, Field, Relationship, SQLModel, UniqueConstraint

from src.domain.enums import DeliveryStatusEnum, DeliveryTypeEnum, ExecutionStatusEnum, JobStatusEnum, VendorEnum


def generate_uuid() -> str:
    return str(uuid.uuid4())


def get_utc_now() -> datetime:
    return datetime.now(timezone.utc)


class SearchQuery(SQLModel, table=True):
    __tablename__: Any = "search_queries"

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    vendor: VendorEnum = Field(default=VendorEnum.OLX, index=True)
    keyword: str = Field(index=True)
    state_region: Optional[str] = Field(default=None, description="e.g. sp, sao-paulo-e-regiao")
    category: Optional[str] = Field(default=None, description="e.g. informatica-e-acessorios/notebooks")
    min_price: Optional[float] = Field(default=None)
    max_price: Optional[float] = Field(default=None)
    require_delivery: bool = Field(default=False)

    created_at: datetime = Field(default_factory=get_utc_now)
    updated_at: datetime = Field(default_factory=get_utc_now)

    executions: List["ScrapingExecution"] = Relationship(back_populates="search_query")


class ScrapingExecution(SQLModel, table=True):
    __tablename__: Any = "scraping_executions"

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    search_query_id: str = Field(foreign_key="search_queries.id", index=True)
    vendor: VendorEnum = Field(default=VendorEnum.OLX, index=True)

    started_at: datetime = Field(default_factory=get_utc_now)
    finished_at: Optional[datetime] = Field(default=None)
    duration_ms: Optional[int] = Field(default=None)

    status: ExecutionStatusEnum = Field(default=ExecutionStatusEnum.RUNNING, index=True)
    total_found: int = Field(default=0)
    new_items_count: int = Field(default=0)
    used_fallback: bool = False
    error_message: Optional[str] = Field(default=None)

    search_query: Optional[SearchQuery] = Relationship(back_populates="executions")
    listings: List["ScrapedListing"] = Relationship(back_populates="execution")


class ScrapedListing(SQLModel, table=True):
    __tablename__: Any = "scraped_listings"
    __table_args__ = (UniqueConstraint("vendor", "vendor_listing_id", name="uq_scraped_listing_vendor_id"),)

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    execution_id: str = Field(foreign_key="scraping_executions.id", index=True)
    vendor: VendorEnum = Field(default=VendorEnum.OLX, index=True)
    vendor_listing_id: str = Field(index=True, description="Marketplace native ID (e.g. 1389472918)")

    title: str = Field(index=True)
    price: float = Field(index=True)
    original_price: Optional[float] = Field(default=None)
    url: str = Field(unique=False)
    description: Optional[str] = Field(default=None)

    state: Optional[str] = Field(default=None, index=True)
    city: Optional[str] = Field(default=None)
    neighborhood: Optional[str] = Field(default=None)

    has_delivery: bool = Field(default=False, index=True)
    delivery_type: DeliveryTypeEnum = Field(default=DeliveryTypeEnum.UNKNOWN)

    images: List[str] = Field(default_factory=list, sa_column=Column(JSON))
    published_at: Optional[datetime] = Field(default=None)
    scraped_at: datetime = Field(default_factory=get_utc_now, index=True)

    raw_payload: Optional[Dict[str, Any]] = Field(default=None, sa_column=Column(JSON))

    execution: Optional[ScrapingExecution] = Relationship(back_populates="listings")


class ScrapeJob(SQLModel, table=True):
    __tablename__: Any = "scrape_jobs"

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    vendor: VendorEnum = Field(default=VendorEnum.OLX, index=True)
    request_payload: Dict[str, Any] = Field(sa_column=Column(JSON))

    status: JobStatusEnum = Field(default=JobStatusEnum.QUEUED, index=True)
    priority: int = Field(default=0, index=True)
    attempts: int = Field(default=0)
    worker_id: Optional[str] = Field(default=None)

    created_at: datetime = Field(default_factory=get_utc_now, index=True)
    started_at: Optional[datetime] = Field(default=None)
    finished_at: Optional[datetime] = Field(default=None)

    response_payload: Optional[Dict[str, Any]] = Field(default=None, sa_column=Column(JSON))
    error_message: Optional[str] = Field(default=None)


class WebhookDelivery(SQLModel, table=True):
    __tablename__: Any = "webhook_deliveries"

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    scrape_job_id: str = Field(foreign_key="scrape_jobs.id", index=True)
    request_id: str = Field(unique=True, index=True)
    webhook_url: str

    status: DeliveryStatusEnum = Field(default=DeliveryStatusEnum.PENDING, index=True)
    attempts: int = Field(default=0)
    max_attempts: int = Field(default=5)
    worker_id: Optional[str] = Field(default=None)
    last_attempt_at: Optional[datetime] = Field(default=None)
    next_attempt_at: Optional[datetime] = Field(default=None, index=True)
    delivered_at: Optional[datetime] = Field(default=None)
    last_error: Optional[str] = Field(default=None)
    last_response_code: Optional[int] = Field(default=None)

    created_at: datetime = Field(default_factory=get_utc_now)
    updated_at: datetime = Field(default_factory=get_utc_now)


class AdImageCache(SQLModel, table=True):
    __tablename__: Any = "ad_image_cache"
    __table_args__ = (UniqueConstraint("vendor", "vendor_listing_id", "image_index", name="uq_ad_image_cache"),)

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    vendor: VendorEnum = Field(default=VendorEnum.OLX, index=True)
    vendor_listing_id: str = Field(index=True)
    image_index: int = Field(default=0)
    original_url: str = Field(sa_column=Column(Text, nullable=False))
    image_bytes: bytes = Field(sa_column=Column(LargeBinary, nullable=False))
    mime_type: str = Field(default="image/jpeg")
    size_bytes: int = Field(default=0)
    created_at: datetime = Field(default_factory=get_utc_now, index=True)
    expires_at: datetime = Field(index=True)


class AdDetailCache(SQLModel, table=True):
    __tablename__: Any = "ad_detail_cache"
    __table_args__ = (UniqueConstraint("vendor", "vendor_listing_id", name="uq_ad_detail_cache"),)

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    vendor: VendorEnum = Field(default=VendorEnum.OLX, index=True)
    vendor_listing_id: str = Field(index=True)
    url: str = Field(sa_column=Column(Text, nullable=False))
    parsed_payload: Dict[str, Any] = Field(sa_column=Column(JSON))
    created_at: datetime = Field(default_factory=get_utc_now, index=True)
    expires_at: datetime = Field(index=True)
