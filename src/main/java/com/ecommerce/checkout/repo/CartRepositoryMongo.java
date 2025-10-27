package com.ecommerce.checkout.repo;

import com.ecommerce.checkout.model.CartDocument;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
@ConditionalOnProperty(name = "app.storage", havingValue = "mongo", matchIfMissing = true)
public interface CartRepositoryMongo
        extends CartRepository, MongoRepository<CartDocument, String> {}
