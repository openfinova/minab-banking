package com.openfinova.banking.loan.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.openfinova.banking.loan.api.entity.ScheduleStatus;
import com.openfinova.banking.loan.testsupport.LoanTestFixtures;

class LoanScheduleTest {

    @Test
    void getRemainingAmount_subtractsPaidPrincipalAndInterest() {
        LoanAccount loan = LoanTestFixtures.activeLoanAccount();
        LoanSchedule s = new LoanSchedule(
                loan,
                1,
                LocalDate.of(2026, 2, 1),
                new BigDecimal("80"),
                new BigDecimal("20"));
        s.setPrincipalPaid(new BigDecimal("30"));
        s.setInterestPaid(new BigDecimal("10"));
        assertThat(s.getRemainingAmount()).isEqualByComparingTo(new BigDecimal("60"));
    }

    @Test
    void isPaid_and_isPending_followStatus() {
        LoanAccount loan = LoanTestFixtures.activeLoanAccount();
        LoanSchedule s = new LoanSchedule(loan, 1, LocalDate.of(2026, 2, 1), BigDecimal.TEN, BigDecimal.TEN);
        assertThat(s.isPending()).isTrue();
        assertThat(s.isPaid()).isFalse();

        s.setStatus(ScheduleStatus.PAID);
        assertThat(s.isPaid()).isTrue();
        assertThat(s.isPending()).isFalse();
    }
}
