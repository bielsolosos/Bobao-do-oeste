"""
Testes da comunicação assíncrona via webhook.

Cobre:
- Service: enqueue, idempotência, transições de status, claim, mark_delivered/retry/failed
- Worker: envio HTTP, retry com backoff, tratamento de 4xx/5xx
- API: POST /scrape/async + GET /webhooks/{requestId}
"""

import asyncio
import base64
import uuid
from datetime import datetime, timezone
from typing import Any, Dict, List

import pytest
from httpx import ASGITransport, AsyncClient
from sqlalchemy import text

from src.core.config import settings
from src.core.database import async_session_maker
from src.core.queues.webhook import WebhookQueueService
from src.core.workers.webhook import WebhookWorker
from src.domain.enums import DeliveryStatusEnum, JobStatusEnum, VendorEnum
from src.domain.schemas import ScrapeRequest
from src.domain.services import AsyncScrapeService, ScrapingService
from src.main import app

AUTH_HEADER = {
    "Authorization": "Basic "
    + base64.b64encode(f"{settings.BASIC_AUTH_USERNAME}:{settings.BASIC_AUTH_PASSWORD}".encode()).decode()
}


async def _cleanup() -> None:
    async with async_session_maker() as session:
        await session.execute(text("DELETE FROM webhook_deliveries"))
        await session.execute(text("DELETE FROM scrape_jobs"))
        await session.execute(text("DELETE FROM scraped_listings"))
        await session.execute(text("DELETE FROM scraping_executions"))
        await session.execute(text("DELETE FROM search_queries"))
        await session.commit()


# ============================================================
# AsyncScrapeService: enqueue + idempotência
# ============================================================


@pytest.mark.asyncio
async def test_enqueue_creates_job_and_delivery_in_pending():
    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad t480")
        job, delivery = await async_scrape.enqueue_with_webhook(
            request=request,
            webhook_url="https://example.com/hook",
            request_id="req-1",
        )

        assert job.id is not None
        assert job.status == JobStatusEnum.QUEUED
        assert delivery.id is not None
        assert delivery.scrape_job_id == job.id
        assert delivery.request_id == "req-1"
        assert delivery.webhook_url == "https://example.com/hook"
        assert delivery.status == DeliveryStatusEnum.PENDING
        assert delivery.attempts == 0
        assert delivery.max_attempts == settings.WEBHOOK_DELIVERY_MAX_ATTEMPTS
    await _cleanup()


@pytest.mark.asyncio
async def test_enqueue_generates_uuid_when_request_id_omitted():
    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        _, delivery = await async_scrape.enqueue_with_webhook(request=request, webhook_url="https://example.com/hook")

        parsed = uuid.UUID(delivery.request_id)
        assert str(parsed) == delivery.request_id
    await _cleanup()


@pytest.mark.asyncio
async def test_enqueue_raises_on_duplicate_request_id():
    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        await async_scrape.enqueue_with_webhook(
            request=request,
            webhook_url="https://example.com/hook",
            request_id="dup-1",
        )

    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        with pytest.raises(ValueError, match="já existe"):
            await async_scrape.enqueue_with_webhook(
                request=request,
                webhook_url="https://example.com/hook",
                request_id="dup-1",
            )
    await _cleanup()


# ============================================================
# WebhookQueueService: transições e claim
# ============================================================


@pytest.mark.asyncio
async def test_mark_ready_transitions_pending_to_ready():
    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        webhook_queue = WebhookQueueService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        job, delivery = await async_scrape.enqueue_with_webhook(request=request, webhook_url="https://example.com/hook")

        updated = await webhook_queue.mark_ready(job.id)
        assert updated == 1

        refreshed = await webhook_queue.get_by_request_id(delivery.request_id)
        assert refreshed is not None
        assert refreshed.status == DeliveryStatusEnum.READY
    await _cleanup()


@pytest.mark.asyncio
async def test_claim_next_picks_ready_first_then_retry_with_past_next_attempt():
    from datetime import timedelta

    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        webhook_queue = WebhookQueueService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        job_a, del_a = await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://a.example.com", request_id="a"
        )
        job_b, del_b = await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://b.example.com", request_id="b"
        )
        await webhook_queue.mark_ready(job_a.id)
        await webhook_queue.mark_ready(job_b.id)

        # Pega o primeiro (mais antigo = job_a)
        first = await webhook_queue.claim_next("w1")
        assert first is not None
        assert first.id == del_a.id

        # Marca como retry agendado para o passado
        await webhook_queue.mark_retry(
            first,
            response_code=500,
            error_message="boom",
            next_attempt_at=datetime.now(timezone.utc) - timedelta(seconds=10),
        )

        # Próximo claim deve pegar del_b (READY, FIFO)
        second = await webhook_queue.claim_next("w2")
        assert second is not None
        assert second.id == del_b.id

        # Marca del_b como delivered
        await webhook_queue.mark_delivered(second, 202)

        # Próximo claim deve pegar del_a (SENDING com next_attempt_at no passado)
        third = await webhook_queue.claim_next("w3")
        assert third is not None
        assert third.id == del_a.id
    await _cleanup()


