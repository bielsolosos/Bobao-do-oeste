@echo off
setlocal
echo ===================================================
echo   Starting Marketplace Scraper FastAPI Service
echo   Docs: http://localhost:8001/docs
echo ===================================================

uv run uvicorn src.main:app --reload --host 127.0.0.1 --port 8001
