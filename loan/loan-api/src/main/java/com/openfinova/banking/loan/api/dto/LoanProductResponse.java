package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.AmortizationType;
import com.openfinova.banking.loan.api.entity.InterestCalculationMethod;
import com.openfinova.banking.loan.api.entity.LoanProductType;
import com.openfinova.banking.loan.api.entity.RepaymentFrequency;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Response DTO for loan product information.
 */
public class LoanProductResponse {

    private UUID id;
    private String productCode;
    private String productName;
    private LoanProductType productType;
    private String description;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Integer minTenorMonths;
    private Integer maxTenorMonths;
    private BigDecimal interestRate;
    private InterestCalculationMethod interestCalculationMethod;
    private RepaymentFrequency repaymentFrequency;
    private AmortizationType amortizationType;
    private String currency;
    private Boolean collateralRequired;
    private Boolean guarantorRequired;
    private Integer gracePeriodDays;
    private BigDecimal processingFeePercentage;
    private BigDecimal processingFeeFixed;
    private BigDecimal lateFeePercentage;
    private BigDecimal lateFeeFixed;
    private BigDecimal prepaymentPenaltyPercentage;
    private Boolean active;
    private Instant createdAt;
    private Instant updatedAt;

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public Boolean getActive() {
        return active;
    }

    public void setActive(Boolean active) {
        this.active = active;
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
}
