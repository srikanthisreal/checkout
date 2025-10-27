package com.ecommerce.checkout.service.impl;

import com.ecommerce.checkout.model.IdempotencyEntry;
import com.ecommerce.checkout.repo.IdempotencyRepository;
import com.ecommerce.checkout.service.IdempotencyService;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Optional;

/**
 * Implementation of the idempotency service.
 */
@Service
@AllArgsConstructor
public class IdempotencyServiceImpl implements IdempotencyService {
    private final IdempotencyRepository repo;

    /**
     * Retrieves an idempotency entry from the database given a key.
     * @param key the key to look up the idempotency entry for
     * @return an optional containing the idempotency entry if it exists, or an empty optional otherwise
     */
    @Override
    public Optional<IdempotencyEntry> get(String key) {
        return repo.findById(key);
    }

    /**
     * Stores an idempotency entry in the database.
     * @param key the key to store the idempotency entry for
     * @param ownerKey the owner key for which the idempotency entry is stored
     * @param requestHash the hash of the request for which the idempotency entry is stored
     * @param responseJson the response JSON to store in the idempotency entry
     */
    @Override
    public void store(String key, String ownerKey, String requestHash, String responseJson) {
        var e = new IdempotencyEntry(key, ownerKey, requestHash);
        e.responseCache = responseJson;
        repo.save(e);
    }
}
