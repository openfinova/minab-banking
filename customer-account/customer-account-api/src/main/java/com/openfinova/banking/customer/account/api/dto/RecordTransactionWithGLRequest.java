package com.openfinova.banking.customer.account.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request to record a transaction with immediate GL linking.
 * Used by management dashboard and external systems that have already posted to GL.
 */
@Schema(description = "Request to record a transaction with GL link")
public class RecordTransactionWithGLRequest {

    @NotNull(message = "Transaction type is required")
    @Schema(description = "Transaction type", requiredMode = Schema.RequiredMode.REQUIRED, example = "DEPOSIT")
    private AccountTransactionType transactionType;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be positive")
    @Schema(description = "Transaction amount (always positive)", requiredMode = Schema.RequiredMode.REQUIRED, example = "100.00")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be 3 characters")
    @Schema(description = "Currency code (ISO 4217)", requiredMode = Schema.RequiredMode.REQUIRED, example = "USD")
    private String currency;

    @NotNull(message = "Transaction date is required")
    @Schema(description = "Business date/time when the transaction occurred", requiredMode = Schema.RequiredMode.REQUIRED, example = "2026-05-12T10:30:00")
    private LocalDateTime transactionDate;

    @Schema(description = "Customer-friendly description", example = "Branch deposit - Main Street")
    private String description;

    @Schema(description = "External reference ID for reconciliation", example = "ATM-2024-001234")
    private String referenceId;

    @NotNull(message = "GL transaction ID is required")
    @Schema(description = "GL transaction ID to link to", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID glTransactionId;

    // Constructors
    public RecordTransactionWithGLRequest() {
    }

    public RecordTransactionWithGLRequest(AccountTransactionType transactionType, BigDecimal amount, String currency,
            LocalDateTime transactionDate, String description, String referenceId, UUID glTransactionId) {
        this.transactionType = transactionType;
        this.amount = amount;
        this.currency = currency;
        this.transactionDate = transactionDate;
        this.description = description;
        this.referenceId = referenceId;
        this.glTransactionId = glTransactionId;
    }

    // Getters and setters
    public AccountTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(AccountTransactionType transactionType) {
        this.transactionType = transactionType;
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

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public UUID getGlTransactionId() {
        return glTransactionId;
    }

    public void setGlTransactionId(UUID glTransactionId) {
        this.glTransactionId = glTransactionId;
    }

    @Override
    public String toString() {
        return "RecordTransactionWithGLRequest{" + "transactionType=" + transactionType + ", amount=" + amount
                + ", currency='" + currency + '\'' + ", transactionDate=" + transactionDate + ", description='"
                + description + '\'' + ", referenceId='" + referenceId + '\'' + ", glTransactionId=" + glTransactionId
                + '}';
    }
}
