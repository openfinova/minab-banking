package com.openfinova.banking.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Published when an account is within the configured lead time of {@code accountExpiresAt}
 * and a warning has not yet been recorded for the current expiry instant.
 */
public class UserAccountExpiryWarningEvent extends ApplicationEvent {

    private final UUID userId;
    private final String username;
    private final LocalDateTime accountExpiresAt;
    private final LocalDateTime detectedAt;

    public UserAccountExpiryWarningEvent(Object source, UUID userId, String username, LocalDateTime accountExpiresAt,
            LocalDateTime detectedAt) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.accountExpiresAt = accountExpiresAt;
        this.detectedAt = detectedAt;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getAccountExpiresAt() {
        return accountExpiresAt;
    }

    public LocalDateTime getDetectedAt() {
        return detectedAt;
    }
}
