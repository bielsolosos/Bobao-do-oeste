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
from src.domain.schemas import ScrapedListingDTO


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
