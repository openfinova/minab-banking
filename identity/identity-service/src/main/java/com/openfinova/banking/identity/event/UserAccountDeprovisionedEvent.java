package com.openfinova.banking.identity.event;

import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.context.ApplicationEvent;

/**
 * Published after a user account is fully deprovisioned (access revoked, credentials cleared).
 * Downstream modules may listen to revoke local caches, entitlements, or notify other systems.
 */
public class UserAccountDeprovisionedEvent extends ApplicationEvent {

    private final UUID userId;
    private final String username;
    private final LocalDateTime occurredAt;
    private final String reason;
    private final UUID actorUserId;
    private final String actorUsername;

    public UserAccountDeprovisionedEvent(Object source, UUID userId, String username, LocalDateTime occurredAt,
            String reason, UUID actorUserId, String actorUsername) {
        super(source);
        this.userId = userId;
        this.username = username;
        this.occurredAt = occurredAt;
        this.reason = reason;
        this.actorUserId = actorUserId;
        this.actorUsername = actorUsername;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public LocalDateTime getOccurredAt() {
        return occurredAt;
    }

    public String getReason() {
        return reason;
    }

    public UUID getActorUserId() {
        return actorUserId;
    }

    public String getActorUsername() {
        return actorUsername;
    }
}
