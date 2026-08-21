from enum import Enum


class VendorEnum(str, Enum):
    OLX = "OLX"
    MERCADO_LIVRE = "MERCADO_LIVRE"
    ENJOEI = "ENJOEI"


class ExecutionStatusEnum(str, Enum):
    PENDING = "PENDING"
    RUNNING = "RUNNING"
    SUCCESS = "SUCCESS"
    FAILED = "FAILED"
    BLOCKED_CAPTCHA = "BLOCKED_CAPTCHA"
    PARTIAL = "PARTIAL"


class DeliveryTypeEnum(str, Enum):
    OLX_PAY = "OLX_PAY"
    MERCADO_ENVIOS = "MERCADO_ENVIOS"
    CORREIOS = "CORREIOS"
    HAND_DELIVERY = "HAND_DELIVERY"
    UNKNOWN = "UNKNOWN"
