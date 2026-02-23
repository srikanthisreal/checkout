package com.payments.accounts.controller;

import com.ecommerce.checkout.service.IdempotencyService;
import com.payments.accounts.EnterpriseTransferRequest;
import com.payments.accounts.service.LiquidityTransferService;
import jakarta.validation.Valid;
import org.slf4j.MDC;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/liquidity")
public class LiquidityController {

    private final LiquidityTransferService transferService;
    private final IdempotencyService idempotencyService;

    public LiquidityController(
            LiquidityTransferService transferService,
            IdempotencyService idempotencyService) {
        this.transferService = transferService;
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/transfers")
    public ResponseEntity<ApiResponse<TransferResult>> initiateTransfer(
            @RequestHeader(value = "Idempotency-Key") String idempotencyKey,
            @RequestHeader(value = "X-Correlation-ID", required = false) String correlationId,
            @Valid @RequestBody EnterpriseTransferRequest request) {

        // 1. Distributed Tracing (MDC)
        // This ensures every log line written by this thread includes the Correlation ID,
        // allowing us to trace this exact request across Kafka, DB, and other microservices.
        String traceId = correlationId != null ? correlationId : UUID.randomUUID().toString();
        MDC.put("correlationId", traceId);

        try {
            // 2. The Real Idempotency Check (Backed by Redis)
            // If the client retried a request that already succeeded, return the cached result
            // with a 200 OK (not a 201 Created, because we didn't create a new transaction).
            Optional<TransferResult> cachedResult = idempotencyService.get(idempotencyKey);
            if (cachedResult.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success(
                        cachedResult.get(),
                        "Recovered from Idempotency Cache",
                        traceId
                ));
            }

            // 3. Execute the Business Logic (Orchestration)
            // Note: Service is updated to return the Transaction ID so we can build a URI.
            TransferResult result = transferService.transferFunds(request);

            // 4. Save the successful result to Redis for future identical requests (e.g., valid for 24h)
            idempotencyService.save(idempotencyKey, result);

            // 5. Strict HTTP Semantics (Location Header)
            // REST standards mandate that a POST creating a resource returns the URI of that new resource.
            URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                    .path("/{id}")
                    .buildAndExpand(result.transactionId())
                    .toUri();

            // Return 201 CREATED (not 200 OK)
            return ResponseEntity.created(location)
                    .body(ApiResponse.success(result, "Transfer processed successfully", traceId));

        } finally {
            // ALWAYS clean up the thread-local MDC to prevent memory leaks in the thread pool!
            MDC.remove("correlationId");
        }
    }
}
