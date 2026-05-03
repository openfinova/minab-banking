package com.openfinova.banking.customer.entity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class CustomerDataRetentionTest {

    @Test
    void constructor_andExpiryRecalculationOnEndDateChange() {
        Customer c = CustomerTestFixtures.individual("RET1");
        LocalDate ended = LocalDate.of(2026, 1, 1);
        CustomerDataRetention r = new CustomerDataRetention(c, ended, 5, "EU_5AMLD");

        assertThat(r.getRetentionExpiresAt()).isEqualTo(LocalDate.of(2031, 1, 1));

        r.setRelationshipEndedAt(LocalDate.of(2020, 6, 15));
        assertThat(r.getRetentionExpiresAt()).isEqualTo(LocalDate.of(2025, 6, 15));
    }

    @Test
    void isRetentionExpired_andRecordAnonymization() {
        Customer c = CustomerTestFixtures.individual("RET2");
        CustomerDataRetention r = new CustomerDataRetention(c, LocalDate.of(2018, 1, 1), 5, "FATF");
        assertThat(r.isRetentionExpired()).isTrue();

        r.recordAnonymization("SYSTEM_SCHEDULER", "job-42");
        assertThat(r.isAnonymized()).isTrue();
        assertThat(r.getAnonymizedBy()).isEqualTo("SYSTEM_SCHEDULER");
        assertThat(r.getAnonymizationJobReference()).isEqualTo("job-42");
        assertThat(r.isRetentionExpired()).isFalse();
    }
}
