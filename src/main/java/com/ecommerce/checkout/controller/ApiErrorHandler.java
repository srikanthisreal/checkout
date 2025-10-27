package com.ecommerce.checkout.controller;

import com.ecommerce.checkout.exception.IdempotencyConflictException;
import com.ecommerce.checkout.exception.OutOfStockException;
import com.ecommerce.checkout.exception.PriceChangedException;
import com.ecommerce.checkout.exception.VersionMismatchException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiErrorHandler {
    record ErrorBody(String error, String message, Map<String,Object> meta) {}

    @ExceptionHandler(VersionMismatchException.class)
    public ResponseEntity<ErrorBody> handleVersion(VersionMismatchException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody(ex.code(), ex.getMessage(),
                        Map.of("cartId", ex.cartId, "currentVersion", ex.currentVersion)));
    }

    @ExceptionHandler(IdempotencyConflictException.class)
    public ResponseEntity<ErrorBody> handleIdem(IdempotencyConflictException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody(ex.code(), ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(OutOfStockException.class)
    public ResponseEntity<ErrorBody> handleOos(OutOfStockException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(new ErrorBody(ex.code(), ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(PriceChangedException.class)
    public ResponseEntity<ErrorBody> handlePrice(PriceChangedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorBody(ex.code(), ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidation(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new HashMap<>();
        for (var e : ex.getBindingResult().getFieldErrors()) {
            errors.put(e.getField(), e.getDefaultMessage());
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody("VALIDATION_FAILED",
                        "Invalid request", Map.of("fields", errors)));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<?> handleIllegalArg(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(new ErrorBody("BAD_REQUEST", ex.getMessage(), Map.of()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneric(Exception ex) {
        // log full stack internally; keep response tidy
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ErrorBody("INTERNAL_ERROR", "Something went wrong", Map.of()));
    }
}

