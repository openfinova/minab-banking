package com.openfinova.banking.customer.repository;

import com.openfinova.banking.customer.entity.CustomerDataRetention;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CustomerDataRetentionRepository extends JpaRepository<CustomerDataRetention, UUID> {

    /**
     * Find the retention record for a specific customer.
     */
    Optional<CustomerDataRetention> findByCustomerId(UUID customerId);

    /**
     * Find all customers whose retention period has expired and PII has not yet been anonymized.
     * Used by the nightly anonymization scheduler.
     */
    List<CustomerDataRetention> findByAnonymizedFalseAndRetentionExpiresAtBefore(LocalDate date);

    /**
     * Check whether a customer's retention record exists.
     */
    boolean existsByCustomerId(UUID customerId);

    /**
     * Find all retention records for customers anonymized within a given time range.
     * Useful for compliance reporting.
     */
    @Query("""
            SELECT r FROM CustomerDataRetention r WHERE r.anonymized = true
            AND r.anonymizedAt >= :from
            AND r.anonymizedAt <= :to
            ORDER BY r.anonymizedAt DESC
            """)
    List<CustomerDataRetention> findAnonymizedBetween(@Param("from") java.time.LocalDateTime from,
            @Param("to") java.time.LocalDateTime to);
}
