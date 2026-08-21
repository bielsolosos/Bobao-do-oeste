from datetime import datetime
from typing import Any, Dict, List, Optional
import uuid
from sqlmodel import Field, Relationship, SQLModel, Column, JSON
from src.domain.enums import DeliveryTypeEnum, ExecutionStatusEnum, VendorEnum


def generate_uuid() -> str:
    return str(uuid.uuid4())


class SearchQuery(SQLModel, table=True):
    __tablename__ = "search_queries"

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    vendor: VendorEnum = Field(default=VendorEnum.OLX, index=True)
    keyword: str = Field(index=True)
    state_region: Optional[str] = Field(default=None, description="e.g. sp, sao-paulo-e-regiao")
    category: Optional[str] = Field(default=None, description="e.g. informatica-e-acessorios/notebooks")
    min_price: Optional[float] = Field(default=None)
    max_price: Optional[float] = Field(default=None)
    require_delivery: bool = Field(default=False)

    created_at: datetime = Field(default_factory=datetime.utcnow)
    updated_at: datetime = Field(default_factory=datetime.utcnow)

    executions: List["ScrapingExecution"] = Relationship(back_populates="search_query")


class ScrapingExecution(SQLModel, table=True):
    __tablename__ = "scraping_executions"

    id: str = Field(default_factory=generate_uuid, primary_key=True, index=True)
    search_query_id: str = Field(foreign_key="search_queries.id", index=True)
    vendor: VendorEnum = Field(default=VendorEnum.OLX, index=True)

    started_at: datetime = Field(default_factory=datetime.utcnow)
    finished_at: Optional[datetime] = Field(default=None)
    duration_ms: Optional[int] = Field(default=None)

    status: ExecutionStatusEnum = Field(default=ExecutionStatusEnum.PENDING, index=True)
    total_found: int = Field(default=0)
    new_items_count: int = Field(default=0)
    used_fallback: bool = Field(default=False)
    error_message: Optional[str] = Field(default=None)

    search_query: Optional[SearchQuery] = Relationship(back_populates="executions")
    listings: List["ScrapedListing"] = Relationship(back_populates="execution")


class ScrapedListing(SQLModel, table=True):
    __tablename__ = "scraped_listings"

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
    scraped_at: datetime = Field(default_factory=datetime.utcnow, index=True)

    raw_payload: Optional[Dict[str, Any]] = Field(default=None, sa_column=Column(JSON))

    execution: Optional[ScrapingExecution] = Relationship(back_populates="listings")
