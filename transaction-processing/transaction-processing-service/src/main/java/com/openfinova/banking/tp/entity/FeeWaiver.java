package com.openfinova.banking.tp.entity;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
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
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing fee waivers for promotional campaigns or customer
 * benefits.
 * Allows for temporary or permanent fee reductions/eliminations.
 *
 * Requirements addressed:
 * - Promotional fee waiver support
 */
@Entity
@Table(name = "fee_waivers", indexes = { @Index(name = "idx_fee_waivers_account", columnList = "account_id"),
        @Index(name = "idx_fee_waivers_transaction_type", columnList = "transaction_type"),
        @Index(name = "idx_fee_waivers_active", columnList = "is_active"),
        @Index(name = "idx_fee_waivers_effective_date", columnList = "effective_from, effective_to"),
        @Index(name = "idx_fee_waivers_campaign", columnList = "campaign_code") })
public class FeeWaiver {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id")
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", length = 50)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "customer_tier", length = 20)
    private CustomerTier customerTier;

    @Column(name = "campaign_code", length = 50)
    private String campaignCode;

    @Column(name = "waiver_name", nullable = false, length = 100)
    @NotNull(message = "Waiver name is required")
    private String waiverName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    @Column(name = "effective_from", nullable = false)
    @NotNull(message = "Effective from date is required")
    private LocalDateTime effectiveFrom;

    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    @Column(name = "usage_count", nullable = false)
    private Integer usageCount = 0;

    @Column(name = "max_usage_count")
    private Integer maxUsageCount;

    @Column(name = "is_global", nullable = false)
    private Boolean isGlobal = false;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "conditions", columnDefinition = "jsonb")
    private Map<String, Object> conditions;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Version
    private Long version;

    @Column(name = "created_by", nullable = false, length = 100)
    private String createdBy;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    // Constructors
    public FeeWaiver() {
    }

    public FeeWaiver(String waiverName, String campaignCode) {
        this.waiverName = waiverName;
        this.campaignCode = campaignCode;
        this.effectiveFrom = LocalDateTime.now();
        this.createdBy = "SYSTEM";
    }

    // Business logic methods

    /**
     * Checks if this waiver is currently effective
     *
     * @return true if waiver is active and within effective date range
     */
    public boolean isCurrentlyEffective() {
        if (!isActive) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        if (effectiveFrom != null && now.isBefore(effectiveFrom)) {
            return false;
        }

        if (effectiveTo != null && now.isAfter(effectiveTo)) {
            return false;
        }

        // Check usage limits
        if (maxUsageCount != null && usageCount >= maxUsageCount) {
            return false;
        }

        return true;
    }

    /**
     * Checks if this waiver applies to the given account
     *
     * @param accountId the account ID to check
     * @return true if waiver applies to the account
     */
    public boolean appliesToAccount(UUID accountId) {
        if (isGlobal) {
            return true;
        }

        return this.accountId != null && this.accountId.equals(accountId);
    }

    /**
     * Checks if this waiver applies to the given transaction type
     *
     * @param transactionType the transaction type to check
     * @return true if waiver applies to the transaction type
     */
    public boolean appliesToTransactionType(TransactionType transactionType) {
        return this.transactionType == null || this.transactionType == transactionType;
    }

    /**
     * Checks if this waiver applies to the given customer tier
     *
     * @param customerTier the customer tier to check
     * @return true if waiver applies to the customer tier
     */
    public boolean appliesToCustomerTier(CustomerTier customerTier) {
        return this.customerTier == null || this.customerTier == customerTier;
    }

    /**
     * Increments the usage count for this waiver
     *
     * @throws IllegalStateException if waiver has reached maximum usage
     */
    public void incrementUsage() {
        if (maxUsageCount != null && usageCount >= maxUsageCount) {
            throw new IllegalStateException("Fee waiver has reached maximum usage count");
        }

        this.usageCount++;
    }

    /**
     * Checks if this waiver has usage remaining
     *
     * @return true if waiver can still be used
     */
    public boolean hasUsageRemaining() {
        return maxUsageCount == null || usageCount < maxUsageCount;
    }

    /**
     * Gets the remaining usage count
     *
     * @return remaining usage count, or null if unlimited
     */
    public Integer getRemainingUsage() {
        if (maxUsageCount == null) {
            return null; // Unlimited
        }

        return Math.max(0, maxUsageCount - usageCount);
    }

    // Getters and Setters

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

    public CustomerTier getCustomerTier() {
        return customerTier;
    }

    public void setCustomerTier(CustomerTier customerTier) {
        this.customerTier = customerTier;
    }

    public String getCampaignCode() {
        return campaignCode;
    }

    public void setCampaignCode(String campaignCode) {
        this.campaignCode = campaignCode;
    }

    public String getWaiverName() {
        return waiverName;
    }

    public void setWaiverName(String waiverName) {
        this.waiverName = waiverName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
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

    public Integer getUsageCount() {
        return usageCount;
    }

    public void setUsageCount(Integer usageCount) {
        this.usageCount = usageCount;
    }

    public Integer getMaxUsageCount() {
        return maxUsageCount;
    }

    public void setMaxUsageCount(Integer maxUsageCount) {
        this.maxUsageCount = maxUsageCount;
    }

    public Boolean getIsGlobal() {
        return isGlobal;
    }

    public void setIsGlobal(Boolean isGlobal) {
        this.isGlobal = isGlobal;
    }

    public Map<String, Object> getConditions() {
        return conditions;
    }

    public void setConditions(Map<String, Object> conditions) {
        this.conditions = conditions;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
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
        if (!(o instanceof FeeWaiver feeWaiver))
            return false;
        return id != null && id.equals(feeWaiver.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "FeeWaiver{" + "id=" + id + ", waiverName='" + waiverName + '\'' + ", campaignCode='" + campaignCode
                + '\'' + ", transactionType=" + transactionType + ", customerTier=" + customerTier + ", isActive="
                + isActive + ", usageCount=" + usageCount + ", maxUsageCount=" + maxUsageCount + '}';
    }
}