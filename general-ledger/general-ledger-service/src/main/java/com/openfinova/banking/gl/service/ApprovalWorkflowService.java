package com.openfinova.banking.gl.service;

import com.openfinova.banking.gl.api.entity.ApprovalAction;
import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.entity.GLAuthorizationLimit;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.entity.GLTransactionApproval;
import com.openfinova.banking.gl.repository.GLTransactionApprovalRepository;
import com.openfinova.banking.gl.repository.GLTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Service for managing GL transaction approval workflow.
 *
 * Implements maker-checker approval process with:
 *    Authorization limits by role
 *    Multi-level approval support
 *    Self-approval prevention
 *    Audit trail of all approval actions
 *
 * Typical workflow:
 *   1. Maker creates draft transaction
 *   2. Maker submits for approval → validates maker limit
 *   3. Transaction enters PENDING_APPROVAL status
 *   4. Checker approves → validates approval limit, no self-approval
 *   5. If all required approvals received → transaction posted
 *   6. Approver can reject → transaction enters REJECTED status
 */
@Service
public class ApprovalWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(ApprovalWorkflowService.class);

    private final GLTransactionRepository transactionRepository;
    private final GLTransactionApprovalRepository approvalRepository;
    private final GLAuthorizationLimitQueryService authorizationLimitQueryService;

    public ApprovalWorkflowService(GLTransactionRepository transactionRepository,
            GLTransactionApprovalRepository approvalRepository,
            GLAuthorizationLimitQueryService authorizationLimitQueryService) {
        this.transactionRepository = transactionRepository;
        this.approvalRepository = approvalRepository;
        this.authorizationLimitQueryService = authorizationLimitQueryService;
    }

    /**
     * Submit a draft transaction for approval.
     *
     * @param transactionId ID of the transaction to submit
     * @param submitterUsername username of the person submitting
     * @param submitterRole role of the person submitting
     * @throws IllegalStateException if transaction is not in DRAFT status
     * @throws SecurityException if submitter doesn't have authority
     */
    @Transactional
    public void submitForApproval(UUID transactionId, String submitterUsername, GLApprovalRole submitterRole) {
        GLTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Validate transaction is in draft status
        if (!transaction.isDraft()) {
            throw new IllegalStateException(
                    "Only draft transactions can be submitted. Current status: " + transaction.getStatus());
        }

        // Validate submitter is the creator
        if (!submitterUsername.equals(transaction.getCreatedBy())) {
            throw new SecurityException("Only the transaction creator can submit for approval");
        }

        // Calculate transaction amount
        BigDecimal amount = calculateTransactionAmount(transaction);

        // Get authorization limit for submitter's role
        GLAuthorizationLimit limit = getAuthorizationLimit(
                submitterRole,
                transaction.getCurrency(),
                transaction.getSource());

        // Validate submitter can create this transaction amount
        if (!limit.canCreate(amount)) {
            throw new SecurityException(
                    String.format(
                            "Transaction amount %s %s exceeds your maker limit of %s",
                            transaction.getCurrency(),
                            amount,
                            limit.getMakerLimit()));
        }

        // Check if auto-approval applies (amount within approval limit and same person can approve)
        // For manual entries, typically require separate approver
        if (transaction.isSystemGenerated()) {
            // System-generated transactions must be handled by GLTransactionService
            // before reaching this method — this branch should never be reached.
            throw new IllegalStateException(
                    "System-generated transactions must not enter the approval workflow. "
                            + "Route them through GLTransactionService.submitTransactionForApproval() instead.");
        } else {
            // All manual entries require a separate checker approval (maker-checker principle).
            transaction.submitForApproval(submitterUsername);
            transactionRepository.save(transaction);
            logger.info("Transaction {} submitted for approval by {}", transactionId, submitterUsername);
        }
    }

    /**
     * Approve a pending transaction.
     *
     * @param transactionId ID of the transaction to approve
     * @param approverUsername username of the approver
     * @param approverRole role of the approver
     * @param comments optional comments from approver
     * @param ipAddress IP address of approver (for audit)
     * @return true if transaction is fully approved and ready to post
     * @throws IllegalStateException if transaction is not pending approval
     * @throws SecurityException if approver doesn't have authority
     */
    @Transactional
    public boolean approveTransaction(UUID transactionId, String approverUsername, GLApprovalRole approverRole,
            String comments, String ipAddress) {
        GLTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Validate transaction is pending approval
        if (!transaction.isPendingApproval()) {
            throw new IllegalStateException(
                    "Only pending transactions can be approved. Current status: " + transaction.getStatus());
        }

        // Prevent self-approval
        if (approverUsername.equals(transaction.getSubmittedBy())) {
            throw new SecurityException("Cannot approve your own transaction (self-approval not allowed)");
        }

        // Check if user has already approved (for multi-level approval)
        if (approvalRepository.hasUserApproved(transactionId, approverUsername)) {
            throw new SecurityException("You have already approved this transaction");
        }

        // Calculate transaction amount
        BigDecimal amount = calculateTransactionAmount(transaction);

        // Get authorization limit for approver's role
        GLAuthorizationLimit limit = getAuthorizationLimit(
                approverRole,
                transaction.getCurrency(),
                transaction.getSource());

        // Validate approver can approve this transaction amount
        if (!limit.canApprove(amount)) {
            throw new SecurityException(
                    String.format(
                            "Transaction amount %s %s exceeds your approval limit of %s",
                            transaction.getCurrency(),
                            amount,
                            limit.getApprovalLimit()));
        }

        // Get current approval level
        Integer currentLevel = approvalRepository.getHighestApprovalLevel(transactionId);
        Integer nextLevel = currentLevel + 1;

        // Record approval
        GLTransactionApproval approval = new GLTransactionApproval(
                transaction,
                nextLevel,
                ApprovalAction.APPROVED,
                approverUsername,
                comments);
        approval.setIpAddress(ipAddress);
        approvalRepository.save(approval);

        logger.info("Transaction {} approved by {} at level {}", transactionId, approverUsername, nextLevel);

        // Check if all required approvals received
        int requiredApprovals = limit.getRequiredApprovals();
        int receivedApprovals = approvalRepository.countApprovals(transactionId);

        if (receivedApprovals >= requiredApprovals) {
            logger.info(
                    "Transaction {} has received all required approvals ({}/{})",
                    transactionId,
                    receivedApprovals,
                    requiredApprovals);
            return true; // Ready to post
        } else {
            logger.info(
                    "Transaction {} needs more approvals ({}/{})",
                    transactionId,
                    receivedApprovals,
                    requiredApprovals);
            return false; // Needs more approvals
        }
    }

    /**
     * Reject a pending transaction.
     *
     * @param transactionId ID of the transaction to reject
     * @param rejecterUsername username of the person rejecting
     * @param rejecterRole role of the person rejecting
     * @param reason reason for rejection (required)
     * @param ipAddress IP address of rejecter (for audit)
     * @throws IllegalStateException if transaction is not pending approval
     * @throws IllegalArgumentException if reason is blank
     */
    @Transactional
    public void rejectTransaction(UUID transactionId, String rejecterUsername, GLApprovalRole rejecterRole,
            String reason, String ipAddress) {
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason is required");
        }

        GLTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Validate transaction is pending approval
        if (!transaction.isPendingApproval()) {
            throw new IllegalStateException(
                    "Only pending transactions can be rejected. Current status: " + transaction.getStatus());
        }

        // Prevent self-rejection (creator rejecting their own transaction)
        if (rejecterUsername.equals(transaction.getSubmittedBy())) {
            throw new SecurityException(
                    "Cannot reject your own transaction. Use cancel instead if you want to withdraw it.");
        }

        // Calculate transaction amount
        BigDecimal amount = calculateTransactionAmount(transaction);

        // Get authorization limit for rejecter's role
        GLAuthorizationLimit limit = getAuthorizationLimit(
                rejecterRole,
                transaction.getCurrency(),
                transaction.getSource());

        // Validate rejecter has authority (can approve = can reject)
        if (!limit.canApprove(amount)) {
            throw new SecurityException("Transaction amount exceeds your approval authority - cannot reject");
        }

        // Record rejection
        GLTransactionApproval rejection = new GLTransactionApproval(
                transaction,
                1, // Rejection stops at level 1
                ApprovalAction.REJECTED,
                rejecterUsername,
                reason);
        rejection.setIpAddress(ipAddress);
        approvalRepository.save(rejection);

        // Update transaction status
        transaction.reject();
        transactionRepository.save(transaction);

        logger.info("Transaction {} rejected by {}: {}", transactionId, rejecterUsername, reason);
    }

    /**
     * Cancel a draft transaction (withdrawn by creator before submission).
     *
     * @param transactionId ID of the transaction to cancel
     * @param username username of the person cancelling
     * @throws IllegalStateException if transaction is not in DRAFT status
     * @throws SecurityException if user is not the creator
     */
    @Transactional
    public void cancelTransaction(UUID transactionId, String username) {
        GLTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Validate transaction is in draft status
        if (!transaction.isDraft()) {
            throw new IllegalStateException(
                    "Only draft transactions can be cancelled. Current status: " + transaction.getStatus());
        }

        // Validate user is the creator
        if (!username.equals(transaction.getCreatedBy())) {
            throw new SecurityException("Only the transaction creator can cancel it");
        }

        transaction.cancel();
        transactionRepository.save(transaction);

        logger.info("Transaction {} cancelled by {}", transactionId, username);
    }

    /**
     * Calculate the total transaction amount (sum of debit or credit amounts).
     * Uses debit amounts as transactions must balance.
     *
     * @param transaction the transaction
     * @return total transaction amount
     */
    private BigDecimal calculateTransactionAmount(GLTransaction transaction) {
        return transaction.getJournalEntries().stream()
                .map(entry -> entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get the authorization limit for a role, currency, and source.
     * Returns most specific limit (source-specific preferred over default).
     *
     * @param GLApprovalRole the user's role
     * @param currency the currency
     * @param source the transaction source
     * @return authorization limit
     * @throws SecurityException if no limit found for role
     */
    private GLAuthorizationLimit getAuthorizationLimit(GLApprovalRole GLApprovalRole, String currency,
            com.openfinova.banking.gl.api.entity.GLTransactionSource source) {
        List<GLAuthorizationLimit> limits = authorizationLimitQueryService
                .findByRoleCurrencyAndSource(GLApprovalRole, currency, source);

        if (limits.isEmpty()) {
            throw new SecurityException(
                    String.format(
                            "No authorization limit configured for role %s, currency %s, source %s",
                            GLApprovalRole,
                            currency,
                            source));
        }

        // Return first result (most specific match)
        return limits.get(0);
    }

    /**
     * Get approval history for a transaction.
     *
     * @param transactionId the transaction ID
     * @return list of approval records
     */
    public List<GLTransactionApproval> getApprovalHistory(UUID transactionId) {
        return approvalRepository.findByTransactionId(transactionId);
    }

    /**
     * Check if a user can approve a specific transaction.
     *
     * @param transactionId the transaction ID
     * @param username the user's username
     * @param GLApprovalRole the user's role
     * @return true if user can approve
     */
    public boolean canUserApprove(UUID transactionId, String username, GLApprovalRole GLApprovalRole) {
        GLTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Can't approve if not pending
        if (!transaction.isPendingApproval()) {
            return false;
        }

        // Can't approve own transaction
        if (username.equals(transaction.getSubmittedBy())) {
            return false;
        }

        // Can't approve if already approved
        if (approvalRepository.hasUserApproved(transactionId, username)) {
            return false;
        }

        // Check authorization limit
        try {
            BigDecimal amount = calculateTransactionAmount(transaction);
            GLAuthorizationLimit limit = getAuthorizationLimit(
                    GLApprovalRole,
                    transaction.getCurrency(),
                    transaction.getSource());
            return limit.canApprove(amount);
        } catch (SecurityException e) {
            return false;
        }
    }
}
