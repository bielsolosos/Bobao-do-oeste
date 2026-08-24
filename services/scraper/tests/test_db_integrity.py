"""
Testes de integridade do schema (constraints do produtor dumb).

- UNIQUE(vendor, vendor_listing_id) em scraped_listings
- FK enforcement ativo (PRAGMA foreign_keys=ON)
"""

import pytest
from sqlalchemy import text
from sqlalchemy.exc import IntegrityError

from src.core.database import async_session_maker
from src.domain.enums import ExecutionStatusEnum, VendorEnum
from src.domain.models import ScrapedListing, ScrapingExecution, SearchQuery


async def _cleanup() -> None:
    async with async_session_maker() as session:
        await session.execute(text("DELETE FROM scraped_listings"))
        await session.execute(text("DELETE FROM scraping_executions"))
        await session.execute(text("DELETE FROM search_queries"))
        await session.commit()


async def _make_execution() -> ScrapingExecution:
    async with async_session_maker() as session:
        query = SearchQuery(
            id="q-test-fk",
            vendor=VendorEnum.OLX,
            keyword="test",
        )
        session.add(query)
        await session.commit()

        exec_ = ScrapingExecution(
            search_query_id="q-test-fk",
            vendor=VendorEnum.OLX,
            status=ExecutionStatusEnum.RUNNING,
        )
        session.add(exec_)
        await session.commit()
        await session.refresh(exec_)
        return exec_


@pytest.mark.asyncio
async def test_pragma_foreign_keys_is_enabled():
    """Garante que o engine habilita FK enforcement por padrão."""
    async with async_session_maker() as session:
        result = await session.execute(text("PRAGMA foreign_keys"))
        assert result.scalar() == 1, "PRAGMA foreign_keys deve estar ON"


@pytest.mark.asyncio
async def test_scraped_listings_unique_constraint_blocks_duplicate_vendor_id():
    """
    UNIQUE(vendor, vendor_listing_id) deve bloquear inserção duplicada
    dentro do mesmo vendor. Esse é o safety-net do parser — se um
    anúncio aparecer 2x no mesmo scrape, o produtor recusa.
    """
    await _cleanup()
    exec_ = await _make_execution()

    async with async_session_maker() as session:
        listing1 = ScrapedListing(
            execution_id=exec_.id,
            vendor=VendorEnum.OLX,
            vendor_listing_id="abc-123",
            title="ThinkPad T480",
            price=1500.0,
            url="https://olx.com.br/thinkpad-abc-123",
        )
        session.add(listing1)
        await session.commit()

    async with async_session_maker() as session:
        listing2 = ScrapedListing(
            execution_id=exec_.id,
            vendor=VendorEnum.OLX,
            vendor_listing_id="abc-123",  # mesmo vendor + id
            title="ThinkPad T480 (duplicata)",
            price=1500.0,
            url="https://olx.com.br/thinkpad-abc-123-dup",
        )
        session.add(listing2)
        with pytest.raises(IntegrityError):
            await session.commit()
    await _cleanup()


@pytest.mark.asyncio
async def test_scraped_listings_allows_same_id_across_different_vendors():
    """
    O UNIQUE é composto (vendor, vendor_listing_id), então o MESMO
    vendor_listing_id pode existir em vendors diferentes (ex: ID
    numérico "12345" na OLX E no Mercado Livre).
    """
    await _cleanup()
    exec_ = await _make_execution()

    async with async_session_maker() as session:
        listing_olx = ScrapedListing(
            execution_id=exec_.id,
            vendor=VendorEnum.OLX,
            vendor_listing_id="12345",
            title="Anúncio OLX",
            price=100.0,
            url="https://olx.com.br/12345",
        )
        listing_ml = ScrapedListing(
            execution_id=exec_.id,
            vendor=VendorEnum.MERCADO_LIVRE,
            vendor_listing_id="12345",  # mesmo id, vendor diferente
            title="Anúncio ML",
            price=100.0,
            url="https://mercadolivre.com.br/12345",
        )
        session.add_all([listing_olx, listing_ml])
        await session.commit()  # não deve dar erro
    await _cleanup()


@pytest.mark.asyncio
async def test_foreign_key_blocks_listing_with_nonexistent_execution():
    """
    FK enforcement deve bloquear ScrapedListing com execution_id
    que não existe em scraping_executions.
    """
    await _cleanup()
    async with async_session_maker() as session:
        orphan = ScrapedListing(
            execution_id="execution-fantasma",
            vendor=VendorEnum.OLX,
            vendor_listing_id="orphan-1",
            title="Anúncio órfão",
            price=100.0,
            url="https://olx.com.br/orphan-1",
        )
        session.add(orphan)
        with pytest.raises(IntegrityError):
            await session.commit()
    await _cleanup()
