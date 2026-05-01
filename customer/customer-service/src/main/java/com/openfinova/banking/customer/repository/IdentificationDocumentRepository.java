package com.openfinova.banking.customer.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.customer.entity.IdentificationDocument;

public interface IdentificationDocumentRepository extends JpaRepository<IdentificationDocument, UUID> {
    List<IdentificationDocument> findByCustomerId(UUID customerId);

    List<IdentificationDocument> findByCustomerIdAndDeletedAtIsNull(UUID customerId);
}
