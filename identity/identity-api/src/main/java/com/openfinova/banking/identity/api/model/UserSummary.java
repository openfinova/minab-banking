package com.openfinova.banking.identity.api.model;

import java.util.UUID;

/**
 * Lightweight identity info returned by {@link IdentityService#resolveUsers}.
 * Contains no sensitive fields -- safe to expose to any module.
 */
public record UserSummary(UUID userId, String username, UserType userType, boolean active) {
}
