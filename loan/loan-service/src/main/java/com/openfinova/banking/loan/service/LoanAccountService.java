package com.openfinova.banking.loan.service;

import com.openfinova.banking.customer.account.api.dto.ValidationResult;
import com.openfinova.banking.loan.api.dto.LoanStatementResponse;
import com.openfinova.banking.loan.api.entity.ApplicationStatus;
import com.openfinova.banking.loan.api.entity.DelinquencyBucket;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.entity.InterestAccrual;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.entity.LoanApplication;
import com.openfinova.banking.loan.entity.LoanSchedule;
import com.openfinova.banking.loan.event.LoanAccountClosedEvent;
import com.openfinova.banking.loan.event.LoanDisbursedEvent;
import com.openfinova.banking.loan.integration.LoanGeneralLedgerBridge;
import com.openfinova.banking.loan.repository.InterestAccrualRepository;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.loan.repository.LoanApplicationRepository;
import com.openfinova.banking.loan.repository.LoanScheduleRepository;
import com.openfinova.banking.setup.api.DateTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.*;

/**
 * Implementation of LoanAccountService for managing loan account lifecycle and operations.
 *
 * This service is the core of loan account management, handling all operations from
 * loan creation through disbursement, repayment tracking, and closure. It coordinates
 * with multiple repositories and enforces business rules and state transitions.
 *
 * Key Responsibilities:
 * - Loan account creation from approved applications
 * - Loan disbursement and activation
 * - Balance tracking (principal, interest, fees, penalties)
 * - Delinquency monitoring and classification
 * - Loan status management and state transitions
 * - Loan closure and write-off processing
 * - Loan restructuring and top-up loans
 * - Customer exposure calculation
 * - Statement generation and reporting
 *
 * State Machine:
 * The service enforces a strict state machine for loan status transitions:
 * - APPROVED → ACTIVE (via disbursement)
 * - ACTIVE → CLOSED, WRITTEN_OFF, RESTRUCTURED
 * - RESTRUCTURED → CLOSED, WRITTEN_OFF
 *
 * Business Rules Enforced:
 * - Loans can only be created from approved applications
 * - Disbursement date cannot be in the future
 * - Outstanding balances cannot be negative
 * - Loans can only be closed with zero balance
 * - Delinquency tracking only applies to active/restructured loans
 * - Write-off requires a reason
 * - All state transitions are validated
 *
 * @see LoanAccountService
 * @see LoanAccount
 * @see com.openfinova.banking.loan.api.entity.LoanStatus
 */
@Service
@Transactional
public class LoanAccountService {

    private static final Logger log = LoggerFactory.getLogger(LoanAccountService.class);

    private final LoanAccountRepository loanAccountRepository;
    private final LoanApplicationRepository applicationRepository;
    private final LoanScheduleRepository scheduleRepository;
    private final InterestAccrualRepository interestAccrualRepository;
    private final LoanGeneralLedgerBridge loanGeneralLedgerBridge;
    private final ApplicationEventPublisher eventPublisher;
    private final DateTimeService dateTimeService;

    public LoanAccountService(LoanAccountRepository loanAccountRepository,
            LoanApplicationRepository applicationRepository, LoanScheduleRepository scheduleRepository,
            InterestAccrualRepository interestAccrualRepository, LoanGeneralLedgerBridge loanGeneralLedgerBridge,
            ApplicationEventPublisher eventPublisher, DateTimeService dateTimeService) {
        this.loanAccountRepository = loanAccountRepository;
        this.applicationRepository = applicationRepository;
        this.scheduleRepository = scheduleRepository;
        this.interestAccrualRepository = interestAccrualRepository;
        this.loanGeneralLedgerBridge = loanGeneralLedgerBridge;
        this.eventPublisher = eventPublisher;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Creates a new loan account from an approved loan application.
     *
     * This is the first step in the loan lifecycle after application approval.
     * The method creates a loan account with APPROVED status, ready for disbursement.
     *
     * The loan account inherits key attributes from the application:
     * - Customer ID
     * - Product ID
     * - Approved amount (becomes principal amount)
     * - Approved tenor
     * - Approved interest rate
     * - Currency
     *
     * Business Rules:
     * - Application must exist
     * - Application must be in APPROVED status
     * - Only one loan account can be created per application
     * - Loan account number is auto-generated
     * - Initial status is APPROVED (not yet disbursed)
     * - Outstanding principal equals principal amount initially
     *
     * After creation, the loan account must be disbursed to become ACTIVE.
     *
     * @param applicationId the ID of the approved loan application
     * @param createdBy the user creating the loan account
     * @return the newly created loan account
     * @throws IllegalArgumentException if application not found, not approved, or loan already exists
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public LoanAccount createLoanAccount(UUID applicationId, String createdBy) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        if (!ApplicationStatus.APPROVED.equals(application.getStatus())) {
            throw new IllegalArgumentException("Application must be approved");
        }

        Optional<LoanAccount> existing = loanAccountRepository.findByApplicationId(applicationId);
        if (existing.isPresent()) {
            throw new IllegalArgumentException("Loan account already exists for this application");
        }

        LoanAccount loanAccount = new LoanAccount();
        loanAccount.setLoanAccountNumber(generateLoanAccountNumber());
        loanAccount.setApplicationId(applicationId);
        loanAccount.setCustomerId(application.getCustomerId());
        loanAccount.setProductId(application.getProductId());
        loanAccount.setPrincipalAmount(application.getApprovedAmount());
        loanAccount.setOutstandingPrincipal(application.getApprovedAmount());
        loanAccount.setTenorMonths(application.getApprovedTenorMonths());
        loanAccount.setInterestRate(application.getApprovedInterestRate());
        loanAccount.setCurrency(application.getCurrency());
        loanAccount.setStatus(LoanStatus.APPROVED);

        return loanAccountRepository.save(loanAccount);
    }

