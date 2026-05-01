package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.dto.ApprovalResponse;
import com.openfinova.banking.gl.dto.AuthorizationLimitResponse;
import com.openfinova.banking.gl.dto.PendingApprovalResponse;
import com.openfinova.banking.gl.entity.GLAuthorizationLimit;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.entity.GLTransactionApproval;
import com.openfinova.banking.gl.repository.GLAuthorizationLimitRepository;
import com.openfinova.banking.gl.repository.GLTransactionApprovalRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;

/**
 * Mapper for approval workflow entities to DTOs.
 * Handles conversion of approval-related entities to response DTOs for REST endpoints.
 */
@Component
public class ApprovalMapper {

    private static final Logger log = LoggerFactory.getLogger(ApprovalMapper.class);

    private final GLTransactionApprovalRepository approvalRepository;
    private final GLAuthorizationLimitRepository authLimitRepository;

    public ApprovalMapper(GLTransactionApprovalRepository approvalRepository,
            GLAuthorizationLimitRepository authLimitRepository) {
        this.approvalRepository = approvalRepository;
        this.authLimitRepository = authLimitRepository;
    }

    /**
     * Map a GL transaction to pending approval response DTO.
     * Includes transaction details and approval workflow status.
     *
     * @param transaction the transaction entity
     * @return pending approval response DTO
     */
    public PendingApprovalResponse toPendingApprovalResponse(GLTransaction transaction) {
        PendingApprovalResponse response = new PendingApprovalResponse();
        response.setTransactionId(transaction.getId());
        response.setReferenceId(transaction.getReferenceId());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setDescription(transaction.getDescription());
        response.setCurrency(transaction.getCurrency());
        response.setTotalAmount(calculateTransactionAmount(transaction));
        response.setSource(transaction.getSource());
        response.setStatus(transaction.getStatus());
        response.setSubmittedBy(transaction.getSubmittedBy());
        response.setSubmittedAt(transaction.getSubmittedAt());

        // Get approval info
        Integer currentLevel = approvalRepository.getHighestApprovalLevel(transaction.getId());
        Integer receivedApprovals = approvalRepository.countApprovals(transaction.getId());

        response.setCurrentApprovalLevel(currentLevel);
        response.setReceivedApprovals(receivedApprovals);

        // Get required approvals from authorization limit
        try {
            List<GLAuthorizationLimit> limits = authLimitRepository.findByRoleCurrencyAndSource(
                    GLApprovalRole.MANAGER, // Default - should be transaction-specific
                    transaction.getCurrency(),
                    transaction.getSource());
            if (!limits.isEmpty()) {
                response.setRequiredApprovals(limits.get(0).getRequiredApprovals());
            }
        } catch (Exception e) {
            log.warn("Could not determine required approvals for transaction {}", transaction.getId());
        }

        return response;
    }

    /**
     * Map a transaction approval entity to approval response DTO.
     * Includes approval action details and audit information.
     *
     * @param approval the approval entity
     * @return approval response DTO
     */
    public ApprovalResponse toApprovalResponse(GLTransactionApproval approval) {
        ApprovalResponse response = new ApprovalResponse();
        response.setId(approval.getId());
        response.setTransactionId(approval.getTransaction().getId());
        response.setApprovalLevel(approval.getApprovalLevel());
        response.setAction(approval.getAction());
        response.setApprovedBy(approval.getApprovedBy());
        response.setApprovalTimestamp(approval.getApprovalTimestamp());
        response.setComments(approval.getComments());
        response.setIpAddress(approval.getIpAddress());
        return response;
    }

    /**
     * Map an authorization limit entity to authorization limit response DTO.
     * Includes maker/approval limits and workflow configuration.
     *
     * @param limit the authorization limit entity
     * @return authorization limit response DTO
     */
    public AuthorizationLimitResponse toAuthorizationLimitResponse(GLAuthorizationLimit limit) {
        return new AuthorizationLimitResponse(
                limit.getId(),
                limit.getApprovalRole(),
                limit.getMakerLimit(),
                limit.getApprovalLimit(),
                limit.getCurrency(),
                limit.getTransactionSource(),
                limit.getRequiredApprovals(),
                limit.getIsActive());
    }

    /**
     * Calculate total transaction amount (sum of debits).
     * Transactions must balance, so total debits equals total credits.
     *
     * @param transaction the transaction entity
     * @return total transaction amount
     */
    public BigDecimal calculateTransactionAmount(GLTransaction transaction) {
        return transaction.getJournalEntries().stream()
                .map(entry -> entry.getDebitAmount() != null ? entry.getDebitAmount() : BigDecimal.ZERO)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
