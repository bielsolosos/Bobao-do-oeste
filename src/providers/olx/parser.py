"""
Módulo de Extração e Higienização de Dados da OLX Brasil (Parser).

Este módulo contém a inteligência de transformação do HTML bruto em objetos
estritamente tipados (`ScrapedListingDTO`).

Estratégias de Parsing Implementadas:
1. `_extract_from_next_data`: Extração do JSON embutido na tag `<script id="__NEXT_DATA__">`.
   Quando disponível, fornece todos os campos originais da API interna da OLX.
2. `_extract_from_dom`: Parser de alta velocidade usando `selectolax` (escrito em C/Modest).
   Analisa a árvore DOM moderna dos cards de anúncio (`section.olx-adcard`).

Tratamentos Críticos:
- Higienização de Preço (`_parse_price`): Converte strings de moeda brasileira (R$ 1.500,00 ou R$ 1.500)
  para valores float numéricos consistentes, tratando separadores de milhar e centavos.
- Detecção de ID Nativo: Extrai o ID único do marketplace a partir do padrão final da URL (-1527993289).
- Detecção de Entrega: Analisa a presença de "Frete grátis", "OLX Pay", "Garantia da OLX" e badges do card.
- Deduplicação em memória durante o parse para evitar anúncios repetidos de blocos patrocinados.
"""

import json
import re
from datetime import datetime
from typing import Any, Dict, List, Optional
from selectolax.parser import HTMLParser
from src.core.logger import logger
from src.domain.enums import DeliveryTypeEnum, VendorEnum
from src.domain.schemas import ScrapedListingDTO


