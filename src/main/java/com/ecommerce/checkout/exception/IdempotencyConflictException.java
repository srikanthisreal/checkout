package com.ecommerce.checkout.exception;

import com.ecommerce.checkout.util.CheckoutConstants;

public class IdempotencyConflictException extends AppException {
    public IdempotencyConflictException(String message) {
        super(CheckoutConstants.IDEMPOTENCY_CONFLICT, message); }
}
