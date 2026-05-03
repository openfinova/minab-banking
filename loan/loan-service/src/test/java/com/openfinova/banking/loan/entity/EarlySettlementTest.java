package com.openfinova.banking.loan.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.loan.api.entity.SettlementCalculationMethod;
import com.openfinova.banking.loan.api.entity.SettlementStatus;
import com.openfinova.banking.loan.testsupport.LoanTestFixtures;

class EarlySettlementTest {

    @Test
    void isValid_onlyForActiveQuoteWithFutureValidity() {
        EarlySettlement es = baseQuote(LoanTestFixtures.activeLoanAccount());
        es.setStatus(SettlementStatus.QUOTE);
        es.setValidUntil(LocalDate.now().plusDays(5));
        assertThat(es.isValid()).isTrue();

        es.setValidUntil(LocalDate.now().minusDays(1));
        assertThat(es.isValid()).isFalse();

        es.setValidUntil(LocalDate.now().plusDays(5));
        es.setStatus(SettlementStatus.COMPLETED);
        assertThat(es.isValid()).isFalse();
    }

    @Test
    void isSettled_whenStatusCompleted() {
        EarlySettlement es = baseQuote(LoanTestFixtures.activeLoanAccount());
        es.setStatus(SettlementStatus.APPROVED);
        assertThat(es.isSettled()).isFalse();
        es.setStatus(SettlementStatus.COMPLETED);
        assertThat(es.isSettled()).isTrue();
    }

    private static EarlySettlement baseQuote(LoanAccount loan) {
        EarlySettlement es = new EarlySettlement();
        es.setQuoteReference("QUOTE-UT-1");
        es.setLoanAccount(loan);
        es.setQuoteDate(LocalDate.now());
        es.setValidUntil(LocalDate.now().plusDays(30));
        BigDecimal z = BigDecimal.ZERO;
        es.setOutstandingPrincipal(new BigDecimal("1000"));
        es.setOutstandingInterest(z);
        es.setOutstandingFees(z);
        es.setRebateAmount(z);
        es.setPenaltyAmount(z);
        es.setSettlementAmount(new BigDecimal("1000"));
        es.setCurrency("USD");
        es.setCalculationMethod(SettlementCalculationMethod.FULL_OUTSTANDING);
        es.setStatus(SettlementStatus.QUOTE);
        return es;
    }
}
