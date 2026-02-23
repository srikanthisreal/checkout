package com.payments.accounts.service;

import com.payments.accounts.AccountNotFoundException;
import com.payments.accounts.EnterpriseTransferRequest;
import com.payments.accounts.controller.TransferResult;
import com.payments.accounts.dto.*;
import com.payments.accounts.exceptions.*;
import com.payments.accounts.repository.AccountRepository;
import com.payments.accounts.repository.TransactionLedgerRepository;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Slf4j // Lombok annotation for standard SLF4J logging
@Service
public class LiquidityTransferService {

    private final AccountRepository accountRepo;
    private final TransactionLedgerRepository ledgerRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final MeterRegistry meterRegistry; // Micrometer for Prometheus/Grafana

    public LiquidityTransferService(
            AccountRepository accountRepo,
            TransactionLedgerRepository ledgerRepo,
            ApplicationEventPublisher eventPublisher,
            MeterRegistry meterRegistry) {
        this.accountRepo = accountRepo;
        this.ledgerRepo = ledgerRepo;
        this.eventPublisher = eventPublisher;
        this.meterRegistry = meterRegistry;
    }

    // 1. Resilience4j Circuit Breaker & Retry
    // If the DB throws a connection timeout, it retries up to 3 times.
    // If failure rate exceeds 50%, the circuit opens and triggers the fallback method immediately.
    @Retry(name = "coreBankingDb")
    @CircuitBreaker(name = "coreBankingDb", fallbackMethod = "dbOfflineFallback")
    @Transactional // Commits the DB changes only if everything succeeds
    public TransferResult transferFunds(EnterpriseTransferRequest request) {

        // Timer to track EXACTLY how long DB locks are held in Grafana
        Timer.Sample timer = Timer.start(meterRegistry);

        String fromIban = request.fromIban();
        String toIban = request.toIban();
        BigDecimal amount = request.amount();

        log.info("Initiating liquidity transfer of {} {} from {} to {}",
                amount, request.currencyCode(), fromIban, toIban);

        // 2. Guard Clauses
        if (fromIban.equals(toIban)) {
            log.warn("Transfer rejected: Source and destination IBANs are identical ({})", fromIban);
            throw new InvalidTransferException("Cannot transfer to the same account.");
        }

        // 3. Consistent Lock Ordering (Deadlock Prevention with Strings!)
        // Java Strings implement Comparable. We lock the alphabetically lower IBAN first.
        String firstLockIban = fromIban.compareTo(toIban) < 0 ? fromIban : toIban;
        String secondLockIban = fromIban.compareTo(toIban) > 0 ? fromIban : toIban;

        try {
            // 4. Fetch and Lock (Pessimistic Locking by IBAN)
            Account firstAccount = accountRepo.findByIbanForUpdate(firstLockIban)
                    .orElseThrow(() -> new AccountNotFoundException("Account missing: " + firstLockIban));
            Account secondAccount = accountRepo.findByIbanForUpdate(secondLockIban)
                    .orElseThrow(() -> new AccountNotFoundException("Account missing: " + secondLockIban));

            // Map back to correct roles
            Account fromAccount = fromIban.equals(firstLockIban) ? firstAccount : secondAccount;
            Account toAccount = toIban.equals(firstLockIban) ? firstAccount : secondAccount;

            // 5. Execute Core Business Logic (Rich Domain Model)
            // Note: In a real multi-currency system, you would call FXService here before crediting.
            fromAccount.debit(amount);
            toAccount.credit(amount);

            // 6. The Audit Trail
            String transactionId = UUID.randomUUID().toString();
            TransactionRecord ledgerEntry = new TransactionRecord(
                    transactionId,
                    fromIban,
                    toIban,
                    amount,
                    request.currencyCode(),
                    Instant.now(),
                    "COMPLETED"
            );
            ledgerRepo.save(ledgerEntry);

            // 7. Event-Driven Notification
            eventPublisher.publishEvent(new FundsTransferredEvent(fromIban, toIban, amount, request.currencyCode(), Instant.now()));

            // 8. Business Metrics (Grafana Counters)
            meterRegistry.counter("business.transfers.success", "currency", request.currencyCode()).increment();
            meterRegistry.summary("business.transfers.amount", "currency", request.currencyCode()).record(amount.doubleValue());

            log.info("Successfully completed transfer {}. DB Locks released.", transactionId);

            return new TransferResult(transactionId, "COMPLETED");

        } catch (InsufficientFundsException e) {
            // Log business errors as warnings, not errors, so they don't trigger PagerDuty alerts
            log.warn("Transfer failed due to insufficient funds for IBAN: {}", fromIban);
            meterRegistry.counter("business.transfers.rejected.nsf").increment();
            throw e;

        } finally {
            // Stop the timer exactly when the method finishes
            timer.stop(meterRegistry.timer("system.db.lock.duration"));
        }
    }

