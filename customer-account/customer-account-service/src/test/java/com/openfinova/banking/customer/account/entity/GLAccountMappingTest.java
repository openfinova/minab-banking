package com.openfinova.banking.customer.account.entity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;
import com.openfinova.banking.customer.account.testsupport.AccountTestFixtures;

class GLAccountMappingTest {

    @Test
    void deactivate_reactivate_andPrimaryBalance() {
        Account a = AccountTestFixtures.checkingAccount();
        GLAccountMapping m = new GLAccountMapping(a, UUID.randomUUID(), GLAccountMappingType.PRIMARY_BALANCE, "t");
        assertThat(m.isActive()).isTrue();
        assertThat(m.isPrimaryBalance()).isTrue();

        m.deactivate("merged", "staff");
        assertThat(m.isActive()).isFalse();
        assertThat(m.getDeactivatedAt()).isNotNull();

        m.reactivate("staff");
        assertThat(m.isActive()).isTrue();
        assertThat(m.getDeactivatedAt()).isNull();

        GLAccountMapping fee = new GLAccountMapping(a, UUID.randomUUID(), GLAccountMappingType.FEE_COLLECTION, "t");
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
