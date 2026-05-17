package com.openfinova.banking.customer.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Immutable audit record for a customer profile or sub-entity change")
public class CustomerAuditEventResponse {

    @Schema(requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID id;

    private String action;

    private String fieldName;

    /** May be masked when viewer lacks customer:pii:read — see maskingPolicy. */
    private String oldValue;

    /** May be masked when viewer lacks customer:pii:read — see maskingPolicy. */
    private String newValue;

    private String changedBy;

    private String channel;

    private LocalDateTime changedAt;

    private String correlationId;

    private UUID relatedEntityId;

    private String relatedEntityType;

    private boolean valueMasked;

    public CustomerAuditEventResponse() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String action) {
        this.action = action;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void setFieldName(String fieldName) {
        this.fieldName = fieldName;
    }

    public String getOldValue() {
        return oldValue;
    }

    public void setOldValue(String oldValue) {
        this.oldValue = oldValue;
    }

    public String getNewValue() {
        return newValue;
    }

    public void setNewValue(String newValue) {
        this.newValue = newValue;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public void setChangedBy(String changedBy) {
        this.changedBy = changedBy;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public LocalDateTime getChangedAt() {
        return changedAt;
    }

    public void setChangedAt(LocalDateTime changedAt) {
        this.changedAt = changedAt;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
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

    public boolean isValueMasked() {
        return valueMasked;
    }

    public void setValueMasked(boolean valueMasked) {
        this.valueMasked = valueMasked;
    }
}
