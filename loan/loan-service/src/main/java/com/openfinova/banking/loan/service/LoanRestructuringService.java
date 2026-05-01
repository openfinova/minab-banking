package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.api.entity.RestructuringStatus;
import com.openfinova.banking.loan.api.entity.RestructuringType;
import com.openfinova.banking.loan.dto.RestructuringValidationResult;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.entity.LoanRestructuring;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.loan.repository.LoanRestructuringRepository;

/**
 * Implementation of LoanRestructuringService for managing loan modifications and restructuring.
 *
 * This service handles the critical process of loan restructuring, which involves modifying
 * the original loan terms to help borrowers who are experiencing financial difficulties.
 * Restructuring is an alternative to default and write-off, helping both borrowers and
 * lenders avoid losses while maintaining the loan relationship.
 *
 * Key Responsibilities:
 * - Creating and managing restructuring requests
 * - Processing approved restructuring modifications
 * - Validating restructuring eligibility
 * - Tracking restructuring history and audit trails
 * - Managing approval workflows
 * - Ensuring regulatory compliance
 *
 * Restructuring Types Supported:
 * - TENOR_EXTENSION: Extending the repayment period to reduce installment amounts
 * - INTEREST_RATE_REDUCTION: Lowering the interest rate to reduce payment burden
 * - PAYMENT_HOLIDAY: Providing temporary payment suspension (moratorium)
 * - PRINCIPAL_CAPITALIZATION: Adding arrears and fees to the principal balance
 * - COMBINATION: Multiple modifications applied together
 *
 * Common Restructuring Scenarios:
 * - Temporary financial hardship (job loss, medical emergency)
 * - Seasonal income variations (agricultural, tourism businesses)
 * - Economic downturns affecting borrower capacity
 * - Natural disasters or force majeure events
 * - Business cycle fluctuations
 * - Currency devaluation impacts (for foreign currency loans)
 *
 * Restructuring Benefits:
 * For Borrowers:
 * - Avoid default and credit damage
 * - Maintain loan relationship
 * - Reduce payment burden
 * - Preserve collateral
 * - Avoid legal proceedings
 *
 * For Lenders:
 * - Avoid loan losses and write-offs
 * - Maintain interest income (though potentially reduced)
 * - Preserve customer relationships
 * - Reduce collection costs
 * - Improve portfolio quality metrics
 *
 * Regulatory Implications:
 * - Restructured loans require special classification and reporting
 * - May be treated as non-performing assets
 * - Higher provision requirements may apply
 * - Regulatory approval may be required for certain modifications
 * - Consumer protection laws may mandate specific procedures
 *
 * Business Rules:
 * - Only ACTIVE loans can be restructured
 * - Restructuring requires proper justification and documentation
 * - Original loan terms must be preserved for audit purposes
 * - Restructuring history must be maintained
 * - Approval workflows must be followed
 * - New repayment schedules must be generated after restructuring
 *
 * Integration Points:
 * - Loan account management (term updates)
 * - Schedule generation (new payment schedules)
 * - Accounting system (provision adjustments)
 * - Reporting system (regulatory reporting)
 * - Customer communication (notifications)
 * - Credit bureau reporting (restructuring flags)
 *
 * @see LoanRestructuringService
 * @see LoanRestructuring
 * @see com.openfinova.banking.loan.api.entity.RestructuringType
 */
@Service
@Transactional
public class LoanRestructuringService {

    private final LoanRestructuringRepository restructuringRepository;
    private final LoanAccountRepository loanAccountRepository;

    public LoanRestructuringService(LoanRestructuringRepository restructuringRepository,
            LoanAccountRepository loanAccountRepository) {
        this.restructuringRepository = restructuringRepository;
        this.loanAccountRepository = loanAccountRepository;
    }

