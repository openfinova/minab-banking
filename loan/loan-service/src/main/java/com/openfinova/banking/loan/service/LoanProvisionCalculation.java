package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Pure financial helpers for loan loss provision amounts (testable without Spring).
 */
public final class LoanProvisionCalculation {

    private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

    private LoanProvisionCalculation() {
    }

    /**
     * Provision = principal × (ratePercent / 100), e.g. 100_000 at 5% → 5_000.00.
     */
    public static BigDecimal provisionAmount(BigDecimal outstandingPrincipal, BigDecimal ratePercent) {
        if (outstandingPrincipal == null || ratePercent == null) {
            throw new IllegalArgumentException("outstandingPrincipal and ratePercent are required");
        }
        return outstandingPrincipal.multiply(ratePercent).divide(PERCENT, 4, RoundingMode.HALF_UP);
    }

    public static BigDecimal ratePercentForDaysPastDue(int daysPastDue) {
        if (daysPastDue <= 0) {
            return BigDecimal.valueOf(1.00);
        }
        if (daysPastDue <= 30) {
            return BigDecimal.valueOf(5.00);
        }
        if (daysPastDue <= 60) {
            return BigDecimal.valueOf(25.00);
        }
        if (daysPastDue <= 90) {
            return BigDecimal.valueOf(50.00);
        }
        return BigDecimal.valueOf(100.00);
    }
}
