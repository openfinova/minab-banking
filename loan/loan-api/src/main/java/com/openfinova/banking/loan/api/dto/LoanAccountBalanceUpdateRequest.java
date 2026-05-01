package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating outstanding balances.
 */
public class LoanAccountBalanceUpdateRequest {

    @NotNull(message = "Principal delta is required")
    private BigDecimal principalDelta;

    @NotNull(message = "Interest delta is required")
    private BigDecimal interestDelta;

    @NotNull(message = "Fees delta is required")
    private BigDecimal feesDelta;

    @NotNull(message = "Penalties delta is required")
    private BigDecimal penaltiesDelta;

    public BigDecimal getPrincipalDelta() {
        return principalDelta;
    }

    public void setPrincipalDelta(BigDecimal principalDelta) {
        this.principalDelta = principalDelta;
    }

    public BigDecimal getInterestDelta() {
        return interestDelta;
    }

    public void setInterestDelta(BigDecimal interestDelta) {
        this.interestDelta = interestDelta;
    }

    public BigDecimal getFeesDelta() {
        return feesDelta;
    }

    public void setFeesDelta(BigDecimal feesDelta) {
        this.feesDelta = feesDelta;
    }

    public BigDecimal getPenaltiesDelta() {
        return penaltiesDelta;
    }

    public void setPenaltiesDelta(BigDecimal penaltiesDelta) {
        this.penaltiesDelta = penaltiesDelta;
    }
}
