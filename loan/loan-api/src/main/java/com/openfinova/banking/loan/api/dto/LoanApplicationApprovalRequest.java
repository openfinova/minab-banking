package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for approving a loan application.
 */
public class LoanApplicationApprovalRequest {

    @NotNull(message = "Approved amount is required")
    @DecimalMin(value = "0.01", message = "Approved amount must be greater than 0")
    private BigDecimal approvedAmount;

    @NotNull(message = "Approved tenor is required")
    @Min(value = 1, message = "Approved tenor must be at least 1 month")
    private Integer approvedTenorMonths;

    @NotNull(message = "Approved interest rate is required")
    @DecimalMin(value = "0.0", message = "Approved interest rate must be positive")
    private BigDecimal approvedInterestRate;

    @NotNull(message = "Guarantors required is required")
    @Min(value = 0, message = "Guarantors required cannot be negative")
    private Integer guarantorsRequired;

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public Integer getApprovedTenorMonths() {
        return approvedTenorMonths;
    }

    public void setApprovedTenorMonths(Integer approvedTenorMonths) {
        this.approvedTenorMonths = approvedTenorMonths;
    }

    public BigDecimal getApprovedInterestRate() {
        return approvedInterestRate;
    }

    public void setApprovedInterestRate(BigDecimal approvedInterestRate) {
        this.approvedInterestRate = approvedInterestRate;
    }

    public Integer getGuarantorsRequired() {
        return guarantorsRequired;
    }

    public void setGuarantorsRequired(Integer guarantorsRequired) {
        this.guarantorsRequired = guarantorsRequired;
    }
}