@pytest.mark.asyncio
async def test_mark_retry_increments_attempts_and_marks_failed_after_max():
    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        webhook_queue = WebhookQueueService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        _, delivery = await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://example.com", request_id="retry-1"
        )
        # Força max_attempts=2 para teste rápido
        delivery.max_attempts = 2
        session.add(delivery)
        await session.commit()

        # 1ª falha → attempts=1, status=SENDING
        await webhook_queue.mark_retry(
            delivery,
            response_code=500,
            error_message="err1",
            next_attempt_at=datetime.now(timezone.utc),
        )
        assert delivery.attempts == 1
        assert delivery.status == DeliveryStatusEnum.SENDING
        assert delivery.next_attempt_at is not None

        # 2ª falha → attempts=2 >= max_attempts → FAILED
        await webhook_queue.mark_retry(
            delivery,
            response_code=500,
            error_message="err2",
            next_attempt_at=datetime.now(timezone.utc),
        )
        assert delivery.attempts == 2
        assert delivery.status == DeliveryStatusEnum.FAILED
        assert delivery.next_attempt_at is None
    await _cleanup()


@pytest.mark.asyncio
async def test_mark_failed_terminal_does_not_retry():
    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        webhook_queue = WebhookQueueService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        _, delivery = await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://example.com", request_id="term-1"
        )

        await webhook_queue.mark_failed_terminal(delivery, "HTTP 404: not found")
        assert delivery.status == DeliveryStatusEnum.FAILED
        assert delivery.attempts == 1
        assert delivery.next_attempt_at is None
    await _cleanup()


@pytest.mark.asyncio
async def test_recover_stuck_deliveries_resets_old_sending_to_ready():
    from datetime import timedelta

    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        webhook_queue = WebhookQueueService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        job, delivery = await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://example.com", request_id="stuck-1"
        )
        # PENDING → READY → SENDING (via claim)
        await webhook_queue.mark_ready(job.id)
        await webhook_queue.claim_next("w-stuck")
        # Reescreve last_attempt_at para 120s atrás
        old = datetime.now(timezone.utc) - timedelta(seconds=120)
        await session.execute(
            text("UPDATE webhook_deliveries SET last_attempt_at = :old WHERE id = :id"),
            {"old": old, "id": delivery.id},
        )
        await session.commit()

        recovered = await webhook_queue.recover_stuck_deliveries(60)
        assert recovered == 1

    # Verifica com sessão nova para evitar cache do identity map
    async with async_session_maker() as session:
        webhook_queue = WebhookQueueService(session)
        refreshed = await webhook_queue.get_by_request_id("stuck-1")
        assert refreshed is not None
        assert refreshed.status == DeliveryStatusEnum.READY
        assert refreshed.worker_id is None
    await _cleanup()


# ============================================================
# WebhookWorker: envio HTTP, retry, 4xx terminal
# ============================================================


@pytest.fixture
def fast_scraper(monkeypatch):
    """Mock do ScrapingService que responde rápido, sem rede."""

    async def fake_execute_scrape(self, request):
        return {
            "success": True,
            "execution": {
                "id": f"exec-{uuid.uuid4().hex[:8]}",
                "vendor": request.vendor.value,
                "status": "SUCCESS",
                "duration_ms": 10,
                "total_found": 0,
                "new_items_count": 0,
                "used_fallback": False,
                "started_at": datetime.now(timezone.utc).isoformat(),
                "finished_at": datetime.now(timezone.utc).isoformat(),
            },
            "items": [],
        }

    monkeypatch.setattr(ScrapingService, "execute_scrape", fake_execute_scrape)
    return fake_execute_scrape


