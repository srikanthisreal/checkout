package com.ecommerce.checkout.exception;

import com.ecommerce.checkout.util.CheckoutConstants;

public class PriceChangedException extends AppException {
    public PriceChangedException(String message) {
        super(CheckoutConstants.PRICE_CHANGED, message); }
}