    /**
     * Retrieves a loan account by its unique identifier.
     *
     * @param id the loan account ID
     * @return Optional containing the loan account if found, empty otherwise
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('loan:read', 'service:loan:read')")
    public Optional<LoanAccount> getLoanAccountById(UUID id) {
        return loanAccountRepository.findById(id);
    }

    /**
     * Retrieves a loan account by its account number.
     *
     * Loan account numbers are unique identifiers used for customer-facing
     * operations and external references.
     *
     * @param loanAccountNumber the loan account number
     * @return Optional containing the loan account if found, empty otherwise
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('loan:read', 'service:loan:read')")
    public Optional<LoanAccount> getLoanAccountByNumber(String loanAccountNumber) {
        return loanAccountRepository.findByLoanAccountNumber(loanAccountNumber);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('loan:read', 'service:loan:read')")
    public boolean isLoanAccountEligibleForRepayment(UUID loanAccountId) {
        return getLoanAccountById(loanAccountId).map(account -> {
            LoanStatus status = account.getStatus();
            return LoanStatus.ACTIVE.equals(status) || LoanStatus.RESTRUCTURED.equals(status);
        }).orElse(false);
    }

    /**
     * Retrieves all loan accounts for a customer.
     *
     * Returns all loans regardless of status, providing a complete loan history.
     *
     * @param customerId the customer ID
     * @return list of all loan accounts for the customer
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public List<LoanAccount> getLoanAccountsByCustomer(UUID customerId) {
        return loanAccountRepository.findByCustomerId(customerId);
    }

    /**
     * Retrieves all loan accounts for a customer with pagination.
     *
     * Paginated version for customers with many loans.
     *
     * @param customerId the customer ID
     * @param pageable pagination parameters
     * @return page of loan accounts for the customer
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanAccount> getLoanAccountsByCustomer(UUID customerId, Pageable pageable) {
        return loanAccountRepository.findByCustomerId(customerId, pageable);
    }

    /**
     * Retrieves only active loan accounts for a customer.
     *
     * Active loans are those currently being repaid. This is useful for:
     * - Calculating current customer exposure
     * - Determining eligibility for new loans
     * - Customer service inquiries
     * - Portfolio management
     *
     * @param customerId the customer ID
     * @return list of active loan accounts for the customer
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public List<LoanAccount> getActiveLoanAccountsByCustomer(UUID customerId) {
        return loanAccountRepository.findByCustomerIdAndStatus(customerId, LoanStatus.ACTIVE);
    }

    /**
     * Retrieves loan accounts by status with pagination.
     *
     * Useful for portfolio management and reporting:
     * - ACTIVE: Current performing loans
     * - CLOSED: Successfully completed loans
     * - WRITTEN_OFF: Defaulted loans
     * - RESTRUCTURED: Modified loans
     *
     * @param status the loan status to filter by
     * @param pageable pagination parameters
     * @return page of loan accounts with the specified status
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanAccount> getLoanAccountsByStatus(LoanStatus status, Pageable pageable) {
        return loanAccountRepository.findByStatus(status, pageable);
    }

    /**
     * Updates the status of a loan account with state transition validation.
     *
     * This method enforces the loan status state machine to ensure only valid
     * transitions are allowed. The state machine rules are defined in the
     * LoanAccount entity.
     *
     * Valid transitions include:
     * - APPROVED → ACTIVE (via disbursement)
     * - ACTIVE → CLOSED, WRITTEN_OFF, RESTRUCTURED
     * - RESTRUCTURED → CLOSED, WRITTEN_OFF
     *
     * The reason parameter is stored in the remarks field for audit purposes.
     *
     * @param loanAccountId the ID of the loan account to update
     * @param newStatus the new status to transition to
     * @param reason the reason for the status change
     * @param updatedBy the user performing the update
     * @return the updated loan account
     * @throws IllegalArgumentException if loan account not found
     * @throws IllegalStateException if the status transition is not allowed
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public LoanAccount updateLoanAccountStatus(UUID loanAccountId, LoanStatus newStatus, String reason,
            String updatedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        if (!loanAccount.canTransitionTo(newStatus)) {
            throw new IllegalStateException(loanAccount.getTransitionErrorMessage(newStatus));
        }

        loanAccount.setStatus(newStatus);
        loanAccount.setRemarks(reason);

        return loanAccountRepository.save(loanAccount);
    }

    /**
     * Disburses a loan and activates it for repayment.
     *
     * Disbursement is the critical step where funds are released to the borrower
     * and the loan becomes active. This method:
     * - Validates the loan can be disbursed (APPROVED status)
     * - Records the disbursement date
     * - Changes status to ACTIVE
     * - Calculates and sets the maturity date
     *
     * Business Rules:
     * - Loan must be in APPROVED status
     * - Disbursement date cannot be null
     * - Disbursement date cannot be in the future
     * - Disbursed by information is required
     * - Loan can only be disbursed once
     * - Maturity date = disbursement date + tenor months
     *
     * After disbursement:
     * - Loan schedule becomes active
     * - Interest accrual begins
     * - Repayments can be recorded
     * - Delinquency tracking starts
     *
     * @param loanAccountId the ID of the loan account to disburse
     * @param disbursementDate the date funds were disbursed
     * @param disbursedBy the user who disbursed the loan
     * @return the disbursed loan account with ACTIVE status
     * @throws IllegalArgumentException if loan not found, date invalid, or disbursedBy missing
     * @throws IllegalStateException if loan already disbursed or cannot transition to ACTIVE
     */
    @PreAuthorize("hasAnyAuthority('loan:disburse', 'service:loan:write')")
    public LoanAccount disburseLoan(UUID loanAccountId, LocalDate disbursementDate, String disbursedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        // Validate disbursement date
        if (disbursementDate == null) {
            throw new IllegalArgumentException("Disbursement date cannot be null");
        }

        // Validate disbursement date is not in the future (allow today)
        if (disbursementDate.isAfter(dateTimeService.today())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Disbursement date cannot be in the future. Provided: %s, Today: %s",
                            disbursementDate,
                            dateTimeService.today()));
        }

        // Validate disbursedBy is provided
        if (disbursedBy == null || disbursedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Disbursed by information is required");
        }

        // Validate state transition
        if (!loanAccount.canTransitionTo(LoanStatus.ACTIVE)) {
            throw new IllegalStateException(loanAccount.getTransitionErrorMessage(LoanStatus.ACTIVE));
        }

        // Validate loan hasn't already been disbursed
        if (loanAccount.getDisbursementDate() != null) {
            throw new IllegalStateException(
                    String.format("Loan has already been disbursed on %s", loanAccount.getDisbursementDate()));
        }

        loanAccount.setDisbursementDate(disbursementDate);
        loanAccount.setStatus(LoanStatus.ACTIVE);
        loanAccount.setMaturityDate(disbursementDate.plusMonths(loanAccount.getTenorMonths()));

        LoanAccount saved = loanAccountRepository.save(loanAccount);
        eventPublisher.publishEvent(new LoanDisbursedEvent(saved.getId(), disbursementDate, disbursedBy));
        return saved;
    }

    /**
     * Updates outstanding balances for a loan account using delta values.
     *
     * This method applies changes to the four balance components:
     * - Principal: The original loan amount still owed
     * - Interest: Accrued interest charges
     * - Fees: Service fees and charges
     * - Penalties: Late payment penalties
     *
     * Delta values can be positive (increase) or negative (decrease):
     * - Positive: Adding charges (interest accrual, penalties)
     * - Negative: Recording payments
     *
     * Business Rules:
     * - All delta values must be provided (not null)
     * - Resulting balances cannot be negative
     * - Outstanding principal cannot exceed original principal amount
     * - All changes are validated before applying
     *
     * This method is typically called by:
     * - Payment processing (negative deltas)
     * - Interest accrual (positive interest delta)
     * - Fee application (positive fees delta)
     * - Penalty assessment (positive penalties delta)
     *
     * @param loanAccountId the ID of the loan account
     * @param principalDelta the change in principal balance
     * @param interestDelta the change in interest balance
     * @param feesDelta the change in fees balance
     * @param penaltiesDelta the change in penalties balance
     * @throws IllegalArgumentException if loan not found, deltas are null, or resulting balances are invalid
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public void updateOutstandingBalances(UUID loanAccountId, BigDecimal principalDelta, BigDecimal interestDelta,
            BigDecimal feesDelta, BigDecimal penaltiesDelta) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        // Validate that deltas are not null
        if (principalDelta == null || interestDelta == null || feesDelta == null || penaltiesDelta == null) {
            throw new IllegalArgumentException("Balance deltas cannot be null");
        }

        // Calculate new balances
        BigDecimal newPrincipal = loanAccount.getOutstandingPrincipal().add(principalDelta);
        BigDecimal newInterest = loanAccount.getOutstandingInterest().add(interestDelta);
        BigDecimal newFees = loanAccount.getOutstandingFees().add(feesDelta);
        BigDecimal newPenalties = loanAccount.getOutstandingPenalties().add(penaltiesDelta);

        // Validate that new balances are not negative
        if (newPrincipal.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Outstanding principal cannot be negative. Current: %s, Delta: %s, Result: %s",
                            loanAccount.getOutstandingPrincipal(),
                            principalDelta,
                            newPrincipal));
        }
        if (newInterest.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Outstanding interest cannot be negative. Current: %s, Delta: %s, Result: %s",
                            loanAccount.getOutstandingInterest(),
                            interestDelta,
                            newInterest));
        }
        if (newFees.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Outstanding fees cannot be negative. Current: %s, Delta: %s, Result: %s",
                            loanAccount.getOutstandingFees(),
                            feesDelta,
                            newFees));
        }
        if (newPenalties.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Outstanding penalties cannot be negative. Current: %s, Delta: %s, Result: %s",
                            loanAccount.getOutstandingPenalties(),
                            penaltiesDelta,
                            newPenalties));
        }

        // Validate that principal doesn't exceed original principal amount
        if (newPrincipal.compareTo(loanAccount.getPrincipalAmount()) > 0) {
            throw new IllegalArgumentException(
                    String.format(
                            "Outstanding principal (%s) cannot exceed original principal amount (%s)",
                            newPrincipal,
                            loanAccount.getPrincipalAmount()));
        }

        // Update balances
        loanAccount.setOutstandingPrincipal(newPrincipal);
        loanAccount.setOutstandingInterest(newInterest);
        loanAccount.setOutstandingFees(newFees);
        loanAccount.setOutstandingPenalties(newPenalties);

        loanAccountRepository.save(loanAccount);
    }

    /**
     * Calculates the total outstanding amount for a loan account.
     *
     * Total outstanding is the sum of all balance components:
     * Total = Principal + Interest + Fees + Penalties
     *
     * This is the amount the borrower must pay to fully settle the loan.
     *
     * @param loanAccountId the ID of the loan account
     * @return the total outstanding amount
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public BigDecimal calculateTotalOutstanding(UUID loanAccountId) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        return loanAccount.getTotalOutstanding();
    }

    /**
     * Updates the delinquency status of a loan account.
     *
     * Delinquency tracking is critical for risk management and regulatory reporting.
     * This method updates two related fields:
     * - Days past due: Number of days since oldest missed payment
     * - Delinquency bucket: Classification for reporting (0-30, 31-60, 61-90, 90+)
     *
     * Business Rules:
     * - Days past due cannot be null or negative
     * - Delinquency bucket is required when days past due > 0
     * - Bucket must be consistent with days past due
     * - Only applicable to ACTIVE or RESTRUCTURED loans
     *
     * Delinquency Buckets:
     * - 0-30: Early delinquency, minimal risk
     * - 31-60: Moderate delinquency, increased risk
     * - 61-90: Serious delinquency, high risk
     * - 90+: Severe delinquency, default risk
     *
     * This method is typically called by:
     * - Scheduled delinquency detection jobs
     * - Payment processing (to clear delinquency)
     * - Manual delinquency adjustments
     *
     * @param loanAccountId the ID of the loan account
     * @param daysPastDue the number of days past due
     * @param delinquencyBucket the delinquency classification bucket
     * @throws IllegalArgumentException if loan not found, days past due invalid, or bucket inconsistent
     * @throws IllegalStateException if loan status doesn't support delinquency tracking
     */
    public void updateDelinquencyStatus(UUID loanAccountId, Integer daysPastDue, DelinquencyBucket delinquencyBucket) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        // Validate daysPastDue
        if (daysPastDue == null) {
            throw new IllegalArgumentException("Days past due cannot be null");
        }
        if (daysPastDue < 0) {
            throw new IllegalArgumentException(String.format("Days past due cannot be negative: %s", daysPastDue));
        }

        // Validate delinquency bucket consistency
        if (daysPastDue > 0 && delinquencyBucket == null) {
            throw new IllegalArgumentException("Delinquency bucket is required when days past due is greater than 0");
        }

        if (delinquencyBucket != null) {
            validateDelinquencyBucket(daysPastDue, delinquencyBucket);
        }

        // Only update if loan is in a state where delinquency tracking makes sense
        if (!loanAccount.isActive() && !LoanStatus.RESTRUCTURED.equals(loanAccount.getStatus())) {
            throw new IllegalStateException(
                    String.format(
                            "Cannot update delinquency status for loan in %s status. Delinquency tracking is only applicable for ACTIVE or RESTRUCTURED loans.",
                            loanAccount.getStatus()));
        }

        loanAccount.setDaysPastDue(daysPastDue);
        loanAccount.setDelinquencyBucket(delinquencyBucket);

        loanAccountRepository.save(loanAccount);
    }

    /**
     * Validates that the delinquency bucket is consistent with the days past due.
     *
     * @param daysPastDue the number of days past due
     * @param delinquencyBucket the delinquency bucket classification
     * @throws IllegalArgumentException if the bucket is invalid or inconsistent
     */
    private void validateDelinquencyBucket(Integer daysPastDue, DelinquencyBucket delinquencyBucket) {
        DelinquencyBucket expected = DelinquencyBucket.fromDaysPastDue(daysPastDue);
        if (expected != delinquencyBucket) {
            throw new IllegalArgumentException(
                    String.format(
                            "Delinquency bucket '%s' is inconsistent with days past due %d. Expected: '%s'",
                            delinquencyBucket,
                            daysPastDue,
                            expected));
        }
    }

    /**
     * Retrieves all delinquent loan accounts with pagination.
     *
     * Delinquent loans are those with days past due greater than zero.
     * Used for collection management and portfolio monitoring.
     *
     * @param pageable pagination parameters
     * @return page of delinquent loan accounts
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanAccount> getDelinquentLoanAccounts(Pageable pageable) {
        return loanAccountRepository.findDelinquentAccounts(pageable);
    }

    /**
     * Retrieves loan accounts by delinquency bucket with pagination.
     *
     * Allows filtering by specific delinquency severity levels for
     * targeted collection strategies and risk reporting.
     *
     * @param delinquencyBucket the delinquency bucket to filter by (e.g., "0-30", "31-60")
     * @param pageable pagination parameters
     * @return page of loan accounts in the specified delinquency bucket
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanAccount> getLoanAccountsByDelinquencyBucket(DelinquencyBucket delinquencyBucket,
            Pageable pageable) {
        return loanAccountRepository.findByDelinquencyBucket(delinquencyBucket, pageable);
    }

    /**
     * Retrieves loan accounts maturing within a date range with pagination.
     *
     * Useful for:
     * - Maturity planning and forecasting
     * - Customer communication about upcoming maturity
     * - Renewal opportunity identification
     * - Liquidity management
     *
     * @param startDate the start of the maturity date range
     * @param endDate the end of the maturity date range
     * @param pageable pagination parameters
     * @return page of loan accounts maturing within the specified date range
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanAccount> getLoanAccountsMaturingBetween(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return loanAccountRepository.findAccountsMaturingBetween(startDate, endDate, pageable);
    }

    /**
     * Closes a loan account after full repayment.
     *
     * Loan closure is the successful completion of the loan lifecycle.
     * This method marks the loan as CLOSED, indicating all obligations
     * have been fulfilled.
     *
     * Business Rules:
     * - Closure date cannot be null
     * - Closure date cannot be before disbursement date
     * - Closed by information is required
     * - Total outstanding balance must be zero
     * - Loan must be in a status that allows closure (ACTIVE or RESTRUCTURED)
     *
     * After closure:
     * - No further payments can be recorded
     * - Loan is excluded from active portfolio metrics
     * - Customer's exposure is reduced
     * - Guarantors can be released
     * - Collateral can be released
     *
     * Closed loans are retained for:
     * - Historical reporting
     * - Customer credit history
     * - Audit purposes
     * - Regulatory compliance
     *
     * @param loanAccountId the ID of the loan account to close
     * @param closureDate the date of closure
     * @param closedBy the user closing the loan
     * @return the closed loan account
     * @throws IllegalArgumentException if loan not found, date invalid, or closedBy missing
     * @throws IllegalStateException if outstanding balance exists or status transition not allowed
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public LoanAccount closeLoanAccount(UUID loanAccountId, LocalDate closureDate, String closedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        // Validate closure date
        if (closureDate == null) {
            throw new IllegalArgumentException("Closure date cannot be null");
        }

        // Validate closure date is not before disbursement date
        if (loanAccount.getDisbursementDate() != null && closureDate.isBefore(loanAccount.getDisbursementDate())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Closure date (%s) cannot be before disbursement date (%s)",
                            closureDate,
                            loanAccount.getDisbursementDate()));
        }

        // Validate closedBy is provided
        if (closedBy == null || closedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Closed by information is required");
        }

        // Validate outstanding balance is zero
        if (loanAccount.getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0) {
            throw new IllegalStateException(
                    String.format("Cannot close loan with outstanding balance: %s", loanAccount.getTotalOutstanding()));
        }

        // Validate state transition
        if (!loanAccount.canTransitionTo(LoanStatus.CLOSED)) {
            throw new IllegalStateException(loanAccount.getTransitionErrorMessage(LoanStatus.CLOSED));
        }

        loanAccount.setClosedDate(closureDate);
        loanAccount.setStatus(LoanStatus.CLOSED);
        LoanAccount saved = loanAccountRepository.save(loanAccount);
        eventPublisher.publishEvent(new LoanAccountClosedEvent(saved.getId(), closureDate, closedBy));
        return saved;
    }

    /**
     * Writes off a loan that is deemed uncollectible.
     *
     * Write-off is an accounting treatment for loans where recovery is unlikely.
     * The loan is removed from performing assets and charged against provisions.
     *
     * Write-off Criteria (typically):
     * - Loan is 180+ days past due
     * - All collection efforts have been exhausted
     * - Legal action has been unsuccessful
     * - Borrower is bankrupt or deceased
     * - Collateral value is insufficient
     *
     * Business Rules:
     * - Write-off date cannot be null
     * - Write-off date cannot be before disbursement date
     * - Reason is required for audit and regulatory purposes
     * - Written off by information is required
     * - Loan must be in a status that allows write-off
     *
     * Important Notes:
     * - Write-off is an accounting treatment, not legal forgiveness
     * - The debt still exists and collection can continue
     * - Provisions should cover the written-off amount
     * - Customer credit score is severely impacted
     * - Regulatory reporting requirements apply
     *
     * After write-off:
     * - Loan is excluded from performing portfolio
     * - Included in default rate calculations
     * - May still pursue recovery through legal means
     * - Customer may be blacklisted
     *
     * @param loanAccountId the ID of the loan account to write off
     * @param writeOffDate the date of write-off
     * @param reason the reason for write-off (required for audit)
     * @param writtenOffBy the user performing the write-off
     * @return the written-off loan account
     * @throws IllegalArgumentException if loan not found, date invalid, reason missing, or writtenOffBy missing
     * @throws IllegalStateException if status transition not allowed
     */
    @PreAuthorize("hasAuthority('loan:write-off')")
    public LoanAccount writeOffLoan(UUID loanAccountId, LocalDate writeOffDate, String reason, String writtenOffBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        // Validate write-off date
        if (writeOffDate == null) {
            throw new IllegalArgumentException("Write-off date cannot be null");
        }

        // Validate write-off date is not before disbursement date
        if (loanAccount.getDisbursementDate() != null && writeOffDate.isBefore(loanAccount.getDisbursementDate())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Write-off date (%s) cannot be before disbursement date (%s)",
                            writeOffDate,
                            loanAccount.getDisbursementDate()));
        }

        // Validate reason is provided
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Write-off reason is required");
        }

        // Validate writtenOffBy is provided
        if (writtenOffBy == null || writtenOffBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Written off by information is required");
        }

        // Validate state transition
        if (!loanAccount.canTransitionTo(LoanStatus.WRITTEN_OFF)) {
            throw new IllegalStateException(loanAccount.getTransitionErrorMessage(LoanStatus.WRITTEN_OFF));
        }

        loanAccount.setStatus(LoanStatus.WRITTEN_OFF);
        loanAccount.setRemarks(reason);

        return loanAccountRepository.save(loanAccount);
    }

    /**
     * Marks a loan as restructured after modification of terms.
     *
     * Loan restructuring involves modifying the original loan terms to help
     * borrowers who are experiencing financial difficulties. This is an
     * alternative to default and write-off.
     *
     * Common Restructuring Actions:
     * - Extending the loan tenor (longer repayment period)
     * - Reducing the interest rate
     * - Providing a payment holiday (moratorium)
     * - Capitalizing arrears into principal
     * - Changing repayment frequency
     *
     * Business Rules:
     * - Restructured date cannot be null
     * - Restructured date cannot be before disbursement date
     * - Loan can only be restructured once (isRestructured flag)
     * - Restructuring details should be recorded separately
     *
     * Regulatory Implications:
     * - Restructured loans require special reporting
     * - May be classified as non-performing
     * - Higher provision requirements may apply
     * - Impacts portfolio quality metrics
     *
     * After restructuring:
     * - New repayment schedule is generated
     * - Delinquency may be reset or continue tracking
     * - Loan remains active for repayment
     * - Customer credit score may be impacted
     *
     * @param loanAccountId the ID of the loan account to mark as restructured
     * @param restructuredDate the date of restructuring
     * @return the restructured loan account
     * @throws IllegalArgumentException if loan not found or date invalid
     * @throws IllegalStateException if loan already restructured
     */
    @PreAuthorize("hasAuthority('loan:restructure')")
    public LoanAccount markAsRestructured(UUID loanAccountId, LocalDate restructuredDate) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        // Validate restructured date
        if (restructuredDate == null) {
            throw new IllegalArgumentException("Restructured date cannot be null");
        }

        // Validate restructured date is not before disbursement date
        if (loanAccount.getDisbursementDate() != null && restructuredDate.isBefore(loanAccount.getDisbursementDate())) {
            throw new IllegalArgumentException(
                    String.format(
                            "Restructured date (%s) cannot be before disbursement date (%s)",
                            restructuredDate,
                            loanAccount.getDisbursementDate()));
        }

        // Validate loan is not already restructured
        if (Boolean.TRUE.equals(loanAccount.getIsRestructured())) {
            throw new IllegalStateException(
                    String.format("Loan has already been restructured on %s", loanAccount.getRestructuredDate()));
        }

        loanAccount.setIsRestructured(true);
        loanAccount.setRestructuredDate(restructuredDate);

        return loanAccountRepository.save(loanAccount);
    }

    /**
     * Creates a top-up loan for an existing customer.
     *
     * A top-up loan provides additional funds to a customer with an existing loan.
     * It's a new loan that references the original loan, often with similar terms.
     *
     * Top-up loans are used for:
     * - Providing additional financing to good customers
     * - Consolidating multiple small loans
     * - Meeting additional funding needs
     * - Customer retention and relationship building
     *
     * The top-up loan:
     * - Is a separate loan account with its own number
     * - Inherits product, tenor, and rate from original loan
     * - Has its own repayment schedule
     * - Is linked to the original loan via originalLoanId
     * - Is flagged with isTopUp = true
     * - Starts in APPROVED status (requires disbursement)
     *
     * Business Considerations:
     * - Customer must have good repayment history
     * - Original loan should be performing
     * - Total exposure should be within limits
     * - Credit assessment may be simplified
     *
     * @param originalLoanId the ID of the original loan account
     * @param topUpAmount the additional amount to be provided
     * @param createdBy the user creating the top-up loan
     * @return the newly created top-up loan account
     * @throws IllegalArgumentException if original loan not found
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public LoanAccount createTopUpLoan(UUID originalLoanId, BigDecimal topUpAmount, String createdBy) {
        LoanAccount originalLoan = loanAccountRepository.findById(originalLoanId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Original loan not found: %s", originalLoanId)));

        LoanAccount topUpLoan = new LoanAccount();
        topUpLoan.setLoanAccountNumber(generateLoanAccountNumber());
        topUpLoan.setCustomerId(originalLoan.getCustomerId());
        topUpLoan.setProductId(originalLoan.getProductId());
        topUpLoan.setPrincipalAmount(topUpAmount);
        topUpLoan.setOutstandingPrincipal(topUpAmount);
        topUpLoan.setTenorMonths(originalLoan.getTenorMonths());
        topUpLoan.setInterestRate(originalLoan.getInterestRate());
        topUpLoan.setCurrency(originalLoan.getCurrency());
        topUpLoan.setIsTopUp(true);
        topUpLoan.setOriginalLoanId(originalLoanId);
        topUpLoan.setStatus(LoanStatus.APPROVED);

        return loanAccountRepository.save(topUpLoan);
    }

    /**
     * Calculates the total loan exposure for a customer.
     *
     * Customer exposure is the sum of outstanding principal across all
     * active loans. This is critical for:
     *
     * Credit Risk Management:
     * - Determining if customer can take additional loans
     * - Assessing concentration risk
     * - Setting credit limits
     * - Portfolio diversification
     *
     * Regulatory Compliance:
     * - Single borrower limits
     * - Large exposure reporting
     * - Capital adequacy calculations
     *
     * The calculation includes only outstanding principal, not interest,
     * fees, or penalties, as principal represents the actual credit exposure.
     *
     * @param customerId the customer ID
     * @return the total outstanding principal across all customer loans
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public BigDecimal calculateCustomerTotalExposure(UUID customerId) {
        return loanAccountRepository.sumOutstandingPrincipalByCustomer(customerId);
    }

    /**
     * Counts the number of active loans for a customer.
     *
     * Used for:
     * - Loan eligibility checks (maximum active loans policy)
     * - Customer segmentation
     * - Cross-selling opportunities
     * - Portfolio analysis
     *
     * @param customerId the customer ID
     * @return the count of active loan accounts for the customer
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public long countActiveLoansByCustomer(UUID customerId) {
        return loanAccountRepository.countActiveAccountsByCustomer(customerId);
    }

    /**
     * Processes delinquency detection for all active loans as of a specific date.
     *
     * This method scans all active loans and updates their delinquency status
     * by checking for overdue schedules. For each loan with overdue payments:
     * - Finds the oldest overdue schedule
     * - Calculates days past due from that schedule's due date
     * - Assigns appropriate delinquency bucket
     *
     * For loans with no overdue schedules, delinquency is cleared.
     *
     * This is typically called by scheduled jobs to maintain accurate
     * delinquency tracking across the portfolio.
     *
     * @param asOfDate the date to calculate delinquency as of
     * @return the number of loans with updated delinquency status
     */
    public int processDelinquencyDetection(LocalDate asOfDate) {
        List<LoanAccount> activeLoans = loanAccountRepository.findByStatus(LoanStatus.ACTIVE, Pageable.unpaged())
                .getContent();
        int updated = 0;

        for (LoanAccount loan : activeLoans) {
            List<LoanSchedule> overdueSchedules = scheduleRepository.findOverdueSchedulesByLoanAccount(loan.getId());

            if (!overdueSchedules.isEmpty()) {
                LoanSchedule oldestOverdue = overdueSchedules.get(0);
                int daysPastDue = (int) java.time.temporal.ChronoUnit.DAYS
                        .between(oldestOverdue.getDueDate(), asOfDate);

                DelinquencyBucket bucket = DelinquencyBucket.fromDaysPastDue(daysPastDue);
                loan.setDaysPastDue(daysPastDue);
                loan.setDelinquencyBucket(bucket);
                updated++;
            } else {
                loan.setDaysPastDue(0);
                loan.setDelinquencyBucket(DelinquencyBucket.CURRENT);
            }
        }

        loanAccountRepository.saveAll(activeLoans);
        return updated;
    }

    /**
     * Processes interest accrual for all active loans as of a specific date.
     *
     * Interest accrual is the process of calculating and recording interest
     * charges that have accumulated but not yet been billed or paid.
     *
     * Accrual Methods:
     * - Daily accrual: Interest calculated daily based on outstanding balance
     * - Monthly accrual: Interest calculated at month end
     * - On-schedule: Interest accrued per repayment schedule
     *
     * The accrued interest is added to the outstanding interest balance
     * and will be included in the next payment.
     *
     * Currently a placeholder - full implementation would:
     * - Calculate daily interest for each loan
     * - Update outstanding interest balance
     * - Create interest accrual records
     * - Handle different interest calculation methods
     *
     * @param accrualDate the date to process interest accrual for
     * @return the number of loans processed
     */
    public int processInterestAccrual(LocalDate accrualDate) {
        List<LoanAccount> activeLoans = loanAccountRepository.findAccountsForInterestAccrual();
        int processed = 0;
        for (LoanAccount loan : activeLoans) {
            try {
                if (interestAccrualRepository.existsByLoanAccount_IdAndAccrualDate(loan.getId(), accrualDate)) {
                    continue;
                }
                BigDecimal principal = loan.getOutstandingPrincipal();
                if (principal.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                BigDecimal rate = loan.getInterestRate() != null ? loan.getInterestRate() : BigDecimal.ZERO;
                BigDecimal dailyAccrual = principal.multiply(rate)
                        .divide(BigDecimal.valueOf(36500), 8, RoundingMode.HALF_UP);
                if (dailyAccrual.compareTo(BigDecimal.ZERO) <= 0) {
                    continue;
                }
                InterestAccrual accrual = new InterestAccrual();
                accrual.setLoanAccount(loan);
                accrual.setAccrualDate(accrualDate);
                accrual.setPrincipalBalance(principal);
                accrual.setInterestRate(rate.setScale(4, RoundingMode.HALF_UP));
                accrual.setAccrualAmount(dailyAccrual.setScale(4, RoundingMode.HALF_UP));
                accrual.setCurrency(loan.getCurrency());
                interestAccrualRepository.save(accrual);
                loan.setOutstandingInterest(loan.getOutstandingInterest().add(accrual.getAccrualAmount()));
                loanAccountRepository.save(loan);
                loanGeneralLedgerBridge.postInterestAccrual(
                        loan.getId(),
                        accrualDate,
                        accrual.getAccrualAmount(),
                        loan.getCurrency(),
                        "interest-accrual-job");
                processed++;
            } catch (Exception e) {
                log.error("Interest accrual failed for loan {}", loan.getId(), e);
            }
        }
        return processed;
    }

    /**
     * Generates a loan statement for a specific period.
     *
     * A loan statement provides a summary of loan activity and balances
     * for a specified date range. It typically includes:
     * - Opening balance
     * - Payments received
     * - Interest charged
     * - Fees and penalties
     * - Closing balance
     * - Transaction details
     *
     * Statements are used for:
     * - Customer communication
     * - Dispute resolution
     * - Audit trails
     * - Regulatory compliance
     * - Tax reporting
     *
     * Currently returns basic statement structure. Full implementation
     * would include detailed transaction history and calculations.
     *
     * @param loanAccountId the ID of the loan account
     * @param fromDate the start date of the statement period
     * @param toDate the end date of the statement period
     * @return the loan statement for the specified period
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public LoanStatementResponse generateLoanStatement(UUID loanAccountId, LocalDate fromDate, LocalDate toDate) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        LoanStatementResponse statement = new LoanStatementResponse();
        statement.setLoanAccountId(loanAccountId);
        statement.setLoanAccountNumber(loanAccount.getLoanAccountNumber());
        statement.setFromDate(fromDate);
        statement.setToDate(toDate);
        statement.setClosingBalance(loanAccount.getTotalOutstanding());

        return statement;
    }

    /**
     * Validates if a loan account can be closed.
     *
     * Performs pre-closure validation checks to ensure the loan meets
     * all requirements for closure. This allows validation before
     * attempting the actual closure operation.
     *
     * Validation Checks:
     * - Outstanding balance is zero
     * - No pending transactions
     * - All schedules are paid
     * - No active holds or disputes
     *
     * Currently checks only outstanding balance. Full implementation
     * would include additional validation rules.
     *
     * @param loanAccountId the ID of the loan account to validate
     * @return validation result with status and any error messages
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public ValidationResult validateForClosure(UUID loanAccountId) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        List<String> errors = new ArrayList<>();

        if (loanAccount.getTotalOutstanding().compareTo(BigDecimal.ZERO) > 0) {
            errors.add("Loan has outstanding balance");
        }

        boolean valid = errors.isEmpty();
        ValidationResult result = new ValidationResult(valid, valid ? "Loan can be closed" : "Loan cannot be closed");
        result.setErrors(errors);

        return result;
    }

    /**
     * Updates the status of multiple loan accounts in a single batch operation.
     *
     * Batch processing is useful for:
     * - Bulk status changes (e.g., suspending multiple loans)
     * - End-of-day processing
     * - Administrative actions
     * - Data migration or corrections
     *
     * The method processes each loan individually with error handling,
     * ensuring one failure doesn't prevent processing of others.
     *
     * Returns a map of results:
     * - Key: Loan account ID
     * - Value: "Success" or error message
     *
     * This allows the caller to identify which loans were updated
     * successfully and which failed with reasons.
     *
     * @param loanAccountIds list of loan account IDs to update
     * @param newStatus the new status to apply to all loans
     * @param reason the reason for the status change
     * @param updatedBy the user performing the batch update
     * @return map of loan account ID to result message
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Map<UUID, String> batchUpdateLoanAccountStatus(List<UUID> loanAccountIds, LoanStatus newStatus,
            String reason, String updatedBy) {
        Map<UUID, String> results = new HashMap<>();

        for (UUID loanAccountId : loanAccountIds) {
            try {
                updateLoanAccountStatus(loanAccountId, newStatus, reason, updatedBy);
                results.put(loanAccountId, "Success");
            } catch (Exception e) {
                results.put(loanAccountId, String.format("Failed: %s", e.getMessage()));
            }
        }

        return results;
    }

    /**
     * Generates a unique loan account number.
     *
     * Currently uses timestamp-based generation. Production implementation
     * should use a more robust approach:
     * - Sequential numbering with check digits
     * - Branch/product code prefixes
     * - Database sequence generators
     * - UUID-based identifiers
     *
     * @return a unique loan account number
     */
    private String generateLoanAccountNumber() {
        return String.format("LOAN-%s", System.currentTimeMillis());
    }

}
