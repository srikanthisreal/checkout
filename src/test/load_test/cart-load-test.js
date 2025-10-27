import http from 'k6/http';
import { check, sleep } from 'k6';
import { Rate, Trend, Counter } from 'k6/metrics';

// Custom metrics
const addToCartFailureRate = new Rate('add_to_cart_failures');
const cartOperationDuration = new Trend('cart_operation_duration');
const successfulOperations = new Counter('successful_operations');

export const options = {
  stages: [
    { duration: '2m', target: 50 },  // Ramp up to 50 users
    { duration: '5m', target: 50 },  // Stay at 50 users
    { duration: '2m', target: 100 }, // Ramp up to 100 users
    { duration: '5m', target: 100 }, // Stay at 100 users
    { duration: '2m', target: 0 },   // Ramp down to 0 users
  ],
  thresholds: {
    http_req_failed: ['rate<0.02'],           // Less than 2% failures
    http_req_duration: ['p(95)<1000'],        // 95% < 1s
    add_to_cart_failures: ['rate<0.1'],       // Less than 10% add to cart failures
    cart_operation_duration: ['p(95)<800'],   // 95% cart ops < 800ms
  },
};

const BASE_URL = 'http://localhost:9090';

export default function () {
  const userId = `load-user-${__VU}-${__ITER}`;
  const anonId = `load-anon-${__VU}-${__ITER}`;
  const startTime = Date.now();

  // 1. Add item to cart
  const addItemPayload = JSON.stringify({
    productId: 1000 + Math.floor(Math.random() * 1000),
    quantity: Math.floor(Math.random() * 3) + 1,
    placement: ['PDP', 'PLP', 'SEARCH'][Math.floor(Math.random() * 3)],
    channel: ['WEB', 'IOS', 'ANDROID'][Math.floor(Math.random() * 2)],
    giftWrap: Math.random() > 0.8,
    acceptBackorder: Math.random() > 0.5,
    variantAttributes: Math.random() > 0.5 ? {
      size: ['S', 'M', 'L', 'XL'][Math.floor(Math.random() * 4)],
      color: ['Black', 'White', 'Blue', 'Red'][Math.floor(Math.random() * 4)],
    } : undefined,
  });

  const addItemRes = http.post(`${BASE_URL}/cart/items`, addItemPayload, {
    headers: {
      'Idempotency-Key': `idem-${userId}-${Date.now()}`,
      'X-User-Id': userId,
      'X-Country': 'US',
      'X-Currency': 'USD',
      'Content-Type': 'application/json',
    },
  });

  const addItemSuccess = check(addItemRes, {
    'add item successful': (r) => r.status === 201,
  });

  addToCartFailureRate.add(!addItemSuccess);
  if (addItemSuccess) successfulOperations.add(1);

  // 2. Get cart (80% of the time after adding)
  if (Math.random() > 0.2) {
    const getCartRes = http.get(`${BASE_URL}/cart`, {
      headers: {
        'X-User-Id': userId,
        'Content-Type': 'application/json',
      },
    });

    check(getCartRes, {
      'get cart successful': (r) => r.status === 200,
    });
  }

  // 3. Update quantity (30% of the time)
  if (Math.random() > 0.7 && addItemSuccess) {
    const cart = addItemRes.json();
    if (cart.lines && cart.lines.length > 0) {
      const lineId = cart.lines[0].lineId;

      const updatePayload = JSON.stringify({
        quantity: Math.floor(Math.random() * 5) + 1,
        clientCartVersion: cart.version,
      });

      const updateRes = http.patch(`${BASE_URL}/cart/items/${lineId}`, updatePayload, {
        headers: {
          'X-User-Id': userId,
          'Content-Type': 'application/json',
        },
      });

      check(updateRes, {
        'update quantity successful': (r) => r.status === 200,
      });
    }
  }

  const endTime = Date.now();
  cartOperationDuration.add(endTime - startTime);

  sleep(Math.random() * 2 + 1); // Random sleep between 1-3 seconds
}