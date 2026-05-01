package com.openfinova.banking.tp.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.tp.api.entity.TransactionType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing a transaction processing request.
 * Contains all the information needed to process a transaction including
 * idempotency controls.
 */
@Entity
@Table(name = "transaction_requests", indexes = {
        @Index(name = "idx_transaction_requests_idempotency", columnList = "idempotency_key"),
        @Index(name = "idx_transaction_requests_source_account", columnList = "source_account_id"),
        @Index(name = "idx_transaction_requests_created_at", columnList = "created_at") })
public class TransactionRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "idempotency_key", unique = true, nullable = false, length = 255)
    @NotBlank(message = "Idempotency key is required")
    @Size(max = 255, message = "Idempotency key must not exceed 255 characters")
    private String idempotencyKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "amount", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer digits and 4 decimal places")
    private BigDecimal amount;

    @Column(name = "currency", length = 3, nullable = false)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency = "USD";

    @Column(name = "requested_transaction_date")
    private LocalDate requestedTransactionDate;

    @Column(name = "requested_value_date")
    private LocalDate requestedValueDate;

    @Column(name = "requested_reservation_timeout")
    private Integer requestedReservationTimeout; // In minutes

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "client_reference", length = 255)
    @Size(max = 255, message = "Client reference must not exceed 255 characters")
    private String clientReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    @NotBlank(message = "Created by is required")
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    @Column(name = "ip_address", length = 45) // IPv6 support
    private String ipAddress;

    @Column(name = "user_agent", columnDefinition = "TEXT")
    private String userAgent;

    @Version
    private Long version;

    // Constructors
    public TransactionRequest() {
    }

    public TransactionRequest(String idempotencyKey, TransactionType transactionType, BigDecimal amount,
            String createdBy) {
        this.idempotencyKey = idempotencyKey;
        this.transactionType = transactionType;
        this.amount = amount;
        this.createdBy = createdBy;
    }

    // Business logic methods

    /**
     * Validates that required accounts are provided based on transaction type
     *
     * @throws IllegalStateException if required accounts are missing
     */
    public void validateAccountRequirements() {
        if (transactionType.requiresSourceAccount() && sourceAccountId == null) {
            throw new IllegalStateException("Source account is required for " + transactionType.getDisplayName());
        }
        if (transactionType.requiresDestinationAccount() && destinationAccountId == null) {
            throw new IllegalStateException("Destination account is required for " + transactionType.getDisplayName());
        }
    }

    /**
     * Checks if this is a P2P transaction
     *
     * @return true if transaction type is P2P
     */
    public boolean isP2P() {
        return TransactionType.P2P.equals(transactionType);
    }

    /**
     * Checks if this transaction requires external gateway processing
     *
     * @return true if external gateway is required
     */
    public boolean requiresExternalGateway() {
        return transactionType.requiresExternalGateway();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public void setCurrency(String currency) {
        this.currency = currency;
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

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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

    public Long getVersion() {
        return version;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TransactionRequest that))
            return false;
        return idempotencyKey != null && idempotencyKey.equals(that.idempotencyKey);
    }

    @Override
    public int hashCode() {
        return idempotencyKey != null ? idempotencyKey.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TransactionRequest{" + "id=" + id + ", idempotencyKey='" + idempotencyKey + '\'' + ", transactionType="
                + transactionType + ", amount=" + amount + ", currency='" + currency + '\'' + '}';
    }
}