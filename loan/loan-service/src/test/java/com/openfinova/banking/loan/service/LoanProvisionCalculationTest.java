package com.openfinova.banking.loan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;

import org.junit.jupiter.api.Test;

class LoanProvisionCalculationTest {

    @Test
    void provisionAmount_fivePercentOf100k_is5000() {
        BigDecimal amount = LoanProvisionCalculation
                .provisionAmount(new BigDecimal("100000.00"), new BigDecimal("5.00"));
        assertEquals(0, new BigDecimal("5000.0000").compareTo(amount));
    }

    @Test
    void provisionAmount_onePercentOf100k_is1000() {
        BigDecimal amount = LoanProvisionCalculation
                .provisionAmount(new BigDecimal("100000.00"), new BigDecimal("1.00"));
        assertEquals(0, new BigDecimal("1000.0000").compareTo(amount));
    }

    @Test
    void ratePercentForDaysPastDue_tiers() {
        assertEquals(0, new BigDecimal("1.00").compareTo(LoanProvisionCalculation.ratePercentForDaysPastDue(0)));
        assertEquals(0, new BigDecimal("5.00").compareTo(LoanProvisionCalculation.ratePercentForDaysPastDue(15)));
        assertEquals(0, new BigDecimal("25.00").compareTo(LoanProvisionCalculation.ratePercentForDaysPastDue(45)));
        assertEquals(0, new BigDecimal("50.00").compareTo(LoanProvisionCalculation.ratePercentForDaysPastDue(75)));
        assertEquals(0, new BigDecimal("100.00").compareTo(LoanProvisionCalculation.ratePercentForDaysPastDue(120)));
    }

    @Test
    void provisionAmount_nullArguments_throws() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LoanProvisionCalculation.provisionAmount(null, BigDecimal.ONE));
        assertThrows(
                IllegalArgumentException.class,
                () -> LoanProvisionCalculation.provisionAmount(BigDecimal.ONE, null));
    }
}
