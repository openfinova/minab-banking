package com.openfinova.banking.customer.account.api.dto;

import java.math.BigDecimal;

/**
 * DTO for balance update operations (ledger and available balance deltas).
 */
public class BalanceUpdate {

    private BigDecimal ledgerDelta;
    private BigDecimal availableDelta;

    public BalanceUpdate() {
    }

    public BalanceUpdate(BigDecimal ledgerDelta, BigDecimal availableDelta) {
        this.ledgerDelta = ledgerDelta;
        this.availableDelta = availableDelta;
    }

    public BigDecimal getLedgerDelta() {
        return ledgerDelta;
    }

    public void setLedgerDelta(BigDecimal ledgerDelta) {
        this.ledgerDelta = ledgerDelta;
    }

    public BigDecimal getAvailableDelta() {
        return availableDelta;
    }

    public void setAvailableDelta(BigDecimal availableDelta) {
        this.availableDelta = availableDelta;
    }
}
