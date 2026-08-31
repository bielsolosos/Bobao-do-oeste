from src.domain.enums import VendorEnum
from src.domain.providers.olx.parser import OlxPayloadParser
from src.domain.providers.olx.url_builder import OlxUrlBuilder
from src.domain.schemas import ScrapeRequest


def test_olx_url_builder_national_search_default():
    """Valida busca nacional no Brasil todo quando state é None ou 'brasil' ou 'br'"""
    req1 = ScrapeRequest(
        vendor=VendorEnum.OLX,
        keyword="thinkpad",
    )
    url1 = OlxUrlBuilder.build(req1)
    assert url1 == "https://www.olx.com.br/brasil?sf=1&q=thinkpad"

    req2 = ScrapeRequest(
        vendor=VendorEnum.OLX,
        keyword="notebook",
        state="brasil",
        min_price=500.0,
        max_price=1000.0,
        require_delivery=True,
    )
    url2 = OlxUrlBuilder.build(req2)
    assert url2 == "https://www.olx.com.br/brasil?sf=1&q=notebook&ps=500&pe=1000&olxpay=1"

    req3 = ScrapeRequest(
        vendor=VendorEnum.OLX,
        keyword="rtx 4090",
        state="br",
    )
    url3 = OlxUrlBuilder.build(req3)
    assert url3 == "https://www.olx.com.br/brasil?sf=1&q=rtx+4090"


def test_olx_url_builder_state_search():
    """Valida busca por estado específico (ex: SP)"""
    req = ScrapeRequest(
        vendor=VendorEnum.OLX,
        keyword="thinkpad t480",
        state="sp",
        region="sao-paulo-e-regiao",
        category="informatica-e-acessorios/notebooks",
        min_price=800.0,
        max_price=2000.0,
        require_delivery=True,
    )
    url = OlxUrlBuilder.build(req, page=1)
    assert "olx.com.br" in url
    assert "informatica-e-acessorios/notebooks" in url
    assert "estado-sp" in url
    assert "sao-paulo-e-regiao" in url
    assert "sf=1" in url
    assert "q=thinkpad+t480" in url
    assert "ps=800" in url
    assert "pe=2000" in url
    assert "olxpay=1" in url


def test_olx_url_builder_pagination():
    req = ScrapeRequest(
        vendor=VendorEnum.OLX,
        keyword="macbook",
    )
    url_p2 = OlxUrlBuilder.build(req, page=2)
    assert "o=2" in url_p2
    assert "sf=1" in url_p2


def test_olx_parser_next_data_extraction():
    fake_html = """
    <!DOCTYPE html>
    <html>
      <head>
        <script id="__NEXT_DATA__" type="application/json">
        {
          "props": {
            "pageProps": {
              "ads": [
                {
                  "listId": "1389472918",
                  "subject": "Lenovo ThinkPad T480 i5 8GB 256GB SSD",
                  "price": "R$ 1.450",
                  "oldPrice": "R$ 1.600",
                  "url": "https://sp.olx.com.br/sao-paulo-e-regiao/informatica-e-acessorios/notebooks/lenovo-thinkpad-t480-1389472918",
                  "location": {
                    "uf": "SP",
                    "municipality": "São Paulo",
                    "neighbourhood": "Bela Vista"
                  },
                  "olxPay": true,
                  "images": [
                    { "original": "https://img.olx.com.br/images/12/123456789.jpg" }
                  ]
                }
              ]
            }
          }
        }
        </script>
      </head>
      <body></body>
    </html>
    """
    listings = OlxPayloadParser.parse_html(fake_html)
    assert len(listings) == 1
    item = listings[0]
    assert item.vendor_listing_id == "1389472918"
    assert item.title == "Lenovo ThinkPad T480 i5 8GB 256GB SSD"
    assert item.price == 1450.0
    assert item.original_price == 1600.0
    assert item.state == "SP"
    assert item.city == "São Paulo"
    assert item.neighborhood == "Bela Vista"
    assert item.has_delivery is True
    assert len(item.images) == 1
