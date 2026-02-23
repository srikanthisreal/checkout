package com.payments.accounts;

public class AccountNotFoundException extends RuntimeException {
    public AccountNotFoundException(Long firstLockId) {
    }
    public AccountNotFoundException(String message) {
    }
}
