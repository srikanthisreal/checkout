package com.ecommerce.checkout.repo.immem;

import com.ecommerce.checkout.model.IdempotencyEntry;
import com.ecommerce.checkout.repo.IdempotencyRepository;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Repository;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "inmem")
public class IdempotencyRepositoryInMomery implements IdempotencyRepository {
    private final Map<String, IdempotencyEntry> entries = new HashMap<>();

    @Override
    public Optional<IdempotencyEntry> findById(String idempotencyKey) {
        return Optional.empty();
    }

    @Override
    public void save(IdempotencyEntry idempotencyEntry) {

    }
}
