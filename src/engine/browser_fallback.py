"""
Módulo de Navegador Headless (Playwright) com Stealth Avançado para Bypass de WAF/Cloudflare.

Este módulo é ativado quando a requisição HTTP rápida (curl_cffi) recebe status 403
(comum em IPs de DataCenter/VPS).

Técnicas de Evasão Aplicadas:
1. Aplicação de `playwright_stealth` (Stealth v2) sobre o contexto do Chromium.
2. Argumentos de evasão de automação do Chromium (--disable-blink-features=AutomationControlled).
3. Loop inteligente de espera para resolução automática do Cloudflare Turnstile Challenge (JS Proof-of-Work).
"""

import asyncio
from typing import Optional
from playwright.async_api import async_playwright
from playwright_stealth import Stealth
from src.core.config import settings
from src.core.logger import logger


class PlaywrightBrowserFallback:
    """
    Navegador Headless baseado em Playwright Chromium com técnicas stealth para contornar desafios.
    """

    def __init__(self, headless: bool = settings.HEADLESS):
        self.headless = headless

    async def get_page_content(self, url: str, wait_selector: Optional[str] = None) -> str:
        """
        Abre o Chromium em modo Stealth, navega até a URL, aguarda a resolução
        de eventuais desafios do Cloudflare e retorna o HTML real da página.
        """
        logger.info(f"Triggering Stealth Playwright Browser Fallback for URL: {url}")
        async with async_playwright() as p:
            browser = await p.chromium.launch(
                headless=self.headless,
                args=[
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-dev-shm-usage",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-infobars",
                    "--window-size=1920,1080",
                ],
            )
            context = await browser.new_context(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                locale="pt-BR",
                viewport={"width": 1920, "height": 1080},
                extra_http_headers={
                    "Accept-Language": "pt-BR,pt;q=0.9,en-US;q=0.8,en;q=0.7",
                    "Sec-Ch-Ua": '"Not A(Brand";v="8", "Chromium";v="120", "Google Chrome";v="120"',
                    "Sec-Ch-Ua-Mobile": "?0",
                    "Sec-Ch-Ua-Platform": '"Windows"',
                },
            )

            # Aplica regras completas de stealth (WebGL, Canvas, Webdriver, Plugins, Permissions)
            stealth = Stealth()
            await stealth.apply_stealth_async(context)

            page = await context.new_page()
            try:
                # Navega até o site
                await page.goto(
                    url,
                    wait_until="domcontentloaded",
                    timeout=settings.DEFAULT_TIMEOUT_SECONDS * 1000,
                )

                # Loop de detecção e espera do Cloudflare Turnstile / Challenge
                # Em datacenters, o Cloudflare pode levar de 2 a 6 segundos para validar o desafio
                for i in range(12):
                    title = await page.title()
                    content = await page.content()

                    is_cloudflare_challenge = (
                        "Just a moment..." in title
                        or "Attention Required! | Cloudflare" in title
                        or len(content) < 8000
                    )

                    if not is_cloudflare_challenge:
                        logger.info(f"Cloudflare challenge cleared after {i}s.")
                        break

                    logger.info(f"Waiting for Cloudflare verification... ({i+1}s)")
                    await page.wait_for_timeout(1000)

                # Se um seletor específico foi solicitado, aguarda
                if wait_selector:
                    try:
                        await page.wait_for_selector(wait_selector, timeout=5000)
                    except Exception:
                        logger.warning(
                            f"Selector '{wait_selector}' not found in time, continuing with raw HTML."
                        )

                # Aguarda hidratação final
                await page.wait_for_timeout(1500)
                final_content = await page.content()
                logger.info(f"Playwright successfully retrieved {len(final_content)} bytes of HTML.")
                return final_content
            finally:
                await context.close()
                await browser.close()
