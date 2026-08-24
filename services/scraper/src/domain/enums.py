from enum import Enum


class VendorEnum(str, Enum):
    OLX = "OLX"
    MERCADO_LIVRE = "MERCADO_LIVRE"
    ENJOEI = "ENJOEI"


class ExecutionStatusEnum(str, Enum):
    """Status do resultado do scrape (vs JobStatusEnum que é o estado na fila)."""

    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"


class DeliveryTypeEnum(str, Enum):
    OLX_PAY = "OLX_PAY"
    MERCADO_ENVIOS = "MERCADO_ENVIOS"
    CORREIOS = "CORREIOS"
    HAND_DELIVERY = "HAND_DELIVERY"
    UNKNOWN = "UNKNOWN"


class JobStatusEnum(str, Enum):
    QUEUED = "QUEUED"
    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"


class DeliveryStatusEnum(str, Enum):
    PENDING = "PENDING"
    READY = "READY"
    SENDING = "SENDING"
    DELIVERED = "DELIVERED"
    FAILED = "FAILED"
