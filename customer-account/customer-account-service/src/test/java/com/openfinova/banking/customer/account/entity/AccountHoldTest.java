package com.openfinova.banking.customer.account.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.api.entity.HoldStatus;
import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class AccountHoldTest {

    @Test
    void isActive_requiresActiveStatusAndNotExpired() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountHold hold = new AccountHold(a, new BigDecimal("10.0000"), "USD", "fraud check");
        assertThat(hold.isActive()).isTrue();

        hold.setExpiresAt(LocalDateTime.now().minusMinutes(1));
        assertThat(hold.isActive()).isFalse();

        hold.setExpiresAt(LocalDateTime.now().plusHours(1));
        hold.setStatus(HoldStatus.RELEASED);
        assertThat(hold.isActive()).isFalse();
    }
}
