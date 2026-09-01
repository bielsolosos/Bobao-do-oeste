"""
Módulo de Extração e Higienização de Dados da OLX Brasil (Parser).
"""

import json
import re
from datetime import datetime, timezone
from typing import Any, Dict, List, Optional

from selectolax.parser import HTMLParser

from src.core.logger import logger
from src.domain.enums import DeliveryTypeEnum, VendorEnum
from src.domain.schemas import ScrapedListingDTO, ScrapedListingDetailDTO


class OlxPayloadParser:
    """Parser responsável por transformar respostas HTML da OLX em instâncias de ScrapedListingDTO."""

    @classmethod
    def parse_html(cls, html_content: str) -> List[ScrapedListingDTO]:
        if not html_content:
            return []

        tree = HTMLParser(html_content)

        # 1. Tentativa de extração via tag Next.js __NEXT_DATA__
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

        # 2. Extração direta da árvore de componentes HTML da OLX
        listings = cls._extract_from_dom(tree)
        logger.info(f"Extracted {len(listings)} listings via OLX DOM card parser.")
        return listings

    @classmethod
    def _extract_from_next_data(cls, data: Dict[str, Any]) -> List[ScrapedListingDTO]:
        listings: List[ScrapedListingDTO] = []
        page_props = data.get("props", {}).get("pageProps", {})

        raw_ads = (
            page_props.get("ads")
            or page_props.get("adList")
            or page_props.get("initialData", {}).get("ads")
            or page_props.get("listingProps", {}).get("adList")
            or []
        )

        now = datetime.now(timezone.utc)

        for ad in raw_ads:
            try:
                if ad.get("isAd") or ad.get("isAdvertising"):
                    continue

                listing_id = str(ad.get("listId") or ad.get("id") or "")
                if not listing_id:
                    continue

                title = ad.get("subject") or ad.get("title") or "Sem título"
                price_val = cls._parse_price(ad.get("price") or ad.get("priceValue") or ad.get("rawPrice"))
                old_price_val = cls._parse_price(ad.get("oldPrice"))
                url = ad.get("url") or ad.get("friendlyUrl") or ""

                location = ad.get("location") or {}
                state = location.get("uf") or location.get("state")
                city = location.get("municipality") or location.get("city")
                neighborhood = location.get("neighbourhood") or location.get("neighborhood")

                has_delivery = bool(
                    ad.get("olxPay") or ad.get("olxDelivery") or ad.get("hasOlxPay") or ad.get("deliveryAvailable")
                )
                delivery_type = DeliveryTypeEnum.OLX_PAY if has_delivery else DeliveryTypeEnum.HAND_DELIVERY

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
                    scraped_at=now,
                )
                listings.append(listing)
            except Exception as e:
                logger.debug(f"Error parsing single OLX ad record: {e}")
                continue

        return listings

    @classmethod
    def _extract_from_dom(cls, tree: HTMLParser) -> List[ScrapedListingDTO]:
        listings: List[ScrapedListingDTO] = []
        cards = tree.css(
            "section.olx-adcard, div.olx-adcard, [class*='olx-adcard__horizontal'], section[class*='olx-adcard']"
        )
        seen_ids = set()
        now = datetime.now(timezone.utc)

        for card in cards:
            try:
                link_node = card.css_first("a[href*='olx.com.br']")
                if not link_node:
                    continue

                url = link_node.attributes.get("href", "")
                if not url:
                    continue

                id_match = re.search(r"-(\d{8,12})(?:\?|$)", url)
                if not id_match:
                    continue

                listing_id = id_match.group(1)
                if listing_id in seen_ids:
                    continue
                seen_ids.add(listing_id)

                h2 = card.css_first("h2")
                title = h2.text(strip=True) if h2 else "Sem título"

                images = []
                for img in card.css("img"):
                    img_src = img.attributes.get("src") or img.attributes.get("data-src")
                    if img_src and "olx.com.br" in img_src and "logo" not in img_src:
                        images.append(img_src)

                price_val = 0.0
                all_text_elements = [n.text(strip=True) for n in card.css("h3, span, p, div") if n.text(strip=True)]
                for text in all_text_elements:
                    if text.startswith("R$") and "10x" not in text and "em até" not in text:
                        parsed = cls._parse_price(text)
                        if parsed > 0:
                            price_val = parsed
                            break

                card_text = card.text() or ""
                has_delivery = (
                    "Frete" in card_text
                    or "Entrega" in card_text
                    or "OLX Pay" in card_text
                    or "Garantia da OLX" in card_text
                )
                delivery_type = DeliveryTypeEnum.OLX_PAY if has_delivery else DeliveryTypeEnum.HAND_DELIVERY

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
                        scraped_at=now,
                    )
                )
            except Exception as e:
                logger.debug(f"Error parsing DOM card: {e}")
                continue

        return listings

    @staticmethod
    def _parse_price(price_raw: Any) -> float:
        if price_raw is None:
            return 0.0
        if isinstance(price_raw, (int, float)):
            return float(price_raw)
        if isinstance(price_raw, str):
            cleaned = re.sub(r"[^\d,.]", "", price_raw).strip()
            if not cleaned:
                return 0.0

            if "," in cleaned and "." in cleaned:
                if cleaned.rfind(",") > cleaned.rfind("."):
                    cleaned = cleaned.replace(".", "").replace(",", ".")
                else:
                    cleaned = cleaned.replace(",", "")
            elif "." in cleaned:
                parts = cleaned.split(".")
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
        if not date_raw:
            return None
        if isinstance(date_raw, (int, float)):
            try:
                ts = date_raw / 1000 if date_raw > 1e11 else date_raw
                return datetime.fromtimestamp(ts, timezone.utc)
            except Exception:
                return None
        if isinstance(date_raw, str):
            try:
                return datetime.fromisoformat(date_raw.replace("Z", "+00:00"))
            except Exception:
                return None
        return None

    @classmethod
    def parse_ad_detail_html(cls, html_content: str, url: str) -> Optional[ScrapedListingDetailDTO]:
        """Extrai todos os dados profundos e galeria de fotos da página interna de um anúncio na OLX."""
        if not html_content:
            return None

        tree = HTMLParser(html_content)

        # 1. Tentativa de extração via __NEXT_DATA__
        next_data_node = tree.css_first('script#__NEXT_DATA__[type="application/json"]')
        if next_data_node and next_data_node.text():
            try:
                data = json.loads(next_data_node.text())
                detail = cls._extract_detail_from_next_data(data, url)
                if detail:
                    logger.info(f"Successfully extracted ad detail from __NEXT_DATA__ for {url}")
                    return detail
            except Exception as e:
                logger.warning(f"Failed extracting ad detail from __NEXT_DATA__: {e}. Falling back to DOM.")

        # 2. Extração via DOM
        detail = cls._extract_detail_from_dom(tree, url)
        if detail:
            logger.info(f"Extracted ad detail via DOM fallback for {url}")
            return detail

        logger.error(f"Failed to extract ad detail from both NEXT_DATA and DOM for {url}")
        return None

    @classmethod
    def _extract_detail_from_next_data(cls, data: Dict[str, Any], url: str) -> Optional[ScrapedListingDetailDTO]:
        page_props = data.get("props", {}).get("pageProps", {})
        ad = (
            page_props.get("ad")
            or page_props.get("initialData", {}).get("ad")
            or page_props.get("adData")
            or page_props
        )

        if not isinstance(ad, dict):
            return None

        # ID do anúncio
        id_match = re.search(r"-(\d{8,12})(?:\?|$)", url)
        listing_id = str(ad.get("listId") or ad.get("id") or (id_match.group(1) if id_match else ""))
        if not listing_id:
            return None

        title = str(ad.get("subject") or ad.get("title") or "Sem título").strip()
        description = ad.get("body") or ad.get("description")
        price = cls._parse_price(ad.get("price") or ad.get("priceValue") or ad.get("rawPrice"))
        old_price = cls._parse_price(ad.get("oldPrice") or ad.get("originalPrice"))

        # Localização
        location = ad.get("location") or {}
        state = location.get("uf") or location.get("state")
        city = location.get("municipality") or location.get("city")
        neighborhood = location.get("neighbourhood") or location.get("neighborhood")

        # Entrega
        has_delivery = bool(
            ad.get("olxPay") or ad.get("olxDelivery") or ad.get("hasOlxPay") or ad.get("deliveryAvailable")
        )
        delivery_type = DeliveryTypeEnum.OLX_PAY if has_delivery else DeliveryTypeEnum.HAND_DELIVERY

        # Fotos completas em alta definição
        images: List[str] = []
        raw_images = ad.get("images") or ad.get("photos") or []
        for img in raw_images:
            if isinstance(img, str):
                images.append(img)
            elif isinstance(img, dict):
                img_url = img.get("original") or img.get("url") or img.get("thumbnail")
                if img_url:
                    images.append(img_url)

        # Propriedades e especificações detalhadas (RAM, SSD, Modelo, Condição, etc)
        properties: Dict[str, Any] = {}
        raw_props = ad.get("properties") or ad.get("adParameters") or ad.get("attributes") or []
        for prop in raw_props:
            if isinstance(prop, dict):
                label = prop.get("label") or prop.get("name") or prop.get("title")
                val = prop.get("value") or prop.get("formattedValue")
                if label and val:
                    properties[str(label).strip()] = val

        # Dados do Vendedor
        user = ad.get("user") or ad.get("seller") or ad.get("owner") or {}
        seller_name = user.get("name") or user.get("nickname")
        seller_info = {
            "user_id": user.get("userId") or user.get("id"),
            "member_since": user.get("memberSince") or user.get("createdAt"),
            "verified": user.get("verified"),
        }

        pub_date = cls._parse_date(ad.get("date") or ad.get("dateCreated") or ad.get("publicationDate"))

        return ScrapedListingDetailDTO(
            vendor=VendorEnum.OLX,
            vendor_listing_id=listing_id,
            url=url,
            title=title,
            price=price,
            original_price=old_price,
            description=description,
            state=state,
            city=city,
            neighborhood=neighborhood,
            has_delivery=has_delivery,
            delivery_type=delivery_type,
            properties=properties,
            images=images,
            seller_name=seller_name,
            seller_info=seller_info,
            published_at=pub_date,
            scraped_at=datetime.now(timezone.utc),
        )

    @classmethod
    def _extract_detail_from_dom(cls, tree: HTMLParser, url: str) -> Optional[ScrapedListingDetailDTO]:
        id_match = re.search(r"-(\d{8,12})(?:\?|$)", url)
        listing_id = id_match.group(1) if id_match else ""
        if not listing_id:
            return None

        # Título
        h1 = tree.css_first("h1")
        title = h1.text(strip=True) if h1 else "Sem título"

        # Preço
        price = 0.0
        price_node = tree.css_first("[class*='ad__price'], [data-testid='ad-price'], h2[class*='price']")
        if price_node:
            price = cls._parse_price(price_node.text(strip=True))

        # Descrição
        desc_node = tree.css_first("[class*='ad__description'], [data-testid='ad-description'], span[class*='description']")
        description = desc_node.text(strip=True) if desc_node else ""

        # Imagens
        images: List[str] = []
        for img in tree.css("img[src*='olx.com.br']"):
            src = img.attributes.get("src") or img.attributes.get("data-src")
            if src and "logo" not in src and src not in images:
                images.append(src)

        # Propriedades
        properties: Dict[str, Any] = {}
        for prop_row in tree.css("[data-testid='ad-properties'] div, [class*='ad__properties'] div"):
            text = prop_row.text(strip=True)
            if ":" in text:
                parts = text.split(":", 1)
                properties[parts[0].strip()] = parts[1].strip()

        state_match = re.search(r"https?://([a-z]{2})\.olx\.com\.br", url)
        state = state_match.group(1).upper() if state_match else None

        return ScrapedListingDetailDTO(
            vendor=VendorEnum.OLX,
            vendor_listing_id=listing_id,
            url=url,
            title=title,
            price=price,
            description=description,
            state=state,
            properties=properties,
            images=images,
            scraped_at=datetime.now(timezone.utc),
        )

