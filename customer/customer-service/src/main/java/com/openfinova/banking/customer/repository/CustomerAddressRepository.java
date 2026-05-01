package com.openfinova.banking.customer.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.customer.entity.CustomerAddress;

public interface CustomerAddressRepository extends JpaRepository<CustomerAddress, UUID> {
    List<CustomerAddress> findByCustomerIdAndDeletedAtIsNull(UUID customerId);

    /**
     * Returns all addresses for a customer (including soft-deleted).
     * Used by DataExportService and AnonymizationService.
     */
    List<CustomerAddress> findByCustomerId(UUID customerId);

    /**
     * Hard-delete all address records for a customer.
     * Called during GDPR anonymization after the AML retention period has expired.
     */
    void deleteByCustomerId(UUID customerId);
}
