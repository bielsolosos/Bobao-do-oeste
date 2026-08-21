from typing import Optional
from playwright.async_api import async_playwright
from src.core.config import settings
from src.core.logger import logger


class PlaywrightBrowserFallback:
    """Headless browser fallback to handle JavaScript challenges and SPAs."""

    def __init__(self, headless: bool = settings.HEADLESS):
        self.headless = headless

    async def get_page_content(self, url: str, wait_selector: Optional[str] = None) -> str:
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

            # Mask webdriver flags
            await context.add_init_script(
                """
                Object.defineProperty(navigator, 'webdriver', {
                    get: () => undefined
                });
                """
            )

            page = await context.new_page()
            try:
                await page.goto(url, wait_until="domcontentloaded", timeout=settings.DEFAULT_TIMEOUT_SECONDS * 1000)

                if wait_selector:
                    try:
                        await page.wait_for_selector(wait_selector, timeout=5000)
                    except Exception:
                        logger.warning(f"Selector '{wait_selector}' not found in time, continuing with raw HTML.")

                # Wait slightly for hydration if needed
                await page.wait_for_timeout(1000)
                content = await page.content()
                logger.info(f"Playwright successfully retrieved {len(content)} bytes of HTML.")
                return content
            finally:
                await context.close()
                await browser.close()
