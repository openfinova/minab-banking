package com.openfinova.banking.gl.service;

import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.api.entity.GLTransactionStatus;
import com.openfinova.banking.gl.dto.ApprovalResponse;
import com.openfinova.banking.gl.dto.AuthorizationLimitResponse;
import com.openfinova.banking.gl.dto.CanApproveResponse;
import com.openfinova.banking.gl.dto.PendingApprovalResponse;
import com.openfinova.banking.gl.entity.GLAuthorizationLimit;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.mapper.ApprovalMapper;
import com.openfinova.banking.gl.repository.GLAuthorizationLimitRepository;
import com.openfinova.banking.gl.repository.GLTransactionApprovalRepository;
import com.openfinova.banking.gl.repository.GLTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Service responsible for approval-queue queries, authorization-limit lookups,
 * and approval-eligibility checks. This is distinct from {@link ApprovalWorkflowService},
 * which handles the state-machine transitions (approve / reject / escalate).
 */
@Service
@Transactional(readOnly = true)
public class ApprovalService {

    private static final Logger log = LoggerFactory.getLogger(ApprovalService.class);

    private final GLTransactionRepository transactionRepository;
    private final GLTransactionApprovalRepository approvalRepository;
    private final GLAuthorizationLimitRepository authLimitRepository;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final ApprovalMapper approvalMapper;

    public ApprovalService(GLTransactionRepository transactionRepository,
            GLTransactionApprovalRepository approvalRepository, GLAuthorizationLimitRepository authLimitRepository,
            ApprovalWorkflowService approvalWorkflowService, ApprovalMapper approvalMapper) {
        this.transactionRepository = transactionRepository;
        this.approvalRepository = approvalRepository;
        this.authLimitRepository = authLimitRepository;
        this.approvalWorkflowService = approvalWorkflowService;
        this.approvalMapper = approvalMapper;
    }

    /**
     * Returns all transactions in PENDING_APPROVAL status that the given user is
     * eligible to approve.
     */
    public List<PendingApprovalResponse> getPendingApprovalsForUser(String username, GLApprovalRole role) {
        log.info("Fetching pending approvals queue for user: {} with role: {}", username, role);

        List<GLTransaction> pendingTransactions = transactionRepository
                .findByStatus(GLTransactionStatus.PENDING_APPROVAL.name());

        List<PendingApprovalResponse> queue = pendingTransactions.stream()
                .filter(txn -> canUserApproveTransaction(txn, username, role))
                .map(approvalMapper::toPendingApprovalResponse).collect(Collectors.toList());

        log.info("Found {} pending approvals for user: {}", queue.size(), username);
        return queue;
    }

    /**
     * Returns the full approval-action history performed by the given user.
     */
    public List<ApprovalResponse> getApprovalActivityForUser(String username) {
        log.info("Fetching approval activity history for user: {}", username);

        List<ApprovalResponse> activity = approvalRepository.findByApprovedBy(username).stream()
                .map(approvalMapper::toApprovalResponse).collect(Collectors.toList());

        log.info("Found {} approval actions for user: {}", activity.size(), username);
        return activity;
    }

    /**
     * Returns all authorization limits configured for the given role.
     * Returns an empty list when none are found, letting the caller decide on the HTTP status.
     */
    public List<AuthorizationLimitResponse> getAuthorizationLimitsForRole(GLApprovalRole role) {
        log.info("Fetching authorization limits for role: {}", role);

        List<AuthorizationLimitResponse> limits = authLimitRepository.findByRole(role).stream()
                .map(approvalMapper::toAuthorizationLimitResponse).collect(Collectors.toList());

        log.info("Found {} authorization limits for role: {}", limits.size(), role);
        return limits;
    }

    /**
     * Checks whether a user is permitted to approve the specified transaction.
     *
     * @return empty Optional when the transaction does not exist (caller maps to 404)
     */
    public Optional<CanApproveResponse> checkCanApprove(UUID transactionId, String username, GLApprovalRole role) {
        log.info("Checking if user {} with role {} can approve transaction {}", username, role, transactionId);

        Optional<GLTransaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            log.warn("Transaction not found: {}", transactionId);
            return Optional.empty();
        }

        GLTransaction transaction = transactionOpt.get();
        boolean canApprove = approvalWorkflowService.canUserApprove(transactionId, username, role);

        CanApproveResponse response;
        if (canApprove) {
            response = CanApproveResponse.allowed(transactionId);
            log.info("User {} CAN approve transaction {}", username, transactionId);
        } else {
            String reason = determineCannotApproveReason(transaction, username, role);
            response = CanApproveResponse.denied(transactionId, reason);
            log.info("User {} CANNOT approve transaction {}: {}", username, transactionId, reason);
        }

        return Optional.of(response);
    }

    // -----------------------------------------------------------------------
    // Private helpers (business logic — previously leaked into the controller)
    // -----------------------------------------------------------------------

    private boolean canUserApproveTransaction(GLTransaction transaction, String username, GLApprovalRole role) {
        if (!transaction.isPendingApproval()) {
            return false;
        }

        if (username.equals(transaction.getSubmittedBy())) {
            return false;
        }

        if (approvalRepository.hasUserApproved(transaction.getId(), username)) {
            return false;
        }

        try {
            BigDecimal amount = approvalMapper.calculateTransactionAmount(transaction);
            List<GLAuthorizationLimit> limits = authLimitRepository
                    .findByRoleCurrencyAndSource(role, transaction.getCurrency(), transaction.getSource());

            if (limits.isEmpty()) {
                return false;
            }

            return limits.get(0).canApprove(amount);
        } catch (Exception e) {
            log.error("Error checking approval authorization for transaction {}", transaction.getId(), e);
            return false;
        }
    }

    private String determineCannotApproveReason(GLTransaction transaction, String username, GLApprovalRole role) {
        if (!transaction.isPendingApproval()) {
            return "Transaction is not pending approval (status: " + transaction.getStatus() + ")";
        }

        if (username.equals(transaction.getSubmittedBy())) {
            return "Cannot approve your own transaction (self-approval not allowed)";
        }

        if (approvalRepository.hasUserApproved(transaction.getId(), username)) {
            return "You have already approved this transaction";
        }

        BigDecimal amount = approvalMapper.calculateTransactionAmount(transaction);
        List<GLAuthorizationLimit> limits = authLimitRepository
                .findByRoleCurrencyAndSource(role, transaction.getCurrency(), transaction.getSource());

        if (limits.isEmpty()) {
            return "No authorization limit configured for role " + role + " and currency " + transaction.getCurrency();
        }

        GLAuthorizationLimit limit = limits.get(0);
        if (!limit.canApprove(amount)) {
            return String.format(
                    "Transaction amount %s %s exceeds your approval limit of %s",
                    transaction.getCurrency(),
                    amount,
                    limit.getApprovalLimit());
        }

        return "Unknown reason";
    }
}
