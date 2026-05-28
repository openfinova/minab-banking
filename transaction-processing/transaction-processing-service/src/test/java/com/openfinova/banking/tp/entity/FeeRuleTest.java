package com.openfinova.banking.tp.entity;

import static com.openfinova.banking.tp.testsupport.TpEntityTestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalTime;

import org.junit.jupiter.api.Test;

import com.openfinova.banking.tp.api.entity.CustomerTier;
import com.openfinova.banking.tp.api.entity.FeeType;
import com.openfinova.banking.tp.api.entity.TransactionType;

class FeeRuleTest {

    @Test
    void isCurrentlyEffective_respectsActiveFlagAndWindow() {
        FeeRule rule = new FeeRule("r1", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.FIXED_AMOUNT, NOW);
        rule.setIsActive(true);
        rule.setEffectiveFrom(NOW.minusDays(1));
        rule.setEffectiveTo(null);
        assertThat(rule.isCurrentlyEffective(NOW)).isTrue();

        rule.setIsActive(false);
        assertThat(rule.isCurrentlyEffective(NOW)).isFalse();

        rule.setIsActive(true);
        rule.setEffectiveFrom(NOW.plusDays(1));
        assertThat(rule.isCurrentlyEffective(NOW)).isFalse();
    }

    @Test
    void appliesToAmount_enforcesMinAndMax() {
        FeeRule rule = new FeeRule("r2", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.FIXED_AMOUNT, NOW);
        rule.setMinTransactionAmount(new BigDecimal("100"));
        rule.setMaxTransactionAmount(new BigDecimal("500"));
        assertThat(rule.appliesToAmount(new BigDecimal("99"))).isFalse();
        assertThat(rule.appliesToAmount(new BigDecimal("100"))).isTrue();
        assertThat(rule.appliesToAmount(new BigDecimal("500"))).isTrue();
        assertThat(rule.appliesToAmount(new BigDecimal("501"))).isFalse();
    }

    @Test
    void appliesToCurrentTime_trueWhenNoWindow() {
        FeeRule rule = new FeeRule("r3", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.NONE, NOW);
        assertThat(rule.appliesToCurrentTime()).isTrue();
    }

    @Test
    void appliesToCurrentTime_handlesWindowWithinSameDay() {
        FeeRule rule = new FeeRule("r4", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.NONE, NOW);
        rule.setTimeBasedStart(LocalTime.of(0, 0));
        rule.setTimeBasedEnd(LocalTime.of(23, 59, 59));
        assertThat(rule.appliesToCurrentTime()).isTrue();
    }

    @Test
    void validateConfiguration_percentageRequiresRate() {
        FeeRule rule = new FeeRule("r5", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.PERCENTAGE, NOW);
        assertThatThrownBy(rule::validateConfiguration).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateConfiguration_noneDoesNotThrow() {
        FeeRule rule = new FeeRule("r6", TransactionType.TRANSFER, CustomerTier.BASIC, FeeType.NONE, NOW);
        rule.validateConfiguration();
    }
}
