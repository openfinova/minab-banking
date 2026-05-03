package com.openfinova.banking.gl.entity;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.CashFlowCategory;
import com.openfinova.banking.gl.api.entity.GLAccountType;

class GLAccountTest {

    @Test
    void constructor_derivesNormalBalanceAndCashFlowByType() {
        GLAccount asset = new GLAccount("A1", "A", GLAccountType.ASSET, "USD", "t");
        assertThat(asset.getNormalBalance()).isEqualTo(BalanceType.DEBIT);
        assertThat(asset.getCashFlowCategory()).isEqualTo(CashFlowCategory.INVESTING);

        GLAccount revenue = new GLAccount("R1", "R", GLAccountType.REVENUE, "USD", "t");
        assertThat(revenue.getNormalBalance()).isEqualTo(BalanceType.CREDIT);
        assertThat(revenue.getCashFlowCategory()).isEqualTo(CashFlowCategory.NONE);
    }

    @Test
    void setContra_flipsNormalBalance_setTypeKeepsContraSemantics() {
        GLAccount acc = new GLAccount("A2", "A", GLAccountType.ASSET, "USD", "t");
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
        GLAccount parent = new GLAccount("P", "P", GLAccountType.ASSET, "USD", "t");
        assertThat(parent.hasChildren()).isFalse();

        List<GLAccount> kids = new ArrayList<>();
        kids.add(new GLAccount("C", "C", GLAccountType.ASSET, "USD", "t"));
        parent.setChildren(kids);
        assertThat(parent.hasChildren()).isTrue();

        assertThat(parent.isActive()).isTrue();
        parent.markInactive("merged");
        assertThat(parent.isActive()).isFalse();
        assertThat(parent.getInactivationReason()).isEqualTo("merged");

        parent.activate();
        assertThat(parent.isActive()).isTrue();
    }
}
