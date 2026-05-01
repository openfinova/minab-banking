package com.openfinova.banking.customer.repository;

import java.util.List;
import java.util.UUID;

import com.openfinova.banking.customer.api.entity.ContactType;
import org.springframework.data.jpa.repository.JpaRepository;

import com.openfinova.banking.customer.entity.ContactDetail;

public interface ContactDetailRepository extends JpaRepository<ContactDetail, UUID> {

    /**
     * Find all contact details for a customer.
     * 
     * @param customerId the customer ID
     * @return list of contact details
     */
    List<ContactDetail> findByCustomerId(UUID customerId);

    List<ContactDetail> findByCustomerIdAndDeletedAtIsNull(UUID customerId);

    List<ContactDetail> findByCustomerIdAndTypeAndDeletedAtIsNull(UUID customerId, ContactType type);

    /**
     * Find contact details by customer ID and type.
     * 
     * @param customerId the customer ID
     * @param type the contact type
     * @return list of contact details matching the criteria
     */
    List<ContactDetail> findByCustomerIdAndType(UUID customerId, ContactType type);

    /**
     * Find primary contact detail by customer ID and type.
     * 
     * @param customerId the customer ID
     * @param type the contact type
     * @return list of primary contact details
     */
    List<ContactDetail> findByCustomerIdAndTypeAndIsPrimaryTrue(UUID customerId, ContactType type);

    /**
     * Find verified contact details for a customer.
     * 
     * @param customerId the customer ID
     * @return list of verified contact details
     */
    List<ContactDetail> findByCustomerIdAndIsVerifiedTrue(UUID customerId);

    /**
     * Hard-delete all contact details for a customer.
     * Called during GDPR anonymization after the AML retention period has expired.
     */
    void deleteByCustomerId(UUID customerId);
}
