"""
Módulo de Comunicação de Rede de Alta Performance com Bypass de Anti-Bot (TLS Impersonation).

Este módulo resolve o principal problema de scraping em marketplaces modernos:
o bloqueio de Web Application Firewalls (WAFs) como Cloudflare e DataDome.

Por que curl_cffi?
Bibliotecas HTTP convencionais (como requests, httpx ou o runtime padrão do Node.js)
utilizam a biblioteca OpenSSL padrão. O Cloudflare inspeciona a assinatura criptográfica
do handshake TLS (JA3/JA4 fingerprint), os conjuntos de cifras (cipher suites) e as
extensões HTTP/2. Ao detectar um client automatizado, retorna HTTP 403 / Desafio Turnstile
antes mesmo de avaliar os headers da requisição.

O `curl_cffi` (construído sobre curl-impersonate) força o socket TCP/TLS no nível C a
reproduzir com exatidão a impressão digital e o comportamento de rede de navegadores
reais como o Google Chrome 120+.
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
    
    Atributos:
        timeout (int): Tempo limite máximo em segundos para a requisição de rede.
        default_headers (dict): Conjunto completo de cabeçalhos que simulam
                                perfeitamente uma navegação legítima no Google Chrome.
    """

    def __init__(self, timeout: int = settings.DEFAULT_TIMEOUT_SECONDS):
        self.timeout = timeout
        # Headers que replicam um navegador Chrome 120 em Windows 64-bit
        self.default_headers = {
            "Accept": "text/html,application/xhtml+xml,application/xml;q=0.9,image/avif,image/webp,image/apng,*/*;q=0.8",
            "Accept-Language": "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
            "Sec-Ch-Ua": '"Not A(Brand";v="8", "Chromium";v="120", "Google Chrome";v="120"',
            "Sec-Ch-Ua-Mobile": "?0",
            "Sec-Ch-Ua-Platform": '"Windows"',
            "Sec-Fetch-Dest": "document",
            "Sec-Fetch-Mode": "navigate",
            "Sec-Fetch-Site": "none",
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
        """
        Executa uma requisição GET assíncrona com impersonação do Chrome 120.

        Parâmetros:
            url (str): A URL completa de destino (ex: página de busca da OLX).
            params (dict, opcional): Parâmetros de query string a serem anexados à URL.
            headers (dict, opcional): Cabeçalhos adicionais para sobrescrever os padrões.

        Retorna:
            str: O conteúdo HTML bruto da resposta em caso de sucesso (HTTP 200).

        Levanta:
            HttpClientBlockedException: Se o servidor retornar 403, 429, 503 ou página de desafio.
            Exception: Se ocorrer qualquer outro erro HTTP inesperado ou falha de conexão.
        """
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

                # Detecção de status de rate-limiting ou bloqueio por firewall
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

                # Verificação se o Cloudflare retornou página de verificação mesmo sob status 200
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
