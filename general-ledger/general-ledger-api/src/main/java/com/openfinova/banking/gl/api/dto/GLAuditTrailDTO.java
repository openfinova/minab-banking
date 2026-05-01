package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * DTO for GL Audit Trail information exposed via REST API.
 * Provides regulatory audit information for compliance reporting and forensic
 * investigations.
 */
@Schema(description = "Audit trail record for GL entity changes")
public class GLAuditTrailDTO {

    @Schema(description = "Unique audit record identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Type of entity being audited", example = "GL_TRANSACTION")
    private GLEntityType entityType;

    @Schema(description = "UUID of the audited entity", example = "987e6543-e89b-12d3-a456-426614174000")
    private UUID entityId;

    @Schema(description = "Action performed on the entity", example = "REVERSE")
    private GLAuditAction action;

    @Schema(description = "Username of person who performed the action", example = "john.doe")
    private String performedBy;

    @Schema(description = "Timestamp when action was performed", example = "2026-02-17T10:15:30Z")
    private Instant performedAt;

    @Schema(description = "Business justification for the action (mandatory for high-risk actions)", example = "Reversal approved by CFO due to duplicate transaction entry error detected during monthly reconciliation")
    private String reason;

    @Schema(description = "Field values before the change (JSON)", example = "{\"status\":\"POSTED\",\"amount\":1000.00}")
    private Map<String, Object> oldValues;

    @Schema(description = "Field values after the change (JSON)", example = "{\"status\":\"REVERSED\",\"amount\":1000.00}")
    private Map<String, Object> newValues;

    @Schema(description = "IP address of user (future: from authentication)", example = "192.168.1.100")
    private String ipAddress;

    @Schema(description = "Session ID of user (future: from authentication)", example = "sess_abc123xyz")
    private String sessionId;

    @Schema(description = "Transaction amount involved (for materiality analysis)", example = "1000000.00")
    private BigDecimal transactionAmount;

    @Schema(description = "Currency of transaction amount", example = "USD")
    private String transactionCurrency;

    @Schema(description = "Correlation ID linking related audit entries in multi-step operations", example = "456e7890-e89b-12d3-a456-426614174000")
    private UUID correlationId;

    // Constructors
    public GLAuditTrailDTO() {
    }

    public GLAuditTrailDTO(UUID id, GLEntityType entityType, UUID entityId, GLAuditAction action, String performedBy,
            Instant performedAt, String reason, Map<String, Object> oldValues, Map<String, Object> newValues,
            String ipAddress, String sessionId, BigDecimal transactionAmount, String transactionCurrency,
            UUID correlationId) {
        this.id = id;
        this.entityType = entityType;
        this.entityId = entityId;
        this.action = action;
        this.performedBy = performedBy;
        this.performedAt = performedAt;
        this.reason = reason;
        this.oldValues = oldValues;
        this.newValues = newValues;
        this.ipAddress = ipAddress;
        this.sessionId = sessionId;
        this.transactionAmount = transactionAmount;
        this.transactionCurrency = transactionCurrency;
        this.correlationId = correlationId;
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
    public String toString() {
        return "GLAuditTrailDTO{" + "id=" + id + ", entityType=" + entityType + ", entityId=" + entityId + ", action="
                + action + ", performedBy='" + performedBy + '\'' + ", performedAt=" + performedAt
                + ", transactionAmount=" + transactionAmount + ", transactionCurrency='" + transactionCurrency + '\''
                + ", correlationId=" + correlationId + '}';
    }
}
