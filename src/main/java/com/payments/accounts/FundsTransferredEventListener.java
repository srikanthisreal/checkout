package com.payments.accounts;

import com.payments.accounts.dto.FundsTransferredEvent;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
public class FundsTransferredEventListener {

    // 1. TransactionPhase.AFTER_COMMIT guarantees we ONLY notify other systems
    // IF the money was successfully saved in the database.
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    // 2. @Async ensures that sending to Kafka/Redis doesn't block the main
    // user's HTTP thread from returning a "200 OK" immediately.
    @Async
    public void onFundsTransferred(FundsTransferredEvent event) {

        // Example integrations you would mention in the interview:
        // 1. Publish to Kafka: kafkaTemplate.send("liquidity-events", event);
        // 2. Invalidate Redis Cache: redisTemplate.delete("balance::" + event.fromAccountId());
        // 3. Trigger Fraud rules engine.

        System.out.println("Async Event Processed: Transferred " + event.amount() +
                " from " + event.fromAccountId() + " to " + event.toAccountId());
    }
}
