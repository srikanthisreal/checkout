@echo off
chcp 65001 >nul
echo Running k6 Smoke Test...
echo =========================

k6 run cart-smoke-test.js

echo.
echo Smoke test completed!
pause