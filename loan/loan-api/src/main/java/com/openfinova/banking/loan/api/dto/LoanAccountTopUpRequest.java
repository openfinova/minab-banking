package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a top-up loan.
 */
public class LoanAccountTopUpRequest {

    @NotNull(message = "Top-up amount is required")
    @DecimalMin(value = "0.01", message = "Top-up amount must be greater than 0")
    private BigDecimal topUpAmount;

    public BigDecimal getTopUpAmount() {
        return topUpAmount;
    }

    public void setTopUpAmount(BigDecimal topUpAmount) {
        this.topUpAmount = topUpAmount;
    }
}
