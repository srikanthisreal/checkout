@echo off
chcp 65001 >nul
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:9090/actuator

echo Quick JVM Metrics
echo ================
echo.

echo 1. Memory Metrics:
curl -s "%BASE_URL%/metrics/jvm.memory.used"
echo.
echo.

echo 2. GC Metrics:
curl -s "%BASE_URL%/metrics/jvm.gc.pause"
echo.
echo.

echo 3. Thread Metrics:
curl -s "%BASE_URL%/metrics/jvm.threads.live"
echo.
echo.

echo 4. Application Info:
curl -s "%BASE_URL%/info"
echo.
echo.

echo 5. All Available Metrics:
curl -s "%BASE_URL%/metrics" | findstr /c:"name"
echo.

pause