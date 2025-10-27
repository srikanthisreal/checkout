@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo Starting Load Test with Real-time Monitoring...
echo =============================================
echo.

echo Step 1: Starting background monitoring...
start monitor-jvm.bat

echo Step 2: Waiting for monitoring to start...
timeout /t 5 /nobreak

echo Step 3: Starting continuous load test...
echo.
echo Load Test Details:
echo - Duration: 1 hour
echo - Users: 10 concurrent
echo - Operations: Add to cart, get cart, update quantities
echo.

k6 run continuous-load.js

echo.
echo Step 4: Stopping monitoring...
taskkill /f /im cmd.exe /t /fi "windowtitle eq monitor-jvm.bat" >nul 2>&1

echo.
echo Test completed!
pause