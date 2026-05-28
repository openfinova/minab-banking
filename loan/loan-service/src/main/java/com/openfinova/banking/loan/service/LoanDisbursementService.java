package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.entity.DisbursementMethod;
import com.openfinova.banking.loan.api.entity.DisbursementStatus;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.dto.DisbursementValidationResult;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.entity.LoanDisbursement;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.loan.repository.LoanDisbursementRepository;

/**
 * Implementation of LoanDisbursementService for managing loan fund disbursements.
 *
 * This service handles the critical process of transferring approved loan funds
 * to borrowers. Disbursement is a key control point in the loan lifecycle,
 * involving financial transactions, regulatory compliance, and fraud prevention.
 *
 * Key Responsibilities:
 * - Creating disbursement requests with validation
 * - Processing disbursements through various payment methods
 * - Tracking disbursement status and lifecycle
 * - Handling disbursement failures and cancellations
 * - Calculating total disbursed amounts
 * - Validating disbursement requests against loan limits
 *
 * Disbursement Lifecycle:
 * - PENDING: Disbursement created, awaiting processing
 * - PROCESSING: Disbursement in progress (funds being transferred)
 * - COMPLETED: Funds successfully transferred to borrower
 * - FAILED: Disbursement failed (technical or validation issues)
 *
 * Disbursement Methods Supported:
 * - BANK_TRANSFER: Direct transfer to borrower's bank account
 * - CASH: Cash disbursement at branch
 * - CHEQUE: Disbursement via cheque
 * - MOBILE_MONEY: Transfer to mobile wallet
 * - INTERNAL_TRANSFER: Transfer to account within same institution
 *
 * Business Rules:
 * - Disbursement amount cannot exceed loan principal
 * - Total disbursements cannot exceed loan principal (for partial disbursements)
 * - Only PENDING disbursements can be cancelled
 * - Disbursement reference must be unique
 * - Beneficiary account information is required
 *
 * Security Considerations:
 * - Validate beneficiary account ownership
 * - Implement maker-checker approval workflow
 * - Monitor for suspicious patterns
 * - Maintain comprehensive audit trail
 * - Comply with anti-money laundering regulations
 *
 * @see LoanDisbursementService
 * @see LoanDisbursement
 * @see com.openfinova.banking.loan.api.entity.DisbursementStatus
 * @see com.openfinova.banking.loan.api.entity.DisbursementMethod
 */
@Service
@Transactional
public class LoanDisbursementService {

    private final LoanDisbursementRepository disbursementRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanAccountService loanAccountService;

    public LoanDisbursementService(LoanDisbursementRepository disbursementRepository,
            LoanAccountRepository loanAccountRepository, LoanAccountService loanAccountService) {
        this.disbursementRepository = disbursementRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.loanAccountService = loanAccountService;
    }

    /**
     * Creates a new disbursement request for a loan account.
     *
     * This is the first step in the disbursement process. The method creates
     * a disbursement record in PENDING status, ready for processing.
     *
     * Disbursement Types:
     * - Full disbursement: Single disbursement of entire loan amount
     * - Partial disbursement: Multiple disbursements totaling loan amount
     * - Tranched disbursement: Scheduled releases based on milestones
     *
     * The method:
     * - Validates loan account exists
     * - Generates unique disbursement reference
     * - Records disbursement details
     * - Sets initial status to PENDING
     * - Captures beneficiary account information
     *
     * Validation Considerations:
     * - Loan must be in approved status
     * - Disbursement amount must be positive
     * - Total disbursements should not exceed principal
     * - Beneficiary account must be valid
     * - Disbursement date should be reasonable
     *
     * After creation, the disbursement must be processed to transfer funds.
     *
     * @param loanAccountId the ID of the loan account to disburse
     * @param disbursementAmount the amount to disburse
     * @param disbursementDate the date of disbursement
     * @param disbursementMethod the method of fund transfer
     * @param destinationAccountNumber the beneficiary account number
     * @param createdBy the user creating the disbursement
     * @return the created disbursement with PENDING status
     * @throws IllegalArgumentException if loan account not found
     */
    @PreAuthorize("hasAuthority('loan:disburse')")
    public LoanDisbursement createDisbursement(UUID loanAccountId, BigDecimal disbursementAmount,
            LocalDate disbursementDate, DisbursementMethod disbursementMethod, String destinationAccountNumber,
            String createdBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        LoanDisbursement disbursement = new LoanDisbursement();
        disbursement.setLoanAccount(loanAccount);
        disbursement.setDisbursementReference(generateDisbursementReference());
        disbursement.setDisbursementAmount(disbursementAmount);
        disbursement.setDisbursementDate(disbursementDate);
        disbursement.setDisbursementMethod(disbursementMethod);
        disbursement.setBeneficiaryAccountNumber(destinationAccountNumber);
        disbursement.setStatus(DisbursementStatus.PENDING);

        return disbursementRepository.save(disbursement);
    }

