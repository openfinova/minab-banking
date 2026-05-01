package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating schedule payment amounts.
 */
public class LoanSchedulePaymentUpdateRequest {

    @NotNull(message = "Principal paid is required")
    @DecimalMin(value = "0.0", message = "Principal paid must be positive")
    private BigDecimal principalPaid;

    @NotNull(message = "Interest paid is required")
    @DecimalMin(value = "0.0", message = "Interest paid must be positive")
    private BigDecimal interestPaid;

    @NotNull(message = "Fees paid is required")
    @DecimalMin(value = "0.0", message = "Fees paid must be positive")
    private BigDecimal feesPaid;

    @NotNull(message = "Penalties paid is required")
    @DecimalMin(value = "0.0", message = "Penalties paid must be positive")
    private BigDecimal penaltiesPaid;

    public BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    public void setPrincipalPaid(BigDecimal principalPaid) {
        this.principalPaid = principalPaid;
    }

    public BigDecimal getInterestPaid() {
        return interestPaid;
    }

    public void setInterestPaid(BigDecimal interestPaid) {
        this.interestPaid = interestPaid;
    }

    public BigDecimal getFeesPaid() {
        return feesPaid;
    }

    public void setFeesPaid(BigDecimal feesPaid) {
        this.feesPaid = feesPaid;
    }

    public BigDecimal getPenaltiesPaid() {
        return penaltiesPaid;
    }

    public void setPenaltiesPaid(BigDecimal penaltiesPaid) {
        this.penaltiesPaid = penaltiesPaid;
    }
}
