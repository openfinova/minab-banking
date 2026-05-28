package com.openfinova.banking.customer.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.api.entity.HoldStatus;
import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class AccountHoldTest {
    private static final LocalDateTime BASE_TIME = LocalDateTime.of(2026, 1, 1, 0, 0);

    @Test
    void isActive_requiresActiveStatusAndNotExpired() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountHold hold = new AccountHold(a, new BigDecimal("10.0000"), "USD", "fraud check");
        assertThat(hold.isActive(BASE_TIME)).isTrue();

        hold.setExpiresAt(BASE_TIME.minusMinutes(1));
        assertThat(hold.isActive(BASE_TIME)).isFalse();

        hold.setExpiresAt(BASE_TIME.plusHours(1));
        hold.setStatus(HoldStatus.RELEASED);
        assertThat(hold.isActive(BASE_TIME)).isFalse();
    }
}
