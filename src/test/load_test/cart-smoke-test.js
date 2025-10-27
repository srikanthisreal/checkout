import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 1, // 1 virtual user
  duration: '30s',
  thresholds: {
    http_req_failed: ['rate<0.01'], // http errors should be less than 1%
    http_req_duration: ['p(95)<500'], // 95% of requests should be below 500ms
  },
};

const BASE_URL = 'http://localhost:9090';

export default function () {
  const userId = `user-${__VU}-${__ITER}`;
  const anonId = `anon-${__VU}-${__ITER}`;

  // Test 1: Get empty cart
  const getCartRes = http.get(`${BASE_URL}/cart`, {
    headers: {
      'X-User-Id': userId,
      'Content-Type': 'application/json',
    },
  });

  check(getCartRes, {
    'get cart status is 200 or 404': (r) => r.status === 200 || r.status === 404,
  });

  // Test 2: Add item to cart
  const addItemPayload = JSON.stringify({
    productId: 1000 + __ITER,
    quantity: Math.floor(Math.random() * 3) + 1,
    placement: 'PDP',
    channel: 'WEB',
    giftWrap: false,
    acceptBackorder: false,
  });

  const addItemRes = http.post(`${BASE_URL}/cart/items`, addItemPayload, {
    headers: {
      'Idempotency-Key': `idem-${userId}-${__ITER}`,
      'X-User-Id': userId,
      'X-Country': 'US',
      'X-Currency': 'USD',
      'Content-Type': 'application/json',
    },
  });

  check(addItemRes, {
    'add item status is 201': (r) => r.status === 201,
    'add item response has cart': (r) => r.json('cartId') !== undefined,
  });

  sleep(1);
}