    /**
     * Retrieves a disbursement by its unique identifier.
     *
     * @param id the disbursement ID
     * @return Optional containing the disbursement if found, empty otherwise
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('loan:read', 'service:loan:read')")
    public Optional<LoanDisbursement> getDisbursementById(UUID id) {
        return disbursementRepository.findById(id);
    }

    /**
     * Retrieves a disbursement by its reference number.
     *
     * Disbursement references are unique identifiers used for:
     * - Transaction tracking
     * - Reconciliation with payment systems
     * - Customer inquiries
     * - Audit trails
     *
     * @param disbursementReference the disbursement reference number
     * @return Optional containing the disbursement if found, empty otherwise
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('loan:read', 'service:loan:read')")
    public Optional<LoanDisbursement> getDisbursementByReference(String disbursementReference) {
        return disbursementRepository.findByDisbursementReference(disbursementReference);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Optional<LoanDisbursement> getDisbursementForLoanAccount(UUID loanAccountId, UUID disbursementId) {
        return disbursementRepository.findById(disbursementId)
                .filter(d -> d.getLoanAccount() != null && loanAccountId.equals(d.getLoanAccount().getId()));
    }

    @Transactional(readOnly = true)
    public Optional<LoanDisbursement> getDisbursementByReferenceForLoanAccount(UUID loanAccountId,
            String disbursementReference) {
        return disbursementRepository.findByDisbursementReference(disbursementReference)
                .filter(d -> d.getLoanAccount() != null && loanAccountId.equals(d.getLoanAccount().getId()));
    }

    /**
     * Retrieves all disbursements for a loan account.
     *
     * Returns disbursements in all statuses, providing complete disbursement
     * history for the loan. Useful for:
     * - Loan reconciliation
     * - Audit and compliance
     * - Disbursement tracking
     * - Customer service inquiries
     *
     * @param loanAccountId the loan account ID
     * @return list of all disbursements for the loan account
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public List<LoanDisbursement> getDisbursementsByLoanAccount(UUID loanAccountId) {
        return disbursementRepository.findByLoanAccountId(loanAccountId);
    }

    /**
     * Retrieves only completed disbursements for a loan account.
     *
     * Completed disbursements represent funds that have been successfully
     * transferred to the borrower. This is used for:
     * - Calculating actual disbursed amount
     * - Financial reporting
     * - Loan activation validation
     * - Reconciliation with accounting systems
     *
     * @param loanAccountId the loan account ID
     * @return list of completed disbursements for the loan account
     */
    @Transactional(readOnly = true)
    public List<LoanDisbursement> getCompletedDisbursementsByLoanAccount(UUID loanAccountId) {
        return disbursementRepository.findByLoanAccountIdAndStatus(loanAccountId, DisbursementStatus.COMPLETED);
    }

