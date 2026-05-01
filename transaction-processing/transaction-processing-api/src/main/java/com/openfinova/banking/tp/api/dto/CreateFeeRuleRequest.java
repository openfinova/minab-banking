package com.openfinova.banking.tp.api.dto;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.FeeType;
import com.openfinova.banking.tp.api.entity.TransactionType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

@Schema(description = "Request to create fee rule")
public class CreateFeeRuleRequest {

    @NotNull(message = "Transaction type is required")
    @Schema(description = "Transaction type", required = true)
    private TransactionType transactionType;

    @NotNull(message = "Customer tier is required")
    @Schema(description = "Customer tier", required = true)
    private CustomerTier customerTier;

    @NotNull(message = "Fee type is required")
    @Schema(description = "Fee type", required = true)
    private FeeType feeType;

    @Schema(description = "Fixed fee amount")
    private BigDecimal fixedAmount;

    @Schema(description = "Percentage rate")
    private BigDecimal percentageRate;

    @Schema(description = "Minimum fee")
    private BigDecimal minFee;

    @Schema(description = "Maximum fee")
    private BigDecimal maxFee;

    // Getters and setters
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
}
