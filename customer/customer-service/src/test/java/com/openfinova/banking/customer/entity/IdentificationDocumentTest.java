package com.openfinova.banking.customer.entity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.DocumentType;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class IdentificationDocumentTest {

    @Test
    void isValid_dependsOnExpiryRelativeToToday() {
        Customer c = CustomerTestFixtures.individual("ID1");
        IdentificationDocument doc = new IdentificationDocument(c, DocumentType.PASSPORT, "N1", "GB");

        assertThat(doc.isValid()).isTrue();

        doc.setExpiryDate(LocalDate.now().plusDays(1));
        assertThat(doc.isValid()).isTrue();

        doc.setExpiryDate(LocalDate.now());
        assertThat(doc.isValid()).isFalse();

        doc.setExpiryDate(LocalDate.now().minusDays(1));
        assertThat(doc.isValid()).isFalse();
    }
}
