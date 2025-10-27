@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:9090/actuator

:loop
cls
echo JVM Monitoring Dashboard
echo ================================
echo Date: %date% Time: %time%
echo.

echo HEALTH CHECK:
curl -s "%BASE_URL%/health"
echo.
echo.

echo MEMORY USAGE:
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.memory.used" ^| findstr "value"') do set MEMORY_USED=%%i
echo Memory Used: !MEMORY_USED! bytes

for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.memory.max" ^| findstr "value"') do set MEMORY_MAX=%%i
echo Memory Max: !MEMORY_MAX! bytes
echo.

echo GARBAGE COLLECTION:
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.gc.pause" ^| findstr "value"') do set GC_PAUSE=%%i
echo GC Pause: !GC_PAUSE! seconds
echo.

echo THREADS:
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.threads.live" ^| findstr "value"') do set LIVE_THREADS=%%i
echo Live Threads: !LIVE_THREADS!

for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/jvm.threads.daemon" ^| findstr "value"') do set DAEMON_THREADS=%%i
echo Daemon Threads: !DAEMON_THREADS!
echo.

echo HTTP REQUESTS:
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/http.server.requests" ^| findstr "COUNT"') do set TOTAL_REQUESTS=%%i
echo Total Requests: !TOTAL_REQUESTS!
echo.

echo SYSTEM:
for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/system.cpu.usage" ^| findstr "value"') do set CPU_USAGE=%%i
echo CPU Usage: !CPU_USAGE!

for /f "tokens=3 delims=:, " %%i in ('curl -s "%BASE_URL%/metrics/process.uptime" ^| findstr "value"') do set UPTIME=%%i
echo Uptime: !UPTIME! seconds
echo.

echo Press Ctrl+C to exit. Refreshing in 10 seconds...
timeout /t 10 /nobreak >nul
goto loop