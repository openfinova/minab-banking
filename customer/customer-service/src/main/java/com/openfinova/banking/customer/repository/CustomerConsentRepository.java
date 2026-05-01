package com.openfinova.banking.customer.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.customer.api.entity.ConsentType;
import com.openfinova.banking.customer.entity.CustomerConsent;

public interface CustomerConsentRepository extends JpaRepository<CustomerConsent, UUID> {

    /**
     * Full consent history for a customer, newest first.
     */
    List<CustomerConsent> findByCustomerIdOrderByRecordedAtDesc(UUID customerId);

    /**
     * Latest consent record per type for a customer (most recent grant or revocation).
     */
    @Query("SELECT c FROM CustomerConsent c WHERE c.customer.id = :customerId "
            + "AND c.consentType = :type ORDER BY c.recordedAt DESC")
    List<CustomerConsent> findByCustomerIdAndConsentTypeOrderByRecordedAtDesc(@Param("customerId") UUID customerId,
            @Param("type") ConsentType type);

    /**
     * Effective active consents for a customer: for each consent type, only the latest record
     * is considered. A consent is active only if that latest record has granted=true.
     * This correctly handles revocation (GDPR Art. 7(3) right to withdraw).
     */
    @Query("SELECT c FROM CustomerConsent c WHERE c.customer.id = :customerId AND c.granted = true "
            + "AND c.recordedAt = (SELECT MAX(c2.recordedAt) FROM CustomerConsent c2 "
            + "WHERE c2.customer.id = :customerId AND c2.consentType = c.consentType)")
    List<CustomerConsent> findActiveConsentsByCustomerId(@Param("customerId") UUID customerId);
}
