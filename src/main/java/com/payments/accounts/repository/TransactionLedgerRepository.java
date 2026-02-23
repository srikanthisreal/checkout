package com.payments.accounts;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface TransactionLedgerRepository extends JpaRepository<TransactionRecord, String> {

    // Senior Tip: In a real DB, you would add an Index on these two columns.
    // This allows the business to quickly fetch the ledger history for a specific corporate account.
    List<TransactionRecord> findByFromAccountIdOrToAccountIdOrderByTimestampDesc(
            Long fromAccountId, Long toAccountId);
}
