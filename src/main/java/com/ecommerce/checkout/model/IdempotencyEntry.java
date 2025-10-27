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

    @Id
    public String key;  // e.g. UUID passed in header: Idempotency-Key

    @Indexed(expireAfterSeconds = 86400)// 24h TTL auto-expire
    public Instant createdAt;            // time of first occurrence

    public String ownerKey;              // user:xxx or anon:xxx (for context)
    public String requestHash;           // optional: hash of payload for stricter match
    public String responseCache;         // optional: cached serialized response

    public Long createdAtEpoch;          // optional (epoch seconds)

    public IdempotencyEntry() {}

    public IdempotencyEntry(String key, String ownerKey, String requestHash) {
        this.key = key;
        this.ownerKey = ownerKey;
        this.requestHash = requestHash;
        this.createdAt = Instant.now();
        this.createdAtEpoch = this.createdAt.getEpochSecond();
    }
}

