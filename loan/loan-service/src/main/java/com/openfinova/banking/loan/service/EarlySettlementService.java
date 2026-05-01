package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.api.entity.SettlementCalculationMethod;
import com.openfinova.banking.loan.api.entity.SettlementStatus;
import com.openfinova.banking.loan.dto.SettlementValidationResult;
import com.openfinova.banking.loan.entity.EarlySettlement;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.repository.EarlySettlementRepository;
import com.openfinova.banking.loan.repository.LoanAccountRepository;

/**
 * Implementation of EarlySettlementService for managing loan early settlement requests.
 *
 * This service handles:
 * - Generating settlement quotes with different calculation methods
 * - Processing settlement approvals and rejections
 * - Calculating settlement amounts and penalties
 * - Managing settlement lifecycle (quote, approval, completion)
 * - Validating settlement requests
 * - Tracking settlement history
 *
 * Early settlement allows borrowers to pay off their loans before the maturity date,
 * potentially with penalties or discounts depending on the calculation method used.
 *
 * Calculation Methods:
 * - FULL_OUTSTANDING: Total outstanding amount without any discount
 *   (principal + interest + fees + penalties)
 * - DISCOUNTED: Provides 10% discount on outstanding interest to incentivize early payment
 *   (principal + 90% of interest + fees + penalties)
 *
 * @see EarlySettlementService
 * @see EarlySettlement
 * @see com.openfinova.banking.loan.api.entity.SettlementStatus
 * @see com.openfinova.banking.loan.api.entity.SettlementCalculationMethod
 */
@Service
@Transactional
public class EarlySettlementService {

    private final EarlySettlementRepository settlementRepository;
    private final LoanAccountRepository loanAccountRepository;

    public EarlySettlementService(EarlySettlementRepository settlementRepository,
            LoanAccountRepository loanAccountRepository) {
        this.settlementRepository = settlementRepository;
        this.loanAccountRepository = loanAccountRepository;
    }

    /**
     * Generates an early settlement quote for a loan account.
     *
     * This method calculates the total amount required to settle the loan early,
     * including any applicable penalties or discounts based on the calculation method.
     * The quote is valid for 30 days from the settlement date.
     *
     * The settlement amount includes:
     * - Outstanding principal
     * - Outstanding interest (discounted if using DISCOUNTED method)
     * - Outstanding fees
     * - Outstanding penalties
     * - Early settlement penalty (if applicable)
     *
     * Calculation methods:
     * - FULL_OUTSTANDING: Pay full amount without any discount
     * - DISCOUNTED: Get 10% discount on outstanding interest
     *
     * @param loanAccountId the ID of the loan account
     * @param settlementDate the proposed settlement date
     * @param calculationMethod the method to calculate settlement amount (FULL_OUTSTANDING or DISCOUNTED)
     * @param requestedBy the user requesting the quote
     * @return the generated settlement quote with QUOTE status
     * @throws IllegalArgumentException if loan account not found
     */
    public EarlySettlement generateSettlementQuote(UUID loanAccountId, LocalDate settlementDate,
            SettlementCalculationMethod calculationMethod, String requestedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        BigDecimal settlementAmount = calculateSettlementAmount(loanAccountId, settlementDate, calculationMethod);
        BigDecimal penalty = calculateEarlySettlementPenalty(loanAccountId, settlementDate);

        EarlySettlement settlement = new EarlySettlement();
        settlement.setLoanAccount(loanAccount);
        settlement.setQuoteDate(settlementDate);
        settlement.setCalculationMethod(calculationMethod);
        settlement.setOutstandingPrincipal(loanAccount.getOutstandingPrincipal());
        settlement.setOutstandingInterest(loanAccount.getOutstandingInterest());
        settlement.setOutstandingFees(loanAccount.getOutstandingFees());
        settlement.setRebateAmount(BigDecimal.ZERO);
        settlement.setPenaltyAmount(penalty);
        settlement.setSettlementAmount(settlementAmount.add(penalty));
        settlement.setCurrency(loanAccount.getCurrency());
        settlement.setStatus(SettlementStatus.QUOTE);
        settlement.setValidUntil(settlementDate.plusDays(30));

        return settlementRepository.save(settlement);
    }

