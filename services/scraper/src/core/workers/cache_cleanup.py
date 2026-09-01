"""
Worker assíncrono em background para limpeza periódica de imagens e caches expirados no SQLite.
"""

import asyncio
from typing import Optional

from src.core.config import settings
from src.core.database import async_session_maker
from src.core.logger import logger
from src.domain.services.image_cache_service import ImageCacheService


class CacheCleanupWorker:
    """Worker em background que roda periodicamente para purgar registros com TTL expirado."""

    def __init__(self, interval_seconds: Optional[int] = None):
        self.interval_seconds = interval_seconds or settings.CACHE_CLEANUP_INTERVAL_SECONDS
        self._task: Optional[asyncio.Task] = None
        self._stop_event = asyncio.Event()

    async def start(self) -> None:
        """Inicia a task assíncrona periódica."""
        logger.info(
            f"Iniciando CacheCleanupWorker (intervalo de limpeza = {self.interval_seconds}s / {self.interval_seconds / 3600:.1f}h)..."
        )
        self._stop_event.clear()
        self._task = asyncio.create_task(self._run_loop(), name="cache-cleanup-worker")

    async def stop(self) -> None:
        """Sinaliza parada e aguarda a finalização."""
        logger.info("Parando CacheCleanupWorker...")
        self._stop_event.set()
        if self._task and not self._task.done():
            self._task.cancel()
            try:
                await self._task
            except asyncio.CancelledError:
                pass

    async def run_once(self) -> int:
        """Executa um ciclo único de limpeza (útil para testes ou triggers manuais)."""
        async with async_session_maker() as session:
            service = ImageCacheService(session)
            return await service.cleanup_expired()

    async def _run_loop(self) -> None:
        # Executa uma limpeza inicial na inicialização
        try:
            await self.run_once()
        except Exception as e:
            logger.error(f"Erro na limpeza inicial de cache expirado: {e}")

        while not self._stop_event.is_set():
            try:
                # Aguarda o intervalo ou o sinal de parada
                await asyncio.wait_for(self._stop_event.wait(), timeout=float(self.interval_seconds))
                break
            except asyncio.TimeoutError:
                # Timeout esperado: hora de rodar a limpeza
                try:
                    logger.debug("Executando ciclo periódico de limpeza de cache SQLite...")
                    await self.run_once()
                except Exception as e:
                    logger.error(f"Erro durante ciclo de limpeza periódica de cache: {e}")


_cleanup_worker_instance: Optional[CacheCleanupWorker] = None


def get_cache_cleanup_worker() -> CacheCleanupWorker:
    global _cleanup_worker_instance
    if _cleanup_worker_instance is None:
        _cleanup_worker_instance = CacheCleanupWorker()
    return _cleanup_worker_instance