@pytest.mark.asyncio
async def test_dispatcher_sends_webhook_and_marks_delivered_on_2xx(fast_scraper, monkeypatch):
    await _cleanup()

    received_payloads: List[Dict[str, Any]] = []

    async def patched_post(self, url, payload):
        received_payloads.append({"url": url, "payload": payload})
        return "success", 202, ""

    monkeypatch.setattr(WebhookWorker, "_post_with_timeout", patched_post)

    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        webhook_queue = WebhookQueueService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        job, _ = await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://hook.example.com", request_id="d-1"
        )
        # Preenche o ScrapeJob com response_payload simulando scrape concluído
        job.status = JobStatusEnum.SUCCESS
        job.response_payload = {
            "success": True,
            "execution": {
                "id": job.id,
                "vendor": "OLX",
                "status": "SUCCESS",
                "duration_ms": 10,
                "total_found": 0,
                "new_items_count": 0,
                "used_fallback": False,
                "started_at": datetime.now(timezone.utc).isoformat(),
                "finished_at": datetime.now(timezone.utc).isoformat(),
            },
            "items": [],
        }
        session.add(job)
        await session.commit()
        await webhook_queue.mark_ready(job.id)

    worker = WebhookWorker(concurrency=1, poll_interval=0.05, timeout=5)
    await worker.start()
    try:
        for _ in range(40):
            async with async_session_maker() as session:
                webhook_queue = WebhookQueueService(session)
                d = await webhook_queue.get_by_request_id("d-1")
                if d is not None and d.status == DeliveryStatusEnum.DELIVERED:
                    break
            await asyncio.sleep(0.1)
        else:
            pytest.fail("Delivery não foi marcada como DELIVERED em tempo hábil")
    finally:
        await worker.stop()

    assert len(received_payloads) == 1
    sent = received_payloads[0]
    assert sent["url"] == "https://hook.example.com"
    assert sent["payload"]["requestId"] == "d-1"
    assert sent["payload"]["jobId"] == job.id
    assert sent["payload"]["status"] == "SUCCESS"
    assert "response" in sent["payload"]
    await _cleanup()


@pytest.mark.asyncio
async def test_dispatcher_retries_on_5xx_until_success(monkeypatch):
    await _cleanup()

    calls = {"n": 0}

    async def flaky_post(self, url, payload):
        calls["n"] += 1
        if calls["n"] < 3:
            return "retryable", 503, "Service Unavailable"
        return "success", 202, ""

    monkeypatch.setattr(WebhookWorker, "_post_with_timeout", flaky_post)
    monkeypatch.setattr(settings, "WEBHOOK_DELIVERY_BASE_BACKOFF_SECONDS", 0.05)
    monkeypatch.setattr(settings, "WEBHOOK_DELIVERY_MAX_ATTEMPTS", 5)

    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        webhook_queue = WebhookQueueService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        job, _ = await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://flaky.example.com", request_id="d-flaky"
        )
        job.status = JobStatusEnum.SUCCESS
        job.response_payload = {
            "success": True,
            "execution": {
                "id": job.id,
                "vendor": "OLX",
                "status": "SUCCESS",
                "duration_ms": 10,
                "total_found": 0,
                "new_items_count": 0,
                "used_fallback": False,
                "started_at": datetime.now(timezone.utc).isoformat(),
                "finished_at": datetime.now(timezone.utc).isoformat(),
            },
            "items": [],
        }
        session.add(job)
        await session.commit()
        await webhook_queue.mark_ready(job.id)

    worker = WebhookWorker(concurrency=1, poll_interval=0.05, timeout=2)
    await worker.start()
    try:
        for _ in range(60):
            async with async_session_maker() as session:
                webhook_queue = WebhookQueueService(session)
                d = await webhook_queue.get_by_request_id("d-flaky")
                if d is not None and d.status in (
                    DeliveryStatusEnum.DELIVERED,
                    DeliveryStatusEnum.FAILED,
                ):
                    break
            await asyncio.sleep(0.1)
    finally:
        await worker.stop()

    assert calls["n"] == 3  # 2 falhas + 1 sucesso
    async with async_session_maker() as session:
        webhook_queue = WebhookQueueService(session)
        d = await webhook_queue.get_by_request_id("d-flaky")
        assert d is not None
        assert d.status == DeliveryStatusEnum.DELIVERED
        assert d.attempts == 2
        assert d.last_response_code == 202
    await _cleanup()


