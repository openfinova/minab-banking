package com.openfinova.banking.gl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.gl.testsupport.GlEntityFixtures;

class GLJournalEntryTest {

    private static final LocalDate VALUE_DATE = LocalDate.of(2026, 5, 1);

    @Test
    void debitAndCreditFactories_sideAndAmount() {
        GLAccount acc = GlEntityFixtures.usdAssetAccount();

        GLJournalEntry debit = GLJournalEntry.debit(acc, new BigDecimal("100.00"), "d", VALUE_DATE);
        assertThat(debit.isDebit()).isTrue();
        assertThat(debit.isCredit()).isFalse();
        assertThat(debit.getAmount()).isEqualByComparingTo("100.00");
        assertThat(debit.getEntryType()).isEqualTo("DEBIT");

        GLJournalEntry credit = GLJournalEntry.credit(acc, new BigDecimal("50.0000"), "c", VALUE_DATE);
        assertThat(credit.isCredit()).isTrue();
        assertThat(credit.getAmount()).isEqualByComparingTo("50.0000");
        assertThat(credit.getEntryType()).isEqualTo("CREDIT");
    }

    @Test
    void amounts_mustBeExactlyOneSide() {
        GLAccount acc = GlEntityFixtures.usdAssetAccount();

        assertThatThrownBy(() -> new GLJournalEntry(acc, new BigDecimal("1"), new BigDecimal("1"), "x", VALUE_DATE))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("both debit and credit");

        assertThatThrownBy(() -> new GLJournalEntry(acc, BigDecimal.ZERO, BigDecimal.ZERO, "x", VALUE_DATE))
                .isInstanceOf(IllegalArgumentException.class).hasMessageContaining("either debit or credit");

        GLJournalEntry line = GLJournalEntry.debit(acc, new BigDecimal("10"), "ok", VALUE_DATE);
        assertThatThrownBy(() -> line.setCreditAmount(new BigDecimal("5")))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