class OlxPayloadParser:
    """
    Parser responsável por transformar respostas HTML da OLX em instâncias de ScrapedListingDTO.
    """

    @classmethod
    def parse_html(cls, html_content: str) -> List[ScrapedListingDTO]:
        """
        Ponto de entrada principal para o parsing do HTML da OLX.

        Parâmetros:
            html_content (str): String contendo o HTML bruto da página.

        Retorna:
            List[ScrapedListingDTO]: Lista de anúncios extraídos e normalizados.
        """
        if not html_content:
            return []

        tree = HTMLParser(html_content)

        # 1. Tentativa de extração via tag de hidratação Next.js
        next_data_node = tree.css_first('script#__NEXT_DATA__[type="application/json"]')
        if next_data_node and next_data_node.text():
            try:
                data = json.loads(next_data_node.text())
                listings = cls._extract_from_next_data(data)
                if listings:
                    logger.info(f"Successfully extracted {len(listings)} listings from OLX __NEXT_DATA__")
                    return listings
            except Exception as e:
                logger.warning(f"Failed parsing __NEXT_DATA__ JSON: {e}. Falling back to DOM parsing.")

        # 2. Extração direta da árvore de componentes HTML modernos da OLX
        listings = cls._extract_from_dom(tree)
        logger.info(f"Extracted {len(listings)} listings via OLX DOM card parser.")
        return listings

    @classmethod
    def _extract_from_next_data(cls, data: Dict[str, Any]) -> List[ScrapedListingDTO]:
        """
        Extrai anúncios a partir do objeto JSON do Next.js (__NEXT_DATA__).
        """
        listings: List[ScrapedListingDTO] = []
        page_props = data.get("props", {}).get("pageProps", {})

        # Navega pelas possíveis localizações da lista de anúncios no JSON da OLX
        raw_ads = (
            page_props.get("ads")
            or page_props.get("adList")
            or page_props.get("initialData", {}).get("ads")
            or page_props.get("listingProps", {}).get("adList")
            or []
        )

        for ad in raw_ads:
            try:
                # Ignora banners de publicidade e itens inválidos
                if ad.get("isAd") or ad.get("isAdvertising"):
                    continue

                listing_id = str(ad.get("listId") or ad.get("id") or "")
                if not listing_id:
                    continue

                title = ad.get("subject") or ad.get("title") or "Sem título"
                price_val = cls._parse_price(ad.get("price") or ad.get("priceValue") or ad.get("rawPrice"))
                old_price_val = cls._parse_price(ad.get("oldPrice"))
                url = ad.get("url") or ad.get("friendlyUrl") or ""

                # Extração de Localização
                location = ad.get("location") or {}
                state = location.get("uf") or location.get("state")
                city = location.get("municipality") or location.get("city")
                neighborhood = location.get("neighbourhood") or location.get("neighborhood")

                # Extração de Entrega / OLX Pay
                has_delivery = bool(
                    ad.get("olxPay")
                    or ad.get("olxDelivery")
                    or ad.get("hasOlxPay")
                    or ad.get("deliveryAvailable")
                )
                delivery_type = DeliveryTypeEnum.OLX_PAY if has_delivery else DeliveryTypeEnum.HAND_DELIVERY

                # Extração de Imagens
                images: List[str] = []
                for img in ad.get("images") or ad.get("photos") or []:
                    if isinstance(img, str):
                        images.append(img)
                    elif isinstance(img, dict):
                        img_url = img.get("original") or img.get("url") or img.get("thumbnail")
                        if img_url:
                            images.append(img_url)

                pub_date = cls._parse_date(ad.get("date") or ad.get("dateCreated") or ad.get("publicationDate"))

                listing = ScrapedListingDTO(
                    vendor=VendorEnum.OLX,
                    vendor_listing_id=listing_id,
                    title=title.strip(),
                    price=price_val,
                    original_price=old_price_val,
                    url=url,
                    description=ad.get("body") or ad.get("description"),
                    state=state,
                    city=city,
                    neighborhood=neighborhood,
                    has_delivery=has_delivery,
                    delivery_type=delivery_type,
                    images=images,
                    published_at=pub_date,
                    scraped_at=datetime.utcnow(),
                )
                listings.append(listing)
            except Exception as e:
                logger.debug(f"Error parsing single OLX ad record: {e}")
                continue

        return listings

    @classmethod
    def _extract_from_dom(cls, tree: HTMLParser) -> List[ScrapedListingDTO]:
        """
        Extrai anúncios analisando as tags de card do HTML moderno (`section.olx-adcard`).
        """
        listings: List[ScrapedListingDTO] = []
        cards = tree.css(
            "section.olx-adcard, div.olx-adcard, [class*='olx-adcard__horizontal'], section[class*='olx-adcard']"
        )

        seen_ids = set()

        for card in cards:
            try:
                link_node = card.css_first("a[href*='olx.com.br']")
                if not link_node:
                    continue

                url = link_node.attributes.get("href", "")
                if not url:
                    continue

                # Extrai o ID nativo da OLX do final da URL (ex: ...-1526635055)
                id_match = re.search(r"-(\d{8,12})(?:\?|$)", url)
                if not id_match:
                    continue

                listing_id = id_match.group(1)
                if listing_id in seen_ids:
                    continue
                seen_ids.add(listing_id)

                # Título do anúncio (tag h2 dentro do card)
                h2 = card.css_first("h2")
                title = h2.text(strip=True) if h2 else "Sem título"

                # Imagens do produto
                images = []
                for img in card.css("img"):
                    img_src = img.attributes.get("src") or img.attributes.get("data-src")
                    if img_src and "olx.com.br" in img_src and "logo" not in img_src:
                        images.append(img_src)

                # Extração de Preço (busca elementos de texto contendo 'R$')
                price_val = 0.0
                all_text_elements = [n.text(strip=True) for n in card.css("h3, span, p, div") if n.text(strip=True)]
                for text in all_text_elements:
                    if text.startswith("R$") and "10x" not in text and "em até" not in text:
                        parsed = cls._parse_price(text)
                        if parsed > 0:
                            price_val = parsed
                            break

                # Detecção de Entrega / OLX Pay
                card_text = card.text() or ""
                has_delivery = (
                    "Frete" in card_text
                    or "Entrega" in card_text
                    or "OLX Pay" in card_text
                    or "Garantia da OLX" in card_text
                )
                delivery_type = DeliveryTypeEnum.OLX_PAY if has_delivery else DeliveryTypeEnum.HAND_DELIVERY

                # Extração de Estado e Sub-região a partir da URL
                # Exemplo: https://sp.olx.com.br/grande-campinas/informatica/notebooks/...
                state_match = re.search(r"https?://([a-z]{2})\.olx\.com\.br/([^/]+)", url)
                state = state_match.group(1).upper() if state_match else None
                region_slug = state_match.group(2) if state_match else None

                listings.append(
                    ScrapedListingDTO(
                        vendor=VendorEnum.OLX,
                        vendor_listing_id=listing_id,
                        title=title,
                        price=price_val,
                        url=url,
                        state=state,
                        neighborhood=region_slug,
                        has_delivery=has_delivery,
                        delivery_type=delivery_type,
                        images=images,
                        scraped_at=datetime.utcnow(),
                    )
                )
            except Exception as e:
                logger.debug(f"Error parsing DOM card: {e}")
                continue

        return listings

    @staticmethod
    def _parse_price(price_raw: Any) -> float:
        """
        Normaliza representações textuais de moeda (R$ 1.500, R$ 1.500,00, 1500) para float.
        """
        if price_raw is None:
            return 0.0
        if isinstance(price_raw, (int, float)):
            return float(price_raw)
        if isinstance(price_raw, str):
            cleaned = re.sub(r"[^\d,.]", "", price_raw).strip()
            if not cleaned:
                return 0.0

            # Caso contenha ponto e vírgula (ex: 1.450,50 ou 1,450.50)
            if "," in cleaned and "." in cleaned:
                if cleaned.rfind(",") > cleaned.rfind("."):
                    cleaned = cleaned.replace(".", "").replace(",", ".")
                else:
                    cleaned = cleaned.replace(",", "")
            elif "." in cleaned:
                parts = cleaned.split(".")
                # Se houver 3 dígitos após o ponto (ex: 1.450 ou 12.500), é separador de milhar brasileiro
                if len(parts[-1]) == 3:
                    cleaned = cleaned.replace(".", "")
            elif "," in cleaned:
                parts = cleaned.split(",")
                if len(parts[-1]) == 3:
                    cleaned = cleaned.replace(",", "")
                else:
                    cleaned = cleaned.replace(",", ".")

            try:
                return float(cleaned)
            except ValueError:
                return 0.0
        return 0.0

    @staticmethod
    def _parse_date(date_raw: Any) -> Optional[datetime]:
        """
        Converte timestamps ou strings ISO de data para instâncias de datetime UTC.
        """
        if not date_raw:
            return None
        if isinstance(date_raw, (int, float)):
            try:
                ts = date_raw / 1000 if date_raw > 1e11 else date_raw
                return datetime.utcfromtimestamp(ts)
            except Exception:
                return None
        if isinstance(date_raw, str):
            try:
                return datetime.fromisoformat(date_raw.replace("Z", "+00:00"))
            except Exception:
                return None
        return None
