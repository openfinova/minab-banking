package com.openfinova.banking.identity.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.identity.entity.ApprovalWorkflowInstance;

public interface ApprovalWorkflowInstanceRepository extends JpaRepository<ApprovalWorkflowInstance, UUID> {

    Optional<ApprovalWorkflowInstance> findByResourceTypeAndResourceId(String resourceType, String resourceId);

    /**
     * Returns the most recently approved workflow for the given resource, if one exists.
     * Used by the workflow-enforcement gate to confirm a change has been pre-approved.
     */
    @Query("""
            SELECT w FROM ApprovalWorkflowInstance w
            WHERE w.resourceType = :resourceType
            AND w.resourceId = :resourceId
            AND w.status = 'APPROVED'
            ORDER BY w.updatedAt DESC
            LIMIT 1
            """)
    Optional<ApprovalWorkflowInstance> findLatestApprovedByResourceTypeAndResourceId(
            @Param("resourceType") String resourceType, @Param("resourceId") String resourceId);

    @Query("""
            SELECT DISTINCT w FROM ApprovalWorkflowInstance w
            LEFT JOIN FETCH w.steps
            WHERE w.id = :id
            """)
    Optional<ApprovalWorkflowInstance> findByIdWithSteps(@Param("id") UUID id);

    List<ApprovalWorkflowInstance> findByResourceTypeOrderByCreatedAtDesc(String resourceType);

    @Query("""
            SELECT DISTINCT w FROM ApprovalWorkflowInstance w
            LEFT JOIN FETCH w.steps
            WHERE w.resourceType = :resourceType
            ORDER BY w.createdAt DESC
            """)
    List<ApprovalWorkflowInstance> findByResourceTypeWithStepsOrderByCreatedAtDesc(
            @Param("resourceType") String resourceType);
}
