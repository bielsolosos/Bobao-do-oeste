@echo off
setlocal
echo ===================================================
echo   Running Marketplace Scraper Tests with UV
echo ===================================================

uv run pytest -v %*

if %ERRORLEVEL% equ 0 (
    echo.
    echo [SUCCESS] All tests passed!
) else (
    echo.
    echo [FAILURE] Some tests failed. Check output above.
)

exit /b %ERRORLEVEL%
