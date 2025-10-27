@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo Starting Continuous Load Test...
echo ================================
echo.
echo Test: Continuous 1-hour load with 10 users
echo File: continuous-load.js
echo.

echo Press Ctrl+C to stop the test at any time
echo.

k6 run continuous-load.js

echo.
echo Continuous test completed or stopped.
pause