@pytest.mark.asyncio
async def test_dispatcher_marks_terminal_failed_on_4xx(monkeypatch):
    await _cleanup()

    async def client_error_post(self, url, payload):
        return "client_error", 404, "Not Found"

    monkeypatch.setattr(WebhookWorker, "_post_with_timeout", client_error_post)

    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        webhook_queue = WebhookQueueService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        job, _ = await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://404.example.com", request_id="d-404"
        )
        job.status = JobStatusEnum.SUCCESS
        job.response_payload = {
            "success": True,
            "execution": {
                "id": job.id,
                "vendor": "OLX",
                "status": "SUCCESS",
                "duration_ms": 0,
                "total_found": 0,
                "new_items_count": 0,
                "used_fallback": False,
                "started_at": datetime.now(timezone.utc).isoformat(),
                "finished_at": datetime.now(timezone.utc).isoformat(),
            },
            "items": [],
        }
        session.add(job)
        await session.commit()
        await webhook_queue.mark_ready(job.id)

    worker = WebhookWorker(concurrency=1, poll_interval=0.05, timeout=2)
    await worker.start()
    try:
        for _ in range(40):
            async with async_session_maker() as session:
                webhook_queue = WebhookQueueService(session)
                d = await webhook_queue.get_by_request_id("d-404")
                if d is not None and d.status == DeliveryStatusEnum.FAILED:
                    break
            await asyncio.sleep(0.1)
    finally:
        await worker.stop()

    async with async_session_maker() as session:
        webhook_queue = WebhookQueueService(session)
        d = await webhook_queue.get_by_request_id("d-404")
        assert d is not None
        assert d.status == DeliveryStatusEnum.FAILED
        assert d.attempts == 1
        assert d.last_response_code == 404
        assert d.last_error is not None and "404" in d.last_error
    await _cleanup()


# ============================================================
# API: POST /scrape/async e GET /webhooks/{requestId}
# ============================================================


@pytest.mark.asyncio
async def test_post_async_scrape_returns_202_with_request_id():
    await _cleanup()
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/scrape/async",
            json={
                "request": {"vendor": "OLX", "keyword": "thinkpad t480"},
                "webhookUrl": "https://my-app.com/hook",
                "requestId": "client-abc-123",
            },
            headers=AUTH_HEADER,
        )

    assert response.status_code == 202
    data = response.json()
    assert data["request_id"] == "client-abc-123"
    assert data["status"] == "queued"
    assert data["webhook_url"] == "https://my-app.com/hook"
    assert "job_id" in data and len(data["job_id"]) > 0
    await _cleanup()


@pytest.mark.asyncio
async def test_post_async_scrape_generates_uuid_when_request_id_missing():
    await _cleanup()
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/scrape/async",
            json={
                "request": {"vendor": "OLX", "keyword": "thinkpad"},
                "webhookUrl": "https://my-app.com/hook",
            },
            headers=AUTH_HEADER,
        )

    assert response.status_code == 202
    generated = response.json()["request_id"]
    uuid.UUID(generated)
    await _cleanup()


@pytest.mark.asyncio
async def test_post_async_scrape_returns_409_on_duplicate_request_id():
    await _cleanup()
    payload = {
        "request": {"vendor": "OLX", "keyword": "thinkpad"},
        "webhookUrl": "https://my-app.com/hook",
        "requestId": "dup-test",
    }
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        first = await ac.post("/api/v1/scrape/async", json=payload, headers=AUTH_HEADER)
        second = await ac.post("/api/v1/scrape/async", json=payload, headers=AUTH_HEADER)

    assert first.status_code == 202
    assert second.status_code == 409
    assert "dup-test" in second.json()["detail"]
    await _cleanup()


@pytest.mark.asyncio
async def test_post_async_scrape_requires_auth():
    await _cleanup()
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.post(
            "/api/v1/scrape/async",
            json={
                "request": {"vendor": "OLX", "keyword": "thinkpad"},
                "webhookUrl": "https://my-app.com/hook",
            },
        )
    assert response.status_code == 401
    await _cleanup()


@pytest.mark.asyncio
async def test_get_webhook_status_returns_delivery():
    await _cleanup()
    async with async_session_maker() as session:
        async_scrape = AsyncScrapeService(session)
        request = ScrapeRequest(vendor=VendorEnum.OLX, keyword="thinkpad")
        await async_scrape.enqueue_with_webhook(
            request=request, webhook_url="https://my-app.com/hook", request_id="status-1"
        )

    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/api/v1/webhooks/status-1", headers=AUTH_HEADER)

    assert response.status_code == 200
    data = response.json()
    assert data["request_id"] == "status-1"
    assert data["webhook_url"] == "https://my-app.com/hook"
    assert data["status"] == "PENDING"
    assert data["attempts"] == 0
    await _cleanup()


@pytest.mark.asyncio
async def test_get_webhook_status_returns_404_when_not_found():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/api/v1/webhooks/does-not-exist", headers=AUTH_HEADER)
    assert response.status_code == 404
