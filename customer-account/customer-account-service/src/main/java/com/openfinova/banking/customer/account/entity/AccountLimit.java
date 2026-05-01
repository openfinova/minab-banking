package com.openfinova.banking.customer.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

/**
 * Account Limit entity representing transaction and balance constraints
 * applied to customer accounts. Supports both regulatory and business limits
 * with effective date ranges and override capabilities.
 */
@Entity
@Table(name = "account_limits", indexes = {
        @Index(name = "idx_account_limits_account", columnList = "customer_account_id"),
        @Index(name = "idx_account_limits_type", columnList = "limit_type"),
        @Index(name = "idx_account_limits_period", columnList = "limit_period"),
        @Index(name = "idx_account_limits_effective", columnList = "effective_from, effective_until"),
        @Index(name = "idx_account_limits_regulatory", columnList = "is_regulatory") })
public class AccountLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false)
    private Account customerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_type", nullable = false, length = 30)
    @NotNull(message = "Limit type is required")
    private com.openfinova.banking.customer.account.api.entity.LimitType limitType;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_period", nullable = false, length = 20)
    @NotNull(message = "Limit period is required")
    private com.openfinova.banking.customer.account.api.entity.LimitPeriod limitPeriod;

    /**
     * Maximum cumulative amount allowed within the period.
     * e.g., Max $5000 withdrawal per day.
     */
    @Column(name = "max_amount", precision = 19, scale = 4)
    @DecimalMin(value = "0.0000", message = "Maximum amount must be non-negative")
    private BigDecimal maxAmount;

    /**
     * Maximum number of transactions allowed within the period.
     * e.g., Max 10 transfers per day.
     */
    @Column(name = "max_count")
    @Min(value = 0, message = "Maximum count must be non-negative")
    private Integer maxCount;

    /**
     * Minimum transaction amount or minimum balance required.
     */
    @Column(name = "min_amount", precision = 19, scale = 4)
    @DecimalMin(value = "0.0000", message = "Minimum amount must be non-negative")
    private BigDecimal minAmount;

    /**
     * Indicates if this limit is mandated by regulation (e.g., AML laws).
     * If true, it generally cannot be overridden by staff.
     */
    @Column(name = "is_regulatory", nullable = false)
    private Boolean isRegulatory = false;

    /**
     * Indicates if this limit can be temporarily overridden by authorized staff.
     */
    @Column(name = "override_allowed", nullable = false)
    private Boolean overrideAllowed = true;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "regulatory_reference", length = 255)
    private String regulatoryReference;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    /**
     * Date/Time from when this limit starts applying.
     */
    @Column(name = "effective_from", nullable = false)
    private Instant effectiveFrom;

    /**
     * Date/Time when this limit expires.
     * If null, the limit is effective indefinitely.
     */
    @Column(name = "effective_until")
    private Instant effectiveUntil;

    public AccountLimit() {
        this.effectiveFrom = Instant.now();
    }

    public AccountLimit(Account customerAccount, com.openfinova.banking.customer.account.api.entity.LimitType limitType,
            com.openfinova.banking.customer.account.api.entity.LimitPeriod limitPeriod, String createdBy) {
        this();
        this.customerAccount = customerAccount;
        this.limitType = limitType;
        this.limitPeriod = limitPeriod;
        this.createdBy = createdBy;
    }

    /**
     * Checks if this limit is currently effective.
     *
     * @return true if the limit is within its effective date range
     */
    public boolean isEffective() {
        Instant now = Instant.now();
        return !now.isBefore(effectiveFrom) && (effectiveUntil == null || !now.isAfter(effectiveUntil));
    }

    /**
     * Checks if this limit can be overridden.
     *
     * @return true if the limit allows overrides and is not regulatory
     */
    public boolean canOverride() {
        return Boolean.TRUE.equals(overrideAllowed) && !Boolean.TRUE.equals(isRegulatory);
    }

    /**
     * Checks if this is a regulatory limit.
     *
     * @return true if this is a regulatory limit
     */
    public boolean isRegulatoryLimit() {
        return Boolean.TRUE.equals(isRegulatory);
    }

    /**
     * Validates that the limit has appropriate amount or count constraints.
     *
     * @throws IllegalArgumentException if the limit configuration is invalid
     */
    public void validateLimitConstraints() {
        if (maxAmount == null && maxCount == null && minAmount == null) {
            throw new IllegalArgumentException(
                    "At least one limit constraint (maxAmount, maxCount, or minAmount) must be specified");
        }

        if (maxAmount != null && minAmount != null && maxAmount.compareTo(minAmount) < 0) {
            throw new IllegalArgumentException("Maximum amount cannot be less than minimum amount");
        }

        if (limitType.isBalanceLimit() && maxCount != null) {
            throw new IllegalArgumentException("Balance limits cannot have count constraints");
        }

        if (limitType.isTransactionLimit() && minAmount != null
                && limitType != com.openfinova.banking.customer.account.api.entity.LimitType.MINIMUM_BALANCE) {
            throw new IllegalArgumentException("Transaction limits typically do not have minimum amount constraints");
        }
    }

    /**
     * Sets this limit as a regulatory limit with reference.
     *
     * @param regulatoryReference the regulatory reference or citation
     */
    public void setAsRegulatoryLimit(String regulatoryReference) {
        this.isRegulatory = true;
        this.overrideAllowed = false;
        this.regulatoryReference = regulatoryReference;
    }

    /**
     * Expires this limit by setting the effective until date.
     *
     * @param expiredBy the user expiring the limit
     */
    public void expire(String expiredBy) {
        this.effectiveUntil = Instant.now();
        this.updatedBy = expiredBy;
    }

    /**
     * Extends the effective period of this limit.
     *
     * @param newEffectiveUntil the new expiration date
     * @param extendedBy        the user extending the limit
     */
    public void extendEffectivePeriod(Instant newEffectiveUntil, String extendedBy) {
        if (newEffectiveUntil != null && newEffectiveUntil.isBefore(Instant.now())) {
            throw new IllegalArgumentException("Cannot extend limit to a past date");
        }
        this.effectiveUntil = newEffectiveUntil;
        this.updatedBy = extendedBy;
    }

    // Getters and Setters
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

    public com.openfinova.banking.customer.account.api.entity.LimitType getLimitType() {
        return limitType;
    }

    public void setLimitType(com.openfinova.banking.customer.account.api.entity.LimitType limitType) {
        this.limitType = limitType;
    }

    public com.openfinova.banking.customer.account.api.entity.LimitPeriod getLimitPeriod() {
        return limitPeriod;
    }

    public void setLimitPeriod(com.openfinova.banking.customer.account.api.entity.LimitPeriod limitPeriod) {
        this.limitPeriod = limitPeriod;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public Boolean getIsRegulatory() {
        return isRegulatory;
    }

    public void setIsRegulatory(Boolean isRegulatory) {
        this.isRegulatory = isRegulatory;
    }

    public Boolean getOverrideAllowed() {
        return overrideAllowed;
    }

    public void setOverrideAllowed(Boolean overrideAllowed) {
        this.overrideAllowed = overrideAllowed;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getRegulatoryReference() {
        return regulatoryReference;
    }

    public void setRegulatoryReference(String regulatoryReference) {
        this.regulatoryReference = regulatoryReference;
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

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Instant getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(Instant effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public Instant getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(Instant effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (o == null || getClass() != o.getClass())
            return false;
        AccountLimit that = (AccountLimit) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "AccountLimit{" + "id=" + id + ", limitType=" + limitType + ", limitPeriod=" + limitPeriod
                + ", maxAmount=" + maxAmount + ", maxCount=" + maxCount + ", isRegulatory=" + isRegulatory + '}';
    }
}