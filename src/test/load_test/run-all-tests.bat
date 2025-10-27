@echo off
chcp 65001 >nul

echo Starting k6 Test Suite...
echo ========================

echo.
echo 1. Running Smoke Test...
k6 run cart-smoke-test.js

echo.
echo 2. Running Load Test...
k6 run cart-load-test.js

echo.
echo 3. Running Stress Test...
k6 run cart-stress-test.js

echo.
echo All tests completed!
pause