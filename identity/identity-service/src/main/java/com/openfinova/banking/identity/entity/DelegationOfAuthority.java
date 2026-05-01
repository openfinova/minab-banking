package com.openfinova.banking.identity.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
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
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Records temporary or standing delegation of approval authority between staff users (amount
 * ceiling, transaction type scope, validity window).
 */
@Entity
@Table(name = "identity_delegations", indexes = {
        @Index(name = "idx_delegation_from", columnList = "delegated_from_id"),
        @Index(name = "idx_delegation_to", columnList = "delegated_to_id"),
        @Index(name = "idx_delegation_status", columnList = "status"),
        @Index(name = "idx_delegation_valid", columnList = "valid_from,valid_until") })
public class DelegationOfAuthority {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_from_id", nullable = false)
    private BankingUser delegatedFrom;

    @NotNull
    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "delegated_to_id", nullable = false)
    private BankingUser delegatedTo;

    /**
     * Optional maximum transaction amount this delegation covers (same currency as
     * {@link #currency}). Null means no explicit numeric cap in this record (organizational limits
     * still apply downstream).
     */
    @Column(name = "approval_limit", precision = 19, scale = 4)
    private BigDecimal approvalLimit;

    @Size(max = 3)
    @Column(name = "currency", length = 3)
    private String currency;

    /**
     * Domain-specific type filter, e.g. {@code GL}, {@code LOAN_DISBURSE}, or {@code *} for all
     * supported types.
     */
    @NotNull
    @Size(max = 80)
    @Column(name = "transaction_type", nullable = false, length = 80)
    private String transactionType;

    @NotNull
    @Column(name = "valid_from", nullable = false)
    private LocalDateTime validFrom;

    @Column(name = "valid_until")
    private LocalDateTime validUntil;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DelegationStatus status = DelegationStatus.ACTIVE;

    /**
     * When set, the delegatee exercises authority at least at this GL approval tier (JWT
     * {@code gl_approval_role}).
     */
    @Size(max = 30)
    @Column(name = "acting_gl_approval_role", length = 30)
    private String actingGlApprovalRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected DelegationOfAuthority() {
    }

    public DelegationOfAuthority(BankingUser delegatedFrom, BankingUser delegatedTo, String transactionType,
            LocalDateTime validFrom, LocalDateTime validUntil) {
        this.delegatedFrom = delegatedFrom;
        this.delegatedTo = delegatedTo;
        this.transactionType = transactionType;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public UUID getId() {
        return id;
    }

    public Long getVersion() {
        return version;
    }

    public BankingUser getDelegatedFrom() {
        return delegatedFrom;
    }

    public void setDelegatedFrom(BankingUser v) {
        this.delegatedFrom = v;
    }

    public BankingUser getDelegatedTo() {
        return delegatedTo;
    }

    public void setDelegatedTo(BankingUser v) {
        this.delegatedTo = v;
    }

    public BigDecimal getApprovalLimit() {
        return approvalLimit;
    }

    public void setApprovalLimit(BigDecimal v) {
        this.approvalLimit = v;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String v) {
        this.currency = v;
    }

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String v) {
        this.transactionType = v;
    }

    public LocalDateTime getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDateTime v) {
        this.validFrom = v;
    }

    public LocalDateTime getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDateTime v) {
        this.validUntil = v;
    }

    public DelegationStatus getStatus() {
        return status;
    }

    public void setStatus(DelegationStatus v) {
        this.status = v;
    }

    public String getActingGlApprovalRole() {
        return actingGlApprovalRole;
    }

    public void setActingGlApprovalRole(String v) {
        this.actingGlApprovalRole = v;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
