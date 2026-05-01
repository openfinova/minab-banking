package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.SuspenseReasonCode;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request to create a new suspense item.
 * Typically called internally from Transaction Processing module.
 */
public class CreateSuspenseItemRequest {

    @NotNull(message = "GL transaction ID is required")
    private UUID glTransactionId;

    @NotNull(message = "Amount is required")
    @Positive(message = "Amount must be positive")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(max = 3, message = "Currency must be 3 characters")
    private String currency;

    @NotNull(message = "Reason code is required")
    private SuspenseReasonCode reasonCode;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @NotBlank(message = "Source system is required")
    @Size(max = 100, message = "Source system must not exceed 100 characters")
    private String sourceSystem;

    @Size(max = 100, message = "External reference must not exceed 100 characters")
    private String externalReference;

    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    // Constructors

    public CreateSuspenseItemRequest() {
    }

    // Getters and Setters

    public UUID getGlTransactionId() {
        return glTransactionId;
    }

    public void setGlTransactionId(UUID glTransactionId) {
        this.glTransactionId = glTransactionId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public SuspenseReasonCode getReasonCode() {
        return reasonCode;
    }

    public void setReasonCode(SuspenseReasonCode reasonCode) {
        this.reasonCode = reasonCode;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    @Override
    public String toString() {
        return "CreateSuspenseItemRequest{" + "glTransactionId=" + glTransactionId + ", amount=" + amount
                + ", currency='" + currency + '\'' + ", reasonCode=" + reasonCode + '}';
    }
}
