package com.openfinova.banking.customer.entity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.CustomerRelationshipType;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class CustomerRelationshipTest {

    @Test
    void deactivate_clearsActiveAndRecordsRemoval() {
        Customer primary = CustomerTestFixtures.individual("P1");
        Customer related = CustomerTestFixtures.individual("R1");
        CustomerRelationship rel = new CustomerRelationship(primary, related, CustomerRelationshipType.SPOUSE, "staff");

        assertThat(rel.isActive()).isTrue();

        rel.deactivate("staff2");
        assertThat(rel.isActive()).isFalse();
        assertThat(rel.getRemovedBy()).isEqualTo("staff2");
        assertThat(rel.getRemovedAt()).isNotNull();
    }
}
