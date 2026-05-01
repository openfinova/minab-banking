package com.openfinova.banking.identity.audit;

import java.time.LocalDateTime;
import java.util.UUID;
import com.openfinova.banking.identity.entity.SecurityAuditEvent;

/**
 * Optional regulatory fields appended to a {@link SecurityAuditEvent}.
 */
public record SecurityAuditExtensions(UUID changedByUserId, String changedByUsername, String previousValue,
        String currentValue, UUID approvedByUserId, String approvedByUsername, LocalDateTime approvalDate) {

    public static final SecurityAuditExtensions NONE = new SecurityAuditExtensions(
            null,
            null,
            null,
            null,
            null,
            null,
            null);

    public static SecurityAuditExtensions changeTracking(UUID changedByUserId, String changedByUsername,
            String previousValue, String currentValue) {
        return new SecurityAuditExtensions(
                changedByUserId,
                changedByUsername,
                previousValue,
                currentValue,
                null,
                null,
                null);
    }

    public static SecurityAuditExtensions withApproval(UUID changedByUserId, String changedByUsername,
            UUID approvedByUserId, String approvedByUsername, LocalDateTime approvalDate) {
        return new SecurityAuditExtensions(
                changedByUserId,
                changedByUsername,
                null,
                null,
                approvedByUserId,
                approvedByUsername,
                approvalDate);
    }
}
