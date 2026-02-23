package com.payments.accounts;

import org.springframework.context.ApplicationEvent;

import java.math.BigDecimal;
import java.time.Instant;

import java.math.BigDecimal;
import java.time.Instant;

// A record is inherently immutable, making it thread-safe for event publishing.
public record FundsTransferredEvent(
        Long fromAccountId,
        Long toAccountId,
        BigDecimal amount,
        Instant timestamp
) {}
