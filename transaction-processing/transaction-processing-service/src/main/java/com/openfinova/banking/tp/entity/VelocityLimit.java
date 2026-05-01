package com.openfinova.banking.tp.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Entity representing velocity limits for transaction monitoring and fraud
 * prevention.
 * Tracks transaction counts and amounts within specific time periods per
 * account and transaction type.
 *
 */
@Entity
@Table(name = "velocity_limits", uniqueConstraints = @UniqueConstraint(columnNames = { "account_id", "transaction_type",
        "limit_period" }))
public class VelocityLimit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_period", nullable = false)
    private VelocityLimitPeriod limitPeriod;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_tier", nullable = false)
    private CustomerTier customerTier;

    @Column(name = "currency", length = 3)
    private String currency; // null means applies to all currencies combined

    @Column(name = "max_count")
    private Integer maxCount;

    @Column(name = "is_active", nullable = false)
    private boolean isActive = true;

    @Column(name = "last_reset_at")
    private LocalDateTime lastResetAt;

    @Column(name = "max_amount", precision = 19, scale = 4)
    private BigDecimal maxAmount;

    @Column(name = "current_count", nullable = false)
    private Integer currentCount = 0;

    @Column(name = "current_amount", precision = 19, scale = 4, nullable = false)
    private BigDecimal currentAmount = BigDecimal.ZERO;

    @Column(name = "period_start", nullable = false)
    private LocalDateTime periodStart;

    @Column(name = "period_end", nullable = false)
    private LocalDateTime periodEnd;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    // Default constructor
    public VelocityLimit() {
    }

    // Constructor for creating new velocity limits
    public VelocityLimit(UUID accountId, TransactionType transactionType, VelocityLimitPeriod limitPeriod,
            CustomerTier customerTier, String currency, Integer maxCount, BigDecimal maxAmount) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.limitPeriod = limitPeriod;
        this.customerTier = customerTier;
        this.currency = currency;
        this.maxCount = maxCount;
        this.maxAmount = maxAmount;
        this.currentCount = 0;
        this.currentAmount = BigDecimal.ZERO;

        // Set period boundaries
        LocalDateTime now = LocalDateTime.now();
        this.periodStart = limitPeriod.getPeriodStart(now);
        this.periodEnd = limitPeriod.getPeriodEnd(now);
    }

    public VelocityLimit(TransactionType transactionType, VelocityLimitPeriod limitPeriod, CustomerTier customerTier,
            String currency) {
        this.transactionType = transactionType;
        this.limitPeriod = limitPeriod;
        this.customerTier = customerTier;
        this.currency = currency;
        this.currentCount = 0;
        this.currentAmount = BigDecimal.ZERO;
        this.isActive = true;

        // Set period boundaries
        LocalDateTime now = LocalDateTime.now();
        this.periodStart = limitPeriod.getPeriodStart(now);
        this.periodEnd = limitPeriod.getPeriodEnd(now);
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public VelocityLimitPeriod getVelocityLimitPeriod() {
        return limitPeriod;
    }

    public void setVelocityLimitPeriod(VelocityLimitPeriod limitPeriod) {
        this.limitPeriod = limitPeriod;
    }

    public CustomerTier getCustomerTier() {
        return customerTier;
    }

    public void setCustomerTier(CustomerTier customerTier) {
        this.customerTier = customerTier;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getMaxCount() {
        return maxCount;
    }

    public void setMaxCount(Integer maxCount) {
        this.maxCount = maxCount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Integer getCurrentCount() {
        return currentCount;
    }

    public void setCurrentCount(Integer currentCount) {
        this.currentCount = currentCount;
    }

    public BigDecimal getCurrentAmount() {
        return currentAmount;
    }

    public void setCurrentAmount(BigDecimal currentAmount) {
        this.currentAmount = currentAmount;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public LocalDateTime getLastResetAt() {
        return lastResetAt;
    }

    public void setLastResetAt(LocalDateTime lastResetAt) {
        this.lastResetAt = lastResetAt;
    }

    public LocalDateTime getPeriodStart() {
        return periodStart;
    }

    public void setPeriodStart(LocalDateTime periodStart) {
        this.periodStart = periodStart;
    }

    public LocalDateTime getPeriodEnd() {
        return periodEnd;
    }

    public void setPeriodEnd(LocalDateTime periodEnd) {
        this.periodEnd = periodEnd;
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

    public Long getVersion() {
        return version;
    }

    // Business methods

    /**
     * Checks if the current period is still active
     *
     * @return true if the current period is active
     */
    public boolean isPeriodActive() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(periodStart) && now.isBefore(periodEnd);
    }

    /**
     * Checks if adding the specified count would exceed the count limit
     *
     * @param additionalCount the count to add
     * @return true if the limit would be exceeded
     */
    public boolean wouldExceedCountLimit(int additionalCount) {
        if (maxCount == null) {
            return false;
        }
        return (currentCount + additionalCount) > maxCount;
    }

    /**
     * Checks if adding the specified amount would exceed the amount limit
     *
     * @param additionalAmount the amount to add
     * @return true if the limit would be exceeded
     */
    public boolean wouldExceedAmountLimit(BigDecimal additionalAmount) {
        if (maxAmount == null || additionalAmount == null) {
            return false;
        }
        return currentAmount.add(additionalAmount).compareTo(maxAmount) > 0;
    }

    /**
     * Adds a transaction to the current counters
     *
     * @param amount the transaction amount
     */
    public void addTransaction(BigDecimal amount) {
        this.currentCount++;
        if (amount != null) {
            this.currentAmount = this.currentAmount.add(amount);
        }
    }

    /**
     * Resets the counters for a new period
     *
     * @param newPeriodStart the start of the new period
     */
    public void resetForNewPeriod(LocalDateTime newPeriodStart) {
        this.currentCount = 0;
        this.currentAmount = BigDecimal.ZERO;
        this.periodStart = newPeriodStart;
        this.periodEnd = limitPeriod.getPeriodEnd(newPeriodStart);
        this.lastResetAt = LocalDateTime.now();
    }

    /**
     * Gets the remaining count capacity
     *
     * @return remaining count, or null if no limit
     */
    public Integer getRemainingCount() {
        if (maxCount == null) {
            return null;
        }
        return Math.max(0, maxCount - currentCount);
    }

    /**
     * Gets the remaining amount capacity
     *
     * @return remaining amount, or null if no limit
     */
    public BigDecimal getRemainingAmount() {
        if (maxAmount == null) {
            return null;
        }
        BigDecimal remaining = maxAmount.subtract(currentAmount);
        return remaining.compareTo(BigDecimal.ZERO) > 0 ? remaining : BigDecimal.ZERO;
    }

    @Override
    public String toString() {
        return "VelocityLimit{" + "id=" + id + ", accountId=" + accountId + ", transactionType=" + transactionType
                + ", limitPeriod=" + limitPeriod + ", customerTier=" + customerTier + ", maxCount=" + maxCount
                + ", maxAmount=" + maxAmount + ", currentCount=" + currentCount + ", currentAmount=" + currentAmount
                + ", periodStart=" + periodStart + ", periodEnd=" + periodEnd + '}';
    }
}