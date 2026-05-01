package com.openfinova.banking.tp.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.FeeType;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
import com.openfinova.banking.tp.api.entity.FeeTier;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.converter.FeeTierListConverter;

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
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing fee calculation rules for different transaction types and
 * customer tiers.
 * Supports multiple fee structures including fixed, percentage, and tiered fees.
 */
@Entity
@Table(name = "fee_rules", indexes = { @Index(name = "idx_fee_rules_transaction_type", columnList = "transaction_type"),
        @Index(name = "idx_fee_rules_customer_tier", columnList = "customer_tier"),
        @Index(name = "idx_fee_rules_active", columnList = "is_active"),
        @Index(name = "idx_fee_rules_effective_date", columnList = "effective_from, effective_to") })
public class FeeRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(name = "rule_name", nullable = false, length = 100)
    @NotNull(message = "Fee rule name is required")
    private String ruleName;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    @NotNull(message = "Transaction type is required")
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_tier", nullable = false, length = 20)
    @NotNull(message = "Customer tier is required")
    private CustomerTier customerTier;

    @Column(name = "priority")
    private Integer priority = 0;

    @Column(name = "is_compoundable", nullable = false)
    private boolean isCompoundable = false;

    @Column(name = "gl_revenue_account_id")
    private UUID glRevenueAccountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 50)
    @NotNull(message = "Fee type is required")
    private FeeType feeType;

    @Column(name = "fixed_amount", precision = 19, scale = 4)
    private BigDecimal fixedAmount;

    @Column(name = "currency", length = 3)
    private String currency; // null means all currencies combined

    @Column(name = "percentage_rate", precision = 5, scale = 4)
    @DecimalMin(value = "0.0000", message = "Percentage rate cannot be negative")
    @DecimalMax(value = "1.0000", message = "Percentage rate cannot exceed 100%")
    private BigDecimal percentageRate;

    @Column(name = "minimum_fee", precision = 19, scale = 4)
    private BigDecimal minimumFee;

    @Column(name = "maximum_fee", precision = 19, scale = 4)
    private BigDecimal maximumFee;

    @Convert(converter = FeeTierListConverter.class)
    @Column(name = "tier_configuration", columnDefinition = "jsonb")
    private List<FeeTier> tierConfiguration;

    @Column(name = "min_transaction_amount", precision = 19, scale = 4)
    private BigDecimal minTransactionAmount;

    @Column(name = "max_transaction_amount", precision = 19, scale = 4)
    private BigDecimal maxTransactionAmount;

    @Column(name = "time_based_start")
    private LocalTime timeBasedStart;

    @Column(name = "time_based_end")
    private LocalTime timeBasedEnd;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "is_promotional", nullable = false)
    private Boolean isPromotional = false;

    @Column(name = "effective_from", nullable = false)
    @NotNull(message = "Effective from date is required")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Constructors
    public FeeRule() {
    }

    public FeeRule(String ruleName, TransactionType transactionType, CustomerTier customerTier, FeeType feeType) {
        this.ruleName = ruleName;
        this.transactionType = transactionType;
        this.customerTier = customerTier;
        this.feeType = feeType;
        this.effectiveFrom = LocalDateTime.now();
        this.createdBy = "SYSTEM";
    }

    // Business logic methods

    /**
     * Checks if this rule is currently effective
     *
     * @return true if rule is active and within effective date range
     */
    public boolean isCurrentlyEffective() {
        if (!isActive) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return false;
        }

        return effectiveTo == null || now.isBefore(effectiveTo);
    }

    /**
     * Checks if this rule applies to the given transaction amount
     *
     * @param amount the transaction amount to check
     * @return true if amount is within the rule's range
     */
    public boolean appliesToAmount(BigDecimal amount) {
        if (minTransactionAmount != null && amount.compareTo(minTransactionAmount) < 0) {
            return false;
        }

        return maxTransactionAmount == null || amount.compareTo(maxTransactionAmount) <= 0;
    }

    /**
     * Checks if this rule applies to the current time
     *
     * @return true if current time is within the rule's time range
     */
    public boolean appliesToCurrentTime() {
        if (timeBasedStart == null || timeBasedEnd == null) {
            return true; // No time restrictions
        }

        LocalTime now = LocalTime.now();

        // Handle cases where time range crosses midnight
        if (timeBasedStart.isAfter(timeBasedEnd)) {
            return now.isAfter(timeBasedStart) || now.isBefore(timeBasedEnd);
        } else {
            return !now.isBefore(timeBasedStart) && !now.isAfter(timeBasedEnd);
        }
    }

    /**
     * Validates the fee rule configuration
     *
     * @throws IllegalStateException if configuration is invalid
     */
    public void validateConfiguration() {
        switch (feeType) {
            case FIXED_AMOUNT, FLAT -> {
                if (fixedAmount == null || fixedAmount.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException("Fixed amount must be specified and non-negative for " + feeType);
                }
            }
            case PERCENTAGE -> {
                if (percentageRate == null || percentageRate.compareTo(BigDecimal.ZERO) < 0) {
                    throw new IllegalStateException(
                            "Percentage rate must be specified and non-negative for " + feeType);
                }
            }
            case MINIMUM, MAXIMUM -> {
                if (fixedAmount == null || percentageRate == null) {
                    throw new IllegalStateException(
                            "Both fixed amount and percentage rate must be specified for " + feeType);
                }
            }
            case TIERED -> {
                if (currency == null || currency.isBlank()) {
                    throw new IllegalStateException(
                            "Currency must be specified for tiered fees to ensure threshold accuracy");
                }
                if (tierConfiguration == null || tierConfiguration.isEmpty()) {
                    throw new IllegalStateException("Tier configuration must be specified for tiered fees");
                }
                validateTierConfiguration();
            }
            case NONE -> {
                // No validation needed for waived/promotional fees
            }
        }

        if (minTransactionAmount != null && maxTransactionAmount != null) {
            if (minTransactionAmount.compareTo(maxTransactionAmount) > 0) {
                throw new IllegalStateException("Minimum transaction amount cannot be greater than maximum");
            }
        }
    }

    /**
     * Validates the tier configuration for tiered fees
     *
     * @throws IllegalStateException if tier configuration is invalid
     */
    private void validateTierConfiguration() {
        if (tierConfiguration == null || tierConfiguration.isEmpty()) {
            return;
        }

        BigDecimal previousMaxAmount = BigDecimal.ZERO;
        for (FeeTier tier : tierConfiguration) {
            if (tier.getMinAmount().compareTo(previousMaxAmount) != 0) {
                throw new IllegalStateException("Tier configuration has gaps or overlaps");
            }
            if (tier.getMaxAmount() != null && tier.getMinAmount().compareTo(tier.getMaxAmount()) >= 0) {
                throw new IllegalStateException("Tier min amount must be less than max amount");
            }
            previousMaxAmount = tier.getMaxAmount();
        }
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getRuleName() {
        return ruleName;
    }

    public void setRuleName(String ruleName) {
        this.ruleName = ruleName;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public CustomerTier getCustomerTier() {
        return customerTier;
    }

    public void setCustomerTier(CustomerTier customerTier) {
        this.customerTier = customerTier;
    }

    public FeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(FeeType feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getFixedAmount() {
        return fixedAmount;
    }

    public void setFixedAmount(BigDecimal fixedAmount) {
        this.fixedAmount = fixedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getPercentageRate() {
        return percentageRate;
    }

    public void setPercentageRate(BigDecimal percentageRate) {
        this.percentageRate = percentageRate;
    }

    public BigDecimal getMinimumFee() {
        return minimumFee;
    }

    public void setMinimumFee(BigDecimal minimumFee) {
        this.minimumFee = minimumFee;
    }

    public BigDecimal getMaximumFee() {
        return maximumFee;
    }

    public void setMaximumFee(BigDecimal maximumFee) {
        this.maximumFee = maximumFee;
    }

    public List<FeeTier> getTierConfiguration() {
        return tierConfiguration;
    }

    public void setTierConfiguration(List<FeeTier> tierConfiguration) {
        this.tierConfiguration = tierConfiguration;
    }

    public BigDecimal getMinTransactionAmount() {
        return minTransactionAmount;
    }

    public void setMinTransactionAmount(BigDecimal minTransactionAmount) {
        this.minTransactionAmount = minTransactionAmount;
    }

    public BigDecimal getMaxTransactionAmount() {
        return maxTransactionAmount;
    }

    public void setMaxTransactionAmount(BigDecimal maxTransactionAmount) {
        this.maxTransactionAmount = maxTransactionAmount;
    }

    public LocalTime getTimeBasedStart() {
        return timeBasedStart;
    }

    public void setTimeBasedStart(LocalTime timeBasedStart) {
        this.timeBasedStart = timeBasedStart;
    }

    public LocalTime getTimeBasedEnd() {
        return timeBasedEnd;
    }

    public void setTimeBasedEnd(LocalTime timeBasedEnd) {
        this.timeBasedEnd = timeBasedEnd;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getIsPromotional() {
        return isPromotional;
    }

    public void setIsPromotional(Boolean isPromotional) {
        this.isPromotional = isPromotional;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveTo() {
        return effectiveTo;
    }

    public void setEffectiveTo(LocalDateTime effectiveTo) {
        this.effectiveTo = effectiveTo;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public boolean isCompoundable() {
        return isCompoundable;
    }

    public void setCompoundable(boolean compoundable) {
        isCompoundable = compoundable;
    }

    public UUID getGlRevenueAccountId() {
        return glRevenueAccountId;
    }

    public void setGlRevenueAccountId(UUID glRevenueAccountId) {
        this.glRevenueAccountId = glRevenueAccountId;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof FeeRule feeRule))
            return false;
        return id != null && id.equals(feeRule.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "FeeRule{" + "id=" + id + ", ruleName='" + ruleName + '\'' + ", transactionType=" + transactionType
                + ", customerTier=" + customerTier + ", feeType=" + feeType + ", isActive=" + isActive + '}';
    }
}