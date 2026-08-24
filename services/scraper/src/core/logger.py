import logging
import sys

from src.core.config import settings

LOG_FORMAT = "%(asctime)s | %(levelname)-8s | %(name)s:%(funcName)s:%(lineno)d - %(message)s"


def setup_logger(name: str = "scraper") -> logging.Logger:
    logger = logging.getLogger(name)
    if not logger.handlers:
        handler = logging.StreamHandler(sys.stdout)
        handler.setFormatter(logging.Formatter(LOG_FORMAT, datefmt="%Y-%m-%d %H:%M:%S"))
        logger.addHandler(handler)
        log_level = getattr(logging, settings.LOG_LEVEL.upper(), logging.INFO)
        logger.setLevel(log_level)
    return logger


logger = setup_logger()
