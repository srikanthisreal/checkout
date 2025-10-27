@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:9090/actuator
set INTERVAL=5

echo Starting Real-time Monitoring during k6 test...
echo ==============================================

:loop
cls
echo [%date% %time%] Live Metrics During Load Test
echo ==============================================
echo.

:: Memory
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.memory.used" 2^>nul ^| findstr "area.*heap"') do set HEAP=%%i
if defined HEAP (
    set /a HEAP_MB=!HEAP!/1024/1024
    echo Heap Memory: !HEAP_MB! MB
)

:: GC
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.gc.pause" 2^>nul ^| findstr "count"') do set GC_COUNT=%%i
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.gc.pause" 2^>nul ^| findstr "totalTime"') do set GC_TIME=%%i
if defined GC_COUNT echo GC Count: !GC_COUNT! | if defined GC_TIME echo Total GC Time: !GC_TIME! ms

:: Threads
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.threads.live" 2^>nul ^| findstr "value"') do set THREADS=%%i
if defined THREADS echo Live Threads: !THREADS!

:: HTTP Requests
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/http.server.requests" 2^>nul ^| findstr "COUNT"') do set REQUESTS=%%i
if defined REQUESTS echo Total HTTP Requests: !REQUESTS!

:: CPU
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/system.cpu.usage" 2^>nul ^| findstr "value"') do (
    set CPU=%%i
    if defined CPU (
        set /a CPU_PCT=!CPU:*e-=!*100
        echo CPU Usage: !CPU_PCT!%%
    )
)

echo.
echo Monitoring... (Press Ctrl+C to stop)
timeout /t %INTERVAL% /nobreak >nul
goto loop