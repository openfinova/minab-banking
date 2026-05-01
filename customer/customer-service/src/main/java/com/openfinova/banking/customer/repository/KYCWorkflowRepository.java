package com.openfinova.banking.customer.repository;

import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.entity.KYCWorkflow;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for KYCWorkflow entity operations.
 */
@Repository
public interface KYCWorkflowRepository extends JpaRepository<KYCWorkflow, UUID> {

    /**
     * Finds the most recent KYC workflow for a customer.
     *
     * @param customerId the customer ID
     * @return the most recent KYC workflow
     */
    @Query("SELECT k FROM KYCWorkflow k WHERE k.customer.id = :customerId ORDER BY k.initiatedAt DESC LIMIT 1")
    Optional<KYCWorkflow> findLatestByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Finds all KYC workflows for a customer ordered by initiated date.
     *
     * @param customerId the customer ID
     * @return list of KYC workflows
     */
    @Query("SELECT k FROM KYCWorkflow k WHERE k.customer.id = :customerId ORDER BY k.initiatedAt DESC")
    List<KYCWorkflow> findAllByCustomerId(@Param("customerId") UUID customerId);

    /**
     * Finds all KYC workflows with a specific status.
     *
     * @param status the KYC status
     * @return list of KYC workflows
     */
    List<KYCWorkflow> findByStatus(KYCStatus status);

    /**
     * Finds KYC workflows that were verified before a certain date (for expiration).
     *
     * @param status the status to filter by
     * @param beforeDate the date threshold
     * @return list of KYC workflows
     */
    @Query("SELECT k FROM KYCWorkflow k WHERE k.status = :status AND k.completedAt < :beforeDate")
    List<KYCWorkflow> findByStatusAndCompletedAtBefore(@Param("status") KYCStatus status,
            @Param("beforeDate") LocalDateTime beforeDate);

    /**
     * Counts workflows by status.
     *
     * @param status the KYC status
     * @return count of workflows
     */
    long countByStatus(KYCStatus status);
}