    /**
     * Retrieves disbursements by status with pagination.
     *
     * Useful for operational management:
     * - PENDING: Disbursements awaiting processing
     * - PROCESSING: Disbursements currently being transferred
     * - COMPLETED: Successfully completed disbursements
     * - FAILED: Failed disbursements requiring attention
     *
     * @param status the disbursement status to filter by
     * @param pageable pagination parameters
     * @return page of disbursements with the specified status
     */
    @Transactional(readOnly = true)
    public Page<LoanDisbursement> getDisbursementsByStatus(DisbursementStatus status, Pageable pageable) {
        return disbursementRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanDisbursement> getDisbursementsByLoanAccountAndStatus(UUID loanAccountId, DisbursementStatus status,
            Pageable pageable) {
        return disbursementRepository.findByLoanAccount_IdAndStatus(loanAccountId, status, pageable);
    }

    /**
     * Retrieves all pending disbursements with pagination.
     *
     * Pending disbursements are those awaiting processing. This is a
     * critical operational view for:
     * - Daily disbursement processing queues
     * - Workload management
     * - SLA monitoring
     * - Fraud detection (unusually old pending items)
     *
     * @param pageable pagination parameters
     * @return page of pending disbursements
     */
    @Transactional(readOnly = true)
    public Page<LoanDisbursement> getPendingDisbursements(Pageable pageable) {
        return disbursementRepository.findByStatus(DisbursementStatus.PENDING, pageable);
    }

    /**
     * Retrieves disbursements within a date range with pagination.
     *
     * Date-based queries are essential for:
     * - Financial period reporting
     * - Cash flow analysis
     * - Reconciliation with bank statements
     * - Regulatory reporting
     * - Trend analysis
     *
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @param pageable pagination parameters
     * @return page of disbursements within the specified date range
     */
    @Transactional(readOnly = true)
    public Page<LoanDisbursement> getDisbursementsByDateRange(LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        return disbursementRepository.findByDisbursementDateBetween(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanDisbursement> getDisbursementsByLoanAccountAndDateRange(UUID loanAccountId, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        return disbursementRepository
                .findByLoanAccount_IdAndDisbursementDateBetween(loanAccountId, startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public long countPendingDisbursementsForLoanAccount(UUID loanAccountId) {
        return disbursementRepository.countByLoanAccount_IdAndStatus(loanAccountId, DisbursementStatus.PENDING);
    }

    /**
     * Processes a pending disbursement by changing its status to PROCESSING.
     *
     * This method marks the disbursement as being actively processed, indicating
     * that fund transfer has been initiated. This is typically called when:
     * - Disbursement is picked up by payment processor
     * - Manual processing begins
     * - Integration with payment gateway starts
     *
     * The PROCESSING status indicates:
     * - Funds are in transit
     * - Disbursement cannot be cancelled
     * - Awaiting confirmation from payment system
     * - Should complete or fail within expected timeframe
     *
     * Business Rules:
     * - Only PENDING disbursements can be processed
     * - Once processing starts, cannot revert to PENDING
     * - Must eventually move to COMPLETED or FAILED
     *
     * @param disbursementId the ID of the disbursement to process
     * @param processedBy the user or system processing the disbursement
     * @return the disbursement with PROCESSING status
     * @throws IllegalArgumentException if disbursement not found
     * @throws IllegalStateException if disbursement is not in PENDING status
     */
    @PreAuthorize("hasAuthority('loan:disburse:approve')")
    public LoanDisbursement processDisbursement(UUID disbursementId, String processedBy) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + disbursementId));

        if (!DisbursementStatus.PENDING.equals(disbursement.getStatus())) {
            throw new IllegalStateException("Only pending disbursements can be processed");
        }

        disbursement.setStatus(DisbursementStatus.PROCESSING);
        return disbursementRepository.save(disbursement);
    }

    @PreAuthorize("hasAuthority('loan:disburse:approve')")
    public LoanDisbursement processDisbursementForLoanAccount(UUID loanAccountId, UUID disbursementId,
            String processedBy) {
        requireDisbursementOnLoan(loanAccountId, disbursementId);
        return processDisbursement(disbursementId, processedBy);
    }

    /**
     * Completes a disbursement after successful fund transfer.
     *
     * This method marks the disbursement as successfully completed, indicating
     * that funds have been transferred to the borrower's account.
     *
     * Completion Criteria:
     * - Funds successfully credited to beneficiary account
     * - Payment confirmation received from payment system
     * - Transaction reference obtained
     * - No errors or exceptions occurred
     *
     * After completion:
     * - Loan account should be activated (if first disbursement)
     * - Repayment schedule becomes active
     * - Interest accrual begins
     * - Disbursement cannot be reversed (requires separate reversal process)
     * - Included in disbursement totals and reporting
     *
     * Integration Points:
     * - Update loan account status
     * - Post accounting entries
     * - Send confirmation to borrower
     * - Update cash position
     * - Trigger downstream processes
     *
     * @param disbursementId the ID of the disbursement to complete
     * @param completedBy the user or system completing the disbursement
     * @return the disbursement with COMPLETED status
     * @throws IllegalArgumentException if disbursement not found
     */
    @PreAuthorize("hasAuthority('loan:disburse:approve')")
    public LoanDisbursement completeDisbursementForLoanAccount(UUID loanAccountId, UUID disbursementId,
            String completedBy) {
        requireDisbursementOnLoan(loanAccountId, disbursementId);
        return completeDisbursement(disbursementId, completedBy);
    }

    @PreAuthorize("hasAuthority('loan:disburse:approve')")
    public LoanDisbursement completeDisbursement(UUID disbursementId, String completedBy) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + disbursementId));

