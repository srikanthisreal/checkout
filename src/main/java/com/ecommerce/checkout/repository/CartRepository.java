package com.ecommerce.checkout.repository;

import com.ecommerce.checkout.model.CartDocument;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface CartRepository extends MongoRepository<CartDocument, String> {

    Optional<CartDocument> findByOwnerUserId(String ownerUserId);
    Optional<CartDocument> findByOwnerAnonId(String ownerAnonId);

}
