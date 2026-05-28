package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * DTO representing the detailed result of fee calculation.
 * Provides breakdown of fee components and applied waivers.
 */
public class FeeCalculationResult {

    private BigDecimal baseFee;
    private BigDecimal adjustedFee;
    private BigDecimal totalFee;
    private List<FeeComponent> feeComponents;
    private List<AppliedWaiver> appliedWaivers;
    private String calculationMethod;
    private LocalDateTime calculationTimestamp;

    // Constructors
    public FeeCalculationResult() {
    }

    public FeeCalculationResult(BigDecimal baseFee, BigDecimal adjustedFee, BigDecimal totalFee) {
        this();
        this.baseFee = baseFee;
        this.adjustedFee = adjustedFee;
        this.totalFee = totalFee;
    }

    // Nested classes for fee breakdown
    public static class FeeComponent {
        private String componentName;
        private String componentType;
        private BigDecimal amount;
        private String description;

        public FeeComponent() {
        }

        public FeeComponent(String componentName, String componentType, BigDecimal amount, String description) {
            this.componentName = componentName;
            this.componentType = componentType;
            this.amount = amount;
            this.description = description;
        }

        // Getters and setters
        public String getComponentName() {
            return componentName;
        }

        public void setComponentName(String componentName) {
            this.componentName = componentName;
        }

        public String getComponentType() {
            return componentType;
        }

        public void setComponentType(String componentType) {
            this.componentType = componentType;
        }

        public BigDecimal getAmount() {
            return amount;
        }

        public void setAmount(BigDecimal amount) {
            this.amount = amount;
        }

        public String getDescription() {
            return description;
        }

        public void setDescription(String description) {
            this.description = description;
        }
    }

    public static class AppliedWaiver {
        private UUID waiverId;
        private String waiverName;
        private String campaignCode;
        private BigDecimal waiverAmount;
        private String waiverType;

        public AppliedWaiver() {
        }

        public AppliedWaiver(UUID waiverId, String waiverName, String campaignCode, BigDecimal waiverAmount,
                String waiverType) {
            this.waiverId = waiverId;
            this.waiverName = waiverName;
            this.campaignCode = campaignCode;
            this.waiverAmount = waiverAmount;
            this.waiverType = waiverType;
        }

        // Getters and setters
        public UUID getWaiverId() {
            return waiverId;
        }

        public void setWaiverId(UUID waiverId) {
            this.waiverId = waiverId;
        }

        public String getWaiverName() {
            return waiverName;
        }

        public void setWaiverName(String waiverName) {
            this.waiverName = waiverName;
        }

        public String getCampaignCode() {
            return campaignCode;
        }

        public void setCampaignCode(String campaignCode) {
            this.campaignCode = campaignCode;
        }

        public BigDecimal getWaiverAmount() {
            return waiverAmount;
        }

        public void setWaiverAmount(BigDecimal waiverAmount) {
            this.waiverAmount = waiverAmount;
        }

        public String getWaiverType() {
            return waiverType;
        }

        public void setWaiverType(String waiverType) {
            this.waiverType = waiverType;
        }
    }

    // Getters and setters
    public BigDecimal getBaseFee() {
        return baseFee;
    }

    public void setBaseFee(BigDecimal baseFee) {
        this.baseFee = baseFee;
    }

    public BigDecimal getAdjustedFee() {
        return adjustedFee;
    }

    public void setAdjustedFee(BigDecimal adjustedFee) {
        this.adjustedFee = adjustedFee;
    }

    public BigDecimal getTotalFee() {
        return totalFee;
    }

    public void setTotalFee(BigDecimal totalFee) {
        this.totalFee = totalFee;
    }

    public List<FeeComponent> getFeeComponents() {
        return feeComponents;
    }

    public void setFeeComponents(List<FeeComponent> feeComponents) {
        this.feeComponents = feeComponents;
    }

    public List<AppliedWaiver> getAppliedWaivers() {
        return appliedWaivers;
    }

    public void setAppliedWaivers(List<AppliedWaiver> appliedWaivers) {
        this.appliedWaivers = appliedWaivers;
    }

    public String getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(String calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public LocalDateTime getCalculationTimestamp() {
        return calculationTimestamp;
    }

    public void setCalculationTimestamp(LocalDateTime calculationTimestamp) {
        this.calculationTimestamp = calculationTimestamp;
    }

    @Override
    public String toString() {
        return "FeeCalculationResult{" + "baseFee=" + baseFee + ", adjustedFee=" + adjustedFee + ", totalFee="
                + totalFee + ", calculationMethod='" + calculationMethod + '\'' + ", calculationTimestamp="
                + calculationTimestamp + '}';
    }
}