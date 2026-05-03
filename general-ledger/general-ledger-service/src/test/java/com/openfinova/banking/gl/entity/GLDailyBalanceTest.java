package com.openfinova.banking.gl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.gl.testsupport.GlEntityFixtures;

class GLDailyBalanceTest {

    @Test
    void netChangeClosingMinusOpening() {
        LocalDate d = LocalDate.of(2026, 5, 1);
        GLAccount acc = GlEntityFixtures.usdAssetAccount();
        GLDailyBalance day = new GLDailyBalance(
                acc,
                d,
                new BigDecimal("1000"),
                new BigDecimal("1030"),
                new BigDecimal("50"),
                new BigDecimal("20"),
                3);

        assertThat(day.getNetChange()).isEqualByComparingTo("30");
    }

    @Test
    void netActivity_debitNormal_vsCreditNormal() {
        LocalDate d = LocalDate.of(2026, 5, 1);
        GLAccount debitNormal = GlEntityFixtures.usdAssetAccount();
        GLDailyBalance assetDay = new GLDailyBalance(
                debitNormal,
                d,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("50"),
                new BigDecimal("20"),
                1);
        assertThat(assetDay.getNetActivity()).isEqualByComparingTo("30");

        GLAccount creditNormal = GlEntityFixtures.usdLiabilityAccount();
        GLDailyBalance liabDay = new GLDailyBalance(
                creditNormal,
                d,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("20"),
                new BigDecimal("50"),
                1);
        assertThat(liabDay.getNetActivity()).isEqualByComparingTo("30");
    }

    @Test
    void isBalanceConsistent_whenClosingMatchesOpeningPlusNetActivity() {
        LocalDate d = LocalDate.of(2026, 5, 1);
        GLAccount acc = GlEntityFixtures.usdAssetAccount();
        GLDailyBalance ok = new GLDailyBalance(
                acc,
                d,
                new BigDecimal("100"),
                new BigDecimal("130"),
                new BigDecimal("50"),
                new BigDecimal("20"),
                1);
        assertThat(ok.isBalanceConsistent()).isTrue();

        GLDailyBalance bad = new GLDailyBalance(
                acc,
                d,
                new BigDecimal("100"),
                new BigDecimal("999"),
                new BigDecimal("50"),
                new BigDecimal("20"),
                1);
        assertThat(bad.isBalanceConsistent()).isFalse();
    }

    @Test
    void hasActivity_whenCountsOrTotalsNonZero() {
        LocalDate d = LocalDate.of(2026, 5, 1);
        GLAccount acc = GlEntityFixtures.usdAssetAccount();
        GLDailyBalance quiet = new GLDailyBalance(
                acc,
                d,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0);
        assertThat(quiet.hasActivity()).isFalse();

        GLDailyBalance busy = new GLDailyBalance(
                acc,
                d,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                new BigDecimal("0.01"),
                BigDecimal.ZERO,
                0);
        assertThat(busy.hasActivity()).isTrue();
    }

    @Test
    void addActivity_updatesTotalsClosingAndCount_forDebitNormalAccount() {
        GLAccount acc = GlEntityFixtures.usdAssetAccount();
        GLDailyBalance day = new GLDailyBalance(
                acc,
                LocalDate.of(2026, 5, 2),
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0);

        day.addActivity(new BigDecimal("40"), null);
        assertThat(day.getTotalDebits()).isEqualByComparingTo("40");
        assertThat(day.getClosingBalance()).isEqualByComparingTo("40");
        assertThat(day.getTransactionCount()).isEqualTo(1);

        day.addActivity(null, new BigDecimal("10"));
        assertThat(day.getTotalCredits()).isEqualByComparingTo("10");
        assertThat(day.getClosingBalance()).isEqualByComparingTo("30");
        assertThat(day.getTransactionCount()).isEqualTo(2);
    }
}