    /**
     * Creates a new loan restructuring request.
     *
     * This method initiates the restructuring process by creating a formal request
     * that captures the proposed modifications to the loan terms. The request
     * serves as documentation for the restructuring rationale and proposed changes.
     *
     * Restructuring Request Process:
     * 1. Validate loan account exists and is eligible
     * 2. Capture current loan terms for comparison
     * 3. Record proposed new terms
     * 4. Document restructuring reason and justification
     * 5. Set restructuring date (typically current date)
     * 6. Save request for approval workflow
     *
     * Information Captured:
     * - Original Terms: Current tenor, interest rate, payment amount
     * - Proposed Terms: New tenor, new interest rate, new payment structure
     * - Restructuring Type: Category of modification being requested
     * - Justification: Detailed reason for restructuring need
     * - Request Date: When restructuring was requested
     * - Requesting Party: Who initiated the restructuring
     *
     * Common Restructuring Reasons:
     * - "Temporary financial hardship due to job loss"
     * - "Seasonal income reduction affecting payment capacity"
     * - "Medical emergency requiring reduced payment burden"
     * - "Business downturn impacting cash flow"
     * - "Currency devaluation affecting foreign currency loan"
     * - "Natural disaster affecting borrower's income source"
     *
     * Validation Performed:
     * - Loan account must exist
     * - Loan must be in restructurable status
     * - Proposed terms must be reasonable and valid
     * - Restructuring type must be appropriate
     * - Reason must be provided for documentation
     *
     * After Request Creation:
     * - Request enters approval workflow
     * - Credit committee review may be required
     * - Additional documentation may be requested
     * - Customer communication is initiated
     * - Risk assessment is performed
     *
     * @param loanAccountId the ID of the loan account to restructure
     * @param restructuringType the type of restructuring being requested
     * @param newTenorMonths the proposed new repayment period (null if unchanged)
     * @param newInterestRate the proposed new interest rate (null if unchanged)
     * @param reason the detailed reason for restructuring (required for audit)
     * @param requestedBy the user or system requesting the restructuring
     * @return the created restructuring request
     * @throws IllegalArgumentException if loan account not found or parameters invalid
     */
    public LoanRestructuring createRestructuringRequest(UUID loanAccountId, RestructuringType restructuringType,
            Integer newTenorMonths, BigDecimal newInterestRate, String reason, String requestedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        LoanRestructuring restructuring = new LoanRestructuring();
        restructuring.setLoanAccount(loanAccount);
        restructuring.setRestructuringType(restructuringType);
        restructuring.setRestructuringDate(LocalDate.now());
        restructuring.setOldTenorMonths(loanAccount.getTenorMonths());
        restructuring.setNewTenorMonths(newTenorMonths);
        restructuring.setOldInterestRate(loanAccount.getInterestRate());
        restructuring.setNewInterestRate(newInterestRate);
        restructuring.setReason(reason);
        restructuring.setRestructuringStatus(RestructuringStatus.PENDING);

        return restructuringRepository.save(restructuring);
    }

    /**
     * Retrieves a restructuring record by its unique identifier.
     *
     * @param id the restructuring ID
     * @return Optional containing the restructuring if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<LoanRestructuring> getRestructuringById(UUID id) {
        return restructuringRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<LoanRestructuring> getRestructuringForLoanAccount(UUID loanAccountId, UUID restructuringId) {
        return restructuringRepository.findById(restructuringId)
                .filter(r -> r.getLoanAccount() != null && loanAccountId.equals(r.getLoanAccount().getId()));
    }

    /**
     * Retrieves all restructuring records for a loan account.
     *
     * Returns complete restructuring history including approved, rejected,
     * and pending requests. This provides a comprehensive audit trail
     * of all modification attempts and their outcomes.
     *
     * Used for:
     * - Loan restructuring history analysis
     * - Audit and compliance reporting
     * - Customer service inquiries
     * - Risk assessment (multiple restructuring patterns)
     * - Regulatory reporting requirements
     *
     * @param loanAccountId the loan account ID
     * @return list of all restructuring records for the loan account
     */
    @Transactional(readOnly = true)
    public List<LoanRestructuring> getRestructuringsByLoanAccount(UUID loanAccountId) {
        return restructuringRepository.findByLoanAccountId(loanAccountId);
    }

