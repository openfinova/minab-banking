package com.openfinova.banking.customer.account.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.api.entity.LimitPeriod;
import com.openfinova.banking.customer.account.api.entity.LimitType;
import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class AccountLimitTest {

    @Test
    void isEffective_respectsWindow() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountLimit lim = new AccountLimit(a, LimitType.DAILY_TRANSACTION, LimitPeriod.DAILY, "staff");
        lim.setMaxAmount(new BigDecimal("5000"));

        assertThat(lim.isEffective()).isTrue();

        lim.setEffectiveFrom(Instant.now().plus(1, ChronoUnit.DAYS));
        assertThat(lim.isEffective()).isFalse();
    }

    @Test
    void canOverride_andRegulatoryFlag() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountLimit lim = new AccountLimit(a, LimitType.TRANSFER_LIMIT, LimitPeriod.MONTHLY, "staff");
        lim.setMaxAmount(BigDecimal.ONE);
        assertThat(lim.canOverride()).isTrue();
        assertThat(lim.isRegulatoryLimit()).isFalse();

        lim.setAsRegulatoryLimit("AML-ART12");
        assertThat(lim.isRegulatoryLimit()).isTrue();
        assertThat(lim.canOverride()).isFalse();
    }

    @Test
    void validateLimitConstraints() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountLimit empty = new AccountLimit(a, LimitType.WITHDRAWAL_LIMIT, LimitPeriod.DAILY, "staff");
        assertThatThrownBy(empty::validateLimitConstraints).isInstanceOf(IllegalArgumentException.class);

        AccountLimit badRange = new AccountLimit(a, LimitType.DAILY_TRANSACTION, LimitPeriod.DAILY, "staff");
        badRange.setMinAmount(new BigDecimal("100"));
        badRange.setMaxAmount(new BigDecimal("50"));
        assertThatThrownBy(badRange::validateLimitConstraints).isInstanceOf(IllegalArgumentException.class);

        AccountLimit balanceWithCount = new AccountLimit(a, LimitType.MAXIMUM_BALANCE, LimitPeriod.LIFETIME, "staff");
        balanceWithCount.setMaxAmount(BigDecimal.TEN);
        balanceWithCount.setMaxCount(3);
        assertThatThrownBy(balanceWithCount::validateLimitConstraints).isInstanceOf(IllegalArgumentException.class);

        AccountLimit txnWithMin = new AccountLimit(a, LimitType.DAILY_TRANSACTION, LimitPeriod.DAILY, "staff");
        txnWithMin.setMaxAmount(BigDecimal.TEN);
        txnWithMin.setMinAmount(BigDecimal.ONE);
        assertThatThrownBy(txnWithMin::validateLimitConstraints).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void expire_andExtendEffectivePeriod() {
        Account a = AccountTestFixtures.checkingAccount();
        AccountLimit lim = new AccountLimit(a, LimitType.VELOCITY_LIMIT, LimitPeriod.MONTHLY, "staff");
        lim.setMaxCount(100);

        lim.expire("staff");
        assertThat(lim.getEffectiveUntil()).isNotNull();
        assertThat(lim.getUpdatedBy()).isEqualTo("staff");

        AccountLimit lim2 = new AccountLimit(a, LimitType.TRANSFER_LIMIT, LimitPeriod.WEEKLY, "staff");
        lim2.setMaxAmount(BigDecimal.TEN);
        Instant future = Instant.now().plus(7, ChronoUnit.DAYS);
        lim2.extendEffectivePeriod(future, "staff");
        assertThat(lim2.getEffectiveUntil()).isEqualTo(future);

        assertThatThrownBy(() -> lim2.extendEffectivePeriod(Instant.now().minus(1, ChronoUnit.DAYS), "staff"))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
