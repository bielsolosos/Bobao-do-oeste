import base64

import pytest
from httpx import ASGITransport, AsyncClient

from src.core.config import settings
from src.main import app

# Generate Basic Auth header
auth_str = f"{settings.BASIC_AUTH_USERNAME}:{settings.BASIC_AUTH_PASSWORD}"
auth_header = {"Authorization": f"Basic {base64.b64encode(auth_str.encode()).decode()}"}


@pytest.mark.asyncio
async def test_health_check_is_public():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"


@pytest.mark.asyncio
async def test_metrics_are_public_and_include_http_metrics():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        await ac.get("/health")
        await ac.get("/api/v1/executions/identifier-for-metrics")
        response = await ac.get("/metrics")

    assert response.status_code == 200
    assert response.headers["content-type"].startswith("text/plain")
    assert "scraper_http_requests_total" in response.text
    assert 'route="/health"' in response.text
    assert 'route="/api/v1/executions/{execution_id}"' in response.text
    assert 'route="/api/v1/executions/identifier-for-metrics"' not in response.text
    assert 'route="/metrics"' not in response.text
    assert "python_info" in response.text


@pytest.mark.asyncio
async def test_unauthorized_access_fails():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/api/v1/executions")
    assert response.status_code == 401


@pytest.mark.asyncio
async def test_dashboard_with_auth():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/dashboard", headers=auth_header)
    assert response.status_code == 200
    assert "Marketplace Scraper" in response.text


@pytest.mark.asyncio
async def test_executions_list_with_auth():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/api/v1/executions", headers=auth_header)
    assert response.status_code == 200
    assert isinstance(response.json(), list)


@pytest.mark.asyncio
async def test_listings_list_with_auth():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/api/v1/listings", headers=auth_header)
    assert response.status_code == 200
    assert isinstance(response.json(), list)


@pytest.mark.asyncio
async def test_queue_status_with_auth():
    async with AsyncClient(transport=ASGITransport(app=app), base_url="http://test") as ac:
        response = await ac.get("/api/v1/queue/status", headers=auth_header)
    assert response.status_code == 200
    data = response.json()
    assert "queued_jobs" in data
    assert "running_jobs" in data
    assert "total_pending_jobs" in data
    assert "pending_webhooks" in data
    assert "total_success_jobs" in data
    assert "total_failed_jobs" in data