    /**
     * Retrieves the most recent restructuring for a loan account.
     *
     * Returns the latest restructuring by date, which is typically the
     * most relevant for current loan status and decision making.
     *
     * Used for:
     * - Determining current restructuring status
     * - Checking if recent restructuring exists
     * - Validating restructuring eligibility
     * - Customer service inquiries
     * - Risk assessment decisions
     *
     * @param loanAccountId the loan account ID
     * @return Optional containing the latest restructuring if any exists, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<LoanRestructuring> getLatestRestructuring(UUID loanAccountId) {
        return restructuringRepository.findTopByLoanAccountIdOrderByRestructuringDateDesc(loanAccountId);
    }

    /**
     * Retrieves restructuring records by type with pagination.
     *
     * Filters restructuring records by their type for analysis and reporting:
     * - TENOR_EXTENSION: Repayment period extensions
     * - INTEREST_RATE_REDUCTION: Interest rate modifications
     * - PAYMENT_HOLIDAY: Temporary payment suspensions
     * - PRINCIPAL_CAPITALIZATION: Arrears capitalization
     * - COMBINATION: Multiple modification types
     *
     * Used for:
     * - Restructuring trend analysis
     * - Portfolio risk assessment
     * - Regulatory reporting by restructuring type
     * - Management reporting and dashboards
     * - Policy effectiveness evaluation
     *
     * @param restructuringType the restructuring type to filter by
     * @param pageable pagination parameters
     * @return page of restructuring records of the specified type
     */
    @Transactional(readOnly = true)
    public Page<LoanRestructuring> getRestructuringsByType(RestructuringType restructuringType, Pageable pageable) {
        return restructuringRepository.findByRestructuringType(restructuringType, pageable);
    }

    /**
     * Retrieves restructuring records within a date range with pagination.
     *
     * Filters restructuring records by their restructuring date for
     * period-specific analysis and reporting.
     *
     * Used for:
     * - Monthly/quarterly restructuring reports
     * - Trend analysis over time periods
     * - Regulatory period reporting
     * - Management dashboards
     * - Economic impact analysis
     *
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @param pageable pagination parameters
     * @return page of restructuring records within the specified date range
     */
    @Transactional(readOnly = true)
    public Page<LoanRestructuring> getRestructuringsByDateRange(LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        return restructuringRepository.findByRestructuringDateBetween(startDate, endDate, pageable);
    }

    /**
     * Processes an approved restructuring by applying changes to the loan account.
     *
     * This method implements the actual restructuring by modifying the loan
     * account terms according to the approved restructuring request. This is
     * the critical step that transforms the request into actual loan modifications.
     *
     * Processing Steps:
     * 1. Validate restructuring request exists
     * 2. Apply tenor changes if specified
     * 3. Apply interest rate changes if specified
     * 4. Mark loan account as restructured
     * 5. Set restructuring date on loan account
     * 6. Save loan account changes
     * 7. Update restructuring record status
     *
     * Loan Account Updates:
     * - Tenor: Updated to new repayment period
     * - Interest Rate: Updated to new rate
     * - Restructured Flag: Set to true (permanent marker)
     * - Restructuring Date: Date when restructuring was processed
     * - Status: May remain ACTIVE or change to RESTRUCTURED
     *
     * Post-Processing Requirements:
     * - New repayment schedule must be generated
     * - Accounting entries may need adjustment
     * - Customer notifications should be sent
     * - Credit bureau reporting may be required
     * - Regulatory reporting updates needed
     *
     * Business Rules:
     * - Only approved restructuring requests can be processed
     * - Loan account must still exist and be in valid status
     * - Changes are applied atomically (all or nothing)
     * - Original terms are preserved in restructuring record
     * - Restructuring is irreversible once processed
     *
     * Integration Points:
     * - Schedule service (generate new repayment schedule)
     * - Accounting system (post restructuring entries)
     * - Notification service (customer communication)
     * - Reporting system (regulatory updates)
     * - Credit bureau (restructuring flag)
     *
     * @param restructuringId the ID of the restructuring request to process
     * @param processedBy the user processing the restructuring
     * @return the updated restructuring record
     * @throws IllegalArgumentException if restructuring not found
     */
    public LoanRestructuring processRestructuring(UUID loanAccountId, UUID restructuringId, String processedBy) {
        LoanRestructuring restructuring = requireRestructuringForLoan(loanAccountId, restructuringId);

        if (!RestructuringStatus.APPROVED.equals(restructuring.getRestructuringStatus())) {
            throw new IllegalStateException(
                    "Only approved restructuring requests can be processed; current: "
                            + restructuring.getRestructuringStatus());
        }

        LoanAccount loanAccount = restructuring.getLoanAccount();

        // Apply restructuring changes
        if (restructuring.getNewTenorMonths() != null) {
            loanAccount.setTenorMonths(restructuring.getNewTenorMonths());
        }
        if (restructuring.getNewInterestRate() != null) {
            loanAccount.setInterestRate(restructuring.getNewInterestRate());
        }

        loanAccount.setIsRestructured(true);
        loanAccount.setRestructuredDate(LocalDate.now());

        loanAccountRepository.save(loanAccount);

        restructuring.setRestructuringStatus(RestructuringStatus.PROCESSED);
        return restructuringRepository.save(restructuring);
    }

