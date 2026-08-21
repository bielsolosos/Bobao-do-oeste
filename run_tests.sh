#!/usr/bin/env bash
set -e

echo "==================================================="
echo "  Running Marketplace Scraper Tests with UV"
echo "==================================================="

uv run pytest -v "$@"

echo ""
echo "[SUCCESS] All tests passed!"
