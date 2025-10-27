package com.ecommerce.checkout.exception;

import com.ecommerce.checkout.util.CheckoutConstants;

public class OutOfStockException extends AppException {
    public OutOfStockException(String message) {
        super(CheckoutConstants.OUT_OF_STOCK, message); }
}
