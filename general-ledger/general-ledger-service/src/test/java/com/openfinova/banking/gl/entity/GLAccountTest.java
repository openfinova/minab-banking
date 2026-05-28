package com.openfinova.banking.gl.entity;

import java.util.ArrayList;
import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.CashFlowCategory;
import com.openfinova.banking.gl.api.entity.GLAccountType;

class GLAccountTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void constructor_derivesNormalBalanceAndCashFlowByType() {
        GLAccount asset = new GLAccount("A1", "A", GLAccountType.ASSET, "USD");
        assertThat(asset.getNormalBalance()).isEqualTo(BalanceType.DEBIT);
        assertThat(asset.getCashFlowCategory()).isEqualTo(CashFlowCategory.INVESTING);

        GLAccount revenue = new GLAccount("R1", "R", GLAccountType.REVENUE, "USD");
        assertThat(revenue.getNormalBalance()).isEqualTo(BalanceType.CREDIT);
        assertThat(revenue.getCashFlowCategory()).isEqualTo(CashFlowCategory.NONE);
    }

    @Test
    void setContra_flipsNormalBalance_setTypeKeepsContraSemantics() {
        GLAccount acc = new GLAccount("A2", "A", GLAccountType.ASSET, "USD");
        assertThat(acc.getNormalBalance()).isEqualTo(BalanceType.DEBIT);

        acc.setContra(true);
        assertThat(acc.isContra()).isTrue();
        assertThat(acc.getNormalBalance()).isEqualTo(BalanceType.CREDIT);

        acc.setContra(false);
        assertThat(acc.getNormalBalance()).isEqualTo(BalanceType.DEBIT);

        acc.setContra(true);
        acc.setType(GLAccountType.EXPENSE);
        assertThat(acc.getNormalBalance()).isEqualTo(BalanceType.CREDIT);
    }

    @Test
    void hasChildren_andLifecycleFlags() {
        GLAccount parent = new GLAccount("P", "P", GLAccountType.ASSET, "USD");
        assertThat(parent.hasChildren()).isFalse();

        List<GLAccount> kids = new ArrayList<>();
        kids.add(new GLAccount("C", "C", GLAccountType.ASSET, "USD"));
        parent.setChildren(kids);
        assertThat(parent.hasChildren()).isTrue();

        assertThat(parent.isActive()).isTrue();
        parent.markInactive("merged", TODAY);
        assertThat(parent.isActive()).isFalse();
        assertThat(parent.getInactivationReason()).isEqualTo("merged");

        parent.activate();
        assertThat(parent.isActive()).isTrue();
    }
}
