import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '2m', target: 200 },  // Rapid ramp up
    { duration: '3m', target: 200 },  // Stay at 200 users
    { duration: '1m', target: 500 },  // Spike to 500 users
    { duration: '2m', target: 500 },  // Stay at spike
    { duration: '2m', target: 0 },    // Ramp down
  ],
  thresholds: {
    http_req_failed: ['rate<0.05'],    // Allow 5% failures under stress
    http_req_duration: ['p(95)<2000'], // 95% < 2s under stress
  },
};

const BASE_URL = 'http://localhost:9090';

export default function () {
  const userId = `stress-user-${__VU}-${Date.now()}`;

  // Only do minimal operations under stress
  const addItemPayload = JSON.stringify({
    productId: 1000 + Math.floor(Math.random() * 100),
    quantity: 1,
    placement: 'PDP',
    channel: 'WEB',
    giftWrap: false,
    acceptBackorder: true, // Allow backorder to reduce failures
  });

  const addItemRes = http.post(`${BASE_URL}/cart/items`, addItemPayload, {
    headers: {
      'Idempotency-Key': `stress-${userId}-${__ITER}`,
      'X-User-Id': userId,
      'X-Country': 'US',
      'X-Currency': 'USD',
      'Content-Type': 'application/json',
    },
    timeout: '30s', // Longer timeout for stress conditions
  });

  check(addItemRes, {
    'stress add item status acceptable': (r) => r.status === 201 || r.status === 409, // Allow idempotency conflicts
  });

  // Minimal sleep to maximize load
  sleep(Math.random() * 0.5);
}