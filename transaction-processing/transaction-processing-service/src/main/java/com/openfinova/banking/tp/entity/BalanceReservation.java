package com.openfinova.banking.tp.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
import com.openfinova.banking.tp.api.entity.ReservationStatus;
import com.openfinova.banking.tp.api.entity.ReservationType;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Entity representing a transaction-specific balance reservation on an account.
 * Used to hold funds during transaction processing to prevent overdrafts.
 *
 * ARCHITECTURAL DECISION:
 * BalanceReservation is distinct from AccountHold (in Account module) and serves different purposes:
 *
 * BalanceReservation (TP Module):
 * - Purpose: Short-lived transaction-specific fund reservations
 * - Use Cases: Reserve funds during transaction processing
 * - Lifecycle: Tied to Transaction entity lifecycle (created → confirmed/released)
 * - Duration: Minutes to hours (typically 24 hours max)
 * - Ownership: Transaction Processing domain
 * - Features: Idempotency, optimistic locking, transaction workflow integration
 * - Examples: "Reserve $100 while processing transfer", "Hold during card authorization"
 *
 * AccountHold (Account Module):
 * - Purpose: Long-lived administrative holds on accounts
 * - Use Cases: Court orders, fraud investigations, regulatory compliance
 * - Lifecycle: Independent of transactions, manually or system-triggered
 * - Duration: Days to months
 * - Ownership: Account Management domain
 * - Examples: "Freeze $5,000 per court order", "Fraud investigation hold"
 *
 * BALANCE CALCULATION:
 * Available balance is calculated by AccountBalanceService considering BOTH:
 * - BalanceReservations (this entity) via BalanceReservationService.getTotalReservedAmount()
 * - AccountHolds via AccountHoldService.getTotalHoldAmount()
 */
