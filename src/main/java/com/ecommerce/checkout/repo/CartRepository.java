package com.ecommerce.checkout.repo;

import com.ecommerce.checkout.model.CartDocument;

import java.util.Optional;

public interface CartRepository {
    Optional<CartDocument> findByOwnerUserId(String ownerUserId);
    Optional<CartDocument> findByOwnerAnonId(String ownerAnonId);
    <S extends CartDocument> S save(S entity);
    void delete(CartDocument entity);
}

