import http from 'k6/http';
import { check, sleep } from 'k6';

// Simple smoke test
export const options = {
  vus: 3,
  duration: '1m',
};

const BASE_URL = 'http://localhost:9090';

export default function () {
  const userId = `smoke-user-${__VU}-${__ITER}`;

  // 1. Add item to cart
  const addItemRes = http.post(`${BASE_URL}/cart/items`, JSON.stringify({
    productId: 1000 + __ITER,
    quantity: 1,
    placement: 'PDP',
    channel: 'WEB',
    giftWrap: false,
    acceptBackorder: false
  }), {
    headers: {
      'Idempotency-Key': `smoke-${userId}-${__ITER}`,
      'X-User-Id': userId,
      'X-Country': 'US',
      'X-Currency': 'USD',
      'Content-Type': 'application/json',
    },
  });

  check(addItemRes, {
    'add to cart successful': (r) => r.status === 201,
  });

  // 2. Get cart
  const getCartRes = http.get(`${BASE_URL}/cart`, {
    headers: {
      'X-User-Id': userId,
      'Content-Type': 'application/json',
    },
  });

  check(getCartRes, {
    'get cart successful': (r) => r.status === 200,
  });

  sleep(1);
}