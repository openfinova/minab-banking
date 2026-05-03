package com.openfinova.banking.customer.account.entity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.api.entity.AccountStatus;
import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;
import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class AccountTest {

    @Test
    void activeAndTransact_followStatus() {
        Account a = AccountTestFixtures.checkingAccount();
        assertThat(a.isActive()).isTrue();
        assertThat(a.canTransact()).isTrue();

        a.setStatus(AccountStatus.SUSPENDED);
        assertThat(a.isActive()).isFalse();
        assertThat(a.canTransact()).isFalse();
    }

    @Test
    void changeStatus_validTransitions_andClosureAudit() {
        Account a = AccountTestFixtures.checkingAccount();

        a.changeStatus(AccountStatus.SUSPENDED, "review", "staff");
        assertThat(a.getStatus()).isEqualTo(AccountStatus.SUSPENDED);

        a.changeStatus(AccountStatus.ACTIVE, "cleared", "staff");

        assertThatThrownBy(() -> a.changeStatus(AccountStatus.ACTIVE, "noop", "staff"))
                .isInstanceOf(IllegalStateException.class).hasMessageContaining("Invalid status transition");

        a.changeStatus(AccountStatus.CLOSED, "customer request", "staff");
        assertThat(a.getStatus()).isEqualTo(AccountStatus.CLOSED);
        assertThat(a.getClosureReason()).isEqualTo("customer request");
        assertThat(a.getClosedAt()).isNotNull();

        assertThatThrownBy(() -> a.changeStatus(AccountStatus.ACTIVE, "reopen", "staff"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void addGLAccountMapping_andGetGLAccountIds_ignoresInactive() {
        Account a = AccountTestFixtures.checkingAccount();
        UUID gl1 = UUID.randomUUID();
        UUID gl2 = UUID.randomUUID();
        a.addGLAccountMapping(gl1, GLAccountMappingType.PRIMARY_BALANCE);
        a.addGLAccountMapping(gl2, GLAccountMappingType.FEE_COLLECTION);

        assertThat(a.getGLAccountIds()).containsExactlyInAnyOrder(gl1, gl2);

        a.getGlAccountMappings().get(0).deactivate("restructure", "staff");
        assertThat(a.getGLAccountIds()).containsExactly(gl2);
    }
}
