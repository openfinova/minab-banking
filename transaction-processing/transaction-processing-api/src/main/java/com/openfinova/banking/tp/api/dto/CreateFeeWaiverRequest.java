package com.openfinova.banking.tp.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.TransactionType;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to create a fee waiver (account scoped unless isGlobal is true)")
public class CreateFeeWaiverRequest {

    @NotNull(message = "Customer ID is required")
    @Schema(description = "Account ID this waiver applies to (API field name kept for compatibility)", requiredMode = Schema.RequiredMode.REQUIRED)
    private UUID customerId;

    @Size(max = 100)
    @Schema(description = "Short name shown in operations (defaults if blank)")
    private String waiverName;

    @Schema(description = "Longer description")
    private String description;

    @Schema(description = "Legacy alias: used as waiver name when waiverName is blank")
    private String reason;

    @Schema(description = "Restrict waiver to this transaction type; omit for all types")
    private TransactionType transactionType;

    @Schema(description = "Restrict waiver to this customer tier; omit for all tiers")
    private CustomerTier customerTier;

    @Size(max = 50)
    @Schema(description = "Optional campaign / promo code")
    private String campaignCode;

    @Schema(description = "False to create an inactive waiver (default true)")
    private Boolean isActive;

    @Schema(description = "Start of validity; defaults to bank date-time now when omitted")
    private LocalDateTime effectiveFrom;

    @Schema(description = "End of validity (optional)")
    private LocalDateTime effectiveTo;

    @Min(1)
    @Schema(description = "Maximum times this waiver may be used (optional = unlimited)")
    private Integer maxUsageCount;

    @Schema(description = "When true, applies bank-wide (account id may still be stored for audit)")
    private Boolean isGlobal;

    @Schema(description = "Structured matcher/conditions (optional JSON object)")
    private Map<String, Object> conditions;

    @Schema(description = "Additional metadata (optional JSON object)")
    private Map<String, Object> metadata;

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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
}
