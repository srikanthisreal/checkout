package com.ecommerce.checkout.util;

import jakarta.servlet.http.HttpServletRequest;

public record RequestContext(
        String idempotencyKey,
        String userId,
        String anonId,
        String country,
        String currency,
        String locale
) {
    public static RequestContext from(HttpServletRequest r) {
        return new RequestContext(
                r.getHeader("Idempotency-Key"),
                r.getHeader("X-User-Id"),
                r.getHeader("X-Anon-Id"),
                r.getHeader("X-Country"),
                r.getHeader("X-Currency"),
                r.getHeader("X-Locale")
        );
    }
}

