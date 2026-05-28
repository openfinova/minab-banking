package com.openfinova.banking.customer.account.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;
import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class GLAccountMappingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 28, 12, 0);

    @Test
    void deactivate_reactivate_andPrimaryBalance() {
        Account a = AccountTestFixtures.checkingAccount();
        GLAccountMapping m = new GLAccountMapping(a, UUID.randomUUID(), GLAccountMappingType.PRIMARY_BALANCE);
        assertThat(m.isActive()).isTrue();
        assertThat(m.isPrimaryBalance()).isTrue();

        m.deactivate("merged", "staff", NOW);
        assertThat(m.isActive()).isFalse();
        assertThat(m.getDeactivatedAt()).isNotNull();

        m.reactivate("staff");
        assertThat(m.isActive()).isTrue();
        assertThat(m.getDeactivatedAt()).isNull();

        GLAccountMapping fee = new GLAccountMapping(a, UUID.randomUUID(), GLAccountMappingType.FEE_COLLECTION);
        assertThat(fee.isPrimaryBalance()).isFalse();
    }

    @Test
    void validateWeight() {
        GLAccountMapping m = new GLAccountMapping();
        m.setWeight(null);
        assertThatThrownBy(m::validateWeight).isInstanceOf(IllegalArgumentException.class);

        assertThatThrownBy(() -> m.setWeightWithValidation(0)).isInstanceOf(IllegalArgumentException.class);

        m.setWeightWithValidation(2);
        assertThat(m.getWeight()).isEqualTo(2);
    }
}
