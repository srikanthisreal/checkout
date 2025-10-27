@echo off
chcp 65001 >nul
echo Starting Monitoring Stack...
echo ===========================

echo 1. Checking files...
call check-files.bat

echo.
echo 2. Starting Docker containers...
docker-compose up -d

echo.
echo 3. Waiting for services to start...
timeout /t 15 /nobreak

echo.
echo 4. Checking services...
echo - Testing Spring Boot App...
curl -s -o nul -w "Spring Boot: %%{http_code}\n" http://localhost:9090/actuator/health

echo - Testing Prometheus...
curl -s -o nul -w "Prometheus: %%{http_code}\n" http://localhost:9091/graph

echo - Testing Grafana...
curl -s -o nul -w "Grafana: %%{http_code}\n" http://localhost:3000/api/health

echo.
echo 5. Access URLs:
echo    📊 Your Spring Boot App: http://localhost:9090
echo    📈 Prometheus UI: http://localhost:9091
echo    📉 Grafana Dashboard: http://localhost:3000 (admin/admin)
echo    📚 Swagger API Docs: http://localhost:9090/swagger-ui.html
echo    🔧 Actuator Endpoints: http://localhost:9090/actuator

echo.
echo To stop: docker-compose down
pause