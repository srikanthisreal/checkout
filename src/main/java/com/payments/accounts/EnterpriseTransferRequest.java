package com.payments.accounts;

import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;

public record EnterpriseTransferRequest(

        // In reality, this is an IBAN, not a Long.
        @NotBlank(message = "Source IBAN is required")
        @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$", message = "Invalid IBAN format")
        String fromIban,

        @NotBlank(message = "Destination IBAN is required")
        @Pattern(regexp = "^[A-Z]{2}[0-9]{2}[A-Z0-9]{1,30}$", message = "Invalid IBAN format")
        String toIban,

        @NotNull(message = "Transfer amount is required")
        @Positive(message = "Transfer amount must be strictly positive")
        // @Digits ensures no one passes 100.123456 for a currency that only supports 2 decimals.
        @Digits(integer = 15, fraction = 2, message = "Invalid monetary format")
        BigDecimal amount,

        // ISO 4217 Currency Code Standard
        @NotBlank(message = "Currency code is required")
        @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code (e.g., EUR, USD)")
        String currencyCode,

        // Corporate liquidity deals with forward-dated transactions
        @NotNull(message = "Value date is required")
        @FutureOrPresent(message = "Value date cannot be in the past")
        LocalDate valueDate,

        // Mandatory for Compliance/Fraud checking
        @Size(max = 140, message = "Narration exceeds maximum length")
        String narration,

        // Often used to bypass cache or force real-time FX fetching
        boolean forceRealTimeFx
) {}
