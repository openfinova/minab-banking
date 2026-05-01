package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.AmortizationType;
import com.openfinova.banking.loan.api.entity.InterestCalculationMethod;
import com.openfinova.banking.loan.api.entity.LoanProductType;
import com.openfinova.banking.loan.api.entity.RepaymentFrequency;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Loan Product entity defining the terms and conditions for loan offerings.
 * Represents a template for creating loan accounts with predefined parameters.
 */
@Entity
@Table(name = "loan_products", indexes = { @Index(name = "idx_loan_products_code", columnList = "product_code"),
        @Index(name = "idx_loan_products_type", columnList = "product_type"),
        @Index(name = "idx_loan_products_active", columnList = "active") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoanProduct {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique product code for identification.
     * Used in loan applications and reporting.
     */
    @NotBlank(message = "Product code is required")
    @Column(name = "product_code", nullable = false, unique = true, length = 50)
    @Size(max = 50, message = "Product code must not exceed 50 characters")
    private String productCode;

    @NotBlank(message = "Product name is required")
    @Column(name = "product_name", nullable = false, length = 200)
    @Size(max = 200, message = "Product name must not exceed 200 characters")
    private String productName;

    @Enumerated(EnumType.STRING)
    @Column(name = "product_type", nullable = false, length = 30)
    @NotNull(message = "Product type is required")
    private LoanProductType productType;

    @Column(length = 1000)
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Minimum loan amount that can be disbursed under this product.
     */
    @NotNull(message = "Minimum amount is required")
    @DecimalMin(value = "0.0", message = "Minimum amount must be positive")
    @Column(name = "min_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal minAmount;

    /**
     * Maximum loan amount that can be disbursed under this product.
     */
    @NotNull(message = "Maximum amount is required")
    @DecimalMin(value = "0.0", message = "Maximum amount must be positive")
    @Column(name = "max_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal maxAmount;

    /**
     * Minimum loan tenor in months.
     */
    @NotNull(message = "Minimum tenor is required")
    @Min(value = 1, message = "Minimum tenor must be at least 1 month")
    @Column(name = "min_tenor_months", nullable = false)
    private Integer minTenorMonths;

    /**
     * Maximum loan tenor in months.
     */
    @NotNull(message = "Maximum tenor is required")
    @Min(value = 1, message = "Maximum tenor must be at least 1 month")
    @Column(name = "max_tenor_months", nullable = false)
    private Integer maxTenorMonths;

    /**
     * Annual interest rate as a percentage.
     * e.g., 12.50 represents 12.50% per annum.
     */
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate must be positive")
    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_calculation_method", nullable = false, length = 30)
    @NotNull(message = "Interest calculation method is required")
    private InterestCalculationMethod interestCalculationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_frequency", nullable = false, length = 30)
    @NotNull(message = "Repayment frequency is required")
    private RepaymentFrequency repaymentFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "amortization_type", nullable = false, length = 30)
    @NotNull(message = "Amortization type is required")
    private AmortizationType amortizationType;

    /**
     * Three-letter ISO currency code.
     * e.g., "USD", "EUR", "GBP".
     */
    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    /**
     * Whether collateral is required for this loan product.
     */
    @Column(name = "collateral_required", nullable = false)
    private Boolean collateralRequired = false;

    /**
     * Whether a guarantor is required for this loan product.
     */
    @Column(name = "guarantor_required", nullable = false)
    private Boolean guarantorRequired = false;

    /**
     * Grace period in days before late fees are applied.
     */
    @Min(value = 0, message = "Grace period cannot be negative")
    @Column(name = "grace_period_days", nullable = false)
    private Integer gracePeriodDays = 0;

    /**
     * Processing fee as a percentage of loan amount.
     */
    @DecimalMin(value = "0.0", message = "Processing fee percentage must be positive")
    @Column(name = "processing_fee_percentage", precision = 5, scale = 2)
    private BigDecimal processingFeePercentage;

    /**
     * Fixed processing fee amount.
     */
    @DecimalMin(value = "0.0", message = "Processing fee must be positive")
    @Column(name = "processing_fee_fixed", precision = 19, scale = 4)
    private BigDecimal processingFeeFixed;

    /**
     * Late payment fee as a percentage of overdue amount.
     */
    @DecimalMin(value = "0.0", message = "Late fee percentage must be positive")
    @Column(name = "late_fee_percentage", precision = 5, scale = 2)
    private BigDecimal lateFeePercentage;

    /**
     * Fixed late payment fee amount.
     */
    @DecimalMin(value = "0.0", message = "Late fee must be positive")
    @Column(name = "late_fee_fixed", precision = 19, scale = 4)
    private BigDecimal lateFeeFixed;

    /**
     * Prepayment penalty as a percentage of prepaid amount.
     */
    @DecimalMin(value = "0.0", message = "Prepayment penalty must be positive")
    @Column(name = "prepayment_penalty_percentage", precision = 5, scale = 2)
    private BigDecimal prepaymentPenaltyPercentage;

    /**
     * Whether this product is active and available for new applications.
     */
    @Column(nullable = false)
    private Boolean active = true;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public LoanProduct() {
    }

    // Business Logic
    public boolean isActive() {
        return Boolean.TRUE.equals(active);
    }

    public void activate() {
        this.active = true;
    }

    public void deactivate() {
        this.active = false;
    }

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

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
