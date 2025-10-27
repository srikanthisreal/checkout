package com.ecommerce.checkout.exception;

public abstract class AppException extends RuntimeException {
    private final String code;
    protected AppException(String code, String message) {
        super(message); this.code = code;
    }
    public String code() { return code; }
}
