import logging
import sys
from datetime import datetime, timezone

from pythonjsonlogger.json import JsonFormatter

from src.core.config import settings


class AppJsonFormatter(JsonFormatter):
    def add_fields(
        self,
        log_data: dict[str, object],
        record: logging.LogRecord,
        message_dict: dict[str, object],
    ) -> None:
        super().add_fields(log_data, record, message_dict)
        log_data["@timestamp"] = datetime.now(timezone.utc).isoformat(timespec="milliseconds")
        log_data["level"] = record.levelname
        log_data["logger"] = record.name
        log_data["function"] = record.funcName
        log_data["line"] = record.lineno
        log_data["application"] = "projeto-scrap"
        log_data["service"] = "scraper"
        log_data["environment"] = settings.APP_ENV


def setup_logger(name: str = "scraper") -> logging.Logger:
    logger = logging.getLogger(name)
    if not logger.handlers:
        handler = logging.StreamHandler(sys.stdout)
        handler.setFormatter(AppJsonFormatter("%(message)s"))
        logger.addHandler(handler)
        log_level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)
        logger.setLevel(log_level)
        logger.propagate = False
    return logger


logger = setup_logger()
