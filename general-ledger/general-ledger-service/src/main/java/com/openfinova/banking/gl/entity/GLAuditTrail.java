package com.openfinova.banking.gl.entity;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Entity representing the audit trail for all changes to GL entities.
 * This entity is critical for regulatory compliance (SOX, Basel III, IFRS)
 * and maintains an immutable record of:
 * - What changed (old/new values)
 * - Who made the change (performedBy)
 * - When it occurred (performedAt)
 * - Why it was done (reason for critical actions)
 *
 * Audit retention: 7 years for regulatory compliance (SOX, Basel III).
 * After 7 years, records should be archived to cold storage but maintain query capability.
 *
 * IMMUTABILITY: This entity should NEVER be updated or deleted after creation.
 * All operations are INSERT-only to maintain audit trail integrity.
 */
@Entity
@Table(name = "gl_audit_trail", indexes = {
        @Index(name = "idx_audit_entity", columnList = "entity_type, entity_id, performed_at"),
        @Index(name = "idx_audit_performed_at", columnList = "performed_at"),
        @Index(name = "idx_audit_action", columnList = "action"),
        @Index(name = "idx_audit_correlation", columnList = "correlation_id"),
        @Index(name = "idx_audit_performed_by", columnList = "performed_by") })
public class GLAuditTrail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * The type of entity being audited (e.g., GL_ACCOUNT, GL_TRANSACTION)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "entity_type", nullable = false, length = 50)
    @NotNull(message = "Entity type is required")
    private GLEntityType entityType;

    /**
     * The UUID of the entity being audited
     */
    @Column(name = "entity_id", nullable = false)
    @NotNull(message = "Entity ID is required")
    private UUID entityId;

    /**
     * The action performed (e.g., CREATE, UPDATE, REVERSE)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 50)
    @NotNull(message = "Action is required")
    private GLAuditAction action;

    /**
     * Username of the person who performed the action
     */
    @Column(name = "performed_by", nullable = false, length = 100)
    @NotNull(message = "Performed by is required")
    private String performedBy;

    /**
     * Timestamp when the action was performed
     */
    @CreationTimestamp
    @Column(name = "performed_at", nullable = false, updatable = false)
    private Instant performedAt;

    /**
     * Business justification for the action.
     * Mandatory for high-risk actions: REVERSE, DELETE, STATUS_CHANGE,
     * PERIOD_REOPEN, BALANCE_ADJUSTMENT, REJECTION.
     * Minimum length: 10 characters for meaningful audit trail.
     */
    @Column(name = "reason", columnDefinition = "TEXT")
    private String reason;

    /**
     * Old values before the change, stored as JSON.
     * Example: {"accountCode": "1001", "status": "ACTIVE", "name": "Cash"}
     */
    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "old_values", columnDefinition = "TEXT")
    private Map<String, Object> oldValues;

    /**
     * New values after the change, stored as JSON.
     * Example: {"accountCode": "1001", "status": "INACTIVE", "name": "Cash"}
     */
    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "new_values", columnDefinition = "TEXT")
    private Map<String, Object> newValues;

    /**
     * IP address of the user who performed the action.
     * Currently nullable - will be populated once authentication/authorization
     * infrastructure is implemented.
     * TODO: Populate from SecurityContext once A&A is implemented
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Session ID of the user who performed the action.
     * Currently nullable - will be populated once authentication/authorization
     * infrastructure is implemented.
     * TODO: Populate from SecurityContext once A&A is implemented
     */
    @Column(name = "session_id", length = 255)
    private String sessionId;

    /**
     * Transaction amount involved in the audited action.
     * Used for materiality analysis and regulatory reporting
     * (e.g., "show all reversals > $1M in last 30 days").
     * Nullable for non-monetary operations (account creation, config changes).
     */
    @Column(name = "transaction_amount", precision = 19, scale = 4)
    private BigDecimal transactionAmount;

    /**
     * Currency of the transaction amount (ISO 4217 code: USD, EUR, GBP, etc.)
     * Nullable for non-monetary operations.
     */
    @Column(name = "transaction_currency", length = 3)
    private String transactionCurrency;

    /**
     * Correlation ID to link related audit entries in multi-step operations.
     * Example: Period close operation generates multiple audit entries
     * (account status changes, balance calculations, etc.) - all share same correlationId.
     * Enables tracing complete business operation across multiple entity changes.
     */
    @Column(name = "correlation_id")
    private UUID correlationId;

    // Constructors
    public GLAuditTrail() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLEntityType getEntityType() {
        return entityType;
    }

    public void setEntityType(GLEntityType entityType) {
        this.entityType = entityType;
    }

    public UUID getEntityId() {
        return entityId;
    }

    public void setEntityId(UUID entityId) {
        this.entityId = entityId;
    }

    public GLAuditAction getAction() {
        return action;
    }

    public void setAction(GLAuditAction action) {
        this.action = action;
    }

    public String getPerformedBy() {
        return performedBy;
    }

    public void setPerformedBy(String performedBy) {
        this.performedBy = performedBy;
    }

    public Instant getPerformedAt() {
        return performedAt;
    }

    public void setPerformedAt(Instant performedAt) {
        this.performedAt = performedAt;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public Map<String, Object> getOldValues() {
        return oldValues;
    }

    public void setOldValues(Map<String, Object> oldValues) {
        this.oldValues = oldValues;
    }

    public Map<String, Object> getNewValues() {
        return newValues;
    }

    public void setNewValues(Map<String, Object> newValues) {
        this.newValues = newValues;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public BigDecimal getTransactionAmount() {
        return transactionAmount;
    }

    public void setTransactionAmount(BigDecimal transactionAmount) {
        this.transactionAmount = transactionAmount;
    }

    public String getTransactionCurrency() {
        return transactionCurrency;
    }

    public void setTransactionCurrency(String transactionCurrency) {
        this.transactionCurrency = transactionCurrency;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        GLAuditTrail that = (GLAuditTrail) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "GLAuditTrail{" + "id=" + id + ", entityType=" + entityType + ", entityId=" + entityId + ", action="
                + action + ", performedBy='" + performedBy + '\'' + ", performedAt=" + performedAt + ", reason='"
                + reason + '\'' + ", transactionAmount=" + transactionAmount + ", transactionCurrency='"
                + transactionCurrency + '\'' + ", correlationId=" + correlationId + '}';
    }
}
