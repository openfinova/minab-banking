package com.openfinova.banking.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Immutable audit log entry for customer profile changes.
 *
 * <p>Records every modification to a customer record: who changed what field,
 * from which value to which value, and from which channel. Records must never
 * be updated or deleted — they form the regulatory audit trail.</p>
 *
 * <p>Required by banking and AML regulations (e.g., Basel III, MiFID II, GDPR
 * Article 5(1)(f) accountability principle, PSD2 audit trail obligations).</p>
 */
@Entity
@Table(name = "customer_audit_logs", indexes = { @Index(name = "idx_audit_log_customer", columnList = "customer_id"),
        @Index(name = "idx_audit_log_field", columnList = "field_name"),
        @Index(name = "idx_audit_log_action", columnList = "action"),
        @Index(name = "idx_audit_log_changed_at", columnList = "changed_at"),
        @Index(name = "idx_audit_log_changed_by", columnList = "changed_by") })
public class CustomerAuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;

    /**
     * High-level action type.
     *
     * @see CustomerAuditAction
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 60)
    @NotNull(message = "Action is required")
    private CustomerAuditAction action;

    /**
     * The specific field or sub-entity that was changed.
     * (e.g., "firstName", "status", "kycStatus", "address.line1", "contact.email")
     */
    @Column(name = "field_name", length = 100)
    private String fieldName;

    /**
     * Value of the field before the change. Null for additions.
     */
    @Column(name = "old_value", columnDefinition = "TEXT")
    private String oldValue;

    /**
     * Value of the field after the change. Null for deletions.
     */
    @Column(name = "new_value", columnDefinition = "TEXT")
    private String newValue;

    /**
     * User ID or system identifier that performed the change.
     */
    @Column(name = "changed_by", nullable = false, length = 100)
    @NotBlank(message = "Changed-by identifier is required")
    private String changedBy;

    /**
     * Channel through which the change was made.
     * (e.g., "WEB_PORTAL", "MOBILE_APP", "BRANCH", "API", "BATCH_JOB", "ADMIN_CONSOLE")
     */
    @Column(name = "channel", length = 50)
    private String channel;

    /**
     * IP address of the request that triggered the change. Null for internal/batch operations.
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * Correlation ID linking this audit entry to a specific HTTP request or transaction.
     */
    @Column(name = "correlation_id", length = 100)
    private String correlationId;

    /**
     * Optional human-readable reason for the change
     * (e.g., "Customer requested name correction", "Compliance review").
     */
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    /**
     * Reference to the related entity ID if the change was on a sub-entity
     * (e.g., address ID, document ID, workflow ID).
     */
    @Column(name = "related_entity_id")
    private UUID relatedEntityId;

    /**
     * Type of the related entity (e.g., "CustomerAddress", "IdentificationDocument").
     */
    @Column(name = "related_entity_type", length = 60)
    private String relatedEntityType;

    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;

    public CustomerAuditLog() {
    }

    /**
     * Factory constructor for a field-level change.
     */
    public CustomerAuditLog(Customer customer, CustomerAuditAction action, String fieldName, String oldValue,
            String newValue, String changedBy) {
        this.customer = customer;
        this.action = action;
        this.fieldName = fieldName;
        this.oldValue = oldValue;
        this.newValue = newValue;
        this.changedBy = changedBy;
    }

    /**
     * Factory constructor for a sub-entity action (add, remove).
     */
    public CustomerAuditLog(Customer customer, CustomerAuditAction action, String changedBy, UUID relatedEntityId,
            String relatedEntityType) {
        this.customer = customer;
        this.action = action;
        this.changedBy = changedBy;
        this.relatedEntityId = relatedEntityId;
        this.relatedEntityType = relatedEntityType;
    }

    // Getters — no setters on most fields to enforce immutability after persist

    public UUID getId() {
        return id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public CustomerAuditAction getAction() {
        return action;
    }

    public String getFieldName() {
        return fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public String getChangeReason() {
        return changeReason;
    }

    public void setChangeReason(String changeReason) {
        this.changeReason = changeReason;
    }

    public UUID getRelatedEntityId() {
        return relatedEntityId;
    }

    public void setRelatedEntityId(UUID relatedEntityId) {
        this.relatedEntityId = relatedEntityId;
    }

    public String getRelatedEntityType() {
        return relatedEntityType;
    }

    public void setRelatedEntityType(String relatedEntityType) {
        this.relatedEntityType = relatedEntityType;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }
}
