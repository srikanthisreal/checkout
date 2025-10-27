@echo off
chcp 65001 >nul
echo Running k6 Load Test...
echo =======================

k6 run cart-load-test.js

echo.
echo Load test completed!
pause