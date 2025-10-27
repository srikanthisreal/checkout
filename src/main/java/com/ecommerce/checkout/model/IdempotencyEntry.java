package com.ecommerce.checkout.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;

/**
 * Stores processed idempotency keys to deduplicate client retries.
 * Keys can be stored for 24h or more, depending on system policy.
 */
@Document("idempotency")
public class IdempotencyEntry {
    @Id public String key;                          // the Idempotency-Key header
    @Indexed(expireAfterSeconds = 86400)
    public Instant createdAt;

    public String ownerKey;                         // "user:..." or "anon:..."
    public String requestHash;                      // SHA-256 of normalized body
    public String responseCache;                    // JSON of the response (optional)
    public Long createdAtEpoch;

    public IdempotencyEntry() {}
    public IdempotencyEntry(String key, String ownerKey, String requestHash) {
        this.key = key;
        this.ownerKey = ownerKey;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
        this.createdAtEpoch = this.createdAt.getEpochSecond();
    }
}

