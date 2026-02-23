package com.payments.accounts.controller;

import java.time.Instant;

// A generic, immutable envelope for all API responses
public record ApiResponse<T>(
        String status,           // e.g., "SUCCESS", "ERROR"
        String message,          // Human-readable message
        T data,                  // The actual payload (can be null on error)
        Instant timestamp,
        String correlationId     // Critical for distributed debugging
) {
    public static <T> ApiResponse<T> success(T data, String message, String correlationId) {
        return new ApiResponse<>("SUCCESS", message, data, Instant.now(), correlationId);
    }
}
