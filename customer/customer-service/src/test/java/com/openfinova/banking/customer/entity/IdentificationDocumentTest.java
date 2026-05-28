package com.openfinova.banking.customer.entity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.DocumentType;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class IdentificationDocumentTest {
    private static final LocalDate TODAY = LocalDate.of(2026, 1, 1);

    @Test
    void isValid_dependsOnExpiryRelativeToToday() {
        Customer c = CustomerTestFixtures.individual("ID1");
        IdentificationDocument doc = new IdentificationDocument(c, DocumentType.PASSPORT, "N1", "GB");

        assertThat(doc.isValid(TODAY)).isTrue();

        doc.setExpiryDate(TODAY.plusDays(1));
        assertThat(doc.isValid(TODAY)).isTrue();

        doc.setExpiryDate(TODAY);
        assertThat(doc.isValid(TODAY)).isFalse();

        doc.setExpiryDate(TODAY.minusDays(1));
        assertThat(doc.isValid(TODAY)).isFalse();
    }
}
