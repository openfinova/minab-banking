package com.openfinova.banking.customer.repository;

import com.openfinova.banking.customer.entity.KYCReviewStep;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for KYCReviewStep entity operations.
 */
@Repository
public interface KYCReviewStepRepository extends JpaRepository<KYCReviewStep, UUID> {

    /**
     * Finds all review steps for a KYC workflow.
     *
     * @param kycWorkflowId the KYC workflow ID
     * @return list of review steps
     */
    List<KYCReviewStep> findByKycWorkflowIdOrderByReviewedAtAsc(UUID kycWorkflowId);
}
