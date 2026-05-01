package com.openfinova.banking.tp.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
import com.openfinova.banking.tp.api.entity.TransactionStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.ForeignKey;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entity representing a transaction in the transaction processing system.
 * Links to the original request and tracks processing state and GL integration.
 */
@Entity
@Table(name = "tp_transactions", indexes = { @Index(name = "idx_tp_transactions_status", columnList = "status"),
        @Index(name = "idx_tp_transactions_gl_transaction", columnList = "gl_transaction_id"),
        @Index(name = "idx_tp_transactions_gateway", columnList = "gateway_transaction_id"),
        @Index(name = "idx_tp_transactions_source_account", columnList = "source_account_id"),
        @Index(name = "idx_tp_transactions_destination_account", columnList = "destination_account_id"),
        @Index(name = "idx_tp_transactions_processing_time", columnList = "processing_started_at, completed_at") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Transaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "request_id", nullable = false, unique = true, foreignKey = @ForeignKey(name = "fk_transaction_request"))
    @NotNull(message = "Transaction request is required")
    private TransactionRequest request;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @NotNull(message = "Transaction status is required")
    private TransactionStatus status = TransactionStatus.INITIATED;

    @Column(name = "gl_transaction_id")
    private UUID glTransactionId;

    @Column(name = "gl_reference_number", length = 50)
    private String glReferenceNumber; // For human-readable reference

    @Column(name = "external_reference", length = 255)
    private String externalReference;

    @Column(name = "gateway_transaction_id", length = 255)
    private String gatewayTransactionId;

    @Column(name = "source_account_id")
    private UUID sourceAccountId;

    @Column(name = "destination_account_id")
    private UUID destinationAccountId;

    @Column(name = "value_date", nullable = false)
    @NotNull(message = "Value date is required")
    private LocalDate valueDate;

    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDate transactionDate;

    @Column(name = "currency", length = 3, nullable = false)
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    private String currency = "USD";

    @Column(name = "fee_amount", precision = 19, scale = 4)
    private BigDecimal feeAmount = BigDecimal.ZERO;

    @Column(name = "estimated_fee_amount", precision = 19, scale = 4)
    private BigDecimal estimatedFeeAmount = BigDecimal.ZERO;

    @Column(name = "fee_calculation_at")
    private LocalDateTime feeCalculationAt;

    @Column(name = "principal_amount", precision = 19, scale = 4)
    private BigDecimal principalAmount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "applied_fee_rule_id", foreignKey = @ForeignKey(name = "fk_transaction_fee_rule"))
    private FeeRule appliedFeeRule;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "fee_calculation_details", columnDefinition = "TEXT")
    private Map<String, Object> feeCalculationDetails;

    @Column(name = "processing_started_at")
    private LocalDateTime processingStartedAt;

    @Column(name = "completed_at")
    private LocalDateTime completedAt;

    @Column(name = "failed_at")
    private LocalDateTime failedAt;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "reservation_timeout")
    private Integer reservationTimeout; // In minutes

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @OrderBy("eventSequence ASC")
    @BatchSize(size = 20)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<TransactionEvent> events = new ArrayList<>();

    @OneToMany(mappedBy = "transaction", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @BatchSize(size = 10)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<BalanceReservation> reservations = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public Transaction() {
    }

    public Transaction(TransactionRequest request) {
        this.request = request;
        this.currency = request.getCurrency();
        this.principalAmount = request.getAmount();
        this.sourceAccountId = request.getSourceAccountId();
        this.destinationAccountId = request.getDestinationAccountId();
        this.transactionDate = Optional.ofNullable(request.getRequestedTransactionDate()).orElse(LocalDate.now());
        this.valueDate = Optional.ofNullable(request.getRequestedValueDate()).orElse(this.transactionDate);
        this.reservationTimeout = Optional.ofNullable(request.getRequestedReservationTimeout()).orElse(30);
        this.processingStartedAt = LocalDateTime.now();
        this.status = TransactionStatus.INITIATED;
        this.estimatedFeeAmount = BigDecimal.ZERO; // To be updated by FeeService
        this.feeCalculationAt = LocalDateTime.now();
    }

    // Business logic methods

    /**
     * Transitions the transaction to a new status with validation
     *
     * @param newStatus the target status
     * @param context   additional context for the transition
     * @throws IllegalStateException if transition is not valid
     */
    public void transitionTo(TransactionStatus newStatus, String context) {
        if (!status.canTransitionTo(newStatus)) {
            throw new IllegalStateException(String.format("Invalid state transition from %s to %s", status, newStatus));
        }

        TransactionStatus previousStatus = this.status;
        this.status = newStatus;

        // Update timing fields based on new status
        switch (newStatus) {
            case INITIATED, PENDING_RESERVATION -> {
                // No specific timing update for these middle states
            }
            case AUTHORIZED -> {
                // Lock the fee during authorization
                this.feeCalculationAt = LocalDateTime.now();
                if (this.feeAmount == null || this.feeAmount.compareTo(BigDecimal.ZERO) == 0) {
                    this.feeAmount = this.estimatedFeeAmount;
                }
            }
            case POSTED -> this.completedAt = LocalDateTime.now();
            case FAILED -> {
                this.failedAt = LocalDateTime.now();
                this.failureReason = context;
            }
            case REVERSED -> this.completedAt = LocalDateTime.now();
        }

        // Record the state change event
        recordStateChangeEvent(previousStatus, newStatus, context);
    }

    /**
     * Records a state change event
     *
     * @param previousStatus the previous status
     * @param newStatus      the new status
     * @param context        additional context
     */
    private void recordStateChangeEvent(TransactionStatus previousStatus, TransactionStatus newStatus, String context) {
        TransactionEvent event = new TransactionEvent();
        event.setTransaction(this);
        event.setEventType("STATE_TRANSITION");
        event.setEventSequence(events.size() + 1);
        event.setPreviousStatus(previousStatus);
        event.setNewStatus(newStatus);
        event.setCreatedBy("SYSTEM"); // Could be parameterized

        if (context != null) {
            event.setEventData(Map.of("context", context));
        }

        events.add(event);
    }

    /**
     * Adds a balance reservation to this transaction
     *
     * @param reservation the reservation to add
     */
    public void addReservation(BalanceReservation reservation) {
        reservations.add(reservation);
        reservation.setTransaction(this);
    }

    /**
     * Calculates the total amount including fees
     *
     * @return total amount including fees
     */
    public BigDecimal getTotalAmount() {
        return principalAmount.add(feeAmount != null ? feeAmount : BigDecimal.ZERO);
    }

    /**
     * Checks if this transaction is in a terminal state
     *
     * @return true if transaction cannot be processed further
     */
    public boolean isTerminal() {
        return status.isTerminal();
    }

    /**
     * Checks if this transaction completed successfully
     *
     * @return true if transaction was posted successfully
     */
    public boolean isSuccessful() {
        return status.isSuccessful();
    }

    /**
     * Checks if this transaction failed
     *
     * @return true if transaction failed
     */
    public boolean isFailed() {
        return status.isFailed();
    }

    /**
     * Gets the transaction type from the associated request
     *
     * @return the transaction type
     */
    public TransactionType getTransactionType() {
        return request != null ? request.getTransactionType() : null;
    }

    /**
     * Gets the idempotency key from the associated request
     *
     * @return the idempotency key
     */
    public String getIdempotencyKey() {
        return request != null ? request.getIdempotencyKey() : null;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public TransactionRequest getRequest() {
        return request;
    }

    public void setRequest(TransactionRequest request) {
        this.request = request;
    }

    public TransactionStatus getStatus() {
        return status;
    }

    public void setStatus(TransactionStatus status) {
        this.status = status;
    }

    public UUID getGlTransactionId() {
        return glTransactionId;
    }

    public void setGlTransactionId(UUID glTransactionId) {
        this.glTransactionId = glTransactionId;
    }

    public String getGlReferenceNumber() {
        return glReferenceNumber;
    }

    public void setGlReferenceNumber(String glReferenceNumber) {
        this.glReferenceNumber = glReferenceNumber;
    }

    public String getExternalReference() {
        return externalReference;
    }

    public void setExternalReference(String externalReference) {
        this.externalReference = externalReference;
    }

    public String getGatewayTransactionId() {
        return gatewayTransactionId;
    }

    public void setGatewayTransactionId(String gatewayTransactionId) {
        this.gatewayTransactionId = gatewayTransactionId;
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

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getEstimatedFeeAmount() {
        return estimatedFeeAmount;
    }

    public void setEstimatedFeeAmount(BigDecimal estimatedFeeAmount) {
        this.estimatedFeeAmount = estimatedFeeAmount;
    }

    public LocalDateTime getFeeCalculationAt() {
        return feeCalculationAt;
    }

    public void setFeeCalculationAt(LocalDateTime feeCalculationAt) {
        this.feeCalculationAt = feeCalculationAt;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public FeeRule getAppliedFeeRule() {
        return appliedFeeRule;
    }

    public void setAppliedFeeRule(FeeRule appliedFeeRule) {
        this.appliedFeeRule = appliedFeeRule;
    }

    public Map<String, Object> getFeeCalculationDetails() {
        return feeCalculationDetails;
    }

    public void setFeeCalculationDetails(Map<String, Object> feeCalculationDetails) {
        this.feeCalculationDetails = feeCalculationDetails;
    }

    public LocalDateTime getProcessingStartedAt() {
        return processingStartedAt;
    }

    public void setProcessingStartedAt(LocalDateTime processingStartedAt) {
        this.processingStartedAt = processingStartedAt;
    }

    public LocalDateTime getCompletedAt() {
        return completedAt;
    }

    public void setCompletedAt(LocalDateTime completedAt) {
        this.completedAt = completedAt;
    }

    public LocalDateTime getFailedAt() {
        return failedAt;
    }

    public void setFailedAt(LocalDateTime failedAt) {
        this.failedAt = failedAt;
    }

    public Long getVersion() {
        return version;
    }

    public String getFailureReason() {
        return failureReason;
    }

    public void setFailureReason(String failureReason) {
        this.failureReason = failureReason;
    }

    public List<TransactionEvent> getEvents() {
        return events;
    }

    public void setEvents(List<TransactionEvent> events) {
        this.events = events;
    }

    public List<BalanceReservation> getReservations() {
        return reservations;
    }

    public void setReservations(List<BalanceReservation> reservations) {
        this.reservations = reservations;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Integer getReservationTimeout() {
        return reservationTimeout;
    }

    public void setReservationTimeout(Integer reservationTimeout) {
        this.reservationTimeout = reservationTimeout;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Transaction that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "Transaction{" + "id=" + id + ", status=" + status + ", transactionType=" + getTransactionType()
                + ", amount=" + (request != null ? request.getAmount() : null) + ", feeAmount=" + feeAmount + '}';
    }
}