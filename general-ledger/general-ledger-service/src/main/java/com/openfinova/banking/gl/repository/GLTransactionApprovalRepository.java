package com.openfinova.banking.gl.repository;

import com.openfinova.banking.gl.entity.GLTransactionApproval;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repository for GL transaction approval records.
 * Provides queries for approval workflow management and audit trail.
 */
@Repository
public interface GLTransactionApprovalRepository extends JpaRepository<GLTransactionApproval, UUID> {

    /**
     * Find all approval records for a specific transaction.
     * Results ordered by approval level, then timestamp.
     *
     * @param transactionId the transaction ID
     * @return list of approval records
     */
    @Query("""
            SELECT a FROM GLTransactionApproval a
            WHERE a.transaction.id = :transactionId
            ORDER BY a.approvalLevel ASC, a.approvalTimestamp ASC
            """)
    List<GLTransactionApproval> findByTransactionId(@Param("transactionId") UUID transactionId);

    /**
     * Count approvals (APPROVED action only) for a transaction.
     * Used to check if all required approval levels have been obtained.
     *
     * @param transactionId the transaction ID
     * @return count of approval records with APPROVED action
     */
    @Query("""
            SELECT COUNT(a) FROM GLTransactionApproval a
            WHERE a.transaction.id = :transactionId
            AND a.action = com.openfinova.banking.gl.api.entity.ApprovalAction.APPROVED
            """)
    int countApprovals(@Param("transactionId") UUID transactionId);

    /**
     * Find all transactions pending approval for a specific approver.
     * Used to build approval queue/dashboard.
     *
     * @param approverUsername the approver's username
     * @return list of pending approval transactions
     */
    @Query("""
            SELECT DISTINCT a.transaction FROM GLTransactionApproval a
            WHERE a.approvedBy = :approverUsername
            AND a.transaction.status = com.openfinova.banking.gl.api.entity.GLTransactionStatus.PENDING_APPROVAL
            ORDER BY a.transaction.submittedAt ASC
            """)
    List<UUID> findPendingTransactionsForApprover(@Param("approverUsername") String approverUsername);

    /**
     * Check if a specific user has already approved a transaction.
     * Prevents same person approving multiple levels.
     *
     * @param transactionId the transaction ID
     * @param approverUsername the approver's username
     * @return true if user has already approved this transaction
     */
    @Query("""
            SELECT CASE WHEN COUNT(a) > 0 THEN true ELSE false END
            FROM GLTransactionApproval a
            WHERE a.transaction.id = :transactionId
            AND a.approvedBy = :approverUsername
            AND a.action = com.openfinova.banking.gl.api.entity.ApprovalAction.APPROVED
            """)
    boolean hasUserApproved(@Param("transactionId") UUID transactionId,
            @Param("approverUsername") String approverUsername);

    /**
     * Find the highest approval level reached for a transaction.
     *
     * @param transactionId the transaction ID
     * @return the highest approval level, or 0 if no approvals
     */
    @Query("""
            SELECT COALESCE(MAX(a.approvalLevel), 0) FROM GLTransactionApproval a
            WHERE a.transaction.id = :transactionId
            AND a.action = com.openfinova.banking.gl.api.entity.ApprovalAction.APPROVED
            """)
    Integer getHighestApprovalLevel(@Param("transactionId") UUID transactionId);

    /**
     * Find all approvals by a specific user within a date range.
     * Used for user activity reports and audit.
     *
     * @param approverUsername the approver's username
     * @return list of approval records
     */
    @Query("""
            SELECT a FROM GLTransactionApproval a
            WHERE a.approvedBy = :approverUsername
            ORDER BY a.approvalTimestamp DESC
            """)
    List<GLTransactionApproval> findByApprovedBy(@Param("approverUsername") String approverUsername);

    /**
     * Find rejections for a transaction to get rejection history.
     *
     * @param transactionId the transaction ID
     * @return list of rejection records
     */
    @Query("""
            SELECT a FROM GLTransactionApproval a
            WHERE a.transaction.id = :transactionId
            AND a.action = com.openfinova.banking.gl.api.entity.ApprovalAction.REJECTED
            ORDER BY a.approvalTimestamp DESC
            """)
    List<GLTransactionApproval> findRejections(@Param("transactionId") UUID transactionId);
}
