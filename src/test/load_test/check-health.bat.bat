@echo off
chcp 65001 >nul

set BASE_URL=http://localhost:9090/actuator

echo Simple Health Check
echo ==================
echo.

echo Application Health:
curl -s "%BASE_URL%/health"
echo.
echo.

echo Available Endpoints:
curl -s "%BASE_URL%" | findstr "href"
echo.
echo.

echo Press any key to exit...
pause >nul