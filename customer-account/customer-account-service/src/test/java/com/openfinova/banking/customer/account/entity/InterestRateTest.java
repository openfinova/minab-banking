package com.openfinova.banking.customer.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class InterestRateTest {
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void isEffective_respectsFromAndUntil() {
        Account a = AccountTestFixtures.checkingAccount();
        InterestRate rate = new InterestRate(a, InterestRate.RateType.CREDIT, new BigDecimal("3.25"), BASE_TIME);
        assertThat(rate.isEffective(BASE_TIME.plusHours(1))).isTrue();

        rate.setEffectiveFrom(BASE_TIME.plusDays(1));
        assertThat(rate.isEffective(BASE_TIME)).isFalse();

        rate.setEffectiveFrom(BASE_TIME.minusDays(10));
        rate.setEffectiveUntil(BASE_TIME.minusDays(1));
        assertThat(rate.isEffective(BASE_TIME)).isFalse();
    }
}
