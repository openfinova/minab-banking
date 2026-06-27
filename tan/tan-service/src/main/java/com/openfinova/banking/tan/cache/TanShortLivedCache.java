package com.openfinova.banking.tan.cache;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;

/**
 * Process-local, time-bounded key-value store for TAN module ephemeral state.
 *
 * Holds short-lived markers that must not be persisted to the database: enrollment-token
 * single-use ({@code jti}), attestation nonces, and per-transaction TAN replay prevention.
 * Callers own key naming (typically a prefix plus id, e.g. {@code enroll-jti:}, {@code attest-nonce:},
 * {@code tan-verified:}).
 *
 * Design
 *   Entries expire after caller-supplied TTL; {@link #purgeExpired()} runs lazily on read/write
 *   so no background scheduler is required. {@link ConcurrentHashMap} provides thread-safe access
 *   within a single JVM instance.
 *
 * Deployment
 *   Correct for single-node or development. In a horizontally scaled deployment, each node holds
 *   its own map — replay checks and nonce validation would not be shared across instances. Replace
 *   with a distributed cache (e.g. Redis) before running multiple banking-app replicas in production.
 *
 * @see com.openfinova.banking.tan.service.TanEnrollmentTokenService
 * @see com.openfinova.banking.tan.service.TanAttestationService
 * @see com.openfinova.banking.tan.crypto.TanCodeService
 */
@Component
public class TanShortLivedCache {

    /** Internal storage entry with explicit expiry instant. */
    private record Entry(String value, Instant expiresAt) {
        boolean expired(Instant now) {
            return expiresAt.isBefore(now);
        }
    }

    private final Map<String, Entry> store = new ConcurrentHashMap<>();

    /**
     * Returns whether a non-expired entry exists for {@code key}.
     *
     * @param key caller-scoped cache key
     */
    public boolean exists(String key) {
        purgeExpired();
        Entry entry = store.get(key);
        return entry != null && !entry.expired(Instant.now());
    }

    /**
     * Stores {@code value} under {@code key} until {@code ttl} elapses from now.
     * Overwrites any existing entry for the key.
     */
    public void put(String key, String value, Duration ttl) {
        store.put(key, new Entry(value, Instant.now().plus(ttl)));
    }

    /**
     * Atomically records a presence marker when the key is absent or expired.
     *
     * Used for single-use semantics (e.g. enrollment {@code jti} consumption): returns {@code false}
     * when an unexpired entry already exists, {@code true} when this call installed the marker.
     *
     * @param key caller-scoped cache key
     * @param ttl how long the marker blocks reuse
     * @return {@code false} if the key is already present and not expired; {@code true} otherwise
     */
    public boolean putIfAbsent(String key, Duration ttl) {
        purgeExpired();
        Instant expiresAt = Instant.now().plus(ttl);
        Entry newEntry = new Entry("1", expiresAt);
        Entry previous = store.putIfAbsent(key, newEntry);
        if (previous != null && !previous.expired(Instant.now())) {
            return false;
        }
        if (previous != null && previous.expired(Instant.now())) {
            store.put(key, newEntry);
        }
        return true;
    }

    /** Removes expired entries; invoked on access paths to bound memory use. */
    private void purgeExpired() {
        Instant now = Instant.now();
        store.entrySet().removeIf(e -> e.getValue().expired(now));
    }
}
