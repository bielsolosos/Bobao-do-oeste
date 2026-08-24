"""
Módulo de Comunicação de Rede de Alta Performance com Bypass de Anti-Bot (TLS Impersonation).

Este módulo resolve o principal problema de scraping em marketplaces modernos:
o bloqueio de Web Application Firewalls (WAFs) como Cloudflare e DataDome.
"""

from typing import Any, Dict, Optional
from curl_cffi.requests import AsyncSession
from src.core.config import settings
from src.core.logger import logger


class HttpClientBlockedException(Exception):
    """
    Exceção lançada quando o servidor de destino retorna códigos de status
    de bloqueio ou apresenta uma página de desafio interativo do Cloudflare.
    """
    pass


class SmartHttpClient:
    """
    Cliente HTTP Assíncrono com emulação de TLS Fingerprint e cabeçalhos de navegador.
    """

    def __init__(self, timeout: int = settings.DEFAULT_TIMEOUT_SECONDS):
        self.timeout = timeout
        self.default_headers = {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language": "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
            "Referer": "https://www.google.com/",
            "Sec-Ch-Ua": '"Not A(Brand";v="8", "Chromium";v="120", "Google Chrome";v="120"',
            "Sec-Ch-Ua-Mobile": "?0",
            "Sec-Ch-Ua-Platform": '"Windows"',
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "cross-site",
            "Sec-Fetch-User": "?1",
            "Upgrade-Insecure-Requests": "1",
            "User-Agent": "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
        }

    async def get(
        self,
        url: str,
        params: Optional[Dict[str, Any]] = None,
        headers: Optional[Dict[str, str]] = None,
    ) -> str:
        req_headers = {**self.default_headers, **(headers or {})}
        logger.info(f"Fetching URL via curl_cffi (impersonate chrome120): {url}")

        async with AsyncSession(impersonate="chrome120") as session:
            try:
                response = await session.get(
                    url,
                    params=params,
                    headers=req_headers,
                    timeout=self.timeout,
                    allow_redirects=True,
                )

                if response.status_code in [403, 429, 503]:
                    logger.warning(
                        f"HTTP Client received status {response.status_code}. Possible bot challenge."
                    )
                    raise HttpClientBlockedException(
                        f"Target returned status {response.status_code} (Cloudflare/Anti-bot challenge)"
                    )

                if response.status_code != 200:
                    logger.warning(
                        f"Unexpected HTTP status {response.status_code} for URL: {url}"
                    )
                    raise Exception(f"HTTP request failed with status code {response.status_code}")

                text = response.text
                if "Just a moment..." in text and "Cloudflare" in text:
                    logger.warning("Cloudflare challenge page detected in 200 OK response.")
                    raise HttpClientBlockedException("Cloudflare challenge page detected")

                return text

            except HttpClientBlockedException:
                raise
            except Exception as e:
                logger.error(f"Error fetching URL {url}: {e}")
                raise
