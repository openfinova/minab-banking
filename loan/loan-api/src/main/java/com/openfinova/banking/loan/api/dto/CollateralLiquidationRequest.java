package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for liquidating collateral.
 */
public class CollateralLiquidationRequest {

    @NotNull(message = "Liquidation amount is required")
    @DecimalMin(value = "0.01", message = "Liquidation amount must be greater than 0")
    private BigDecimal liquidationAmount;

    public BigDecimal getLiquidationAmount() {
        return liquidationAmount;
    }

    public void setLiquidationAmount(BigDecimal liquidationAmount) {
        this.liquidationAmount = liquidationAmount;
    }
}
