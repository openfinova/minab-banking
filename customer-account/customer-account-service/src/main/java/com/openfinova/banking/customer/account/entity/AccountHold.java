package com.openfinova.banking.customer.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing an administrative hold or block on customer account funds.
 *
 * ARCHITECTURAL DECISION:
 * AccountHold is distinct from BalanceReservation (in TP module) and serves different purposes:
 *
 * AccountHold (Account Module):
 * - Purpose: Long-lived administrative holds on accounts
 * - Use Cases: Court orders, fraud investigations, regulatory compliance, card authorizations
 * - Lifecycle: Independent of transactions, manually or system-triggered
 * - Duration: Days to months
 * - Ownership: Account Management domain
 * - Examples: "Freeze $5,000 per court order", "Fraud investigation hold"
 *
 * BalanceReservation (TP Module):
 * - Purpose: Short-lived transaction-specific fund reservations
 * - Use Cases: Reserve funds during transaction processing
 * - Lifecycle: Tied to Transaction entity lifecycle (created → confirmed/released)
 * - Duration: Minutes to hours
 * - Ownership: Transaction Processing domain
 * - Examples: "Reserve $100 while processing transfer", "Hold during card authorization"
 *
 * BALANCE CALCULATION:
 * Available balance is calculated by AccountBalanceService considering BOTH:
 * - AccountHolds (this entity) via AccountHoldService.getTotalHoldAmount()
 * - BalanceReservations via BalanceReservationService.getTotalReservedAmount()
 *
 * This service does NOT directly manipulate account balances - it only manages hold records.
 * Balance calculations are centralized in AccountBalanceService for consistency.
 */
@Entity
@Table(name = "account_holds", indexes = {
        @Index(name = "idx_account_holds_account", columnList = "customer_account_id"),
        @Index(name = "idx_account_holds_status", columnList = "status"),
        @Index(name = "idx_account_holds_expires", columnList = "expires_at") })
public class AccountHold {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false)
    private Account customerAccount;

    /**
     * The amount of funds to reserve/block.
     * This amount is subtracted from the available balance but remains in the
     * ledger balance until settlement.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be positive")
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    private String currency;

    /**
     * External reference ID (e.g., Card Authorization Code, Court Order Number).
     */
    @Column(name = "reference_id", length = 100)
    private String referenceId; // External reference (e.g., card auth code)

    @Column(name = "reason", length = 255)
    private String reason;

    /**
     * Current status of the hold.
     * ACTIVE: Funds are blocked.
     * RELEASED: Hold removed, funds available.
     * EXPIRED: Hold timed out (auto-release).
     * SETTLED: Transaction posted, hold consumed.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private com.openfinova.banking.customer.account.api.entity.HoldStatus status = com.openfinova.banking.customer.account.api.entity.HoldStatus.ACTIVE;

    /**
     * Date and time when this hold automatically expires.
     * If null, the hold is indefinite (until manually released).
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    public AccountHold() {
    }

    public AccountHold(Account account, BigDecimal amount, String currency, String reason) {
        this.customerAccount = account;
        this.amount = amount;
        this.currency = currency;
        this.reason = reason;
    }

    public boolean isActive() {
        return status == com.openfinova.banking.customer.account.api.entity.HoldStatus.ACTIVE
                && (expiresAt == null || LocalDateTime.now().isBefore(expiresAt));
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Account getCustomerAccount() {
        return customerAccount;
    }

    public void setCustomerAccount(Account customerAccount) {
        this.customerAccount = customerAccount;
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

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public com.openfinova.banking.customer.account.api.entity.HoldStatus getStatus() {
        return status;
    }

    public void setStatus(com.openfinova.banking.customer.account.api.entity.HoldStatus status) {
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
}