    /**
     * Retrieves an early settlement by its unique identifier.
     *
     * @param id the settlement ID
     * @return Optional containing the settlement if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<EarlySettlement> getEarlySettlementById(UUID id) {
        return settlementRepository.findById(id);
    }

    /**
     * Retrieves all early settlements for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of all early settlements for the loan account (all statuses)
     */
    @Transactional(readOnly = true)
    public List<EarlySettlement> getEarlySettlementsByLoanAccount(UUID loanAccountId) {
        return settlementRepository.findByLoanAccountId(loanAccountId);
    }

    /**
     * Retrieves the most recent early settlement for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return Optional containing the latest settlement if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<EarlySettlement> getLatestSettlement(UUID loanAccountId) {
        return settlementRepository.findTopByLoanAccountIdOrderByCreatedAtDesc(loanAccountId);
    }

    /**
     * Retrieves the active quote for a loan account.
     *
     * Returns the first settlement with QUOTE status for the loan account.
     * Only one active quote should exist per loan account at a time.
     *
     * @param loanAccountId the loan account ID
     * @return Optional containing the active quote if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<EarlySettlement> getActiveQuote(UUID loanAccountId) {
        List<EarlySettlement> settlements = settlementRepository
                .findByLoanAccountIdAndStatus(loanAccountId, SettlementStatus.QUOTE);
        return settlements.isEmpty() ? Optional.empty() : Optional.of(settlements.get(0));
    }

    /**
     * Retrieves early settlements by status with pagination.
     *
     * @param status the settlement status to filter by
     * @param pageable pagination parameters
     * @return page of settlements matching the specified status
     */
    @Transactional(readOnly = true)
    public Page<EarlySettlement> getEarlySettlementsByStatus(SettlementStatus status, Pageable pageable) {
        return settlementRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Optional<EarlySettlement> getEarlySettlementForLoanAccount(UUID loanAccountId, UUID settlementId) {
        return settlementRepository.findById(settlementId)
                .filter(s -> s.getLoanAccount() != null && loanAccountId.equals(s.getLoanAccount().getId()));
    }

    @Transactional(readOnly = true)
    public Page<EarlySettlement> getEarlySettlementsByLoanAccountAndStatus(UUID loanAccountId, SettlementStatus status,
            Pageable pageable) {
        return settlementRepository.findByLoanAccount_IdAndStatus(loanAccountId, status, pageable);
    }

    @Transactional(readOnly = true)
    public long countPendingSettlementsForLoanAccount(UUID loanAccountId) {
        return settlementRepository.countByLoanAccount_IdAndStatus(loanAccountId, SettlementStatus.PENDING_APPROVAL);
    }

    /**
     * Retrieves all settlements pending approval with pagination.
     *
     * @param pageable pagination parameters
     * @return page of settlements with PENDING_APPROVAL status
     */
    @Transactional(readOnly = true)
    public Page<EarlySettlement> getPendingSettlements(Pageable pageable) {
        return settlementRepository.findByStatus(SettlementStatus.PENDING_APPROVAL, pageable);
    }

    /**
     * Retrieves all approved settlements with pagination.
     *
     * @param pageable pagination parameters
     * @return page of settlements with APPROVED status
     */
    @Transactional(readOnly = true)
    public Page<EarlySettlement> getApprovedSettlements(Pageable pageable) {
        return settlementRepository.findByStatus(SettlementStatus.APPROVED, pageable);
    }

    /**
     * Retrieves all completed settlements with pagination.
     *
     * @param pageable pagination parameters
     * @return page of settlements with COMPLETED status
     */
    @Transactional(readOnly = true)
    public Page<EarlySettlement> getCompletedSettlements(Pageable pageable) {
        return settlementRepository.findByStatus(SettlementStatus.COMPLETED, pageable);
    }

    /**
     * Approves an early settlement request.
     *
     * Changes the settlement status from PENDING_APPROVAL to APPROVED and
     * records the approval date and approving user for audit trail.
     * Once approved, the settlement can be processed for payment.
     *
     * @param settlementId the ID of the settlement to approve
     * @param approvedBy the user approving the settlement
     * @return the approved settlement with approval details
     * @throws IllegalArgumentException if settlementId is null, approvedBy is null/empty, or settlement not found
     */
    public EarlySettlement approveSettlement(UUID loanAccountId, UUID settlementId, String approvedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (settlementId == null) {
            throw new IllegalArgumentException("Settlement ID cannot be null");
        }
        if (approvedBy == null || approvedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Approved by cannot be null or empty");
        }

        EarlySettlement settlement = requireSettlementForLoan(loanAccountId, settlementId);

        SettlementStatus st = settlement.getStatus();
        if (!SettlementStatus.QUOTE.equals(st) && !SettlementStatus.PENDING_APPROVAL.equals(st)) {
            throw new IllegalStateException(
                    "Only QUOTE or PENDING_APPROVAL settlements can be approved; current: " + st);
        }

        settlement.setStatus(SettlementStatus.APPROVED);
        settlement.setApprovedDate(LocalDate.now());
        settlement.setApprovedBy(approvedBy);

        return settlementRepository.save(settlement);
    }

    /**
     * Rejects an early settlement request.
     *
     * Changes the settlement status to REJECTED and records the rejection date,
     * rejecting user, and rejection reason for audit trail.
     * Rejected settlements cannot be processed.
     *
     * @param settlementId the ID of the settlement to reject
     * @param rejectionReason the reason for rejection
     * @param rejectedBy the user rejecting the settlement
     * @return the rejected settlement with rejection details
     * @throws IllegalArgumentException if settlementId is null, rejectionReason is null/empty, rejectedBy is null/empty, or settlement not found
     */
    public EarlySettlement rejectSettlement(UUID loanAccountId, UUID settlementId, String rejectionReason,
            String rejectedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (settlementId == null) {
            throw new IllegalArgumentException("Settlement ID cannot be null");
        }
        if (rejectionReason == null || rejectionReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejection reason cannot be null or empty");
        }
        if (rejectedBy == null || rejectedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Rejected by cannot be null or empty");
        }

        EarlySettlement settlement = requireSettlementForLoan(loanAccountId, settlementId);

        SettlementStatus st = settlement.getStatus();
        if (!SettlementStatus.QUOTE.equals(st) && !SettlementStatus.PENDING_APPROVAL.equals(st)) {
            throw new IllegalStateException(
                    "Only QUOTE or PENDING_APPROVAL settlements can be rejected; current: " + st);
        }

        settlement.setStatus(SettlementStatus.REJECTED);
        settlement.setRejectedDate(LocalDate.now());
        settlement.setRejectedBy(rejectedBy);
        settlement.setRejectionReason(rejectionReason);

        return settlementRepository.save(settlement);
    }

    /**
     * Processes an approved early settlement.
     *
     * Marks the settlement as COMPLETED and records the payment date.
     * This method should only be called after payment has been received.
     *
     * Only settlements with APPROVED status can be processed. This ensures
     * proper approval workflow is followed.
     *
     * @param settlementId the ID of the settlement to process
     * @param paymentDate the date when payment was received
     * @param processedBy the user processing the settlement
     * @return the completed settlement
     * @throws IllegalArgumentException if settlementId is null, paymentDate is null, processedBy is null/empty, or settlement not found
     * @throws IllegalStateException if settlement is not in APPROVED status
     */
    public EarlySettlement processSettlement(UUID loanAccountId, UUID settlementId, LocalDate paymentDate,
            String processedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (settlementId == null) {
            throw new IllegalArgumentException("Settlement ID cannot be null");
        }
        if (paymentDate == null) {
            throw new IllegalArgumentException("Payment date cannot be null");
        }
        if (paymentDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Payment date cannot be in the future");
        }
        if (processedBy == null || processedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Processed by cannot be null or empty");
        }

        EarlySettlement settlement = settlementRepository.findById(settlementId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Settlement not found: %s", settlementId)));

        if (!SettlementStatus.APPROVED.equals(settlement.getStatus())) {
            throw new IllegalStateException(
                    String.format(
                            "Only approved settlements can be processed. Current status: %s",
                            settlement.getStatus()));
        }

        settlement.setStatus(SettlementStatus.COMPLETED);
        settlement.setSettledDate(paymentDate);

        return settlementRepository.save(settlement);
    }

    /**
     * Cancels an early settlement request.
     *
     * Changes the settlement status to CANCELLED and records the cancellation date,
     * cancelling user, and cancellation reason for audit trail.
     * This can be used to cancel quotes, pending approvals, or approved settlements
     * that are no longer needed.
     *
     * @param settlementId the ID of the settlement to cancel
     * @param cancellationReason the reason for cancellation
     * @param cancelledBy the user cancelling the settlement
     * @return the cancelled settlement with cancellation details
     * @throws IllegalArgumentException if settlementId is null, cancellationReason is null/empty, cancelledBy is null/empty, or settlement not found
     */
    public EarlySettlement cancelSettlement(UUID loanAccountId, UUID settlementId, String cancellationReason,
            String cancelledBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (settlementId == null) {
            throw new IllegalArgumentException("Settlement ID cannot be null");
        }
        if (cancellationReason == null || cancellationReason.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancellation reason cannot be null or empty");
        }
        if (cancelledBy == null || cancelledBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Cancelled by cannot be null or empty");
        }

        EarlySettlement settlement = settlementRepository.findById(settlementId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Settlement not found: %s", settlementId)));

        SettlementStatus st = settlement.getStatus();
        if (SettlementStatus.COMPLETED.equals(st) || SettlementStatus.CANCELLED.equals(st)
                || SettlementStatus.REJECTED.equals(st) || SettlementStatus.EXPIRED.equals(st)) {
            throw new IllegalStateException("Settlement cannot be cancelled in status: " + st);
        }

        settlement.setStatus(SettlementStatus.CANCELLED);
        settlement.setCancelledDate(LocalDate.now());
        settlement.setCancelledBy(cancelledBy);
        settlement.setCancellationReason(cancellationReason);

        return settlementRepository.save(settlement);
    }

    /**
     * Calculates the settlement amount for early loan payoff.
     *
     * The calculation method determines how the settlement amount is computed:
     *
     * FULL_OUTSTANDING:
     * - Returns the total outstanding amount without any discounts
     * - Formula: principal + interest + fees + penalties
     * - Use when no incentive is offered for early payment
     *
     * DISCOUNTED:
     * - Applies a 10% discount on outstanding interest
     * - Formula: principal + (90% of interest) + fees + penalties
     * - Incentivizes early settlement by reducing interest burden
     * - Benefits both borrower (lower cost) and lender (early capital recovery)
     *
     * @param loanAccountId the ID of the loan account
     * @param settlementDate the proposed settlement date
     * @param calculationMethod the method to calculate settlement amount (FULL_OUTSTANDING or DISCOUNTED)
     * @return the calculated settlement amount (excluding early settlement penalty)
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateSettlementAmount(UUID loanAccountId, LocalDate settlementDate,
            SettlementCalculationMethod calculationMethod) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Loan account not found: %s", loanAccountId)));

        BigDecimal totalOutstanding = loanAccount.getTotalOutstanding();

        return switch (calculationMethod) {
            case FULL_OUTSTANDING -> totalOutstanding;
            case DISCOUNTED -> {
                // Apply 10% discount on interest
                BigDecimal discountedInterest = loanAccount.getOutstandingInterest().multiply(BigDecimal.valueOf(0.9));
                yield loanAccount.getOutstandingPrincipal().add(discountedInterest)
                        .add(loanAccount.getOutstandingFees()).add(loanAccount.getOutstandingPenalties());
            }
        };
    }

    /**
     * Calculates the penalty for early settlement.
     *
     * The penalty compensates the lender for lost interest income when a loan
     * is paid off before maturity. The penalty is calculated as:
     * - 1% of outstanding principal if there are months remaining until maturity
     * - Zero if the settlement date is at or after the maturity date
     *
     * This encourages borrowers to settle closer to maturity while still allowing
     * early payoff with a reasonable penalty.
     *
     * @param loanAccountId the ID of the loan account
     * @param settlementDate the proposed settlement date
     * @return the calculated early settlement penalty
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateEarlySettlementPenalty(UUID loanAccountId, LocalDate settlementDate) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        long monthsRemaining = ChronoUnit.MONTHS.between(settlementDate, loanAccount.getMaturityDate());

        if (monthsRemaining <= 0) {
            return BigDecimal.ZERO;
        }

        // 1% penalty on outstanding principal
        return loanAccount.getOutstandingPrincipal().multiply(BigDecimal.valueOf(0.01))
                .setScale(2, RoundingMode.HALF_UP);
    }

    /**
     * Calculates the total settlement amounts within a date range.
     *
     * Sums all settlement amounts for settlements completed within the specified
     * date range. Useful for reporting and financial analysis.
     *
     * @param startDate the start date of the range (inclusive)
     * @param endDate the end date of the range (inclusive)
     * @return the total settlement amount, or zero if no settlements in range
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalSettlementsByDateRange(LocalDate startDate, LocalDate endDate) {
        return settlementRepository.sumSettlementsByDateRange(startDate, endDate);
    }

    /**
     * Counts the total number of early settlements for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return the count of early settlements (all statuses)
     */
    @Transactional(readOnly = true)
    public long countEarlySettlementsByLoanAccount(UUID loanAccountId) {
        return settlementRepository.countByLoanAccountId(loanAccountId);
    }

    /**
     * Checks if a loan account has an active settlement quote.
     *
     * @param loanAccountId the loan account ID
     * @return true if an active quote exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasActiveQuote(UUID loanAccountId) {
        return settlementRepository.existsByLoanAccountIdAndStatus(loanAccountId, SettlementStatus.QUOTE);
    }

    /**
     * Validates an early settlement request before generating a quote.
     *
     * Performs business rule validation to ensure the settlement request is valid:
     * - Loan must be in ACTIVE status
     * - Settlement date must not be in the past
     * - Settlement date must not be after the loan maturity date
     *
     * Returns a validation result with:
     * - Valid flag indicating if the request passes all validations
     * - Message describing the overall result
     * - List of specific error messages if validation fails
     *
     * @param loanAccountId the ID of the loan account
     * @param settlementDate the proposed settlement date
     * @return validation result with errors if any
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    public SettlementValidationResult validateSettlementRequest(UUID loanAccountId, LocalDate settlementDate) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        List<String> errors = new ArrayList<>();

        if (!LoanStatus.ACTIVE.equals(loanAccount.getStatus())) {
            errors.add("Loan must be active for early settlement");
        }

        if (settlementDate.isBefore(LocalDate.now())) {
            errors.add("Settlement date cannot be in the past");
        }

        if (settlementDate.isAfter(loanAccount.getMaturityDate())) {
            errors.add("Settlement date is after maturity date");
        }

        boolean valid = errors.isEmpty();
        SettlementValidationResult result = new SettlementValidationResult(
                valid,
                valid ? "Settlement request is valid" : "Settlement request has errors");
        result.setErrors(errors);

        return result;
    }

    private EarlySettlement requireSettlementForLoan(UUID loanAccountId, UUID settlementId) {
        EarlySettlement settlement = settlementRepository.findById(settlementId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Settlement not found: %s", settlementId)));
        if (settlement.getLoanAccount() == null || !loanAccountId.equals(settlement.getLoanAccount().getId())) {
            throw new IllegalArgumentException("Settlement does not belong to this loan account");
        }
        return settlement;
    }
}
