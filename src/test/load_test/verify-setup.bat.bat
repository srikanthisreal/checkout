@echo off
echo Verifying Spring Boot Application Setup...
echo ========================================
echo.

echo 1. Checking if application is running on port 9090...
curl -s -o nul -w "HTTP Status: %%{http_code}\n" http://localhost:9090/actuator/health
echo.

echo 2. Checking Actuator endpoints...
curl -s http://localhost:9090/actuator | findstr "_links"
echo.

echo 3. Checking Swagger UI...
curl -s -o nul -w "Swagger UI Status: %%{http_code}\n" http://localhost:9090/swagger-ui.html
echo.

echo 4. Basic JVM Metrics...
curl -s http://localhost:9090/actuator/metrics/jvm.memory.used | findstr "value"
echo.

echo ========================================
echo If you see HTTP Status: 200 above, your app is ready!
echo You can proceed with testing.
pause