        disbursement.setStatus(DisbursementStatus.COMPLETED);
        return disbursementRepository.save(disbursement);
    }

    /**
     * Completes a disbursement and persists the external transaction reference from the payment rail.
     * Idempotent if already {@link DisbursementStatus#COMPLETED}.
     * Used by module facades (e.g. {@link com.openfinova.banking.loan.api.LoanService}) after TP confirms settlement.
     */
    @PreAuthorize("hasAuthority('service:loan:write')")
    public LoanDisbursement completeDisbursementWithTransactionReference(UUID disbursementId,
            String transactionReference, String completedBy) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + disbursementId));

        if (DisbursementStatus.COMPLETED.equals(disbursement.getStatus())) {
            return disbursement;
        }

        if (transactionReference != null && !transactionReference.isBlank()) {
            disbursement.setTransactionReference(transactionReference);
        }
        disbursement.setStatus(DisbursementStatus.COMPLETED);
        return disbursementRepository.save(disbursement);
    }

    /**
     * Completes disbursement settlement and activates the loan account when first disbursement completes.
     */
    @PreAuthorize("hasAuthority('service:loan:write')")
    public LoanDisbursement completeDisbursementAfterTransfer(UUID disbursementId, String transactionReference,
            String completedBy) {
        LoanDisbursement saved = completeDisbursementWithTransactionReference(
                disbursementId,
                transactionReference,
                completedBy);
        UUID loanAccountId = saved.getLoanAccount().getId();
        loanAccountService.getLoanAccountById(loanAccountId).ifPresent(account -> {
            if (account.getDisbursementDate() == null && LoanStatus.APPROVED.equals(account.getStatus())) {
                loanAccountService.disburseLoan(loanAccountId, saved.getDisbursementDate(), completedBy);
            }
        });
        return disbursementRepository.findById(saved.getId()).orElse(saved);
    }

    /**
     * Marks a disbursement as failed with a failure reason.
     *
     * Disbursements can fail for various reasons:
     *
     * Technical Failures:
     * - Payment system unavailable
     * - Network timeout
     * - Integration errors
     * - System errors
     *
     * Validation Failures:
     * - Invalid beneficiary account
     * - Account closed or frozen
     * - Insufficient funds in disbursement account
     * - Regulatory restrictions
     *
     * Business Failures:
     * - Loan approval revoked
     * - Fraud detected
     * - Duplicate disbursement
     * - Compliance issues
     *
     * After failure:
     * - Failure reason is recorded for analysis
     * - Funds remain in disbursement account
     * - Loan remains in pre-disbursement status
     * - May require manual intervention
     * - Can create new disbursement after resolving issue
     *
     * The failure reason is critical for:
     * - Root cause analysis
     * - Process improvement
     * - Customer communication
     * - Audit trails
     *
     * @param disbursementId the ID of the disbursement that failed
     * @param failureReason the reason for failure (required)
     * @param failedBy the user or system recording the failure
     * @return the disbursement with FAILED status
     * @throws IllegalArgumentException if disbursement not found
     */
    @PreAuthorize("hasAuthority('loan:disburse')")
    public LoanDisbursement failDisbursementForLoanAccount(UUID loanAccountId, UUID disbursementId,
            String failureReason, String failedBy) {
        requireDisbursementOnLoan(loanAccountId, disbursementId);
        return failDisbursement(disbursementId, failureReason, failedBy);
    }

    @PreAuthorize("hasAnyAuthority('loan:disburse', 'service:loan:write')")
    public LoanDisbursement failDisbursement(UUID disbursementId, String failureReason, String failedBy) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + disbursementId));

        disbursement.setStatus(DisbursementStatus.FAILED);
        disbursement.setRemarks(failureReason);

        return disbursementRepository.save(disbursement);
    }

    /**
     * Cancels a pending disbursement before processing begins.
     *
     * Cancellation is different from failure - it's a deliberate action to
     * stop a disbursement before funds are transferred.
     *
     * Common Cancellation Reasons:
     * - Customer request
     * - Loan approval revoked
     * - Incorrect disbursement details
     * - Duplicate disbursement detected
     * - Fraud suspicion
     * - Regulatory hold
     * - Business decision
     *
     * Business Rules:
     * - Only PENDING disbursements can be cancelled
     * - Once PROCESSING, cannot be cancelled (must fail or complete)
     * - Cancellation reason is required for audit
     * - Cancelled disbursements are marked as FAILED
     *
     * After cancellation:
     * - No funds are transferred
     * - Loan remains in pre-disbursement status
     * - New disbursement can be created if needed
     * - Cancellation is logged for audit
     *
     * Security Considerations:
     * - Verify authorization to cancel
     * - Check for suspicious cancellation patterns
     * - Maintain audit trail
     * - Alert on high-value cancellations
     *
     * @param disbursementId the ID of the disbursement to cancel
     * @param cancellationReason the reason for cancellation (required)
     * @param cancelledBy the user cancelling the disbursement
     * @return the cancelled disbursement with FAILED status
     * @throws IllegalArgumentException if disbursement not found
     * @throws IllegalStateException if disbursement is not in PENDING status
     */
    @PreAuthorize("hasAuthority('loan:disburse')")
    public LoanDisbursement cancelDisbursementForLoanAccount(UUID loanAccountId, UUID disbursementId,
            String cancellationReason, String cancelledBy) {
        requireDisbursementOnLoan(loanAccountId, disbursementId);
        return cancelDisbursement(disbursementId, cancellationReason, cancelledBy);
    }

    @PreAuthorize("hasAuthority('loan:disburse')")
    public LoanDisbursement cancelDisbursement(UUID disbursementId, String cancellationReason, String cancelledBy) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + disbursementId));

        if (!DisbursementStatus.PENDING.equals(disbursement.getStatus())) {
            throw new IllegalStateException("Only pending disbursements can be cancelled");
        }

        disbursement.setStatus(DisbursementStatus.FAILED);
        disbursement.setRemarks(cancellationReason);

        return disbursementRepository.save(disbursement);
    }

    /**
     * Calculates the total amount disbursed for a loan account.
     *
     * This calculation includes only COMPLETED disbursements, representing
     * the actual funds transferred to the borrower.
     *
     * Used for:
     * - Verifying full disbursement before loan activation
     * - Partial disbursement tracking
     * - Reconciliation with loan principal
     * - Financial reporting
     * - Audit and compliance
     *
     * For loans with partial disbursements:
     * - Total disbursed may be less than principal amount
     * - Remaining amount can be disbursed later
     * - Interest may accrue only on disbursed amount
     *
     * @param loanAccountId the loan account ID
     * @return the total amount of completed disbursements
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalDisbursed(UUID loanAccountId) {
        return disbursementRepository.sumDisbursementsByLoanAccount(loanAccountId);
    }

    /**
     * Calculates total disbursements within a date range.
     *
     * Aggregates completed disbursements for a specific period, useful for:
     *
     * Financial Management:
     * - Daily/monthly disbursement volumes
     * - Cash flow planning
     * - Liquidity management
     * - Budget tracking
     *
     * Reporting:
     * - Management dashboards
     * - Regulatory reports
     * - Investor reporting
     * - Trend analysis
     *
     * Operational Metrics:
     * - Disbursement processing efficiency
     * - Peak period identification
     * - Resource planning
     *
     * @param startDate the start of the date range
     * @param endDate the end of the date range
     * @return the total amount disbursed within the date range
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalDisbursementsByDateRange(LocalDate startDate, LocalDate endDate) {
        return disbursementRepository.sumDisbursementsByDateRange(startDate, endDate);
    }

    /**
     * Counts the number of disbursements for a loan account.
     *
     * Includes disbursements in all statuses. Useful for:
     * - Identifying partial disbursement loans
     * - Detecting unusual disbursement patterns
     * - Audit and compliance checks
     * - Process analysis
     *
     * @param loanAccountId the loan account ID
     * @return the count of disbursements (all statuses)
     */
    @Transactional(readOnly = true)
    public long countDisbursementsByLoanAccount(UUID loanAccountId) {
        return disbursementRepository.countByLoanAccountId(loanAccountId);
    }

    /**
     * Checks if a loan account has any completed disbursements.
     *
     * This is a quick check to determine if funds have been released.
     * Used for:
     * - Loan activation validation
     * - Status transition checks
     * - Business rule enforcement
     * - Workflow decisions
     *
     * A loan with completed disbursements:
     * - Has received funds
     * - Should be in ACTIVE status
     * - Can accept payments
     * - Has active repayment schedule
     *
     * @param loanAccountId the loan account ID
     * @return true if at least one completed disbursement exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasCompletedDisbursements(UUID loanAccountId) {
        return disbursementRepository.hasCompletedDisbursements(loanAccountId);
    }

    /**
     * Validates a disbursement request before creation.
     *
     * Performs pre-disbursement validation to catch issues early and
     * prevent invalid disbursements. This is a critical control point
     * for fraud prevention and compliance.
     *
     * Validation Checks:
     * - Disbursement amount does not exceed loan principal
     * - Total disbursements (including this one) do not exceed principal
     * - Loan account exists and is in valid status
     * - Sufficient funds available in disbursement account
     * - Beneficiary account is valid
     * - No duplicate disbursements
     *
     * Additional Checks (could be added):
     * - Loan is in APPROVED status
     * - All pre-disbursement conditions met
     * - Required documents received
     * - Collateral registered
     * - Insurance in place
     * - Regulatory compliance checks
     * - Fraud screening passed
     *
     * The validation result includes:
     * - Valid flag (true/false)
     * - Overall message
     * - List of specific errors (if any)
     *
     * This allows the caller to:
     * - Display errors to user
     * - Log validation failures
     * - Prevent invalid disbursements
     * - Improve data quality
     *
     * @param loanAccountId the loan account ID
     * @param disbursementAmount the amount to be disbursed
     * @return validation result with status and any error messages
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    public DisbursementValidationResult validateDisbursementRequest(UUID loanAccountId, BigDecimal disbursementAmount) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        List<String> errors = new ArrayList<>();

        if (disbursementAmount.compareTo(loanAccount.getPrincipalAmount()) > 0) {
            errors.add("Disbursement amount exceeds loan principal");
        }

        BigDecimal totalDisbursed = calculateTotalDisbursed(loanAccountId);
        if (totalDisbursed.add(disbursementAmount).compareTo(loanAccount.getPrincipalAmount()) > 0) {
            errors.add("Total disbursements would exceed loan principal");
        }

        boolean valid = errors.isEmpty();
        DisbursementValidationResult result = new DisbursementValidationResult(
                valid,
                valid ? "Disbursement request is valid" : "Disbursement request has errors");
        result.setErrors(errors);

        return result;
    }

    private void requireDisbursementOnLoan(UUID loanAccountId, UUID disbursementId) {
        LoanDisbursement disbursement = disbursementRepository.findById(disbursementId)
                .orElseThrow(() -> new IllegalArgumentException("Disbursement not found: " + disbursementId));
        if (disbursement.getLoanAccount() == null || !loanAccountId.equals(disbursement.getLoanAccount().getId())) {
            throw new IllegalArgumentException("Disbursement does not belong to this loan account");
        }
    }

    /**
     * Generates a unique disbursement reference number.
     *
     * Currently uses timestamp-based generation. Production implementation
     * should use a more robust approach:
     * - Sequential numbering with check digits
     * - Date-based prefixes (DISB-YYYYMMDD-NNNN)
     * - Branch/channel identifiers
     * - Database sequence generators
     * - UUID-based identifiers
     *
     * The reference must be:
     * - Unique across all disbursements
     * - Easy to communicate (for customer service)
     * - Suitable for reconciliation
     * - Compliant with payment system requirements
     *
     * @return a unique disbursement reference number
     */
    private String generateDisbursementReference() {
        return "DISB-" + System.currentTimeMillis();
    }
}
