import urllib.parse
from src.domain.schemas import ScrapeRequest


class OlxUrlBuilder:
    """Builds search URLs for OLX Brazil."""

    BASE_DOMAIN = "https://www.olx.com.br"

    @classmethod
    def build(cls, request: ScrapeRequest, page: int = 1) -> str:
        path_parts = []

        # 1. Category
        if request.category:
            cleaned_cat = request.category.strip("/")
            path_parts.append(cleaned_cat)

        # 2. State
        if request.state:
            state_slug = request.state.lower().strip()
            if not state_slug.startswith("estado-"):
                state_slug = f"estado-{state_slug}"
            path_parts.append(state_slug)

        # 3. Region
        if request.region:
            region_slug = request.region.lower().strip("/")
            path_parts.append(region_slug)

        # Construct path
        path = "/".join(path_parts)
        url = f"{cls.BASE_DOMAIN}/{path}" if path else f"{cls.BASE_DOMAIN}/brasil"

        # 4. Query parameters
        query_params = {}

        if request.keyword:
            query_params["q"] = request.keyword.strip()

        if request.min_price is not None:
            query_params["ps"] = int(request.min_price) if request.min_price.is_integer() else request.min_price

        if request.max_price is not None:
            query_params["pe"] = int(request.max_price) if request.max_price.is_integer() else request.max_price

        if request.require_delivery:
            query_params["olxpay"] = "1"

        if page > 1:
            query_params["o"] = str(page)

        if query_params:
            url += f"?{urllib.parse.urlencode(query_params)}"

        return url
