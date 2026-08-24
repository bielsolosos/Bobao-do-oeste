import asyncio
import sys
from pathlib import Path

sys.path.insert(0, str(Path(__file__).parent.parent))

from src.core import async_session_maker, init_db
from src.domain import ScrapeRequest, ScrapingService, VendorEnum


async def main():
    print("=== Initializing Database ===")
    await init_db()

    print("\n=== Executing Live Scrape on OLX for 'thinkpad t480' ===")
    req = ScrapeRequest(
        vendor=VendorEnum.OLX,
        keyword="thinkpad t480",
        state="sp",
        min_price=500.0,
        max_price=2500.0,
        require_delivery=False,
        max_pages=1,
    )

    async with async_session_maker() as session:
        service = ScrapingService(session)
        response = await service.execute_scrape(req)

        print(f"\nResult: Success={response.success}")
        print(f"Execution ID: {response.execution.execution_id}")
        print(f"Status: {response.execution.status}")
        print(f"Duration: {response.execution.duration_ms}ms")
        print(f"Total Found: {response.execution.total_found}")
        print(f"New Items Count: {response.execution.new_items_count}")
        print(f"Used Browser Fallback: {response.execution.used_fallback}")

        if response.execution.error_message:
            print(f"Error Message: {response.execution.error_message}")

        print("\n--- First 3 Listings Preview ---")
        for i, item in enumerate(response.items[:3], 1):
            print(f"\n[{i}] {item.title}")
            print(f"    Preço: R$ {item.price:.2f} (Original: {item.original_price})")
            print(f"    URL: {item.url}")
            print(f"    Local: {item.city} - {item.state}")
            print(f"    Entrega (OLX Pay): {item.has_delivery}")
            print(f"    Fotos: {len(item.images)}")


if __name__ == "__main__":
    asyncio.run(main())
