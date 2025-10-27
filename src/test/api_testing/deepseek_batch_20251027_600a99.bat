@echo off
setlocal enabledelayedexpansion

REM Cart API Testing Script for Windows
set BASE_URL=http://localhost:9090
set TIMESTAMP=%time::=% 
set TIMESTAMP=%TIMESTAMP: =%
set ANON_ID=anon-test-%TIMESTAMP%
set USER_ID=user-test-%TIMESTAMP%

echo ==============================================
echo    E-Commerce Cart API Testing Script
echo ==============================================
echo Anonymous ID: %ANON_ID%
echo User ID: %USER_ID%
echo Base URL: %BASE_URL%
echo ==============================================
echo.

REM Function-like structure for API calls
set "NL=^& echo."

echo 🎯 STEP 1: Get Empty Cart (First Time)
curl -s -X GET "%BASE_URL%/cart" ^
  -H "X-Anon-Id: %ANON_ID%" ^
  -H "Content-Type: application/json"
echo.%NL%

echo 🎯 STEP 2: Add First Item to Cart
curl -s -X POST "%BASE_URL%/cart/items" ^
  -H "Idempotency-Key: idem-first-%TIMESTAMP%" ^
  -H "X-Anon-Id: %ANON_ID%" ^
  -H "X-Country: US" ^
  -H "X-Currency: USD" ^
  -H "Content-Type: application/json" ^
  -d "{\"productId\": 1001, \"quantity\": 2, \"placement\": \"PDP\", \"channel\": \"WEB\", \"giftWrap\": false, \"acceptBackorder\": false}"
echo.%NL%

echo 🎯 STEP 3: Get Cart to Verify First Item
curl -s -X GET "%BASE_URL%/cart" ^
  -H "X-Anon-Id: %ANON_ID%" ^
  -H "Content-Type: application/json"
echo.%NL%

echo 🎯 STEP 4: Add Second Item with Variants
curl -s -X POST "%BASE_URL%/cart/items" ^
  -H "Idempotency-Key: idem-second-%TIMESTAMP%" ^
  -H "X-Anon-Id: %ANON_ID%" ^
  -H "X-Country: US" ^
  -H "X-Currency: USD" ^
  -H "Content-Type: application/json" ^
  -d "{\"sku\": \"TSHIRT-BLACK-M\", \"quantity\": 1, \"placement\": \"PDP\", \"channel\": \"WEB\", \"giftWrap\": true, \"giftMessage\": \"Happy Birthday!\", \"acceptBackorder\": false, \"variantAttributes\": {\"size\": \"M\", \"color\": \"Black\", \"material\": \"Cotton\"}}"
echo.%NL%

echo 🎯 STEP 5: Get Cart with Both Items
curl -s -X GET "%BASE_URL%/cart" ^
  -H "X-Anon-Id: %ANON_ID%" ^
  -H "Content-Type: application/json"
echo.%NL%

echo 🎯 STEP 6: Apply Coupon Code
curl -s -X POST "%BASE_URL%/cart/coupons" ^
  -H "X-Anon-Id: %ANON_ID%" ^
  -H "Content-Type: application/json" ^
  -d "{\"couponCode\": \"SUMMER2024\", \"clientCartVersion\": 2}"
echo.%NL%

echo 🎯 STEP 7: Get Cart with Coupon Applied
curl -s -X GET "%BASE_URL%/cart" ^
  -H "X-Anon-Id: %ANON_ID%" ^
  -H "Content-Type: application/json"
echo.%NL%

echo 🎯 STEP 8: Test User Cart
curl -s -X GET "%BASE_URL%/cart" ^
  -H "X-User-Id: %USER_ID%" ^
  -H "Content-Type: application/json"
echo.%NL%

echo 🎯 STEP 9: Merge Carts (Guest to User)
curl -s -X POST "%BASE_URL%/cart/merge" ^
  -H "Content-Type: application/json" ^
  -d "{\"guestAnonId\": \"%ANON_ID%\", \"userId\": \"%USER_ID%\"}"
echo.%NL%

echo 🎯 STEP 10: Get Merged User Cart
curl -s -X GET "%BASE_URL%/cart" ^
  -H "X-User-Id: %USER_ID%" ^
  -H "Content-Type: application/json"
echo.%NL%

echo 🎯 STEP 11: Clear Cart
curl -s -X POST "%BASE_URL%/cart/clear?clientCartVersion=3" ^
  -H "X-User-Id: %USER_ID%" ^
  -H "Content-Type: application/json"
echo.%NL%

echo 🎯 STEP 12: Verify Empty Cart
curl -s -X GET "%BASE_URL%/cart" ^
  -H "X-User-Id: %USER_ID%" ^
  -H "Content-Type: application/json"
echo.%NL%

echo ==============================================
echo ✅ Testing completed!
echo ==============================================
echo Check above responses for any errors.
echo You can also visit: %BASE_URL%/swagger-ui.html
echo for interactive API documentation
echo ==============================================
pause