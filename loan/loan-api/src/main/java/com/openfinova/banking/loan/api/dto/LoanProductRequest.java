package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.AmortizationType;
import com.openfinova.banking.loan.api.entity.InterestCalculationMethod;
import com.openfinova.banking.loan.api.entity.LoanProductType;
import com.openfinova.banking.loan.api.entity.RepaymentFrequency;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;

/**
 * Request DTO for creating or updating a loan product.
 */
public class LoanProductRequest {

    @NotBlank(message = "Product code is required")
    @Size(max = 50, message = "Product code must not exceed 50 characters")
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String productName;

    @NotNull(message = "Product type is required")
    private LoanProductType productType;

    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "0.0", message = "Minimum amount must be positive")
    private BigDecimal minAmount;

    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "0.0", message = "Maximum amount must be positive")
    private BigDecimal maxAmount;

    @NotNull(message = "Minimum tenor is required")
    @Min(value = 1, message = "Minimum tenor must be at least 1 month")
    private Integer minTenorMonths;

    @NotNull(message = "Maximum tenor is required")
    @Min(value = 1, message = "Maximum tenor must be at least 1 month")
    private Integer maxTenorMonths;

    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate must be positive")
    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
    private BigDecimal interestRate;

    @NotNull(message = "Interest calculation method is required")
    private InterestCalculationMethod interestCalculationMethod;

    @NotNull(message = "Repayment frequency is required")
    private RepaymentFrequency repaymentFrequency;

    @NotNull(message = "Amortization type is required")
    private AmortizationType amortizationType;

    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    private Boolean collateralRequired = false;

    private Boolean guarantorRequired = false;

    @Min(value = 0, message = "Grace period cannot be negative")
    private Integer gracePeriodDays = 0;

    @DecimalMin(value = "0.0", message = "Processing fee percentage must be positive")
    private BigDecimal processingFeePercentage;

    @DecimalMin(value = "0.0", message = "Processing fee must be positive")
    private BigDecimal processingFeeFixed;

    @DecimalMin(value = "0.0", message = "Late fee percentage must be positive")
    private BigDecimal lateFeePercentage;

    @DecimalMin(value = "0.0", message = "Late fee must be positive")
    private BigDecimal lateFeeFixed;

    @DecimalMin(value = "0.0", message = "Prepayment penalty must be positive")
    private BigDecimal prepaymentPenaltyPercentage;

    @AssertTrue(message = "Minimum amount must be less than or equal to maximum amount")
    public boolean isAmountRangeValid() {
        if (minAmount == null || maxAmount == null) {
            return true;
        }
        return minAmount.compareTo(maxAmount) <= 0;
    }

    @AssertTrue(message = "Minimum tenor must be less than or equal to maximum tenor")
    public boolean isTenorRangeValid() {
        if (minTenorMonths == null || maxTenorMonths == null) {
            return true;
        }
        return minTenorMonths <= maxTenorMonths;
    }

    // Getters and Setters
    public String getProductCode() {
        return productCode;
    }

    public void setProductCode(String productCode) {
        this.productCode = productCode;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public LoanProductType getProductType() {
        return productType;
    }

    public void setProductType(LoanProductType productType) {
        this.productType = productType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Integer getMinTenorMonths() {
        return minTenorMonths;
    }

    public void setMinTenorMonths(Integer minTenorMonths) {
        this.minTenorMonths = minTenorMonths;
    }

    public Integer getMaxTenorMonths() {
        return maxTenorMonths;
    }

    public void setMaxTenorMonths(Integer maxTenorMonths) {
        this.maxTenorMonths = maxTenorMonths;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public InterestCalculationMethod getInterestCalculationMethod() {
        return interestCalculationMethod;
    }

    public void setInterestCalculationMethod(InterestCalculationMethod interestCalculationMethod) {
        this.interestCalculationMethod = interestCalculationMethod;
    }

    public RepaymentFrequency getRepaymentFrequency() {
        return repaymentFrequency;
    }

    public void setRepaymentFrequency(RepaymentFrequency repaymentFrequency) {
        this.repaymentFrequency = repaymentFrequency;
    }

    public AmortizationType getAmortizationType() {
        return amortizationType;
    }

    public void setAmortizationType(AmortizationType amortizationType) {
        this.amortizationType = amortizationType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getCollateralRequired() {
        return collateralRequired;
    }

    public void setCollateralRequired(Boolean collateralRequired) {
        this.collateralRequired = collateralRequired;
    }

    public Boolean getGuarantorRequired() {
        return guarantorRequired;
    }

    public void setGuarantorRequired(Boolean guarantorRequired) {
        this.guarantorRequired = guarantorRequired;
    }

    public Integer getGracePeriodDays() {
        return gracePeriodDays;
    }

    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }

    public BigDecimal getProcessingFeePercentage() {
        return processingFeePercentage;
    }

    public void setProcessingFeePercentage(BigDecimal processingFeePercentage) {
        this.processingFeePercentage = processingFeePercentage;
    }

    public BigDecimal getProcessingFeeFixed() {
        return processingFeeFixed;
    }

    public void setProcessingFeeFixed(BigDecimal processingFeeFixed) {
        this.processingFeeFixed = processingFeeFixed;
    }

    public BigDecimal getLateFeePercentage() {
        return lateFeePercentage;
    }

    public void setLateFeePercentage(BigDecimal lateFeePercentage) {
        this.lateFeePercentage = lateFeePercentage;
    }

    public BigDecimal getLateFeeFixed() {
        return lateFeeFixed;
    }

    public void setLateFeeFixed(BigDecimal lateFeeFixed) {
        this.lateFeeFixed = lateFeeFixed;
    }

    public BigDecimal getPrepaymentPenaltyPercentage() {
        return prepaymentPenaltyPercentage;
    }

    public void setPrepaymentPenaltyPercentage(BigDecimal prepaymentPenaltyPercentage) {
        this.prepaymentPenaltyPercentage = prepaymentPenaltyPercentage;
    }
}
