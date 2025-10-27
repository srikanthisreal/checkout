package com.ecommerce.checkout.repo;

import com.ecommerce.checkout.model.IdempotencyEntry;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IdempotencyRepository {
    Optional<IdempotencyEntry> findById(String idempotencyKey);
    <S extends IdempotencyEntry> void save(IdempotencyEntry idempotencyEntry);
}
