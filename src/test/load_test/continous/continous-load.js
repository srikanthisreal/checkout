import http from 'k6/http';
import { check, sleep } from 'k6';
import { Counter, Rate, Trend } from 'k6/metrics';

// Custom metrics
const successfulAddToCart = new Counter('successful_add_to_cart');
const failedAddToCart = new Rate('failed_add_to_cart');
const cartOperationDuration = new Trend('cart_operation_duration');

export const options = {
  stages: [
    // Ramp up to 10 users over 1 minute
    { duration: '1m', target: 10 },
    // Stay at 10 users for 59 minutes (continuous)
    { duration: '59m', target: 10 },
    // Optionally ramp down (or leave running)
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],      // Less than 5% failures
    http_req_duration: ['p(95)<1000'],   // 95% requests under 1s
    failed_add_to_cart: ['rate<0.1'],    // Less than 10% cart failures
  },
};

const BASE_URL = 'http://localhost:9090';

export default function () {
  const userId = `user-${__VU}-${Date.now()}`;
  const startTime = Date.now();

  try {
    // 1. Add item to cart (main operation)
    const addItemPayload = JSON.stringify({
      productId: 1000 + Math.floor(Math.random() * 1000),
      quantity: Math.floor(Math.random() * 3) + 1,
      placement: ['PDP', 'PLP', 'SEARCH'][Math.floor(Math.random() * 3)],
      channel: ['WEB', 'IOS', 'ANDROID'][Math.floor(Math.random() * 3)],
      giftWrap: Math.random() > 0.9,
      acceptBackorder: Math.random() > 0.5,
      variantAttributes: Math.random() > 0.7 ? {
        size: ['S', 'M', 'L', 'XL'][Math.floor(Math.random() * 4)],
        color: ['Black', 'White', 'Blue', 'Red'][Math.floor(Math.random() * 4)],
      } : undefined,
    });

    const addItemRes = http.post(`${BASE_URL}/cart/items`, addItemPayload, {
      headers: {
        'Idempotency-Key': `idem-${userId}-${__ITER}-${Date.now()}`,
        'X-User-Id': userId,
        'X-Country': 'US',
        'X-Currency': 'USD',
        'Content-Type': 'application/json',
      },
      tags: { name: 'addToCart' },
    });

    const addSuccess = check(addItemRes, {
      'add to cart successful': (r) => r.status === 201,
      'response has cart ID': (r) => r.json('cartId') !== undefined,
    });

    if (addSuccess) {
      successfulAddToCart.add(1);
    } else {
      failedAddToCart.add(1);
      console.log(`Add to cart failed: ${addItemRes.status} - ${addItemRes.body}`);
    }

    // 2. Get cart (60% of the time)
    if (Math.random() > 0.4) {
      const getCartRes = http.get(`${BASE_URL}/cart`, {
        headers: {
          'X-User-Id': userId,
          'Content-Type': 'application/json',
        },
        tags: { name: 'getCart' },
      });

      check(getCartRes, {
        'get cart successful': (r) => r.status === 200 || r.status === 404,
      });
    }

    // 3. Update quantity (20% of the time, if we have a cart)
    if (Math.random() > 0.8 && addItemRes.status === 201) {
      const cart = addItemRes.json();
      if (cart.lines && cart.lines.length > 0) {
        const lineId = cart.lines[0].lineId;

        const updatePayload = JSON.stringify({
          quantity: Math.floor(Math.random() * 5) + 1,
          clientCartVersion: cart.version,
        });

        http.patch(`${BASE_URL}/cart/items/${lineId}`, updatePayload, {
          headers: {
            'X-User-Id': userId,
            'Content-Type': 'application/json',
          },
          tags: { name: 'updateQuantity' },
        });
      }
    }

    const endTime = Date.now();
    cartOperationDuration.add(endTime - startTime);

  } catch (error) {
    failedAddToCart.add(1);
    console.log(`Request failed: ${error.message}`);
  }

  // Random sleep between 2-5 seconds to simulate real user behavior
  sleep(Math.random() * 3 + 2);
}