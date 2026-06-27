package com.openfinova.banking.tan.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.openfinova.banking.common.lib.validation.ValidCurrency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Strong Customer Authentication (SCA) state for a payment awaiting TAN verification.
 *
 * Owned by the tan module, not transaction processing (TP). When a user initiates a payment
 * that requires SCA, this record snapshots the transaction details shown on the TAN app and
 * binds the expected TAN code to {@code amount} and {@code payeeIban}. TP remains the system
 * of record for the payment itself; this entity tracks whether SCA has been satisfied.
 *
 * Lifecycle
 *   Created in {@code PENDING} when the user requests a payment authorization QR
 *   ({@link com.openfinova.banking.tan.service.TanAuthorizationService#buildPaymentQr}).
 *   The TAN app reads the snapshot to display payee and amount, then the user submits a TAN
 *   code ({@link com.openfinova.banking.tan.service.TanAuthorizationService#verifyTan}).
 *   Transitions to {@code SCA_VERIFIED} on successful verification, which may trigger TP to
 *   process the payment. Transitions to {@code EXPIRED} lazily when {@code expiresAt} passes
 *   while still {@code PENDING}.
 *
 * Invariants
 *   At most one row per {@code transactionId}; {@code transactionId} and {@code userId} are
 *   immutable after creation.
 *   Created only while the linked TP transaction is {@code INITIATED}.
 *   {@code tanDeviceId} and {@code verifiedAt} are set together on successful verification.
 *   {@code SCA_VERIFIED} is terminal; re-submission of the same TAN is idempotent.
 *
 * @see TanPendingAuthorizationStatus
 * @see TanDevice
 * @see com.openfinova.banking.tan.service.TanAuthorizationService
 */
@Entity
@Table(name = "tan_pending_authorizations")
@EntityListeners(AuditingEntityListener.class)
public class TanPendingAuthorization {

    /** Surrogate primary key; assigned when the pending authorization is created. */
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /**
     * Identifier of the TP payment awaiting SCA. Unique across all pending authorizations;
     * immutable after insert.
     */
    @Column(name = "transaction_id", nullable = false, unique = true, updatable = false)
    private UUID transactionId;

    /** Identity of the user who initiated the payment; immutable after insert. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /**
     * Payment amount at authorization time. Included in TAN code derivation together with
     * {@code payeeIban} so the code is bound to this specific payment.
     */
    @Column(nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /** ISO 4217 currency code of the payment, copied from the TP transaction snapshot. */
    @Column(nullable = false, length = 3)
    @ValidCurrency
    private String currency;

    /**
     * Destination IBAN shown on the TAN app and used (normalized) when verifying the TAN code.
     * May be a placeholder when payee account details are unavailable at creation time.
     */
    @Column(name = "payee_iban", nullable = false, length = 34)
    private String payeeIban;

    /** Display name of the payee for presentation on the TAN app; optional. */
    @Column(name = "payee_name", length = 255)
    private String payeeName;

    /** Free-text payment reference copied from the TP transaction; optional. */
    @Column(length = 500)
    private String description;

    /**
     * Authorization outcome. Only {@link TanPendingAuthorizationStatus#PENDING} rows accept
     * new TAN submissions; {@link TanPendingAuthorizationStatus#EXPIRED} closes the window.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TanPendingAuthorizationStatus status;

    /**
     * Deadline after which a {@code PENDING} authorization is treated as expired. Derived from
     * configured pending TTL at creation time.
     */
    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    /** Timestamp when TAN verification succeeded; null until {@code status} is {@code SCA_VERIFIED}. */
    @Column(name = "verified_at")
    private Instant verifiedAt;

    /**
     * {@link TanDevice} that submitted the successful TAN code. Set on verification for audit;
     * null while authorization is still pending or expired without verification.
     */
    @Column(name = "tan_device_id")
    private UUID tanDeviceId;

    /** Optimistic-lock version; incremented on every update to detect concurrent modifications. */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    /** Principal that created this record; populated by JPA auditing at insert time. */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /** Instant this record was first persisted; populated by JPA auditing at insert time. */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /** Principal that last modified this record; populated by JPA auditing on each update. */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    /** Instant of the most recent mutation; populated by JPA auditing on each update. */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public String getPayeeIban() {
        return payeeIban;
    }

    public void setPayeeIban(String payeeIban) {
        this.payeeIban = payeeIban;
    }

    public String getPayeeName() {
        return payeeName;
    }

    public void setPayeeName(String payeeName) {
        this.payeeName = payeeName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public TanPendingAuthorizationStatus getStatus() {
        return status;
    }

    public void setStatus(TanPendingAuthorizationStatus status) {
        this.status = status;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(Instant verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public UUID getTanDeviceId() {
        return tanDeviceId;
    }

    public void setTanDeviceId(UUID tanDeviceId) {
        this.tanDeviceId = tanDeviceId;
    }

    public Long getVersion() {
        return version;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
