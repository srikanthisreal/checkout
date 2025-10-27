@echo off
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:9090
set /a CONCURRENT_USERS=5
set /a REQUESTS_PER_USER=20

echo Starting Cart API Load Test
echo ================================
echo.

REM Function to generate random user ID
set /a RAND=%random%
set USER_ID=user-test-!RAND!

echo Testing with User: !USER_ID!
echo.

REM Warm up
echo Warming up JVM...
for /l %%i in (1,1,10) do (
    curl -s -X GET "%BASE_URL%/cart" -H "X-Anon-Id: warmup-%%i" > nul
)
echo Warmup completed.
echo.

REM Start load test
echo Starting load test: !CONCURRENT_USERS! concurrent requests
echo.

set /a SUCCESS=0
set /a FAIL=0

for /l %%i in (1,1,%CONCURRENT_USERS%) do (
    curl -s -X POST "%BASE_URL%/cart/items" ^
        -H "Idempotency-Key: idem-!USER_ID!-%%i" ^
        -H "X-User-Id: !USER_ID!" ^
        -H "X-Country: US" ^
        -H "X-Currency: USD" ^
        -H "Content-Type: application/json" ^
        -d "{\"productId\": 100%%i, \"quantity\": 2, \"placement\": \"PDP\", \"channel\": \"WEB\", \"giftWrap\": false, \"acceptBackorder\": false}" > nul
    
    if !errorlevel! equ 0 (
        set /a SUCCESS+=1
    ) else (
        set /a FAIL+=1
    )
    
    echo Request %%i completed
)

echo.
echo Load Test Results:
echo =================
echo Successful requests: !SUCCESS!
echo Failed requests: !FAIL!
echo.

echo Final Cart State:
curl -s -X GET "%BASE_URL%/cart" -H "X-User-Id: !USER_ID!"
echo.

echo Test completed!
pause