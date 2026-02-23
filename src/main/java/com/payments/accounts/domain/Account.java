package com.payments.accounts.dto;

import java.math.BigDecimal;

import com.payments.accounts.exceptions.InsufficientFundsException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;


@Entity
@Table(name = "accounts")
public class Account {

    @Id
    private Long id;

    // Financial systems ALWAYS explicitly define precision and scale in the DB mapping.
    // 19 digits total, 4 after the decimal (standard for handling fractional pennies in liquidity).
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    // 1. JPA requires a no-args constructor, but it shouldn't be public
    // to prevent developers from creating invalid empty accounts.
    protected Account() {}

    public Account(Long id, BigDecimal initialBalance) {
        this.id = id;
        this.balance = initialBalance != null ? initialBalance : BigDecimal.ZERO;
    }

    // 2. The Domain explicitly protects its own state.
    public void debit(BigDecimal amount) {
        validateTransactionAmount(amount);

        if (this.balance.compareTo(amount) < 0) {
            throw new InsufficientFundsException("Account ID: " + this.id + " has insufficient funds for debit.");
        }
        this.balance = this.balance.subtract(amount);
    }

    public void credit(BigDecimal amount) {
        validateTransactionAmount(amount);

        this.balance = this.balance.add(amount);
    }

    // 3. Centralized, reusable validation
    private void validateTransactionAmount(BigDecimal amount) {
        if (amount == null) {
            throw new IllegalArgumentException("Transaction amount cannot be null.");
        }
        // Amount must be strictly greater than zero.
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be strictly positive. Value rejected: " + amount);
        }
    }

    // Getters omitted for brevity...
    // Note: NEVER provide a setBalance() method. The only way to change
    // the balance should be through the debit() and credit() behaviors.
}
