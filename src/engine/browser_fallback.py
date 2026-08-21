"""
Módulo de Navegador Headless (Playwright) para Fallback Resiliente.

Este módulo serve como camada de contingência (Tier 2) quando a extração direta
via requisição HTTP (Tier 1) for bloqueada por um desafio interativo de JavaScript,
CAPTCHA do Cloudflare Turnstile ou quando a página for uma Single Page Application (SPA)
que exija renderização no lado do cliente.

Técnicas de Evasão Aplicadas:
1. Mascaramento de flags de automação do Chromium (`navigator.webdriver = undefined`).
2. Desativação de flags internas do Blink (`AutomationControlled`).
3. Emulação de Viewport (1920x1080), Locale (`pt-BR`) e User-Agent de um desktop Windows real.
"""

from typing import Optional
from playwright.async_api import async_playwright
from src.core.config import settings
from src.core.logger import logger


class PlaywrightBrowserFallback:
    """
    Navegador Headless baseado em Playwright Chromium com técnicas stealth para contornar desafios.
    
    Atributos:
        headless (bool): Define se o navegador roda em background (True) ou com janela visível (False).
    """

    def __init__(self, headless: bool = settings.HEADLESS):
        self.headless = headless

    async def get_page_content(self, url: str, wait_selector: Optional[str] = None) -> str:
        """
        Abre o Chromium, navega até a URL especificada, aguarda a hidratação/renderização
        do DOM e retorna o HTML completo da página.

        Parâmetros:
            url (str): Endereço web a ser acessado.
            wait_selector (str, opcional): Seletor CSS a ser aguardado antes de capturar o HTML.

        Retorna:
            str: O código HTML renderizado da página.
        """
        logger.info(f"Triggering Playwright Browser Fallback for URL: {url}")
        async with async_playwright() as p:
            browser = await p.chromium.launch(
                headless=self.headless,
                args=[
                    "--no-sandbox",
                    "--disable-setuid-sandbox",
                    "--disable-blink-features=AutomationControlled",
                    "--disable-infobars",
                ],
            )
            context = await browser.new_context(
                user_agent="Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36",
                locale="pt-BR",
                viewport={"width": 1920, "height": 1080},
            )

            # Injeta script para mascarar a propriedade navigator.webdriver (evita detecção por WAFs)
            await context.add_init_script(
                """
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined
                });
                """
            )

            page = await context.new_page()
            try:
                # Navega até que o evento DOMContentLoaded seja disparado
                await page.goto(
                    url,
                    wait_until="domcontentloaded",
                    timeout=settings.DEFAULT_TIMEOUT_SECONDS * 1000,
                )

                if wait_selector:
                    try:
                        await page.wait_for_selector(wait_selector, timeout=5000)
                    except Exception:
                        logger.warning(
                            f"Selector '{wait_selector}' not found in time, continuing with raw HTML."
                        )

                # Pausa estratégica para permitir que scripts de hidratação (Next.js/React) finalizem
                await page.wait_for_timeout(1000)
                content = await page.content()
                logger.info(f"Playwright successfully retrieved {len(content)} bytes of HTML.")
                return content
            finally:
                await context.close()
                await browser.close()
