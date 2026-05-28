package com.openfinova.banking.customer.entity;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.CustomerRelationshipType;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class CustomerRelationshipTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 28, 12, 0);

    @Test
    void deactivate_clearsActiveAndRecordsRemoval() {
        Customer primary = CustomerTestFixtures.individual("P1");
        Customer related = CustomerTestFixtures.individual("R1");
        CustomerRelationship rel = new CustomerRelationship(primary, related, CustomerRelationshipType.SPOUSE, "staff");

        assertThat(rel.isActive()).isTrue();

        rel.deactivate("staff2", NOW);
        assertThat(rel.isActive()).isFalse();
        assertThat(rel.getRemovedBy()).isEqualTo("staff2");
        assertThat(rel.getRemovedAt()).isNotNull();
    }
}
