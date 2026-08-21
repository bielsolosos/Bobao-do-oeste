"""
Construtor de URLs Canônicas para a OLX Brasil.

A OLX possui uma hierarquia de roteamento muito específica em seu frontend Next.js:
1. Rota Nacional (Brasil Todo): `https://www.olx.com.br/brasil`
2. Categoria: `https://www.olx.com.br/{categoria}`
3. Categoria + Estado: `https://www.olx.com.br/{categoria}/estado-{uf}`
4. Categoria + Estado + Região: `https://www.olx.com.br/{categoria}/estado-{uf}/{regiao}`

Parâmetros de Busca (Query String):
- `q`: Termo de pesquisa (ex: `q=thinkpad+t480`).
- `ps`: Preço Mínimo (Price Start), ex: `ps=800`.
- `pe`: Preço Máximo (Price End), ex: `pe=2000`.
- `olxpay`: Flag para filtrar somente anúncios com compra segura / entrega OLX Pay (`olxpay=1`).
- `o`: Offset / Número da página de resultados (ex: `o=2`).
"""

import urllib.parse
from src.domain.schemas import ScrapeRequest

VALID_UFS = {
    "ac", "al", "ap", "am", "ba", "ce", "df", "es", "go", "ma",
    "mt", "ms", "mg", "pa", "pb", "pr", "pe", "pi", "rj", "rn",
    "rs", "ro", "rr", "sc", "sp", "se", "to",
}


class OlxUrlBuilder:
    """
    Construtor de URLs para requisições de busca na OLX.
    """

    BASE_DOMAIN = "https://www.olx.com.br"

    @classmethod
    def build(cls, request: ScrapeRequest, page: int = 1) -> str:
        """
        Monta a URL completa e canônica da OLX com base nos parâmetros do ScrapeRequest.

        Parâmetros:
            request (ScrapeRequest): DTO com os filtros solicitados (keyword, state, region, min_price, etc.).
            page (int): Número da página a ser consultada (padrão = 1).

        Retorna:
            str: URL pronta e codificada para ser requisitada pelo cliente HTTP.
        """
        path_parts = []

        # 1. Categoria (ex: 'informatica-e-acessorios/notebooks')
        if request.category:
            cleaned_cat = request.category.strip("/")
            path_parts.append(cleaned_cat)

        # 2. Estado (apenas se for uma UF brasileira válida, ex: 'sp' -> 'estado-sp')
        if request.state:
            raw_state = request.state.lower().strip().replace("estado-", "")
            if raw_state in VALID_UFS:
                path_parts.append(f"estado-{raw_state}")

        # 3. Sub-região (ex: 'sao-paulo-e-regiao', 'grande-campinas')
        if request.region:
            region_slug = request.region.lower().strip("/")
            path_parts.append(region_slug)

        # Se nenhum estado ou categoria foi especificado (busca no Brasil todo), usa rota canônica /brasil
        if not path_parts:
            path_parts.append("brasil")

        path = "/".join(path_parts)
        url = f"{cls.BASE_DOMAIN}/{path}"

        # 4. Parâmetros de Query String
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
