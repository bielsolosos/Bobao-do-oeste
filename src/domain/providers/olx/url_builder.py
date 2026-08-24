"""
Construtor de URLs Canônicas para a OLX Brasil.
"""

import urllib.parse
from src.domain.schemas import ScrapeRequest

VALID_UFS = {
    "ac", "al", "ap", "am", "ba", "ce", "df", "es", "go", "ma",
    "mt", "ms", "mg", "pa", "pb", "pr", "pe", "pi", "rj", "rn",
    "rs", "ro", "rr", "sc", "sp", "se", "to",
}


class OlxUrlBuilder:
    """Construtor de URLs para requisições de busca na OLX."""

    BASE_DOMAIN = "https://www.olx.com.br"

    @classmethod
    def build(cls, request: ScrapeRequest, page: int = 1) -> str:
        path_parts = []

        if request.category:
            path_parts.append(request.category.strip("/"))

        if request.state:
            raw_state = request.state.lower().strip().replace("estado-", "")
            if raw_state in VALID_UFS:
                path_parts.append(f"estado-{raw_state}")

        if request.region:
            path_parts.append(request.region.lower().strip("/"))

        if not path_parts:
            path_parts.append("brasil")

        path = "/".join(path_parts)
        url = f"{cls.BASE_DOMAIN}/{path}"

        query_params = {}

        if request.keyword:
            query_params["q"] = request.keyword.strip()

        if request.min_price is not None:
            query_params["ps"] = (
                int(request.min_price) if request.min_price.is_integer() else request.min_price
            )

        if request.max_price is not None:
            query_params["pe"] = (
                int(request.max_price) if request.max_price.is_integer() else request.max_price
            )

        if request.require_delivery:
            query_params["olxpay"] = "1"

        if page > 1:
            query_params["o"] = str(page)

        if query_params:
            url += f"?{urllib.parse.urlencode(query_params)}"

        return url