    // 9. The Circuit Breaker Fallback Method
    // This executes IF the database is down or throwing timeouts.
    // It prevents the thread pool from exhausting and gives the client a clean error.
    public TransferResult dbOfflineFallback(EnterpriseTransferRequest request, Throwable t) {

        // IMPORTANT: We only fall back for infrastructure errors.
        // We do NOT fall back for business errors like InsufficientFundsException.
        if (t instanceof InvalidTransferException || t instanceof InsufficientFundsException || t instanceof AccountNotFoundException) {
            throw (RuntimeException) t;
        }

        log.error("CORE BANKING DB OFFLINE! Circuit breaker tripped for transfer from {} to {}. Reason: {}",
                request.fromIban(), request.toIban(), t.getMessage());

        meterRegistry.counter("system.db.circuit_breaker.tripped").increment();

        // Throw a specific exception that the GlobalExceptionHandler will turn into a 503 Service Unavailable
        throw new CoreBankingSystemOfflineException("The liquidity ledger is currently unreachable. Please try again later.");
    }
}
/** What’s strong (keep it)
 * Clear orchestration: validate → lock → debit/credit → ledger → event → metrics.
 * Deadlock prevention via consistent lock ordering is the right instinct.
 * Pessimistic locking for transfers is realistic in many systems.
 * Domain behavior (debit/credit) in Account is correct DDD style.
 * Business vs infra error separation is trying to be correct (needs refinement, see below).
 * Metrics + structured logging: good operational mindset.
 * 🚨 High severity issues (must fix)
 * 1) @Retry + @Transactional together can cause duplicate side-effects
 *
 * This is the biggest correctness risk.
 *
 * If a transient DB error occurs after:
 *
 * debit/credit done
 *
 * ledger saved
 *
 * event published
 *
 * metrics recorded
 *
 * …then retry may run again, potentially:
 *
 * double-debiting / double-crediting
 *
 * creating duplicate ledger entries
 *
 * publishing duplicate events
 *
 * Even if the transaction rolls back, your event publish is not transactional and your metrics increments are not transactional.
 *
 * Fix:
 *
 * Either remove @Retry from this method, OR
 *
 * Make the whole operation idempotent (use request idempotencyKey), AND
 *
 * Use outbox pattern for events so publishing happens after commit.
 *
 * Best practice in banking: DB retries should be done on repository calls or on pure reads, not around a method that does side effects unless idempotent.
 *
 * 2) idempotencyKey is in request but not used at all
 *
 * Your DTO includes it, but the service ignores it. That makes “exactly-once effect” impossible.
 *
 * Fix options:
 *
 * Add a table transfer_requests(idempotency_key UNIQUE, transaction_id, status, hash, created_at)
 *
 * At start:
 *
 * if key exists → return stored result (same txId/status)
 *
 * else → create “IN_PROGRESS”
 *
 * After success → mark “COMPLETED” with transactionId.
 *
 * This also protects you from retries and client replays.
 *
 * 3) Circuit breaker and retry around a database is usually the wrong tool
 *
 * Circuit breaker is best for remote calls (HTTP/gRPC), not your own DB.
 *
 * With DB:
 *
 * a circuit breaker can amplify outage patterns
 *
 * retry storm can worsen overload
 *
 * If you keep it, configure very carefully and only for connection-level transient failures.
 *
 * Fix:
 *
 * Prefer DB connection pool tuning + proper timeouts
 *
 * Retry on specific SQL transient exceptions only (and only if idempotent)
 *
 * Consider bulkhead isolation at controller layer, not here.
 *
 * 4) You publish event inside the transaction (not safe)
 *
 * eventPublisher.publishEvent(...) will run immediately. If transaction later rolls back, downstream systems still believe transfer happened.
 *
 * Fix:
 *
 * Use @TransactionalEventListener(phase = AFTER_COMMIT) somewhere else, OR
 *
 * Use Outbox: write event to DB in same transaction, publish asynchronously after commit.
 *
 * This is very important for banking/audit correctness.
 *
 * 5) Money metrics recorded as doubleValue() is dangerous
 *
 * BigDecimal.doubleValue() loses precision (especially for large amounts or 4-decimal scales).
 *
 * Fix:
 *
 * Record in minor units (long cents) or use DistributionSummary with integer-ish scaling.
 * Example: record(amount.movePointRight(2).longValueExact())
 * Or expose as BigDecimal via custom meter (Micrometer is mostly double-based, so prefer minor-unit long).
 *
 * ⚠️ Medium severity issues (should fix)
 * 6) Lock ordering by IBAN string can be fragile
 *
 * Lexicographic ordering is stable, but:
 *
 * IBAN case sensitivity? (you assume uppercase)
 *
 * whitespace? (should be normalized)
 *
 * if input can be lower-case or have spaces, ordering becomes inconsistent.
 *
 * Fix:
 *
 * Normalize IBANs (trim, uppercase, remove spaces) at the boundary (DTO or controller).
 *
 * Better: store canonical IBAN format in DB.
 *
 * Also: if fromIban.compareTo(toIban) == 0 you already reject, so ok.
 *
 * 7) AccountNotFoundException package inconsistency
 *
 * You import:
 *
 * com.payments.accounts.AccountNotFoundException
 * and also com.payments.accounts.exceptions.*
 *
 * This smells like duplicated exception types or messy packaging.
 *
 * Fix:
 *
 * Standardize exceptions under com.payments.accounts.exceptions
 *
 * Keep API error mapping consistent.
 *
 * 8) Missing validation use / relying on controller
 *
 * You assume DTO validation already ran. Good in Spring MVC, but add defensive checks or ensure @Validated at controller/service boundaries.
 *
 * At least:
 *
 * amount.scale() normalization
 *
 * currencyCode uppercase normalization
 *
 * 9) Ledger entry is too thin for audit-grade trails
 *
 * For banking, ledger typically stores:
 *
 * idempotencyKey
 *
 * valueDate
 *
 * narration
 *
 * status transitions (INITIATED → COMPLETED)
 *
 * reversal/correlation ids
 *
 * who initiated (customerId / channel)
 *
 * Fix:
 *
 * include idempotencyKey, valueDate, narration in ledger record
 *
 * consider status lifecycle
 *
 * 10) Balance mutation only (no double-entry accounting)
 *
 * You debit one balance and credit another. For “banking-like” correctness, you usually want double-entry ledger postings:
 *
 * debit line
 *
 * credit line
 *
 * same transaction id
 *
 * If this is “simplified payments”, fine — but call it out and don’t call it “ledger” unless it behaves like one.
 *
 * ✅ Low severity improvements (nice to have)
 * 11) Timer name and meaning
 *
 * system.db.lock.duration currently measures entire method duration (including validation/logging/event/metrics), not strictly lock duration.
 *
 * Fix:
 * Start timer right before first lock acquisition, stop right after DB work is done, or use separate timers:
 *
 * db.lock.acquire.duration
 *
 * db.tx.duration
 *
 * transfer.total.duration
 *
 * 12) Use structured logging fields
 *
 * Instead of interpolating values into text, prefer key-value (if you have logback encoder):
 *
 * transactionId
 *
 * fromIban
 *
 * toIban
 *
 * currency
 *
 * amount
 *
 * 13) Use final locals where possible
 *
 * Not critical, but improves readability.
 *
 * 🔧 Concrete recommended refactor shape (high-level, no redesign lecture)
 * A) Make transfer idempotent first
 *
 * Check idempotency key at start
 *
 * Return existing result if already processed
 *
 * B) Remove retry/circuit breaker around side effects OR move retry inside repository calls
 *
 * If you keep retry, ensure retry is safe via idempotency + outbox.
 *
 * C) Publish event AFTER_COMMIT
 *
 * outbox or transactional event listener
 *
 * D) Lock accounts + update + write ledger in one TX
 *
 * keep DB work atomic
 *
 * Example review comment text you can paste into PR
 *
 * Blocker: @Retry + side effects (debit/credit, ledgerRepo.save, publishEvent) can cause duplicated transfers on retry. Must add idempotency key enforcement + transactional outbox / AFTER_COMMIT events or remove retry from this method.
 *
 * Blocker: idempotencyKey is present in request but not used; required for banking-grade correctness and replay safety.
 *
 * Blocker: Event published inside transaction can leak “transfer completed” events on rollback. Use outbox or @TransactionalEventListener(AFTER_COMMIT).
 *
 * Major: amount.doubleValue() loses monetary precision. Record metrics in minor units (long) or scaled values.
 *
 * Major: Normalize IBAN (trim/uppercase/remove spaces) to keep lock ordering consistent and queries stable.
 *
 * Minor: Timer name suggests lock duration but currently measures total execution time; split timers.
 *
 * Minor: Clean up exception package imports and use a single exception namespace.
 * */