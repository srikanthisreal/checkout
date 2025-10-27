@echo off
chcp 65001 >nul
echo Running k6 Stress Test...
echo =========================

k6 run cart-stress-test.js

echo.
echo Stress test completed!
pause