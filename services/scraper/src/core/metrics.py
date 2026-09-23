import time
from collections.abc import Awaitable, Callable

from fastapi import Request, Response
from prometheus_client import CONTENT_TYPE_LATEST, REGISTRY, Counter, Gauge, Histogram, generate_latest
from starlette.responses import Response as StarletteResponse

HTTP_REQUESTS = Counter(
    "scraper_http_requests_total",
    "Total number of HTTP requests.",
    ("method", "route", "status"),
)
HTTP_REQUEST_DURATION = Histogram(
    "scraper_http_request_duration_seconds",
    "HTTP request duration in seconds.",
    ("method", "route"),
)
HTTP_REQUESTS_IN_PROGRESS = Gauge(
    "scraper_http_requests_in_progress",
    "Number of HTTP requests currently being processed.",
    ("method",),
)


def _route_template(request: Request) -> str:
    route = request.scope.get("route")
    if route is None:
        return "unmatched"

    path_parts = request.url.path.strip("/").split("/")
    route_parts = route.path.strip("/").split("/")
    offset = len(path_parts) - len(route_parts)
    for index, part in enumerate(route_parts):
        if part.startswith("{") and part.endswith("}"):
            parameter = part[1:-1].split(":", maxsplit=1)[0]
            path_parts[offset + index] = f"{{{parameter}}}"
    return f"/{'/'.join(path_parts)}"


async def observe_request(
    request: Request,
    call_next: Callable[[Request], Awaitable[Response]],
) -> Response:
    if request.url.path == "/metrics":
        return await call_next(request)

    method = request.method
    started_at = time.perf_counter()
    status = 500
    HTTP_REQUESTS_IN_PROGRESS.labels(method=method).inc()

    try:
        response = await call_next(request)
        status = response.status_code
        return response
    finally:
        route = _route_template(request)
        HTTP_REQUESTS.labels(method=method, route=route, status=str(status)).inc()
        HTTP_REQUEST_DURATION.labels(method=method, route=route).observe(time.perf_counter() - started_at)
        HTTP_REQUESTS_IN_PROGRESS.labels(method=method).dec()


def prometheus_response() -> StarletteResponse:
    return StarletteResponse(content=generate_latest(REGISTRY), media_type=CONTENT_TYPE_LATEST)
