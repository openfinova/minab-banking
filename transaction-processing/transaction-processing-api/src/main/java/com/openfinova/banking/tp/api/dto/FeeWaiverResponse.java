package com.openfinova.banking.tp.api.dto;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Fee waiver response")
public class FeeWaiverResponse {

    @Schema(description = "Waiver ID")
    private UUID id;

    @Schema(description = "Account ID (legacy field name customerId in JSON)")
    private UUID customerId;

    @Schema(description = "Same as waiverName; retained for older clients")
    private String reason;

    @Schema(description = "Short name")
    private String waiverName;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Transaction type filter")
    private String transactionType;

    @Schema(description = "Customer tier filter")
    private String customerTier;

    @Schema(description = "Campaign code")
    private String campaignCode;

    @Schema(description = "Active flag")
    private Boolean isActive;

    @Schema(description = "Validity start")
    private LocalDateTime effectiveFrom;

    @Schema(description = "Validity end")
    private LocalDateTime effectiveTo;

    @Schema(description = "Max usage cap")
    private Integer maxUsageCount;

    @Schema(description = "Bank-global waiver")
    private Boolean isGlobal;

    @Schema(description = "Conditions payload")
    private Map<String, Object> conditions;

    @Schema(description = "Metadata payload")
    private Map<String, Object> metadata;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
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

    public String getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(String transactionType) {
        this.transactionType = transactionType;
    }

    public String getCustomerTier() {
        return customerTier;
    }

    public void setCustomerTier(String customerTier) {
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