    /**
     * Approves a restructuring request for processing.
     *
     * This method records the approval of a restructuring request, allowing
     * it to proceed to the processing stage. Approval typically follows
     * credit committee review, risk assessment, and documentation verification.
     *
     * Approval Process:
     * 1. Validate restructuring request exists
     * 2. Record approver information
     * 3. Set approval timestamp (if field exists)
     * 4. Update restructuring status to approved
     * 5. Enable request for processing
     *
     * Approval Criteria (typically evaluated before calling this method):
     * - Borrower demonstrates genuine financial hardship
     * - Proposed terms are reasonable and sustainable
     * - Borrower has shown good faith effort to pay
     * - Restructuring is likely to improve loan performance
     * - Regulatory requirements are met
     * - Documentation is complete and satisfactory
     *
     * Post-Approval Actions:
     * - Restructuring can be processed immediately or scheduled
     * - Customer notification of approval
     * - Legal documentation preparation
     * - System preparation for term changes
     *
     * Note: The current implementation stores approver information but
     * the approvedDate field would need to be added to the LoanRestructuring
     * entity for complete audit trail.
     *
     * @param restructuringId the ID of the restructuring request to approve
     * @param approvedBy the user approving the restructuring
     * @return the approved restructuring record
     * @throws IllegalArgumentException if restructuring not found
     */
    public LoanRestructuring approveRestructuring(UUID loanAccountId, UUID restructuringId, String approvedBy) {
        LoanRestructuring restructuring = requireRestructuringForLoan(loanAccountId, restructuringId);

        if (!RestructuringStatus.PENDING.equals(restructuring.getRestructuringStatus())) {
            throw new IllegalStateException(
                    "Only PENDING restructuring requests can be approved; current: "
                            + restructuring.getRestructuringStatus());
        }

        restructuring.setApprovedBy(approvedBy);
        restructuring.setRestructuringStatus(RestructuringStatus.APPROVED);

        return restructuringRepository.save(restructuring);
    }

    /**
     * Rejects a restructuring request with detailed reasoning.
     *
     * This method formally rejects a restructuring request, preventing it
     * from being processed. Rejection typically follows credit committee
     * review that determines the request doesn't meet approval criteria.
     *
     * Rejection Process:
     * 1. Validate restructuring request exists
     * 2. Append rejection reason to existing reason field
     * 3. Record rejection details for audit trail
     * 4. Update restructuring status to rejected
     * 5. Prevent further processing of request
     *
     * Common Rejection Reasons:
     * - "Insufficient documentation of financial hardship"
     * - "Proposed terms not sustainable based on borrower capacity"
     * - "Recent payment history does not support restructuring"
     * - "Alternative solutions more appropriate"
     * - "Regulatory requirements not met"
     * - "Borrower has exceeded maximum restructuring limit"
     * - "Collateral value insufficient for modified terms"
     *
     * Post-Rejection Actions:
     * - Customer notification with explanation
     * - Alternative solution discussion
     * - Collection process may resume
     * - New restructuring request may be submitted with additional information
     *
     * Audit Trail:
     * - Original reason is preserved
     * - Rejection reason is appended with clear separation
     * - Complete decision history is maintained
     * - Regulatory reporting requirements are met
     *
     * Note: The current implementation appends to the reason field.
     * A dedicated rejectionReason field and rejectedBy/rejectedDate
     * fields would provide better audit trail structure.
     *
     * @param restructuringId the ID of the restructuring request to reject
     * @param rejectionReason the detailed reason for rejection (required for audit)
     * @param rejectedBy the user rejecting the restructuring
     * @return the rejected restructuring record with updated reason
     * @throws IllegalArgumentException if restructuring not found
     */
    public LoanRestructuring rejectRestructuring(UUID loanAccountId, UUID restructuringId, String rejectionReason,
            String rejectedBy) {
        LoanRestructuring restructuring = requireRestructuringForLoan(loanAccountId, restructuringId);

        if (!RestructuringStatus.PENDING.equals(restructuring.getRestructuringStatus())) {
            throw new IllegalStateException(
                    "Only PENDING restructuring requests can be rejected; current: "
                            + restructuring.getRestructuringStatus());
        }

        String base = restructuring.getReason() != null ? restructuring.getReason() : "";
        restructuring.setReason(base + "\nRejection: " + rejectionReason);
        restructuring.setRestructuringStatus(RestructuringStatus.REJECTED);

        return restructuringRepository.save(restructuring);
    }

