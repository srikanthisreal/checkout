package com.payments.accounts;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Service
public class LiquidityTransferService {

    private final AccountRepository accountRepo;
    private final TransactionLedgerRepository ledgerRepo;
    private final ApplicationEventPublisher eventPublisher;

    // Constructor Injection (Lombok @RequiredArgsConstructor is also fine)
    public LiquidityTransferService(
            AccountRepository accountRepo,
            TransactionLedgerRepository ledgerRepo,
            ApplicationEventPublisher eventPublisher) {
        this.accountRepo = accountRepo;
        this.ledgerRepo = ledgerRepo;
        this.eventPublisher = eventPublisher;
    }

    @Transactional // Critical: Balances, Ledger, and Events commit together
    public void transferFunds(Long fromId, Long toId, BigDecimal amount) {

        // 1. Guard Clauses (Security & Validation)
        if (fromId.equals(toId)) {
            throw new InvalidTransferException("Cannot transfer to the same account.");
        }
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new InvalidTransferException("Transfer amount must be strictly positive.");
        }

        // 2. Consistent Lock Ordering (Deadlock Prevention)
        Long firstLockId = fromId < toId ? fromId : toId;
        Long secondLockId = fromId > toId ? fromId : toId;

        // 3. Fetch and Lock (Pessimistic Locking)
        Account firstAccount = accountRepo.findByIdForUpdate(firstLockId)
                .orElseThrow(() -> new AccountNotFoundException("Account missing: " + firstLockId));
        Account secondAccount = accountRepo.findByIdForUpdate(secondLockId)
                .orElseThrow(() -> new AccountNotFoundException("Account missing: " + secondLockId));

        // 4. Map back to correct roles
        Account fromAccount = fromId.equals(firstLockId) ? firstAccount : secondAccount;
        Account toAccount = toId.equals(firstLockId) ? firstAccount : secondAccount;

        // 5. Execute Core Business Logic (Rich Domain Model)
        fromAccount.debit(amount);
        toAccount.credit(amount);
        // (JPA Dirty Checking will automatically update the account balances)

        // 6. The Audit Trail (Crucial for Banking!)
        // Create an immutable record of the transaction.
        TransactionRecord ledgerEntry = new TransactionRecord(
                UUID.randomUUID().toString(), // Unique idempotency key
                fromId,
                toId,
                amount,
                Instant.now(),
                "COMPLETED"
        );
        ledgerRepo.save(ledgerEntry);

        // 7. Event-Driven Notification
        // Notify other systems (like the CDN cache invalidator, Fraud Detection, or Kafka producer)
        // that a transfer occurred, without tightly coupling them to this service.
        eventPublisher.publishEvent(new FundsTransferredEvent(fromId, toId, amount, Instant.now()));
    }
}