@Entity
@Table(name = "balance_reservations", indexes = {
        @Index(name = "idx_balance_reservations_account", columnList = "account_id, status"),
        @Index(name = "idx_balance_reservations_expires", columnList = "expires_at"),
        @Index(name = "idx_balance_reservations_status_expiration", columnList = "status, expires_at"),
        @Index(name = "idx_balance_reservations_transaction", columnList = "transaction_id"),
        @Index(name = "idx_balance_reservations_key", columnList = "reservation_key", unique = true) }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_balance_reservations_transaction_id", columnNames = "transaction_id") })
public class BalanceReservation {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    @NotNull(message = "Transaction is required")
    private Transaction transaction;

    @Column(name = "account_id", nullable = false)
    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @Column(name = "reserved_amount", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Reserved amount is required")
    @DecimalMin(value = "0.01", message = "Reserved amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Reserved amount must have at most 15 integer digits and 4 decimal places")
    private BigDecimal reservedAmount;

    @Column(name = "original_amount", precision = 19, scale = 4)
    private BigDecimal originalAmount;

    @Column(name = "currency", length = 3, nullable = false)
    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be exactly 3 characters")
    @Pattern(regexp = "^[A-Z]{3}$", message = "Currency must be a valid 3-letter ISO code")
    private String currency = "USD";

    @Enumerated(EnumType.STRING)
    @Column(name = "reservation_type", nullable = false, length = 20)
    @NotNull(message = "Reservation type is required")
    private ReservationType reservationType;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Reservation status is required")
    private ReservationStatus status = ReservationStatus.ACTIVE;

    @Column(name = "expires_at", nullable = false)
    @NotNull(message = "Expiration time is required")
    @Future(message = "Expiration time must be in the future")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @Column(name = "released_at")
    private LocalDateTime releasedAt;

    @Column(name = "release_reason", length = 255)
    @Size(max = 255, message = "Release reason must not exceed 255 characters")
    private String releaseReason;

    @Column(name = "reservation_reference", length = 255)
    private String reservationReference;

    @Column(name = "reservation_key", unique = true, nullable = false, length = 255)
    @NotBlank(message = "Reservation key is required for idempotency")
    @Size(max = 255, message = "Reservation key must not exceed 255 characters")
    private String reservationKey;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "reservation_metadata", columnDefinition = "jsonb")
    private Map<String, Object> reservationMetadata;

    // Constructors
    public BalanceReservation() {
    }

    public BalanceReservation(Transaction transaction, UUID accountId, BigDecimal reservedAmount, String currency,
            ReservationType reservationType, LocalDateTime expiresAt, String reservationKey,
            String reservationReference) {
        this.transaction = transaction;
        this.accountId = accountId;
        this.reservedAmount = reservedAmount;
        this.originalAmount = reservedAmount;
        this.currency = currency;
        this.reservationType = reservationType;
        this.expiresAt = expiresAt;
        this.reservationKey = reservationKey;
        this.reservationReference = reservationReference;
    }

    // Business logic methods

    /**
     * Releases this reservation with a reason
     *
     * @param reason the reason for release
     * @throws IllegalStateException if reservation is not active
     */
    public void release(String reason, LocalDateTime now) {
        if (!status.isHoldingFunds()) {
            throw new IllegalStateException("Cannot release reservation that is not active");
        }

        this.status = ReservationStatus.RELEASED;
        this.releasedAt = now;
        this.releaseReason = reason;
    }

    /**
     * Converts this reservation to a posting (marks as converted)
     *
     * @throws IllegalStateException if reservation is not active
     */
    public void convertToPosting(LocalDateTime now) {
        if (!status.isHoldingFunds()) {
            throw new IllegalStateException("Cannot convert reservation that is not active");
        }

        this.status = ReservationStatus.CONVERTED;
        this.releasedAt = now;
        this.releaseReason = "Converted to GL posting";
    }

    /**
     * Marks this reservation as EXPIRED. Idempotent: if already not ACTIVE (e.g. RELEASED by
     * failTimedOutTransactions), no-op to avoid conflicts when both schedulers touch the same row.
     */
    public void markExpired(LocalDateTime now) {
        if (!status.isHoldingFunds()) {
            return;
        }
        this.status = ReservationStatus.EXPIRED;
        this.releasedAt = now;
        this.releaseReason = "Reservation timeout expired";
    }

    /**
     * Checks if this reservation has expired
     *
     * @return true if current time is past expiration time
     */
    public boolean hasExpired(LocalDateTime now) {
        return now.isAfter(expiresAt);
    }

    /**
     * Checks if this reservation is currently holding funds
     *
     * @return true if reservation is active and not expired
     */
    public boolean isHoldingFunds(LocalDateTime now) {
        return status.isHoldingFunds() && !hasExpired(now);
    }

    /**
     * Checks if this reservation affects available balance
     *
     * @return true if this reservation reduces available balance
     */
    public boolean affectsAvailableBalance(LocalDateTime now) {
        return isHoldingFunds(now) && reservationType.reducesAvailableBalance();
    }

    /**
     * Gets the effective amount that affects available balance
     *
     * @return reserved amount if it affects balance, zero otherwise
     */
    public BigDecimal getEffectiveAmount(LocalDateTime now) {
        return affectsAvailableBalance(now) ? reservedAmount : BigDecimal.ZERO;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(BigDecimal reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public String getReservationReference() {
        return reservationReference;
    }

    public void setReservationReference(String reservationReference) {
        this.reservationReference = reservationReference;
    }

    public String getReservationKey() {
        return reservationKey;
    }

    public void setReservationKey(String reservationKey) {
        this.reservationKey = reservationKey;
    }

    public Map<String, Object> getReservationMetadata() {
        return reservationMetadata;
    }

    public void setReservationMetadata(Map<String, Object> reservationMetadata) {
        this.reservationMetadata = reservationMetadata;
    }

    public ReservationType getReservationType() {
        return reservationType;
    }

    public void setReservationType(ReservationType reservationType) {
        this.reservationType = reservationType;
    }

    public ReservationStatus getStatus() {
        return status;
    }

    public void setStatus(ReservationStatus status) {
        this.status = status;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }

    public String getReleaseReason() {
        return releaseReason;
    }

    public void setReleaseReason(String releaseReason) {
        this.releaseReason = releaseReason;
    }

    public Long getVersion() {
        return version;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof BalanceReservation that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "BalanceReservation{" + "id=" + id + ", accountId=" + accountId + ", reservedAmount=" + reservedAmount
                + ", reservationType=" + reservationType + ", status=" + status + ", expiresAt=" + expiresAt + '}';
    }
}