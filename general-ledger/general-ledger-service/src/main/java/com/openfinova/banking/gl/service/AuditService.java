package com.openfinova.banking.gl.service;

import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import com.openfinova.banking.gl.entity.GLAuditTrail;
import com.openfinova.banking.gl.repository.GLAuditTrailRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Service for logging audit trail entries for all GL entity changes.
 * This service is critical for regulatory compliance (SOX, Basel III, IFRS)
 * and maintains an immutable audit log of all changes to GL data.
 *
 * Audit retention: 7 years for regulatory compliance (SOX, Basel III).
 * After 7 years, records should be archived to cold storage but maintain query capability.
 *
 * Usage Pattern: Call logAudit() from service layer BEFORE or WITHIN
 * the same transaction as the business operation to ensure atomic logging.
 * If the business operation fails and rolls back, the audit log will also roll back.
 *
 * Reason Validation: High-risk actions require a mandatory reason with
 * minimum length of 10 characters. This prevents vague justifications like "test" or "fix".
 */
@Service
@Transactional
public class AuditService {

    private static final Logger logger = LoggerFactory.getLogger(AuditService.class);

    /**
     * Minimum length for audit reason to ensure meaningful business justification.
     * Prevents vague entries like "test", "fix", "update".
     */
    private static final int MIN_REASON_LENGTH = 10;

    /**
     * Actions that require mandatory reason with minimum length validation.
     * These are high-risk operations that need clear business justification
     * for regulatory compliance and forensic investigations.
     */
    private static final Set<GLAuditAction> MANDATORY_REASON_ACTIONS = Set.of(
            GLAuditAction.REVERSE,
            GLAuditAction.DELETE,
            GLAuditAction.STATUS_CHANGE,
            GLAuditAction.PERIOD_REOPEN,
            GLAuditAction.BALANCE_ADJUSTMENT,
            GLAuditAction.REJECTION);

    @Value("${audit.retention.years:7}")
    private int retentionYears;

    private final GLAuditTrailRepository auditTrailRepository;

    public AuditService(GLAuditTrailRepository auditTrailRepository) {
        this.auditTrailRepository = auditTrailRepository;
    }

    /**
     * Log an audit trail entry for an entity change.
     * This method validates reason requirements and saves the audit record
     * in the same transaction as the business operation.
     *
     * @param entityType type of entity being audited (e.g., GL_ACCOUNT, GL_TRANSACTION)
     * @param entityId UUID of the entity being audited
     * @param action action performed (e.g., CREATE, UPDATE, REVERSE)
     * @param performedBy username of the person performing the action
     * @param oldValues map of field values before the change (null for CREATE)
     * @param newValues map of field values after the change (null for DELETE)
     * @param reason business justification (mandatory for high-risk actions)
     * @param transactionAmount transaction amount for materiality analysis (nullable)
     * @param transactionCurrency currency code (ISO 4217) for the amount (nullable)
     * @param correlationId UUID to link related audit entries in multi-step operations (nullable)
     * @param ipAddress IP address of the user (nullable, TODO: populate from SecurityContext)
     * @param sessionId session ID of the user (nullable, TODO: populate from SecurityContext)
     * @return the saved audit trail entity
     * @throws IllegalArgumentException if reason is missing/invalid for mandatory-reason actions
     */
    public GLAuditTrail logAudit(GLEntityType entityType, UUID entityId, GLAuditAction action, String performedBy,
            Map<String, Object> oldValues, Map<String, Object> newValues, String reason, BigDecimal transactionAmount,
            String transactionCurrency, UUID correlationId, String ipAddress, String sessionId) {

        // Validate reason for high-risk actions
        validateReason(action, reason);

        // Create audit trail entity
        GLAuditTrail auditTrail = new GLAuditTrail();
        auditTrail.setEntityType(entityType);
        auditTrail.setEntityId(entityId);
        auditTrail.setAction(action);
        auditTrail.setPerformedBy(performedBy);
        auditTrail.setReason(reason);
        auditTrail.setOldValues(oldValues);
        auditTrail.setNewValues(newValues);
        auditTrail.setTransactionAmount(transactionAmount);
        auditTrail.setTransactionCurrency(transactionCurrency);
        auditTrail.setCorrelationId(correlationId);
        auditTrail.setIpAddress(ipAddress);
        auditTrail.setSessionId(sessionId);

        // Save audit record
        GLAuditTrail saved = auditTrailRepository.save(auditTrail);

        // Log for operational visibility
        if (transactionAmount != null && transactionCurrency != null) {
            logger.info(
                    "Audit logged: {} {} by {} (amount: {} {})",
                    action,
                    entityType,
                    performedBy,
                    transactionAmount,
                    transactionCurrency);
        } else {
            logger.info("Audit logged: {} {} by {}", action, entityType, performedBy);
        }

        return saved;
    }

    /**
     * Simplified overload for non-monetary operations (account creation, config changes).
     * Uses null for amount, currency, and correlation ID.
     *
     * @param entityType type of entity being audited
     * @param entityId UUID of the entity
     * @param action action performed
     * @param performedBy username
     * @param oldValues old field values
     * @param newValues new field values
     * @param reason business justification
     * @return the saved audit trail entity
     */
    public GLAuditTrail logAudit(GLEntityType entityType, UUID entityId, GLAuditAction action, String performedBy,
            Map<String, Object> oldValues, Map<String, Object> newValues, String reason) {
        return logAudit(
                entityType,
                entityId,
                action,
                performedBy,
                oldValues,
                newValues,
                reason,
                null,
                null,
                null,
                null,
                null);
    }

    /**
     * Validate that reason is provided and meets minimum length requirement
     * for high-risk actions.
     *
     * @param action the action being performed
     * @param reason the business justification provided
     * @throws IllegalArgumentException if reason is missing or too short for mandatory actions
     */
    private void validateReason(GLAuditAction action, String reason) {
        if (MANDATORY_REASON_ACTIONS.contains(action)) {
            if (reason == null || reason.trim().length() < MIN_REASON_LENGTH) {
                throw new IllegalArgumentException(
                        String.format(
                                "Action %s requires a detailed reason (minimum %d characters). "
                                        + "Provided reason is insufficient for regulatory compliance.",
                                action,
                                MIN_REASON_LENGTH));
            }
        }
    }

    /**
     * Build a map of relevant field values from an entity for audit logging.
     * Override this method or use custom logic in service layer to extract
     * only the fields relevant for audit trail (avoid sensitive data, large objects).
     *
     * @param entity the entity to extract values from
     * @return map of field names to values
     */
    public Map<String, Object> buildChangeMap(Object entity) {
        // This is a helper method - implementation should be customized
        // per entity type in the service layer where the entity structure is known.
        // For now, return empty map as a placeholder.
        // Services should manually build maps with relevant fields.
        return Map.of();
    }

    /**
     * Get configured retention period in years.
     * After this period, audit records should be archived to cold storage.
     *
     * @return retention period in years (default: 7 for SOX/Basel III compliance)
     */
    public int getRetentionYears() {
        return retentionYears;
    }
}
