#!/usr/bin/env bash
set -euo pipefail

echo "==================================================="
echo "  Starting Marketplace Scraper FastAPI Service"
echo "  Docs: http://localhost:8001/docs"
echo "==================================================="

exec uv run uvicorn src.main:app --host 127.0.0.1 --port 8001 --no-access-log
