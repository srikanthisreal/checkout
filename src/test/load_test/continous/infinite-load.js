import http from 'k6/http';
import { check, sleep } from 'k6';

// No stages - runs until manually stopped
export const options = {
  vus: 5,           // 5 concurrent users
  duration: '1h',    // Safety duration (will stop after 1 hour)
};

const BASE_URL = 'http://localhost:9090';

export default function () {
  let iteration = 0;

  // Infinite loop within each virtual user
  while (true) {
    const userId = `infinite-user-${__VU}-${iteration}`;

    // Add item to cart
    const addItemRes = http.post(`${BASE_URL}/cart/items`, JSON.stringify({
      productId: 1000 + Math.floor(Math.random() * 500),
      quantity: Math.floor(Math.random() * 2) + 1,
      placement: 'PDP',
      channel: 'WEB',
      giftWrap: false,
      acceptBackorder: true,
    }), {
      headers: {
        'Idempotency-Key': `infinite-${userId}-${iteration}`,
        'X-User-Id': userId,
        'X-Country': 'US',
        'X-Currency': 'USD',
        'Content-Type': 'application/json',
      },
    });

    check(addItemRes, {
      'request successful': (r) => r.status === 201 || r.status === 409, // 409 is idempotency conflict (ok)
    });

    // Get cart occasionally
    if (iteration % 3 === 0) {
      http.get(`${BASE_URL}/cart`, {
        headers: {
          'X-User-Id': userId,
          'Content-Type': 'application/json',
        },
      });
    }

    iteration++;
    sleep(3); // Wait 3 seconds between iterations
  }
}