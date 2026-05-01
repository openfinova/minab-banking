package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.TransactionType;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for transaction processing requests.
 * Contains all the information needed to process a transaction including
 * idempotency controls. Used in both single and batch transaction flows.
 */
public class TransactionRequestDTO {

    @NotBlank(message = "Idempotency key is required")
    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    private String idempotencyKey;

    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    private UUID sourceAccountId;

    private UUID destinationAccountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer digits and 4 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    private String currency = "USD";

    private LocalDate requestedTransactionDate;

    private LocalDate requestedValueDate;

    private Integer requestedReservationTimeout; // In minutes

    private String description;

    private Map<String, Object> metadata;

    @Size(max = 255, message = "Client reference must not exceed 255 characters")
    private String clientReference;

    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    @Size(max = 45) // IPv6 support
    private String ipAddress;

    private String userAgent;

    // Constructors
    public TransactionRequestDTO() {
    }

    public TransactionRequestDTO(String idempotencyKey, TransactionType transactionType, BigDecimal amount,
            String createdBy) {
        this.idempotencyKey = idempotencyKey;
        this.transactionType = transactionType;
        this.amount = amount;
        this.createdBy = createdBy;
    }

    // Getters and Setters

    public String getIdempotencyKey() {
        return idempotencyKey;
    }

    public void setIdempotencyKey(String idempotencyKey) {
        this.idempotencyKey = idempotencyKey;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public UUID getDestinationAccountId() {
        return destinationAccountId;
    }

    public void setDestinationAccountId(UUID destinationAccountId) {
        this.destinationAccountId = destinationAccountId;
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

    public LocalDate getRequestedTransactionDate() {
        return requestedTransactionDate;
    }

    public void setRequestedTransactionDate(LocalDate requestedTransactionDate) {
        this.requestedTransactionDate = requestedTransactionDate;
    }

    public LocalDate getRequestedValueDate() {
        return requestedValueDate;
    }

    public void setRequestedValueDate(LocalDate requestedValueDate) {
        this.requestedValueDate = requestedValueDate;
    }

    public Integer getRequestedReservationTimeout() {
        return requestedReservationTimeout;
    }

    public void setRequestedReservationTimeout(Integer requestedReservationTimeout) {
        this.requestedReservationTimeout = requestedReservationTimeout;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getClientReference() {
        return clientReference;
    }

    public void setClientReference(String clientReference) {
        this.clientReference = clientReference;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getUserAgent() {
        return userAgent;
    }

    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }
}
