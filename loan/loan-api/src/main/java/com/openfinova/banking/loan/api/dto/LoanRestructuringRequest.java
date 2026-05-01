package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.RestructuringType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Request DTO for creating a restructuring request.
 */
public class LoanRestructuringRequest {

    @NotNull(message = "Loan account ID is required")
    private UUID loanAccountId;

    @NotNull(message = "Restructuring type is required")
    private RestructuringType restructuringType;

    @Min(value = 1, message = "New tenor must be at least 1 month")
    private Integer newTenorMonths;

    @DecimalMin(value = "0.0", message = "New interest rate must be positive")
    private BigDecimal newInterestRate;

    @NotBlank(message = "Reason is required")
    private String reason;

    private String requestedBy;

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public RestructuringType getRestructuringType() {
        return restructuringType;
    }

    public void setRestructuringType(RestructuringType restructuringType) {
        this.restructuringType = restructuringType;
    }

    public Integer getNewTenorMonths() {
        return newTenorMonths;
    }

    public void setNewTenorMonths(Integer newTenorMonths) {
        this.newTenorMonths = newTenorMonths;
    }

    public BigDecimal getNewInterestRate() {
        return newInterestRate;
    }

    public void setNewInterestRate(BigDecimal newInterestRate) {
        this.newInterestRate = newInterestRate;
    }

    public String getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(String requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
