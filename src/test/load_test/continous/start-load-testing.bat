@echo off
chcp 65001 >nul
title k6 Load Test Runner

set SCRIPT_DIR=%~dp0

:menu
cls
echo ================================
echo    k6 LOAD TESTING MENU
echo ================================
echo Current Directory: %SCRIPT_DIR%
echo.
echo 1. Quick Smoke Test (1 minute)
echo 2. Continuous Load (1 hour)
echo 3. Infinite Load (until stopped)
echo 4. Run with JVM Monitoring
echo 5. Stop All Tests
echo 6. Exit
echo.
set /p choice="Choose option (1-6): "

if "%choice%"=="1" goto smoke
if "%choice%"=="2" goto continuous
if "%choice%"=="3" goto infinite
if "%choice%"=="4" goto monitoring
if "%choice%"=="5" goto stop
if "%choice%"=="6" goto exit

echo Invalid choice. Press any key to try again.
pause
goto menu

:smoke
echo Running Quick Smoke Test...
cd /d "%SCRIPT_DIR%"
k6 run --vus 3 --duration 1m continuous-load.js
pause
goto menu

:continuous
echo Running Continuous Load Test...
cd /d "%SCRIPT_DIR%"
k6 run continuous-load.js
pause
goto menu

:infinite
echo Running Infinite Load Test...
echo Press Ctrl+C to stop when done.
cd /d "%SCRIPT_DIR%"
k6 run infinite-load.js
pause
goto menu

:monitoring
echo Starting with Monitoring...
cd /d "%SCRIPT_DIR%"
start monitor-jvm-enhanced.bat
timeout /t 3 /nobreak
k6 run continuous-load.js
taskkill /f /im cmd.exe /t /fi "windowtitle eq monitor-jvm-enhanced.bat" >nul 2>&1
pause
goto menu

:stop
echo Stopping all k6 processes...
taskkill /f /im k6.exe >nul 2>&1
echo All tests stopped.
pause
goto menu

:exit
echo Goodbye!
exit