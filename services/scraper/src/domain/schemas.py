from datetime import datetime
from typing import Any, Dict, List, Optional

from pydantic import BaseModel, ConfigDict, Field, HttpUrl

from src.domain.enums import (
    DeliveryStatusEnum,
    DeliveryTypeEnum,
    ExecutionStatusEnum,
    VendorEnum,
)


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
    require_delivery: bool = Field(default=False, description="Filter/prioritize listings offering delivery (OLX Pay)")
    max_pages: int = Field(default=1, ge=1, le=10, description="Maximum pages to scrape")
    force_browser: bool = Field(default=False, description="Force headless browser instead of HTTP client")


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


class AsyncScrapeRequest(BaseModel):
    """Request para o endpoint assíncrono. Enfileira e retorna 202 imediatamente."""

    model_config = ConfigDict(populate_by_name=True)

    request: ScrapeRequest = Field(..., description="Parâmetros do scraping (igual ao endpoint síncrono)")
    webhook_url: HttpUrl = Field(
        ...,
        alias="webhookUrl",
        description="URL que receberá o resultado via POST quando o scraping terminar",
    )
    request_id: Optional[str] = Field(
        default=None,
        alias="requestId",
        max_length=255,
        description="ID opcional do cliente. Se omitido, um UUID será gerado. Único no sistema (idempotência).",
    )


class AsyncScrapeResponse(BaseModel):
    """Resposta 202 do endpoint assíncrono."""

    request_id: str = Field(..., description="ID efetivo (fornecido ou gerado)")
    job_id: str = Field(..., description="ID interno do ScrapeJob na fila")
    status: str = Field(default="queued", description="Status inicial: 'queued'")
    webhook_url: str = Field(..., description="URL que receberá o resultado")


class WebhookStatusResponse(BaseModel):
    """Status de uma entrega de webhook. Consultável via GET /api/v1/webhooks/{request_id}."""

    model_config = ConfigDict(from_attributes=True)

    request_id: str
    job_id: str
    webhook_url: str
    status: DeliveryStatusEnum
    attempts: int
    max_attempts: int
    last_attempt_at: Optional[datetime] = None
    next_attempt_at: Optional[datetime] = None
    delivered_at: Optional[datetime] = None
    last_error: Optional[str] = None
    last_response_code: Optional[int] = None
    created_at: datetime
    updated_at: datetime


class WebhookPayload(BaseModel):
    """Envelope enviado para a URL do cliente quando o scraping termina."""

    request_id: str
    job_id: str
    status: str = Field(..., description="SUCCESS ou FAILED")
    response: Dict[str, Any] = Field(..., description="ScrapeResponse completo em formato dict")


class QueueStatusDTO(BaseModel):
    """Métricas em tempo real da fila de execução do Scraper e de entregas de webhooks."""

    queued_jobs: int = 0
    running_jobs: int = 0
    total_pending_jobs: int = 0
    pending_webhooks: int = 0
    total_success_jobs: int = 0
    total_failed_jobs: int = 0

