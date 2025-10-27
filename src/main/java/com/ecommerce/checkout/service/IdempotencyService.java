package com.ecommerce.checkout.service;

import com.ecommerce.checkout.model.IdempotencyEntry;

import java.util.Optional;

/**
 * Interface for idempotency service.
 */
public interface IdempotencyService {
    /**
     * Returns an idempotency entry for the given key, or an empty optional if no such entry exists.
     * @param key the key to look up the idempotency entry for
     * @return an optional containing the idempotency entry if it exists, or an empty optional otherwise
     */
    Optional<IdempotencyEntry> get(String key);

    /**
     * Stores an idempotency entry in the database.
     * @param key the key to store the idempotency entry for
     * @param ownerKey the owner key for which the idempotency entry is stored
     * @param requestHash the hash of the request for which the idempotency entry is stored
     * @param responseJson the response JSON to store in the idempotency entry
     */
    void store(String key, String ownerKey, String requestHash, String responseJson);
}
