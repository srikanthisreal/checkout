package com.payments.accounts.repository;

import com.payments.accounts.dto.Account;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;      // <--- Added this
import org.springframework.data.jpa.repository.Query;     // <--- Fixed (JPA, not MongoDB)
import org.springframework.data.repository.query.Param;
import java.util.Optional;

public interface AccountRepository extends JpaRepository<Account, Long> {
    // Uses SELECT ... FOR UPDATE to lock the row
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT a FROM Account a WHERE a.id = :id")
    Optional<Account> findByIbanForUpdate(@Param("id") String id);
}