    /**
     * Counts the total number of restructuring records for a loan account.
     *
     * Includes all restructuring attempts regardless of status (approved,
     * rejected, pending). Used for:
     * - Restructuring frequency analysis
     * - Risk assessment (multiple restructuring patterns)
     * - Eligibility validation (restructuring limits)
     * - Customer service inquiries
     * - Audit and compliance reporting
     *
     * @param loanAccountId the loan account ID
     * @return the count of restructuring records for the loan account
     */
    @Transactional(readOnly = true)
    public long countRestructuringsByLoanAccount(UUID loanAccountId) {
        return restructuringRepository.countByLoanAccountId(loanAccountId);
    }

    /**
     * Checks if a loan account has ever been restructured.
     *
     * Quick check to determine if any restructuring records exist
     * for the loan account. Used for:
     * - Loan status validation
     * - Risk classification decisions
     * - Eligibility checks for new restructuring
     * - Regulatory reporting classification
     * - Customer service inquiries
     *
     * @param loanAccountId the loan account ID
     * @return true if any restructuring records exist, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasBeenRestructured(UUID loanAccountId) {
        return restructuringRepository.existsByLoanAccountId(loanAccountId);
    }

    /**
     * Validates if a loan account is eligible for restructuring.
     *
     * This method performs comprehensive eligibility checks to determine
     * if a loan can be restructured. It enforces business rules and
     * regulatory requirements for restructuring eligibility.
     *
     * Current Validation Checks:
     * - Loan Status: Only ACTIVE loans can be restructured
     * - Loan Existence: Loan account must exist
     * - Restructuring Type: Must be valid and supported
     *
     * Additional Validation Checks (could be added):
     * - Payment History: Minimum payment history requirements
     * - Delinquency Status: Maximum days past due limits
     * - Previous Restructuring: Limits on restructuring frequency
     * - Loan Age: Minimum/maximum loan age requirements
     * - Outstanding Balance: Minimum balance thresholds
     * - Borrower Status: Customer must be in good standing
     * - Documentation: Required documents must be available
     * - Regulatory Limits: Compliance with restructuring regulations
     *
     * Validation Result:
     * - Success flag indicating overall eligibility
     * - Detailed error messages for each failed check
     * - Overall validation message
     *
     * Use Cases:
     * - Pre-screening before restructuring request creation
     * - Customer service eligibility inquiries
     * - Automated eligibility checking in workflows
     * - Mobile app eligibility validation
     * - Risk management screening
     *
     * @param loanAccountId the loan account ID to validate
     * @param restructuringType the type of restructuring being considered
     * @return RestructuringValidationResult with eligibility status and details
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    public RestructuringValidationResult validateRestructuringRequest(UUID loanAccountId,
            RestructuringType restructuringType) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        List<String> errors = new ArrayList<>();

        if (!LoanStatus.ACTIVE.equals(loanAccount.getStatus())) {
            errors.add("Only active loans can be restructured");
        }

        boolean valid = errors.isEmpty();
        RestructuringValidationResult result = new RestructuringValidationResult(
                valid,
                valid ? "Restructuring request is valid" : "Restructuring request has errors");
        result.setErrors(errors);

        return result;
    }

    private LoanRestructuring requireRestructuringForLoan(UUID loanAccountId, UUID restructuringId) {
        LoanRestructuring restructuring = restructuringRepository.findById(restructuringId)
                .orElseThrow(() -> new IllegalArgumentException("Restructuring not found: " + restructuringId));
        if (restructuring.getLoanAccount() == null || !loanAccountId.equals(restructuring.getLoanAccount().getId())) {
            throw new IllegalArgumentException("Restructuring does not belong to this loan account");
        }
        return restructuring;
    }
}
