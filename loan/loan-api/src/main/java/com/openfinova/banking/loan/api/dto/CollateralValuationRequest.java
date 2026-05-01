package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for updating collateral valuation.
 */
public class CollateralValuationRequest {

    @NotNull(message = "Valuation amount is required")
    @DecimalMin(value = "0.01", message = "Valuation amount must be greater than 0")
    private BigDecimal valuationAmount;

    @NotNull(message = "Valuation date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate valuationDate;

    public BigDecimal getValuationAmount() {
        return valuationAmount;
    }

    public void setValuationAmount(BigDecimal valuationAmount) {
        this.valuationAmount = valuationAmount;
    }

    public LocalDate getValuationDate() {
        return valuationDate;
    }

    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }
}
