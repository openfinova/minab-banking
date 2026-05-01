package com.openfinova.banking.gl.entity;

import com.openfinova.banking.gl.api.entity.ClearingRuleType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Automatic clearing rule configuration for suspense items.
 *
 * BUSINESS CONTEXT:
 * Reduces manual workload by automatically clearing suspense items that match
 * predictable patterns. Common use cases:
 * - Immaterial amounts below threshold → clear to Sundry Income/Expense
 * - Old items (>90 days) → clear to Unclaimed Funds account
 * - Pattern matching (e.g., "REF-\\d{6}") → route to specific accounts
 * - Recurring source systems → apply standing instructions
 *
 * REGULATORY COMPLIANCE:
 * - Requires audit trail of automatic clearing decisions
 * - Senior management approval for rule configuration
 * - Regular review of rule effectiveness
 * - Cannot circumvent proper controls (materiality/aging limits apply)
 */
@Entity
@Table(name = "gl_suspense_clearing_rules", indexes = {
        @Index(name = "idx_clearing_rule_active", columnList = "is_active"),
        @Index(name = "idx_clearing_rule_type", columnList = "rule_type"),
        @Index(name = "idx_clearing_rule_priority", columnList = "priority") })
public class SuspenseClearingRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Descriptive name for the rule.
     */
    @Column(nullable = false, length = 200)
    @NotBlank(message = "Rule name is required")
    @Size(max = 200, message = "Rule name must not exceed 200 characters")
    private String name;

    /**
     * Type of clearing rule.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 50)
    @NotNull(message = "Rule type is required")
    private ClearingRuleType ruleType;

    /**
     * Pattern to match (for PATTERN_MATCH type).
     * Regular expression applied to transaction reference/description.
     */
    @Column(name = "match_pattern", length = 500)
    @Size(max = 500, message = "Match pattern must not exceed 500 characters")
    private String matchPattern;

    /**
     * Maximum amount threshold (for AMOUNT_THRESHOLD type).
     * Items below this amount are auto-cleared.
     */
    @Column(name = "amount_threshold", precision = 19, scale = 4)
    private BigDecimal amountThreshold;

    /**
     * Minimum age in days (for AGE_THRESHOLD type).
     * Items older than this are auto-cleared.
     */
    @Column(name = "age_threshold_days")
    private Integer ageThresholdDays;

    /**
     * Source system filter (for SOURCE_SYSTEM type).
     * Only items from this source system are matched.
     */
    @Column(name = "source_system_filter", length = 100)
    @Size(max = 100, message = "Source system filter must not exceed 100 characters")
    private String sourceSystemFilter;

    /**
     * Target GL account for automatic clearing.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "target_account_id", nullable = false)
    @NotNull(message = "Target account is required")
    private GLAccount targetAccount;

    /**
     * Currency filter (optional - if null, applies to all currencies).
     */
    @Column(length = 3)
    @Size(max = 3, message = "Currency must be 3 characters")
    private String currency;

    /**
     * Rule execution priority (lower number = higher priority).
     * When multiple rules match, highest priority wins.
     */
    @Column(nullable = false)
    @NotNull(message = "Priority is required")
    @Positive(message = "Priority must be positive")
    private Integer priority = 100;

    /**
     * Whether this rule is currently active.
     */
    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * Requires manual approval even when auto-clearing?
     * For high-risk patterns requiring human verification.
     */
    @Column(name = "requires_approval", nullable = false)
    private Boolean requiresApproval = false;

    /**
     * Who approved this clearing rule configuration.
     */
    @Column(name = "approved_by", length = 100)
    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    private String approvedBy;

    /**
     * When this rule was approved.
     */
    @Column(name = "approved_date")
    private Instant approvedDate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", length = 100)
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    // Constructors

    public SuspenseClearingRule() {
    }

    public SuspenseClearingRule(String name, ClearingRuleType ruleType, GLAccount targetAccount) {
        this.name = name;
        this.ruleType = ruleType;
        this.targetAccount = targetAccount;
    }

    // Business Logic Methods

    /**
     * Check if this rule matches a suspense item.
     */
    public boolean matches(SuspenseItem item) {
        if (!isActive) {
            return false;
        }

        // Currency filter
        if (currency != null && !currency.equals(item.getCurrency())) {
            return false;
        }

        // Type-specific matching
        return switch (ruleType) {
            case PATTERN_MATCH -> matchesPattern(item);
            case AMOUNT_THRESHOLD -> matchesAmountThreshold(item);
            case AGE_THRESHOLD -> matchesAgeThreshold(item);
            case SOURCE_SYSTEM -> matchesSourceSystem(item);
            case STANDING_INSTRUCTION -> true; // Always matches if currency filter passes
        };
    }

    private boolean matchesPattern(SuspenseItem item) {
        if (matchPattern == null || matchPattern.isBlank()) {
            return false;
        }
        String reference = item.getExternalReference();
        String description = item.getDescription();
        return (reference != null && reference.matches(matchPattern))
                || (description != null && description.matches(matchPattern));
    }

    private boolean matchesAmountThreshold(SuspenseItem item) {
        return amountThreshold != null && item.getAmount().compareTo(amountThreshold) <= 0;
    }

    private boolean matchesAgeThreshold(SuspenseItem item) {
        return ageThresholdDays != null && item.getAgeDays() >= ageThresholdDays;
    }

    private boolean matchesSourceSystem(SuspenseItem item) {
        return sourceSystemFilter != null && sourceSystemFilter.equals(item.getSourceSystem());
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public ClearingRuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(ClearingRuleType ruleType) {
        this.ruleType = ruleType;
    }

    public String getMatchPattern() {
        return matchPattern;
    }

    public void setMatchPattern(String matchPattern) {
        this.matchPattern = matchPattern;
    }

    public BigDecimal getAmountThreshold() {
        return amountThreshold;
    }

    public void setAmountThreshold(BigDecimal amountThreshold) {
        this.amountThreshold = amountThreshold;
    }

    public Integer getAgeThresholdDays() {
        return ageThresholdDays;
    }

    public void setAgeThresholdDays(Integer ageThresholdDays) {
        this.ageThresholdDays = ageThresholdDays;
    }

    public String getSourceSystemFilter() {
        return sourceSystemFilter;
    }

    public void setSourceSystemFilter(String sourceSystemFilter) {
        this.sourceSystemFilter = sourceSystemFilter;
    }

    public GLAccount getTargetAccount() {
        return targetAccount;
    }

    public void setTargetAccount(GLAccount targetAccount) {
        this.targetAccount = targetAccount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getPriority() {
        return priority;
    }

    public void setPriority(Integer priority) {
        this.priority = priority;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }

    public Boolean getRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(Boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(Instant approvedDate) {
        this.approvedDate = approvedDate;
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

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof SuspenseClearingRule that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "SuspenseClearingRule{" + "id=" + id + ", name='" + name + '\'' + ", ruleType=" + ruleType
                + ", isActive=" + isActive + ", priority=" + priority + '}';
    }
}
