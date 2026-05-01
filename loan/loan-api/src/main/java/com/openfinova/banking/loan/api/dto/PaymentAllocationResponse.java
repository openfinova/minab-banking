package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;

/**
 * Response DTO for payment allocation breakdown.
 */
public class PaymentAllocationResponse {

    private BigDecimal totalAmount;
    private BigDecimal principalAmount;
    private BigDecimal interestAmount;
    private BigDecimal feesAmount;
    private BigDecimal penaltiesAmount;
    private BigDecimal excessAmount;

    // Getters and Setters
    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
    }

    public BigDecimal getInterestAmount() {
        return interestAmount;
    }

    public void setInterestAmount(BigDecimal interestAmount) {
        this.interestAmount = interestAmount;
    }

    public BigDecimal getFeesAmount() {
        return feesAmount;
    }

    public void setFeesAmount(BigDecimal feesAmount) {
        this.feesAmount = feesAmount;
    }

    public BigDecimal getPenaltiesAmount() {
        return penaltiesAmount;
    }

    public void setPenaltiesAmount(BigDecimal penaltiesAmount) {
        this.penaltiesAmount = penaltiesAmount;
    }

    public BigDecimal getExcessAmount() {
        return excessAmount;
    }

    public void setExcessAmount(BigDecimal excessAmount) {
        this.excessAmount = excessAmount;
    }
}
