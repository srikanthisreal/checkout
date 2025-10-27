package com.ecommerce.checkout.repository;

import com.ecommerce.checkout.model.IdempotencyEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface IdempotencyRepository extends MongoRepository<IdempotencyEntry, String> {{
    public Optional<Object> findById(String idempotencyKey) {

    }
}
