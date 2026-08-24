from datetime import datetime, timezone
from typing import Any, Dict, List, Optional
from pydantic import BaseModel, ConfigDict, Field
from src.domain.enums import DeliveryTypeEnum, ExecutionStatusEnum, VendorEnum


class ScrapeRequest(BaseModel):
    vendor: VendorEnum = Field(default=VendorEnum.OLX, description="Marketplace platform")
    keyword: str = Field(..., min_length=2, description="Search term, e.g., 'thinkpad t480'")
    state: Optional[str] = Field(default=None, description="State code or slug, e.g., 'sp'")
    region: Optional[str] = Field(default=None, description="Region slug, e.g., 'sao-paulo-e-regiao'")
    category: Optional[str] = Field(
        default=None, description="Category slug, e.g., 'informatica-e-acessorios/notebooks'"
    )
    min_price: Optional[float] = Field(default=None, ge=0, description="Minimum price filter")
    max_price: Optional[float] = Field(default=None, ge=0, description="Maximum price filter")
    require_delivery: bool = Field(
        default=False, description="Filter/prioritize listings offering delivery (OLX Pay)"
    )
    max_pages: int = Field(default=1, ge=1, le=10, description="Maximum pages to scrape")
    force_browser: bool = Field(
        default=False, description="Force headless browser instead of HTTP client"
    )


class ScrapedListingDTO(BaseModel):
    model_config = ConfigDict(from_attributes=True)

    id: Optional[str] = None
    vendor: VendorEnum
    vendor_listing_id: str
    title: str
    price: float
    original_price: Optional[float] = None
    url: str
    description: Optional[str] = None
    state: Optional[str] = None
    city: Optional[str] = None
    neighborhood: Optional[str] = None
    has_delivery: bool = False
    delivery_type: DeliveryTypeEnum = DeliveryTypeEnum.UNKNOWN
    images: List[str] = Field(default_factory=list)
    published_at: Optional[datetime] = None
    scraped_at: Optional[datetime] = None


class ExecutionSummaryDTO(BaseModel):
    model_config = ConfigDict(from_attributes=True, populate_by_name=True)

    execution_id: str = Field(..., alias="id")
    vendor: VendorEnum
    status: ExecutionStatusEnum
    duration_ms: Optional[int] = None
    total_found: int = 0
    new_items_count: int = 0
    used_fallback: bool = False
    error_message: Optional[str] = None
    started_at: datetime
    finished_at: Optional[datetime] = None


class ScrapeResponse(BaseModel):
    success: bool
    execution: ExecutionSummaryDTO
    items: List[ScrapedListingDTO]

