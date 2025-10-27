@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

echo Starting Infinite Load Test...
echo =============================
echo.
echo Test: Infinite loop with 5 users (runs until stopped)
echo File: infinite-load.js
echo.
echo WARNING: This will run until manually stopped with Ctrl+C
echo.

k6 run infinite-load.js

echo.
echo Infinite test stopped.
pause