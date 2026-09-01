"""
Serviço de Cache de Imagens no SQLite com TTL (ImageCacheService).
"""

from datetime import datetime, timedelta, timezone
from typing import List, Optional, Tuple

from sqlalchemy.ext.asyncio import AsyncSession
from sqlmodel import col, delete, select

from src.core.engine.http_client import SmartHttpClient
from src.core.logger import logger
from src.domain.enums import VendorEnum
from src.domain.models import AdImageCache
from src.domain.schemas import CachedImageDTO


class ImageCacheService:
    """Gerencia o ciclo de vida, download, recuperação e expiração de imagens no SQLite."""

    def __init__(self, session: AsyncSession):
        self.session = session
        self.http_client = SmartHttpClient()

    async def get_image_by_id(self, image_id: str) -> Optional[Tuple[bytes, str]]:
        """Retorna os bytes e mime_type da imagem se ela existir e não tiver expirado."""
        now = datetime.now(timezone.utc)
        stmt = select(AdImageCache).where(
            AdImageCache.id == image_id,
            AdImageCache.expires_at > now,
        )
        res = await self.session.execute(stmt)
        record = res.scalars().first()
        if not record:
            return None
        return record.image_bytes, record.mime_type

    async def get_cached_images_for_listing(
        self, vendor: VendorEnum, vendor_listing_id: str
    ) -> List[CachedImageDTO]:
        """Retorna os metadados das imagens cacheadas e válidas para um anúncio."""
        now = datetime.now(timezone.utc)
        stmt = (
            select(AdImageCache)
            .where(
                AdImageCache.vendor == vendor,
                AdImageCache.vendor_listing_id == vendor_listing_id,
                AdImageCache.expires_at > now,
            )
            .order_by(AdImageCache.image_index)
        )
        res = await self.session.execute(stmt)
        records = res.scalars().all()

        return [
            CachedImageDTO(
                id=r.id,
                image_index=r.image_index,
                original_url=r.original_url,
                endpoint_url=f"/api/v1/scrape/images/{r.id}",
                mime_type=r.mime_type,
                size_bytes=r.size_bytes,
                expires_at=r.expires_at,
            )
            for r in records
        ]

    async def cache_images_for_listing(
        self,
        vendor: VendorEnum,
        vendor_listing_id: str,
        image_urls: List[str],
        ttl_hours: int = 12,
        max_images: int = 10,
    ) -> List[CachedImageDTO]:
        """
        Baixa e persiste no SQLite as imagens da lista fornecida com tempo de expiração.
        """
        if not image_urls:
            return []

        now = datetime.now(timezone.utc)
        expires_at = now + timedelta(hours=ttl_hours)

        # 1. Checa imagens já cacheadas e válidas para evitar re-download
        existing_cached = await self.get_cached_images_for_listing(vendor, vendor_listing_id)
        if existing_cached:
            logger.info(
                f"Found {len(existing_cached)} active cached images for listing {vendor_listing_id}. Skipping download."
            )
            return existing_cached

        cached_dtos: List[CachedImageDTO] = []
        urls_to_download = image_urls[:max_images]

        for idx, img_url in enumerate(urls_to_download):
            try:
                img_bytes, mime_type = await self.http_client.get_bytes(img_url)
                size_bytes = len(img_bytes)

                cache_record = AdImageCache(
                    vendor=vendor,
                    vendor_listing_id=vendor_listing_id,
                    image_index=idx,
                    original_url=img_url,
                    image_bytes=img_bytes,
                    mime_type=mime_type,
                    size_bytes=size_bytes,
                    created_at=now,
                    expires_at=expires_at,
                )
                self.session.add(cache_record)
                await self.session.flush()

                cached_dtos.append(
                    CachedImageDTO(
                        id=cache_record.id,
                        image_index=idx,
                        original_url=img_url,
                        endpoint_url=f"/api/v1/scrape/images/{cache_record.id}",
                        mime_type=mime_type,
                        size_bytes=size_bytes,
                        expires_at=expires_at,
                    )
                )
            except Exception as e:
                logger.warning(f"Failed to download and cache image {idx} from {img_url}: {e}")
                continue

        await self.session.commit()
        logger.info(
            f"Successfully cached {len(cached_dtos)} images for listing {vendor_listing_id} (TTL: {ttl_hours}h)"
        )
        return cached_dtos

    async def cleanup_expired(self) -> int:
        """Exclui todas as imagens cujo TTL expirou."""
        now = datetime.now(timezone.utc)
        stmt = delete(AdImageCache).where(AdImageCache.expires_at <= now)
        res = await self.session.execute(stmt)
        await self.session.commit()
        deleted_count = res.rowcount or 0
        if deleted_count > 0:
            logger.info(f"Cleaned up {deleted_count} expired images from SQLite cache.")
        return deleted_count
