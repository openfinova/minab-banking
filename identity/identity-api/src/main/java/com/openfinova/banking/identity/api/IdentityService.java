package com.openfinova.banking.identity.api;

import java.util.Map;

import com.openfinova.banking.identity.api.model.UserSummary;
import com.openfinova.banking.identity.api.model.UserType;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Cross-module identity contract. Other modules depend on {@code identity-api}
 * and inject this service to validate users, resolve identities, and check
 * permissions without coupling to the identity persistence layer.
 */
public interface IdentityService {

    /** Check if a user ID exists and is active (enabled, not locked, not expired). */
    boolean isUserActive(UUID userId);

    /** Check if a username exists and maps to an active user. */
    boolean isUsernameActive(String username);

    /**
     * Validate that a username belongs to a STAFF user with the given permission.
     * Used by loan/GL to verify assignees (e.g., underwriter has loan:write).
     */
    boolean hasUserPermission(String username, String permission);

    /** Resolve a username to a user ID. Returns empty if not found. */
    Optional<UUID> getUserIdByUsername(String username);

    /** Resolve a user ID to a username. Returns empty if not found. */
    Optional<String> getUsernameById(UUID userId);

    /** Get the UserType (STAFF/CUSTOMER/SYSTEM) for a given username. */
    Optional<UserType> getUserType(String username);

    /** Get all permission strings for a user (union of all role permissions). */
    Set<String> getUserPermissions(String username);

    /** Find the identity user ID linked to a customer party record. */
    Optional<UUID> getUserIdByCustomerPartyId(UUID customerPartyId);

    /** Find the customer party ID linked to an identity user. */
    Optional<UUID> getCustomerPartyIdByUserId(UUID userId);

    /** Check whether a customer party has an active identity (login) account. */
    boolean customerHasActiveLogin(UUID customerPartyId);

    /**
     * Resolve multiple usernames to display-ready info in one call.
     * Returns a map of username to {@link UserSummary}.
     * Unknown usernames are omitted from the result.
     */
    Map<String, UserSummary> resolveUsers(Set<String> usernames);

    /** Returns whether the user must change their password before using banking APIs. */
    boolean isForcePasswordChangeRequired(UUID userId);
}
