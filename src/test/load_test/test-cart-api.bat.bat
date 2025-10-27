@echo off
setlocal enabledelayedexpansion

set BASE_URL=http://localhost:9090
set /a RAND=%random%
set ANON_ID=anon-test-!RAND!
set USER_ID=user-test-!RAND!

echo Cart API Test Script
echo ====================
echo Anonymous ID: !ANON_ID!
echo User ID: !USER_ID!
echo.

echo Step 1: Get Empty Cart
curl -s -X GET "%BASE_URL%/cart" -H "X-Anon-Id: !ANON_ID!"
echo.
echo.

echo Step 2: Add Item to Cart
curl -s -X POST "%BASE_URL%/cart/items" ^
  -H "Idempotency-Key: idem-first-!RAND!" ^
  -H "X-Anon-Id: !ANON_ID!" ^
  -H "X-Country: US" ^
  -H "X-Currency: USD" ^
  -H "Content-Type: application/json" ^
  -d "{\"productId\": 1001, \"quantity\": 2, \"placement\": \"PDP\", \"channel\": \"WEB\", \"giftWrap\": false, \"acceptBackorder\": false}"
echo.
echo.

echo Step 3: Get Cart with Item
curl -s -X GET "%BASE_URL%/cart" -H "X-Anon-Id: !ANON_ID!"
echo.
echo.

echo Step 4: Apply Coupon
curl -s -X POST "%BASE_URL%/cart/coupons" ^
  -H "X-Anon-Id: !ANON_ID!" ^
  -H "Content-Type: application/json" ^
  -d "{\"couponCode\": \"TEST2024\", \"clientCartVersion\": 1}"
echo.
echo.

echo Step 5: Final Cart State
curl -s -X GET "%BASE_URL%/cart" -H "X-Anon-Id: !ANON_ID!"
echo.
echo.

echo Test completed!
echo.
echo You can also visit: http://localhost:9090/swagger-ui.html
echo For actuator: http://localhost:9090/actuator
pause