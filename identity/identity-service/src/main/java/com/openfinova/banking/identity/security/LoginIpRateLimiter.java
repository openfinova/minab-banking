package com.openfinova.banking.identity.security;

import java.util.concurrent.ConcurrentHashMap;

/**
 * Fixed-window rate limiter keyed by client identifier (typically IP).
 */
public final class LoginIpRateLimiter {

    private static final long WINDOW_MS = 60_000L;

    private final int maxAttemptsPerWindow;
    private final ConcurrentHashMap<String, long[]> buckets = new ConcurrentHashMap<>();

    /**
     * @param maxAttemptsPerWindow at least 1 successful consumption per window after reset
     */
    public LoginIpRateLimiter(int maxAttemptsPerWindow) {
        this.maxAttemptsPerWindow = Math.max(1, maxAttemptsPerWindow);
    }

    /**
     * @return {@code true} if the request is within the limit; {@code false} if the limit was exceeded
     */
    public boolean tryConsume(String clientKey) {
        if (clientKey == null || clientKey.isBlank()) {
            return true;
        }
        long now = System.currentTimeMillis();
        boolean[] allowed = { true };
        buckets.compute(clientKey, (key, existing) -> {
            if (existing == null || now - existing[0] >= WINDOW_MS) {
                return new long[] { now, 1 };
            }
            if (existing[1] >= maxAttemptsPerWindow) {
                allowed[0] = false;
                return existing;
            }
            existing[1]++;
            return existing;
        });
        return allowed[0];
    }
}
