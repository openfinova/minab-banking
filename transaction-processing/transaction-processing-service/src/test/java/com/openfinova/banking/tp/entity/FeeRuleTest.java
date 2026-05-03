package com.openfinova.banking.tp.entity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.LocalTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.FeeType;
import com.openfinova.banking.tp.api.entity.TransactionType;

class FeeRuleTest {

    @Test
    void isCurrentlyEffective_respectsActiveFlagAndWindow() {
        FeeRule rule = new FeeRule("r1", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.FIXED_AMOUNT);
        rule.setIsActive(true);
        rule.setEffectiveFrom(LocalDateTime.now().minusDays(1));
        rule.setEffectiveTo(null);
        assertThat(rule.isCurrentlyEffective()).isTrue();

        rule.setIsActive(false);
        assertThat(rule.isCurrentlyEffective()).isFalse();

        rule.setIsActive(true);
        rule.setEffectiveFrom(LocalDateTime.now().plusDays(1));
        assertThat(rule.isCurrentlyEffective()).isFalse();
    }

    @Test
    void appliesToAmount_enforcesMinAndMax() {
        FeeRule rule = new FeeRule("r2", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.FIXED_AMOUNT);
        rule.setMinTransactionAmount(new BigDecimal("100"));
        rule.setMaxTransactionAmount(new BigDecimal("500"));
        assertThat(rule.appliesToAmount(new BigDecimal("99"))).isFalse();
        assertThat(rule.appliesToAmount(new BigDecimal("100"))).isTrue();
        assertThat(rule.appliesToAmount(new BigDecimal("500"))).isTrue();
        assertThat(rule.appliesToAmount(new BigDecimal("501"))).isFalse();
    }

    @Test
    void appliesToCurrentTime_trueWhenNoWindow() {
        FeeRule rule = new FeeRule("r3", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.NONE);
        assertThat(rule.appliesToCurrentTime()).isTrue();
    }

    @Test
    void appliesToCurrentTime_handlesWindowWithinSameDay() {
        FeeRule rule = new FeeRule("r4", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.NONE);
        rule.setTimeBasedStart(LocalTime.of(0, 0));
        rule.setTimeBasedEnd(LocalTime.of(23, 59, 59));
        assertThat(rule.appliesToCurrentTime()).isTrue();
    }

    @Test
    void validateConfiguration_percentageRequiresRate() {
        FeeRule rule = new FeeRule("r5", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.PERCENTAGE);
        assertThatThrownBy(rule::validateConfiguration).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateConfiguration_noneDoesNotThrow() {
        FeeRule rule = new FeeRule("r6", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.NONE);
        rule.validateConfiguration();
    }
}
