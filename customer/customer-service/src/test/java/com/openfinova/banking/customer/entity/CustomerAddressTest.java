package com.openfinova.banking.customer.entity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.AddressType;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class CustomerAddressTest {

    @Test
    void isCurrentlyValid_respectsOptionalBounds() {
        Customer c = CustomerTestFixtures.individual("A1");
        CustomerAddress addr = new CustomerAddress(c, AddressType.LEGAL, "1 Main", "City", "X0", "GB");
        assertThat(addr.isCurrentlyValid()).isTrue();

        addr.setValidFrom(LocalDate.now().plusDays(1));
        assertThat(addr.isCurrentlyValid()).isFalse();

        addr.setValidFrom(LocalDate.now().minusDays(10));
        addr.setValidTo(LocalDate.now().plusDays(1));
        assertThat(addr.isCurrentlyValid()).isTrue();

        addr.setValidTo(LocalDate.now());
        assertThat(addr.isCurrentlyValid()).isFalse();
    }
}
