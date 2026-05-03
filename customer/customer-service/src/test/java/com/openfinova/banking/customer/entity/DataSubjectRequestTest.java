package com.openfinova.banking.customer.entity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.DataSubjectRequestStatus;
import com.openfinova.banking.customer.api.entity.DataSubjectRequestType;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class DataSubjectRequestTest {

    @Test
    void constructor_setsReceivedAndDefaultDueBy30Days() {
        Customer c = CustomerTestFixtures.individual("D1");
        DataSubjectRequest r = new DataSubjectRequest(c, DataSubjectRequestType.ACCESS, "EMAIL", "please");

        assertThat(r.getStatus()).isEqualTo(DataSubjectRequestStatus.RECEIVED);
        assertThat(r.getDueBy()).isEqualTo(r.getReceivedAt().plusDays(30));
    }

    @Test
    void isOverdue_whenPastDueAndStillOpen() {
        DataSubjectRequest r = new DataSubjectRequest();
        r.setStatus(DataSubjectRequestStatus.IN_REVIEW);
        r.setDueBy(LocalDate.now().minusDays(1));

        assertThat(r.isOverdue()).isTrue();

        r.setStatus(DataSubjectRequestStatus.FULFILLED);
        assertThat(r.isOverdue()).isFalse();
    }

    @Test
    void extendDeadline_respectsMax60Days() {
        DataSubjectRequest r = new DataSubjectRequest();
        r.setDueBy(LocalDate.of(2026, 6, 1));

        assertThatThrownBy(() -> r.extendDeadline(61)).isInstanceOf(IllegalArgumentException.class);

        r.extendDeadline(14);
        assertThat(r.getDueBy()).isEqualTo(LocalDate.of(2026, 6, 15));
        assertThat(r.isExtended()).isTrue();
        assertThat(r.getExtensionNotifiedAt()).isEqualTo(LocalDate.now());
    }

    @Test
    void markFulfilled_defer_reject() {
        DataSubjectRequest r = new DataSubjectRequest();

        r.markFulfilled("dpo");
        assertThat(r.getStatus()).isEqualTo(DataSubjectRequestStatus.FULFILLED);
        assertThat(r.getHandledBy()).isEqualTo("dpo");
        assertThat(r.getFulfilledAt()).isEqualTo(LocalDate.now());

        DataSubjectRequest r2 = new DataSubjectRequest();
        r2.defer(LocalDate.of(2027, 1, 1), "retention", "legal");
        assertThat(r2.getStatus()).isEqualTo(DataSubjectRequestStatus.DEFERRED);
        assertThat(r2.getDeferredUntil()).isEqualTo(LocalDate.of(2027, 1, 1));

        DataSubjectRequest r3 = new DataSubjectRequest();
        r3.reject("no basis", "dpo");
        assertThat(r3.getStatus()).isEqualTo(DataSubjectRequestStatus.REJECTED);
        assertThat(r3.getOutcomeReason()).isEqualTo("no basis");
    }
}
