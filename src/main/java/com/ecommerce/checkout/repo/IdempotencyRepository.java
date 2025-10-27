package com.ecommerce.checkout.repo;

import com.ecommerce.checkout.model.IdempotencyEntry;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface IdempotencyRepository extends MongoRepository<IdempotencyEntry, String> {
}
