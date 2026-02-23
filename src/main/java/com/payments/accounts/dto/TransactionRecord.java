package com.payments.accounts.dto;

import java.math.BigDecimal;

import jakarta.persistence.*;

import java.time.Instant;

@Entity
@Table(name = "transaction_ledger")
public class TransactionRecord {

    @Id
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private String transactionId;

    @Column(name = "from_account_id", updatable = false, nullable = false)
    private Long fromAccountId;

    @Column(name = "to_account_id", updatable = false, nullable = false)
    private Long toAccountId;

    // Financial precision is mandatory
    @Column(updatable = false, nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    @Column(updatable = false, nullable = false)
    private Instant timestamp;

    @Column(nullable = false)
    private String status; // Ideally, this would be an Enum (e.g., COMPLETED, FAILED, PENDING)

    // 1. Protected no-args constructor required by JPA/Hibernate.
    // Making it protected prevents developers from instantiating empty/invalid records.
    protected TransactionRecord() {}

    // 2. All-args constructor for the Service layer to use.
    public TransactionRecord(String transactionId, Long fromAccountId, Long toAccountId,
                             BigDecimal amount, Instant timestamp, String status) {
        this.transactionId = transactionId;
        this.fromAccountId = fromAccountId;
        this.toAccountId = toAccountId;
        this.amount = amount;
        this.timestamp = timestamp;
        this.status = status;
    }

    // 3. GETTERS ONLY. No setters. This enforces the immutability of the audit trail.
    public String getTransactionId() { return transactionId; }
    public Long getFromAccountId() { return fromAccountId; }
    public Long getToAccountId() { return toAccountId; }
    public BigDecimal getAmount() { return amount; }
    public Instant getTimestamp() { return timestamp; }
    public String getStatus() { return status; }
}