import json
import logging

from src.core.logger import AppJsonFormatter


def test_json_formatter_adds_stable_application_fields():
    record = logging.LogRecord(
        name="scraper",
        level=logging.INFO,
        pathname=__file__,
        lineno=10,
        msg="Job completed",
        args=(),
        exc_info=None,
    )

    payload = json.loads(AppJsonFormatter("%(message)s").format(record))

    assert payload["message"] == "Job completed"
    assert payload["level"] == "INFO"
    assert payload["application"] == "projeto-scrap"
    assert payload["service"] == "scraper"
    assert "@timestamp" in payload
