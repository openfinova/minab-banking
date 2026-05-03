package com.openfinova.banking.customer.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class CustomerTest {

    @Test
    void predicates_andDisplayName() {
        Customer ind = CustomerTestFixtures.individual("I1");
        assertThat(ind.isIndividual()).isTrue();
        ind.setStatus(CustomerStatus.ACTIVE);
        assertThat(ind.isActive()).isTrue();
        assertThat(ind.isClosed()).isFalse();
        assertThat(ind.getDisplayName().trim()).isEqualTo("Jane Doe");

        Customer biz = CustomerTestFixtures.business("B1");
        assertThat(biz.isIndividual()).isFalse();
        assertThat(biz.getDisplayName()).isEqualTo("Acme Ltd");
    }

    @Test
    void activate_requiresVerifiedKyc() {
        Customer c = CustomerTestFixtures.individual("I2");
        c.setKycStatus(KYCStatus.PENDING);
        assertThatThrownBy(() -> c.activate()).isInstanceOf(IllegalStateException.class);

        c.setKycStatus(KYCStatus.VERIFIED);
        c.activate();
        assertThat(c.getStatus()).isEqualTo(CustomerStatus.ACTIVE);
    }

    @Test
    void block_recordsReason() {
        Customer c = CustomerTestFixtures.individual("I3");
        c.block("sanctions");
        assertThat(c.getStatus()).isEqualTo(CustomerStatus.BLOCKED);
        assertThat(c.getBlockedReason()).isEqualTo("sanctions");
    }

    @Test
    void close_rejectsWhileActive() {
        Customer c = CustomerTestFixtures.individual("I4");
        c.setStatus(CustomerStatus.ACTIVE);
        assertThatThrownBy(() -> c.close()).isInstanceOf(IllegalStateException.class);

        c.setStatus(CustomerStatus.BLOCKED);
        c.close();
        assertThat(c.isClosed()).isTrue();
    }

    @Test
    void markDeceased_onlyForIndividuals() {
        Customer biz = CustomerTestFixtures.business("B2");
        assertThatThrownBy(() -> biz.markDeceased()).isInstanceOf(IllegalStateException.class);

        Customer ind = CustomerTestFixtures.individual("I5");
        ind.markDeceased();
        assertThat(ind.getStatus()).isEqualTo(CustomerStatus.DECEASED);
    }
}
