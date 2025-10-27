package com.ecommerce.checkout.repo;

import com.ecommerce.checkout.model.IdempotencyEntry;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "mongo", matchIfMissing = true)
public interface IdempotencyRepositoryMongo extends IdempotencyRepository, MongoRepository<IdempotencyEntry, String> {
}
