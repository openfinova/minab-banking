package com.openfinova.banking.identity.api.principal;

import java.util.UUID;

import com.openfinova.banking.identity.api.model.UserType;

/**
 * Immutable snapshot of the authenticated caller's identity and HTTP request metadata.
 *
 * Built once per request by {@link CallerContextResolver} and passed to service
 * methods that need audit attribution or caller-aware business logic.
 *
 * <p>Example:
 * <pre>{@code
 *   CallerContext caller = CallerContextResolver.resolve(auth, httpRequest);
 *   loanService.createApplication(dto, caller.username());
 * }</pre>
 */
public record CallerContext(UUID userId, String username, UserType userType, String branchCode, UUID customerPartyId,
        String kycStatus, String ipAddress, String userAgent) {

    /** The username to use in audit "createdBy" / "approvedBy" fields. */
    public String auditor() {
        return username;
    }

    public boolean isStaff() {
        return userType == UserType.STAFF;
    }

    public boolean isCustomer() {
        return userType == UserType.CUSTOMER;
    }

    public boolean isSystem() {
        return userType == UserType.SYSTEM;
    }
}
