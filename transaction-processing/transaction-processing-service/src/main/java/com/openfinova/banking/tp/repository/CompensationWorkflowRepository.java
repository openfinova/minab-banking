package com.openfinova.banking.tp.repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.openfinova.banking.tp.api.entity.CompensationStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.CompensationWorkflow;

/**
 * Repository for CompensationWorkflow entities.
 * Enhanced with additional query methods for monitoring and reporting.
 */
public interface CompensationWorkflowRepository
        extends JpaRepository<CompensationWorkflow, UUID>, CompensationWorkflowRepositoryCustom {

    /**
     * Find compensation workflow by transaction ID.
     *
     * @param transactionId the UUID of the transaction
     * @return an optional containing the workflow if found
     */
    Optional<CompensationWorkflow> findByOriginalTransactionId(UUID transactionId);

    /**
     * Find workflows ready for retry.
     *
     * @param currentTime the current time
     * @return list of workflows ready for retry
     */
    @Query("""
            SELECT cw FROM CompensationWorkflow cw
            WHERE cw.workflowStatus = 'FAILED'
            AND cw.nextRetryAt IS NOT NULL
            AND cw.nextRetryAt <= :currentTime
            AND cw.retryCount < cw.maxRetries
            """)
    List<CompensationWorkflow> findWorkflowsReadyForRetry(@Param("currentTime") LocalDateTime currentTime);

    /**
     * Find workflows by status.
     *
     * @param status the workflow status
     * @return list of workflows with the specified status
     */
    List<CompensationWorkflow> findByWorkflowStatus(CompensationStatus status);

    /**
     * Find workflows that need escalation (failed with max retries).
     *
     * @return list of workflows needing escalation
     */
    @Query("""
            SELECT cw FROM CompensationWorkflow cw
            WHERE cw.workflowStatus = 'FAILED'
            AND cw.retryCount >= cw.maxRetries
            AND cw.escalatedAt IS NULL
            """)
    List<CompensationWorkflow> findWorkflowsNeedingEscalation();

    // New query methods for enhanced functionality

    /**
     * Find active workflows (in progress or initiated).
     *
     * @return list of active workflows
     */
    @Query("""
            SELECT cw FROM CompensationWorkflow cw
            WHERE cw.workflowStatus IN ('INITIATED', 'IN_PROGRESS')
            """)
    List<CompensationWorkflow> findActiveWorkflows();

    /**
     * Find failed workflows.
     *
     * @return list of failed workflows
     */
    @Query("""
            SELECT cw FROM CompensationWorkflow cw
            WHERE cw.workflowStatus = 'FAILED'
            """)
    List<CompensationWorkflow> findFailedWorkflows();

    /**
     * Find workflows created within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of workflows created in the date range
     */
    @Query("""
            SELECT cw FROM CompensationWorkflow cw
            WHERE DATE(cw.createdAt) BETWEEN :startDate AND :endDate
            """)
    List<CompensationWorkflow> findByCreatedAtBetween(@Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Count workflows by status within a date range.
     *
     * @param status the workflow status
     * @param startDate the start date
     * @param endDate the end date
     * @return count of workflows
     */
    @Query("""
            SELECT COUNT(cw) FROM CompensationWorkflow cw
            WHERE cw.workflowStatus = :status
            AND DATE(cw.createdAt) BETWEEN :startDate AND :endDate
            """)
    long countByStatusAndDateRange(@Param("status") CompensationStatus status, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find workflows by transaction type within a date range.
     *
     * @param transactionType the transaction type
     * @param startDate the start date
     * @param endDate the end date
     * @return list of workflows
     */
    @Query("""
            SELECT cw FROM CompensationWorkflow cw
            WHERE cw.originalTransaction.request.transactionType = :transactionType
            AND DATE(cw.createdAt) BETWEEN :startDate AND :endDate
            """)
    List<CompensationWorkflow> findByTransactionTypeAndDateRange(
            @Param("transactionType") TransactionType transactionType, @Param("startDate") LocalDate startDate,
            @Param("endDate") LocalDate endDate);

    /**
     * Find completed workflows with completion times for performance analysis.
     *
     * @param transactionType the transaction type
     * @return list of completed workflows
     */
    @Query("""
            SELECT cw FROM CompensationWorkflow cw
            WHERE cw.workflowStatus = 'COMPLETED'
            AND cw.originalTransaction.request.transactionType = :transactionType
            AND cw.completedAt IS NOT NULL
            """)
    List<CompensationWorkflow> findCompletedWorkflowsByTransactionType(
            @Param("transactionType") TransactionType transactionType);

    /**
     * Get daily workflow counts for reporting.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @return list of daily counts
     */
    @Query("""
            SELECT DATE(cw.createdAt) as date, COUNT(cw) as count, cw.workflowStatus as status
            FROM CompensationWorkflow cw
            WHERE DATE(cw.createdAt) BETWEEN :startDate AND :endDate
            GROUP BY DATE(cw.createdAt), cw.workflowStatus
            ORDER BY DATE(cw.createdAt)
            """)
    List<Object[]> getDailyWorkflowCounts(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    /**
     * Get top failure reasons within a date range.
     *
     * @param startDate the start date
     * @param endDate the end date
     * @param limit the maximum number of results
     * @return list of failure reasons with counts
     */
    @Query("""
            SELECT cw.failureReason, COUNT(cw) as count
            FROM CompensationWorkflow cw
            WHERE cw.workflowStatus = 'FAILED'
            AND DATE(cw.createdAt) BETWEEN :startDate AND :endDate
            GROUP BY cw.failureReason
            ORDER BY COUNT(cw) DESC
            """)
    List<Object[]> getTopFailureReasons(@Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
}
