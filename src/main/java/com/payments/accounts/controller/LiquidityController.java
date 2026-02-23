package com.payments.accounts.controller;

import com.payments.accounts.EnterpriseTransferRequest;
import com.payments.accounts.Idempotent;
import com.payments.accounts.service.LiquidityTransferService;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/api/v1/liquidity")
public class LiquidityController {

    private final LiquidityTransferService transferService;
    private final MeterRegistry meterRegistry; // For custom business metrics

    public LiquidityController(LiquidityTransferService transferService, MeterRegistry meterRegistry) {
        this.transferService = transferService;
        this.meterRegistry = meterRegistry;
    }

    @Idempotent(ttlSeconds = 86400) // 24-hour cache
    @RateLimiter(name = "liquidityApi", fallbackMethod = "rateLimitFallback") // Prevents DDoS
    @Timed(value = "api.liquidity.transfer.time", description = "Time taken to process transfer") // APM tracking
    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<TransferResult>> initiateTransfer(
            @Valid @RequestBody EnterpriseTransferRequest request) {

        // The Controller now strictly focuses on orchestrating the happy path.
        TransferResult result = transferService.transferFunds(request);

        // Custom Business Metric (Creates a Grafana dashboard spike for successful transfers)
        meterRegistry.counter("business.transfers.success", "currency", request.currencyCode()).increment();

        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(result.transactionId())
                .toUri();

        // Let the Filter handle traceId extraction!
        return ResponseEntity.created(location)
                .body(ApiResponse.success(result, "Transfer processed successfully", null));
    }

    // Resilience4j Fallback method if the client exceeds their allowed requests per second
    public ResponseEntity<ApiResponse<Void>> rateLimitFallback(EnterpriseTransferRequest request, Throwable t) {
        return ResponseEntity.status(429).body(
                new ApiResponse<>("ERROR", "Too many transfer requests. Please slow down.", null, null, null)
        );
    }
}