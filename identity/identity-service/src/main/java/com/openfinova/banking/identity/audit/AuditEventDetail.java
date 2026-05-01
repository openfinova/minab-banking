package com.openfinova.banking.identity.audit;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import com.openfinova.banking.identity.converter.AuditEventDetailConverter;
import com.openfinova.banking.identity.entity.SecurityAuditEvent;

/**
 * Structured, typed context for a {@link SecurityAuditEvent}.
 *
 * <p>
 * Each static factory method produces an {@code AuditEventDetail} with a fixed set of fields
 * appropriate for that event category. The result is serialised to JSON and stored in the
 * {@code details_json} column by {@link AuditEventDetailConverter}, enabling log-aggregation
 * pipelines and compliance tooling to parse audit data without string-splitting.
 *
 * <p>
 * The existing human-readable {@code details} string column is kept for backward compatibility and
 * operator display purposes.
 */
public final class AuditEventDetail {

    private final Map<String, Object> fields;

    private AuditEventDetail(Map<String, Object> fields) {
        this.fields = Collections.unmodifiableMap(new LinkedHashMap<>(fields));
    }

    /**
     * Returns the underlying field map as serialised by {@link AuditEventDetailConverter}.
     */
    public Map<String, Object> getFields() {
        return fields;
    }

    public static AuditEventDetail loginSuccess() {
        return build("AUTH", Map.of("outcome", "SUCCESS"));
    }

    public static AuditEventDetail loginFailure(String reason) {
        return build("AUTH", Map.of("outcome", "FAILURE", "reason", nullToEmpty(reason)));
    }

    public static AuditEventDetail accountAutoLocked(int failedAttempts, LocalDateTime lockedUntil) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("failedAttempts", failedAttempts);
        f.put("lockedUntil", lockedUntil != null ? lockedUntil.toString() : null);
        return build("LOCKOUT", f);
    }

    public static AuditEventDetail accessDenied(String httpMethod, String requestPath) {
        return build("ACCESS_DENIED", Map.of("httpMethod", nullToEmpty(httpMethod), "path", nullToEmpty(requestPath)));
    }

    public static AuditEventDetail mfaSuccess(String verificationMethod) {
        return build("MFA", Map.of("outcome", "SUCCESS", "method", nullToEmpty(verificationMethod)));
    }

    public static AuditEventDetail mfaFailure(String reason) {
        return build("MFA", Map.of("outcome", "FAILURE", "reason", nullToEmpty(reason)));
    }

    public static AuditEventDetail mfaRecoveryCodeUsed(int remainingCodes) {
        return build("MFA", Map.of("outcome", "SUCCESS", "method", "RECOVERY_CODE", "remainingCodes", remainingCodes));
    }

    public static AuditEventDetail mfaEnabled() {
        return build("MFA", Map.of("action", "ENABLED"));
    }

    public static AuditEventDetail mfaDisabled() {
        return build("MFA", Map.of("action", "DISABLED"));
    }

    public static AuditEventDetail userCreated(String userType, Collection<String> initialRoles,
            boolean pendingApproval) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("userType", nullToEmpty(userType));
        f.put("initialRoles", initialRoles != null ? initialRoles : Collections.emptyList());
        f.put("pendingApproval", pendingApproval);
        return build("USER_MGMT", f);
    }

    public static AuditEventDetail rolesAssigned(String targetUsername, Collection<String> previousRoles,
            Collection<String> newRoles) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("targetUsername", nullToEmpty(targetUsername));
        f.put("previousRoles", previousRoles != null ? previousRoles : Collections.emptyList());
        f.put("newRoles", newRoles != null ? newRoles : Collections.emptyList());
        return build("ROLE_ASSIGNMENT", f);
    }

    public static AuditEventDetail passwordChanged(String trigger) {
        return build("PASSWORD", Map.of("trigger", nullToEmpty(trigger)));
    }

    public static AuditEventDetail roleCreated(String roleName, Collection<String> permissions) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("roleName", nullToEmpty(roleName));
        f.put("permissions", permissions != null ? permissions : Collections.emptyList());
        return build("ROLE_MGMT", f);
    }

    public static AuditEventDetail permissionsReplaced(String roleName, String previous, String current) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("roleName", nullToEmpty(roleName));
        f.put("changeType", "REPLACED");
        f.put("previous", nullToEmpty(previous));
        f.put("current", nullToEmpty(current));
        return build("ROLE_MGMT", f);
    }

    public static AuditEventDetail permissionsAdded(String roleName, Collection<String> added) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("roleName", nullToEmpty(roleName));
        f.put("changeType", "ADDED");
        f.put("added", added != null ? added : Collections.emptyList());
        return build("ROLE_MGMT", f);
    }

    public static AuditEventDetail permissionsRemoved(String roleName, Collection<String> removed) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("roleName", nullToEmpty(roleName));
        f.put("changeType", "REMOVED");
        f.put("removed", removed != null ? removed : Collections.emptyList());
        return build("ROLE_MGMT", f);
    }

    public static AuditEventDetail roleDeleted(String roleName) {
        return build("ROLE_MGMT", Map.of("roleName", nullToEmpty(roleName), "changeType", "DELETED"));
    }

    public static AuditEventDetail workflowStarted(UUID workflowId, String resourceType, String resourceId) {
        return build(
                "WORKFLOW",
                Map.of(
                        "workflowId",
                        workflowId != null ? workflowId.toString() : "",
                        "resourceType",
                        nullToEmpty(resourceType),
                        "resourceId",
                        nullToEmpty(resourceId),
                        "action",
                        "STARTED"));
    }

    public static AuditEventDetail workflowStepActed(UUID workflowId, int stepOrder, String action) {
        return build(
                "WORKFLOW",
                Map.of(
                        "workflowId",
                        workflowId != null ? workflowId.toString() : "",
                        "stepOrder",
                        stepOrder,
                        "action",
                        nullToEmpty(action)));
    }

    public static AuditEventDetail workflowCancelled(UUID workflowId) {
        return build(
                "WORKFLOW",
                Map.of("workflowId", workflowId != null ? workflowId.toString() : "", "action", "CANCELLED"));
    }

    /**
     * Records the automatic identity-account disablement triggered by a customer party status
     * transition to an access-revoking state (BLOCKED, CLOSED, DECEASED, ANONYMIZED).
     *
     * @param customerStatus the new customer status that triggered the revocation
     * @param customerId the customer party ID
     */
    public static AuditEventDetail customerStatusRevocation(String customerStatus, UUID customerId) {
        return build(
                "CUSTOMER_LIFECYCLE",
                Map.of(
                        "trigger",
                        "CUSTOMER_STATUS_CHANGE",
                        "customerStatus",
                        nullToEmpty(customerStatus),
                        "customerId",
                        customerId != null ? customerId.toString() : ""));
    }

    /**
     * Reconstructs an {@code AuditEventDetail} from a raw field map as deserialised by
     * {@link AuditEventDetailConverter}. Not intended for direct use in business code.
     */
    public static AuditEventDetail fromRawFields(Map<String, Object> fields) {
        return new AuditEventDetail(fields);
    }

    private static AuditEventDetail build(String category, Map<String, Object> extra) {
        Map<String, Object> f = new LinkedHashMap<>();
        f.put("category", category);
        f.putAll(extra);
        return new AuditEventDetail(f);
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}
