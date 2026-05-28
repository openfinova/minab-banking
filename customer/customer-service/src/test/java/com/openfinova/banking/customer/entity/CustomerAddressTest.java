package com.openfinova.banking.customer.entity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.AddressType;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class CustomerAddressTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void isCurrentlyValid_respectsOptionalBounds() {
        Customer c = CustomerTestFixtures.individual("A1");
        CustomerAddress addr = new CustomerAddress(c, AddressType.LEGAL, "1 Main", "City", "X0", "GB");
        assertThat(addr.isCurrentlyValid(TODAY)).isTrue();

        addr.setValidFrom(TODAY.plusDays(1));
        assertThat(addr.isCurrentlyValid(TODAY)).isFalse();

        addr.setValidFrom(TODAY.minusDays(10));
        addr.setValidTo(TODAY.plusDays(1));
        assertThat(addr.isCurrentlyValid(TODAY)).isTrue();

        addr.setValidTo(TODAY);
        assertThat(addr.isCurrentlyValid(TODAY)).isFalse();
    }
}
