package com.openfinova.banking.identity.api.audit;

import java.util.UUID;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;

/**
 * Identifies the authenticated party who performed an auditable action (regulatory "who").
 *
 * @param userId   subject user id when present in the token (may be null for some auth modes)
 * @param username non-blank username or preferred name from the security principal
 */
public record AuditActor(UUID userId, String username) {

    public AuditActor {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException("username is required for audit actor");
        }
    }

    /**
     * Builds an actor from the current JWT-backed principal (admin API or self-service).
     */
    public static AuditActor fromPrincipal(BankingPrincipal principal) {
        return new AuditActor(principal.userId(), principal.username());
    }
}
