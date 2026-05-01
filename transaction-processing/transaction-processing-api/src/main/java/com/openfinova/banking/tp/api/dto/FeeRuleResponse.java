package com.openfinova.banking.tp.api.dto;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.FeeType;
import com.openfinova.banking.tp.api.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "Fee rule response")
public class FeeRuleResponse {

    @Schema(description = "Rule ID")
    private UUID id;

    @Schema(description = "Transaction type")
    private TransactionType transactionType;

    @Schema(description = "Customer tier")
    private CustomerTier customerTier;

    @Schema(description = "Fee type")
    private FeeType feeType;

    @Schema(description = "Fixed fee amount")
    private BigDecimal fixedAmount;

    @Schema(description = "Percentage rate")
    private BigDecimal percentageRate;

    @Schema(description = "Minimum fee")
    private BigDecimal minFee;

    @Schema(description = "Maximum fee")
    private BigDecimal maxFee;

    @Schema(description = "Is active")
    private Boolean isActive;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public BigDecimal getPercentageRate() {
        return percentageRate;
    }

    public void setPercentageRate(BigDecimal percentageRate) {
        this.percentageRate = percentageRate;
    }

    public BigDecimal getMinFee() {
        return minFee;
    }

    public void setMinFee(BigDecimal minFee) {
        this.minFee = minFee;
    }

    public BigDecimal getMaxFee() {
        return maxFee;
    }

    public void setMaxFee(BigDecimal maxFee) {
        this.maxFee = maxFee;
    }

    public Boolean getIsActive() {
        return isActive;
    }

    public void setIsActive(Boolean isActive) {
        this.isActive = isActive;
    }
}
