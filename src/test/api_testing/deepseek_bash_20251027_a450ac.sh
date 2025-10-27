#!/bin/bash

# Cart API Testing Script
BASE_URL="http://localhost:8080"
TIMESTAMP=$(date +%s)
ANON_ID="anon-test-$TIMESTAMP"
USER_ID="user-test-$TIMESTAMP"

echo "=============================================="
echo "   E-Commerce Cart API Testing Script"
echo "=============================================="
echo "Anonymous ID: $ANON_ID"
echo "User ID: $USER_ID"
echo "Base URL: $BASE_URL"
echo "=============================================="
echo ""

# Function to make API calls with better formatting
make_api_call() {
    echo "➡️ $1"
    echo "🔗 URL: $2"
    if [ ! -z "$3" ]; then
        echo "📦 Request Body: $3"
    fi
    echo "📋 Response:"
    curl -s -w "\n⏱️ HTTP Status: %{http_code}\n" "$2" \
        -H "Content-Type: application/json" \
        -H "$4" \
        -d "$3"
    echo ""
    echo "----------------------------------------------"
}

echo "🎯 STEP 1: Get Empty Cart (First Time)"
make_api_call "GET Empty Cart" \
    "$BASE_URL/cart" \
    "" \
    "X-Anon-Id: $ANON_ID"

echo "🎯 STEP 2: Add First Item to Cart"
make_api_call "POST Add First Item" \
    "$BASE_URL/cart/items" \
    '{
        "productId": 1001,
        "quantity": 2,
        "placement": "PDP",
        "channel": "WEB",
        "giftWrap": false,
        "acceptBackorder": false
    }' \
    "X-Anon-Id: $ANON_ID" \
    "X-Country: US" \
    "X-Currency: USD" \
    "Idempotency-Key: idem-first-$TIMESTAMP"

echo "🎯 STEP 3: Get Cart to Verify First Item"
make_api_call "GET Cart" \
    "$BASE_URL/cart" \
    "" \
    "X-Anon-Id: $ANON_ID"

echo "🎯 STEP 4: Add Second Item with Variants"
make_api_call "POST Add Second Item" \
    "$BASE_URL/cart/items" \
    '{
        "sku": "TSHIRT-BLACK-M",
        "quantity": 1,
        "placement": "PDP",
        "channel": "WEB",
        "giftWrap": true,
        "giftMessage": "Happy Birthday!",
        "acceptBackorder": false,
        "variantAttributes": {
            "size": "M",
            "color": "Black",
            "material": "Cotton"
        }
    }' \
    "X-Anon-Id: $ANON_ID" \
    "X-Country: US" \
    "X-Currency: USD" \
    "Idempotency-Key: idem-second-$TIMESTAMP"

echo "🎯 STEP 5: Get Cart with Both Items"
make_api_call "GET Cart" \
    "$BASE_URL/cart" \
    "" \
    "X-Anon-Id: $ANON_ID"

echo "🎯 STEP 6: Apply Coupon Code"
make_api_call "POST Apply Coupon" \
    "$BASE_URL/cart/coupons" \
    '{
        "couponCode": "SUMMER2024",
        "clientCartVersion": 2
    }' \
    "X-Anon-Id: $ANON_ID"

echo "🎯 STEP 7: Get Cart with Coupon Applied"
make_api_call "GET Cart" \
    "$BASE_URL/cart" \
    "" \
    "X-Anon-Id: $ANON_ID"

echo "🎯 STEP 8: Test User Cart"
make_api_call "GET User Cart" \
    "$BASE_URL/cart" \
    "" \
    "X-User-Id: $USER_ID"

echo "🎯 STEP 9: Merge Carts (Guest to User)"
make_api_call "POST Merge Carts" \
    "$BASE_URL/cart/merge" \
    "{
        \"guestAnonId\": \"$ANON_ID\",
        \"userId\": \"$USER_ID\"
    }" \
    ""

echo "🎯 STEP 10: Get Merged User Cart"
make_api_call "GET Merged Cart" \
    "$BASE_URL/cart" \
    "" \
    "X-User-Id: $USER_ID"

echo "🎯 STEP 11: Clear Cart"
make_api_call "POST Clear Cart" \
    "$BASE_URL/cart/clear?clientCartVersion=3" \
    "" \
    "X-User-Id: $USER_ID"

echo "🎯 STEP 12: Verify Empty Cart"
make_api_call "GET Empty Cart After Clear" \
    "$BASE_URL/cart" \
    "" \
    "X-User-Id: $USER_ID"

echo "=============================================="
echo "✅ Testing completed!"
echo "=============================================="
echo "Check above responses for any errors."
echo "You can also visit: $BASE_URL/swagger-ui.html"
echo "for interactive API documentation"
echo "=============================================="