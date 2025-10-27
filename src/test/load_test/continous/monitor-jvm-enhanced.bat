@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:9090/actuator
set INTERVAL=10

title JVM Monitor - Load Test

:loop
cls
echo [%date% %time%] JVM MONITOR - LOAD TEST IN PROGRESS
echo ==================================================
echo.

:: Memory Usage
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.memory.used" 2^>nul ^| findstr "area.*heap"') do set HEAP_USED=%%i
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.memory.max" 2^>nul ^| findstr "area.*heap"') do set HEAP_MAX=%%i
if defined HEAP_USED if defined HEAP_MAX (
    set /a HEAP_USED_MB=!HEAP_USED!/1024/1024
    set /a HEAP_MAX_MB=!HEAP_MAX!/1024/1024
    set /a HEAP_PCT=!HEAP_USED!*100/!HEAP_MAX!
    echo 🧠 HEAP MEMORY: !HEAP_USED_MB!MB / !HEAP_MAX_MB!MB (!HEAP_PCT!%%)
)

:: GC Activity
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.gc.pause" 2^>nul ^| findstr "count"') do set GC_COUNT=%%i
if defined GC_COUNT echo 🗑️  GC COUNT: !GC_COUNT!

:: Threads
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.threads.live" 2^>nul ^| findstr "value"') do set THREADS=%%i
if defined THREADS echo 🧵 LIVE THREADS: !THREADS!

:: HTTP Requests
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/http.server.requests" 2^>nul ^| findstr "COUNT"') do set REQUESTS=%%i
if defined REQUESTS echo 🌐 TOTAL REQUESTS: !REQUESTS!

:: CPU
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/system.cpu.usage" 2^>nul ^| findstr "value"') do (
    set CPU=%%i
    if defined CPU (
        set /a CPU_PCT=!CPU:*e-=!*100
        echo 💻 CPU USAGE: !CPU_PCT!%%
    )
)

:: Process Info
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/process.uptime" 2^>nul ^| findstr "value"') do set UPTIME=%%i
if defined UPTIME (
    set /a UPTIME_MIN=!UPTIME!/60
    echo ⏰ UPTIME: !UPTIME_MIN! minutes
)

echo.
echo ==================================================
echo Monitoring... (Refresh every !INTERVAL!s - Ctrl+C to stop)
echo Load Test should be running in another window...
timeout /t %INTERVAL% /nobreak >nul
goto loop