package com.openfinova.banking.customer.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class InterestRateTest {

    @Test
    void isEffective_respectsFromAndUntil() {
        Account a = AccountTestFixtures.checkingAccount();
        InterestRate rate = new InterestRate(a, InterestRate.RateType.CREDIT, new BigDecimal("3.25"));
        assertThat(rate.isEffective()).isTrue();

        rate.setEffectiveFrom(LocalDateTime.now().plusDays(1));
        assertThat(rate.isEffective()).isFalse();

        rate.setEffectiveFrom(LocalDateTime.now().minusDays(10));
        rate.setEffectiveUntil(LocalDateTime.now().minusDays(1));
        assertThat(rate.isEffective()).isFalse();
    }
}
