@echo off
chcp 65001 >nul
echo Checking required files...
echo =========================

if exist "docker-compose.yml" (
    echo ✅ docker-compose.yml - FOUND
) else (
    echo ❌ docker-compose.yml - MISSING
)

if exist "prometheus.yml" (
    echo ✅ prometheus.yml - FOUND
) else (
    echo ❌ prometheus.yml - MISSING
)

if exist "grafana-datasources.yml" (
    echo ✅ grafana-datasources.yml - FOUND
) else (
    echo ❌ grafana-datasources.yml - MISSING
)

echo.
echo Current directory: %CD%
echo.
dir *.yml *.yaml

echo.
pause