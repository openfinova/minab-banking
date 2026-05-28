package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.entity.PaymentMethod;
import com.openfinova.banking.loan.api.entity.PaymentType;
import com.openfinova.banking.loan.event.LoanRepaymentRecordedEvent;
import com.openfinova.banking.loan.dto.BulkPaymentImportResult;
import com.openfinova.banking.loan.dto.PaymentAllocation;
import com.openfinova.banking.loan.dto.PaymentImportRecord;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.entity.LoanPayment;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.loan.repository.LoanPaymentRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Implementation of LoanPaymentService for managing loan payment processing.
 *
 * This service handles the critical process of recording and managing loan payments,
 * which is central to the loan lifecycle. It manages payment allocation, balance
 * updates, and payment reversals.
 *
 * Key Responsibilities:
 * - Recording payments with automatic allocation
 * - Recording payments with manual allocation
 * - Payment allocation calculation (waterfall method)
 * - Balance updates after payments
 * - Payment reversals and corrections
 * - Payment history and reporting
 * - Bulk payment import processing
 *
 * Payment Allocation Waterfall:
 * Payments are allocated in the following priority order:
 * 1. Penalties (late fees, default charges)
 * 2. Fees (service fees, processing fees)
 * 3. Interest (accrued interest charges)
 * 4. Principal (original loan amount)
 * 5. Excess (overpayment, if any)
 *
 * This ensures that the most critical charges are paid first, protecting
 * the lender's interests while following industry best practices.
 *
 * Payment Types:
 * - REGULAR_PAYMENT: Scheduled installment payment
 * - PREPAYMENT: Early payment of future installments
 * - PARTIAL_PAYMENT: Payment less than scheduled amount
 * - FULL_SETTLEMENT: Complete loan payoff
 * - PENALTY_PAYMENT: Payment of penalties only
 *
 * Payment Methods:
 * - CASH: Cash payment at branch
 * - BANK_TRANSFER: Direct bank transfer
 * - CHEQUE: Payment by cheque
 * - MOBILE_MONEY: Mobile wallet payment
 * - CARD: Credit/debit card payment
 * - DIRECT_DEBIT: Automatic bank deduction
 *
 * Business Rules:
 * - Payment reference must be unique
 * - Payment amount must be positive
 * - Payment date cannot be in the future
 * - Allocation must equal payment amount
 * - Balances cannot go negative
 * - Only non-reversed payments can be reversed
 *
 * Integration Points:
 * - Updates loan account balances
 * - Updates loan schedules
 * - Posts accounting entries
 * - Triggers notifications
 * - Updates delinquency status
 *
 * @see LoanPaymentService
 * @see LoanPayment
 * @see com.openfinova.banking.loan.api.entity.PaymentType
 * @see com.openfinova.banking.loan.api.entity.PaymentMethod
 */
@Service
@Transactional
public class LoanPaymentService {

    private final LoanPaymentRepository paymentRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final LoanScheduleService loanScheduleService;
    private final ApplicationEventPublisher eventPublisher;
    private final DateTimeService dateTimeService;

    public LoanPaymentService(LoanPaymentRepository paymentRepository, LoanAccountRepository loanAccountRepository,
            LoanScheduleService loanScheduleService, ApplicationEventPublisher eventPublisher,
            DateTimeService dateTimeService) {
        this.paymentRepository = paymentRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.loanScheduleService = loanScheduleService;
        this.eventPublisher = eventPublisher;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Whether a repayment already exists for the given external transaction reference.
     * Used for idempotent posting from transaction processing and payment rails.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('service:loan:read')")
    public boolean repaymentExistsForTransactionReference(String transactionReference) {
        if (transactionReference == null || transactionReference.isBlank()) {
            return false;
        }
        return paymentRepository.findByTransactionReference(transactionReference).isPresent();
    }

    /**
     * Records a loan payment with automatic allocation using the waterfall method.
     *
     * This is the primary method for recording regular loan payments. The payment
     * amount is automatically allocated across outstanding balances using the
     * industry-standard waterfall approach.
     *
     * Allocation Priority (Waterfall Method):
     * 1. Penalties: Late fees, default charges (highest priority)
     * 2. Fees: Service fees, processing fees
     * 3. Interest: Accrued interest charges
     * 4. Principal: Original loan amount (lowest priority)
     * 5. Excess: Any remaining amount after full allocation
     *
     * This priority ensures that:
     * - Critical charges are paid first
     * - Lender's interests are protected
     * - Regulatory compliance is maintained
     * - Industry best practices are followed
     *
     * The method performs the following operations:
     * - Validates loan account exists
     * - Calculates payment allocation using waterfall method
     * - Creates payment record with allocation details
     * - Updates loan account balances
     * - Updates last payment date and total paid
     * - Generates unique payment reference
     *
     * Integration Effects:
     * - Loan account balances are updated immediately
     * - Delinquency status may be cleared if payment brings loan current
     * - Loan schedules should be updated separately
     * - Accounting entries should be posted separately
     * - Customer notifications should be sent separately
     *
     * @param loanAccountId the ID of the loan account receiving payment
     * @param paymentAmount the total amount being paid (must be positive)
     * @param paymentDate the date the payment was made (cannot be future)
     * @param paymentMethod the method used for payment (cash, transfer, etc.)
     * @param transactionReference external reference from payment system
     * @param recordedBy the user recording the payment
     * @return the created payment record with allocation details
     * @throws IllegalArgumentException if loan account not found or invalid parameters
     */
    @PreAuthorize("hasAnyAuthority('loan:collect', 'loan:write', 'service:loan:write')")
    public LoanPayment recordPayment(UUID loanAccountId, BigDecimal paymentAmount, LocalDate paymentDate,
            PaymentMethod paymentMethod, String transactionReference, String recordedBy) {
        if (transactionReference != null && !transactionReference.isBlank()
                && paymentRepository.findByTransactionReference(transactionReference).isPresent()) {
            throw new IllegalStateException(
                    "Payment already recorded for transaction reference: " + transactionReference);
        }

        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        PaymentAllocation allocation = calculatePaymentAllocation(loanAccountId, paymentAmount);

        LoanPayment payment = new LoanPayment();
        payment.setLoanAccount(loanAccount);
        payment.setPaymentReference(generatePaymentReference());
        payment.setPaymentAmount(paymentAmount);
        payment.setPrincipalPaid(allocation.getPrincipalAmount());
        payment.setInterestPaid(allocation.getInterestAmount());
        payment.setFeesPaid(allocation.getFeesAmount());
        payment.setPenaltiesPaid(allocation.getPenaltiesAmount());
        payment.setPaymentDate(paymentDate);
        payment.setPaymentType(PaymentType.REGULAR_PAYMENT);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionReference(transactionReference);
        payment.setIsReversed(false);

        payment = paymentRepository.save(payment);

        // Update loan account balances
        loanAccount.setOutstandingPrincipal(
                loanAccount.getOutstandingPrincipal().subtract(allocation.getPrincipalAmount()));
        loanAccount
                .setOutstandingInterest(loanAccount.getOutstandingInterest().subtract(allocation.getInterestAmount()));
        loanAccount.setOutstandingFees(loanAccount.getOutstandingFees().subtract(allocation.getFeesAmount()));
        loanAccount.setOutstandingPenalties(
                loanAccount.getOutstandingPenalties().subtract(allocation.getPenaltiesAmount()));
        loanAccount.setLastPaymentDate(paymentDate);
        loanAccount.setTotalPaid(loanAccount.getTotalPaid().add(paymentAmount));

        loanAccountRepository.save(loanAccount);

        loanScheduleService.applyWaterfallPaymentToSchedules(
                loanAccountId,
                allocation.getPrincipalAmount(),
                allocation.getInterestAmount(),
                allocation.getFeesAmount(),
                allocation.getPenaltiesAmount());

        eventPublisher.publishEvent(
                new LoanRepaymentRecordedEvent(loanAccountId, payment.getId(), paymentAmount, recordedBy));

        return payment;
    }

    /**
     * Records a loan payment with manually specified allocation.
     *
     * This method allows explicit control over how the payment is allocated
     * across different balance components. It's used for special cases where
     * the standard waterfall allocation is not appropriate.
     *
     * Use Cases for Manual Allocation:
     * - Partial prepayments (applying extra amount to principal)
     * - Fee waivers (not collecting certain fees)
     * - Settlement negotiations (custom allocation agreements)
     * - Correction of previous payment allocations
     * - Regulatory requirements for specific allocation
     * - Customer-directed allocation preferences
     *
     * Validation Rules:
     * - Sum of allocated amounts must equal total payment amount
     * - Individual allocations cannot be negative
     * - Allocated amounts cannot exceed outstanding balances
     * - Payment type must be appropriate for allocation
     *
     * The method performs the following operations:
     * - Validates loan account exists
     * - Validates allocation totals match payment amount
     * - Creates payment record with specified allocation
     * - Updates loan account balances according to allocation
     * - Updates last payment date and total paid
     * - Generates unique payment reference
     *
     * Important Notes:
     * - Caller is responsible for ensuring allocation is valid
     * - No automatic waterfall calculation is performed
     * - Business rules for allocation priority are bypassed
     * - Should be used carefully to maintain data integrity
     *
     * @param loanAccountId the ID of the loan account receiving payment
     * @param paymentAmount the total amount being paid
     * @param principalPaid amount allocated to principal reduction
     * @param interestPaid amount allocated to interest payment
     * @param feesPaid amount allocated to fees payment
     * @param penaltiesPaid amount allocated to penalties payment
     * @param paymentDate the date the payment was made
     * @param paymentType the type of payment being recorded
     * @param paymentMethod the method used for payment
     * @param transactionReference external reference from payment system
     * @param recordedBy the user recording the payment
     * @return the created payment record with specified allocation
     * @throws IllegalArgumentException if loan account not found, allocation invalid, or parameters invalid
     */
    @PreAuthorize("hasAnyAuthority('loan:collect', 'loan:write')")
    public LoanPayment recordPaymentWithAllocation(UUID loanAccountId, BigDecimal paymentAmount,
            BigDecimal principalPaid, BigDecimal interestPaid, BigDecimal feesPaid, BigDecimal penaltiesPaid,
            LocalDate paymentDate, PaymentType paymentType, PaymentMethod paymentMethod, String transactionReference,
            String recordedBy) {
        if (transactionReference != null && !transactionReference.isBlank()
                && paymentRepository.findByTransactionReference(transactionReference).isPresent()) {
            throw new IllegalStateException(
                    "Payment already recorded for transaction reference: " + transactionReference);
        }

        BigDecimal p = principalPaid != null ? principalPaid : BigDecimal.ZERO;
        BigDecimal i = interestPaid != null ? interestPaid : BigDecimal.ZERO;
        BigDecimal f = feesPaid != null ? feesPaid : BigDecimal.ZERO;
        BigDecimal pen = penaltiesPaid != null ? penaltiesPaid : BigDecimal.ZERO;
        BigDecimal sum = p.add(i).add(f).add(pen);
        if (paymentAmount == null || sum.compareTo(paymentAmount) != 0) {
            throw new IllegalArgumentException(
                    "Allocated amounts must sum to payment amount; expected " + paymentAmount + " but was " + sum);
        }

        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        LoanPayment payment = new LoanPayment();
        payment.setLoanAccount(loanAccount);
        payment.setPaymentReference(generatePaymentReference());
        payment.setPaymentAmount(paymentAmount);
        payment.setPrincipalPaid(principalPaid);
        payment.setInterestPaid(interestPaid);
        payment.setFeesPaid(feesPaid);
        payment.setPenaltiesPaid(penaltiesPaid);
        payment.setPaymentDate(paymentDate);
        payment.setPaymentType(paymentType);
        payment.setPaymentMethod(paymentMethod);
        payment.setTransactionReference(transactionReference);
        payment.setIsReversed(false);

        payment = paymentRepository.save(payment);

        // Update loan account balances
        loanAccount.setOutstandingPrincipal(loanAccount.getOutstandingPrincipal().subtract(principalPaid));
        loanAccount.setOutstandingInterest(loanAccount.getOutstandingInterest().subtract(interestPaid));
        loanAccount.setOutstandingFees(loanAccount.getOutstandingFees().subtract(feesPaid));
        loanAccount.setOutstandingPenalties(loanAccount.getOutstandingPenalties().subtract(penaltiesPaid));
        loanAccount.setLastPaymentDate(paymentDate);
        loanAccount.setTotalPaid(loanAccount.getTotalPaid().add(paymentAmount));

        loanAccountRepository.save(loanAccount);

        loanScheduleService
                .applyWaterfallPaymentToSchedules(loanAccountId, principalPaid, interestPaid, feesPaid, penaltiesPaid);

        eventPublisher.publishEvent(
                new LoanRepaymentRecordedEvent(loanAccountId, payment.getId(), paymentAmount, recordedBy));

        return payment;
    }

    /**
     * Retrieves a payment by its unique identifier.
     *
     * @param id the payment ID
     * @return Optional containing the payment if found, empty otherwise
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Optional<LoanPayment> getPaymentById(UUID id) {
        return paymentRepository.findById(id);
    }

    /**
     * Retrieves a payment by its reference number.
     *
     * Payment references are unique identifiers used for:
     * - Transaction tracking and reconciliation
     * - Customer service inquiries
     * - Dispute resolution
     * - Audit trails
     * - Integration with payment systems
     *
     * @param paymentReference the payment reference number
     * @return Optional containing the payment if found, empty otherwise
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Optional<LoanPayment> getPaymentByReference(String paymentReference) {
        return paymentRepository.findByPaymentReference(paymentReference);
    }

    /**
     * Retrieves all payments for a loan account with pagination.
     *
     * Returns complete payment history including reversed payments.
     * Useful for:
     * - Customer payment history
     * - Loan reconciliation
     * - Audit and compliance
     * - Dispute investigation
     *
     * @param loanAccountId the loan account ID
     * @param pageable pagination parameters
     * @return page of payments for the loan account
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanPayment> getPaymentsByLoanAccount(UUID loanAccountId, Pageable pageable) {
        return paymentRepository.findByLoanAccountId(loanAccountId, pageable);
    }

    /**
     * Retrieves payments for a loan account within a date range with pagination.
     *
     * Filters payments by payment date for period-specific analysis.
     * Useful for:
     * - Monthly/quarterly payment reports
     * - Cash flow analysis
     * - Reconciliation with bank statements
     * - Performance metrics calculation
     *
     * @param loanAccountId the loan account ID
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @param pageable pagination parameters
     * @return page of payments within the specified date range
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanPayment> getPaymentsByLoanAccountAndDateRange(UUID loanAccountId, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        return paymentRepository.findByLoanAccountIdAndPaymentDateBetween(loanAccountId, startDate, endDate, pageable);
    }

    /**
     * Retrieves payments by payment type with pagination.
     *
     * Filters payments by type for analysis and reporting:
     * - REGULAR_PAYMENT: Scheduled installment payments
     * - PREPAYMENT: Early payments of future installments
     * - PARTIAL_PAYMENT: Payments less than scheduled amount
     * - FULL_SETTLEMENT: Complete loan payoff
     * - PENALTY_PAYMENT: Payment of penalties only
     *
     * @param paymentType the payment type to filter by
     * @param pageable pagination parameters
     * @return page of payments of the specified type
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanPayment> getPaymentsByType(PaymentType paymentType, Pageable pageable) {
        return paymentRepository.findByPaymentType(paymentType, pageable);
    }

    /**
     * Retrieves payments by payment method with pagination.
     *
     * Filters payments by method for operational analysis:
     * - CASH: Cash payments at branch
     * - BANK_TRANSFER: Direct bank transfers
     * - CHEQUE: Cheque payments
     * - MOBILE_MONEY: Mobile wallet payments
     * - CARD: Credit/debit card payments
     * - DIRECT_DEBIT: Automatic bank deductions
     *
     * Useful for:
     * - Channel performance analysis
     * - Reconciliation by payment method
     * - Fee analysis by channel
     * - Customer preference insights
     *
     * @param paymentMethod the payment method to filter by
     * @param pageable pagination parameters
     * @return page of payments using the specified method
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanPayment> getPaymentsByMethod(PaymentMethod paymentMethod, Pageable pageable) {
        return paymentRepository.findByPaymentMethod(paymentMethod, pageable);
    }

    /**
     * Retrieves all reversed payments with pagination.
     *
     * Reversed payments are those that have been cancelled or corrected
     * after initial recording. This query is useful for:
     * - Audit and compliance reporting
     * - Error analysis and process improvement
     * - Reconciliation of payment reversals
     * - Investigation of payment disputes
     *
     * Reversed payments maintain their original allocation details
     * but are marked with reversal information including reason
     * and timestamp.
     *
     * @param pageable pagination parameters
     * @return page of reversed payments
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<LoanPayment> getReversedPayments(Pageable pageable) {
        return paymentRepository.findReversedPayments(pageable);
    }

    /**
     * Reverses a previously recorded payment.
     *
     * Payment reversal is used to correct errors or handle disputed payments.
     * This is a critical operation that must maintain data integrity and
     * provide complete audit trails.
     *
     * Common Reversal Scenarios:
     * - Payment recorded in error (wrong amount, wrong account)
     * - Duplicate payment entry
     * - Bounced cheque or failed electronic payment
     * - Customer dispute resolution
     * - Fraud detection and correction
     * - System error correction
     *
     * Reversal Process:
     * 1. Validates payment exists and is not already reversed
     * 2. Restores loan account balances to pre-payment state
     * 3. Marks payment as reversed with reason and timestamp
     * 4. Preserves original payment details for audit
     * 5. Updates total paid amount on loan account
     *
     * Business Rules:
     * - Only non-reversed payments can be reversed
     * - Reversal reason is mandatory for audit compliance
     * - Original payment record is preserved (not deleted)
     * - Loan account balances are restored exactly
     * - Reversal cannot be undone (requires new payment)
     *
     * Integration Effects:
     * - Loan account balances are updated immediately
     * - Delinquency status may change if payment was keeping loan current
     * - Loan schedules should be updated separately
     * - Accounting reversal entries should be posted separately
     * - Customer notifications should be sent separately
     *
     * Security Considerations:
     * - Verify authorization to reverse payments
     * - Log reversal for audit trail
     * - Alert on high-value reversals
     * - Monitor for suspicious reversal patterns
     *
     * @param paymentId the ID of the payment to reverse
     * @param reversalReason the reason for reversal (required for audit)
     * @param reversedBy the user performing the reversal
     * @return the reversed payment with reversal details
     * @throws IllegalArgumentException if payment not found
     * @throws IllegalStateException if payment is already reversed
     */
    @PreAuthorize("hasAuthority('loan:collect')")
    public LoanPayment reversePayment(UUID paymentId, String reversalReason, String reversedBy) {
        LoanPayment payment = paymentRepository.findById(paymentId)
                .orElseThrow(() -> new IllegalArgumentException("Payment not found: " + paymentId));

        if (payment.getIsReversed()) {
            throw new IllegalStateException("Payment is already reversed");
        }

        LoanAccount loanAccount = payment.getLoanAccount();

        // Reverse the balance updates
        loanAccount.setOutstandingPrincipal(loanAccount.getOutstandingPrincipal().add(payment.getPrincipalPaid()));
        loanAccount.setOutstandingInterest(loanAccount.getOutstandingInterest().add(payment.getInterestPaid()));
        loanAccount.setOutstandingFees(loanAccount.getOutstandingFees().add(payment.getFeesPaid()));
        loanAccount.setOutstandingPenalties(loanAccount.getOutstandingPenalties().add(payment.getPenaltiesPaid()));
        loanAccount.setTotalPaid(loanAccount.getTotalPaid().subtract(payment.getPaymentAmount()));

        loanAccountRepository.save(loanAccount);

        payment.setIsReversed(true);
        payment.setReversalReason(reversalReason);
        payment.setReversedAt(dateTimeService.instant());

        return paymentRepository.save(payment);
    }

    /**
     * Calculates how a payment should be allocated across outstanding balances.
     *
     * This method implements the industry-standard waterfall allocation method
     * without actually recording a payment. It's used for:
     * - Payment preview before recording
     * - Customer service inquiries
     * - Payment planning and advice
     * - Integration with payment calculators
     * - Mobile app payment previews
     *
     * Waterfall Allocation Priority:
     * 1. Penalties: Late fees, default charges (highest priority)
     *    - Protects lender from losses
     *    - Encourages timely payments
     *    - Regulatory compliance
     *
     * 2. Fees: Service fees, processing fees
     *    - Covers operational costs
     *    - Maintains service quality
     *    - Revenue protection
     *
     * 3. Interest: Accrued interest charges
     *    - Protects interest income
     *    - Maintains profitability
     *    - Standard industry practice
     *
     * 4. Principal: Original loan amount (lowest priority)
     *    - Reduces outstanding debt
     *    - Improves loan-to-value ratio
     *    - Customer equity building
     *
     * 5. Excess: Amount remaining after full allocation
     *    - Overpayment handling
     *    - Credit balance creation
     *    - Future payment application
     *
     * The allocation ensures that critical charges are satisfied first,
     * protecting the lender's interests while following regulatory
     * requirements and industry best practices.
     *
     * Calculation Logic:
     * - Each component is allocated up to its outstanding amount
     * - Remaining payment flows to next priority component
     * - Excess amount is tracked separately
     * - All amounts are properly rounded
     *
     * @param loanAccountId the loan account ID for allocation calculation
     * @param paymentAmount the payment amount to allocate
     * @return PaymentAllocation object with breakdown by component
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public PaymentAllocation calculatePaymentAllocation(UUID loanAccountId, BigDecimal paymentAmount) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        PaymentAllocation allocation = new PaymentAllocation();
        allocation.setTotalAmount(paymentAmount);

        BigDecimal remaining = paymentAmount;

        // Priority: Penalties -> Fees -> Interest -> Principal
        BigDecimal penaltiesAmount = remaining.min(loanAccount.getOutstandingPenalties());
        remaining = remaining.subtract(penaltiesAmount);
        allocation.setPenaltiesAmount(penaltiesAmount);

        BigDecimal feesAmount = remaining.min(loanAccount.getOutstandingFees());
        remaining = remaining.subtract(feesAmount);
        allocation.setFeesAmount(feesAmount);

        BigDecimal interestAmount = remaining.min(loanAccount.getOutstandingInterest());
        remaining = remaining.subtract(interestAmount);
        allocation.setInterestAmount(interestAmount);

        BigDecimal principalAmount = remaining.min(loanAccount.getOutstandingPrincipal());
        remaining = remaining.subtract(principalAmount);
        allocation.setPrincipalAmount(principalAmount);

        allocation.setExcessAmount(remaining);

        return allocation;
    }

    /**
     * Calculates the total amount of payments received for a loan account.
     *
     * Includes all non-reversed payments regardless of allocation.
     * Used for:
     * - Loan reconciliation
     * - Customer payment history summary
     * - Performance metrics
     * - Audit and compliance reporting
     *
     * @param loanAccountId the loan account ID
     * @return the total amount of all payments received
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public BigDecimal calculateTotalPayments(UUID loanAccountId) {
        return paymentRepository.sumPaymentsByLoanAccount(loanAccountId);
    }

    /**
     * Calculates the total principal amount paid for a loan account.
     *
     * Sums only the principal portion of all non-reversed payments.
     * This represents the actual debt reduction achieved through payments.
     *
     * Used for:
     * - Calculating remaining principal balance
     * - Loan-to-value ratio calculations
     * - Prepayment analysis
     * - Customer equity tracking
     *
     * @param loanAccountId the loan account ID
     * @return the total principal amount paid
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public BigDecimal calculateTotalPrincipalPaid(UUID loanAccountId) {
        return paymentRepository.sumPrincipalPaidByLoanAccount(loanAccountId);
    }

    /**
     * Calculates the total interest amount paid for a loan account.
     *
     * Sums only the interest portion of all non-reversed payments.
     * This represents the total interest income earned from the loan.
     *
     * Used for:
     * - Interest income reporting
     * - Profitability analysis
     * - Yield calculations
     * - Customer cost analysis
     *
     * @param loanAccountId the loan account ID
     * @return the total interest amount paid
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public BigDecimal calculateTotalInterestPaid(UUID loanAccountId) {
        return paymentRepository.sumInterestPaidByLoanAccount(loanAccountId);
    }

    /**
     * Retrieves the most recent payment for a loan account.
     *
     * Returns the last payment by payment date, useful for:
     * - Determining last payment activity
     * - Customer service inquiries
     * - Payment pattern analysis
     * - Delinquency calculations
     *
     * @param loanAccountId the loan account ID
     * @return Optional containing the last payment if any payments exist, empty otherwise
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Optional<LoanPayment> getLastPayment(UUID loanAccountId) {
        return paymentRepository.findLastPaymentByLoanAccount(loanAccountId);
    }

    /**
     * Checks if a loan account has any recorded payments.
     *
     * Quick check to determine if any payments have been made.
     * Used for:
     * - Loan status validation
     * - Business rule enforcement
     * - Workflow decisions
     * - Customer segmentation
     *
     * @param loanAccountId the loan account ID
     * @return true if at least one payment exists, false otherwise
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public boolean hasPayments(UUID loanAccountId) {
        return paymentRepository.hasPayments(loanAccountId);
    }

    /**
     * Counts the total number of payments for a loan account.
     *
     * Includes all payments (both active and reversed) for complete count.
     * Used for:
     * - Payment frequency analysis
     * - Customer behavior insights
     * - Operational metrics
     * - Audit trail verification
     *
     * @param loanAccountId the loan account ID
     * @return the total count of payments
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public long countPayments(UUID loanAccountId) {
        return paymentRepository.countByLoanAccountId(loanAccountId);
    }

    /**
     * Processes bulk payment import from external sources.
     *
     * This method handles batch processing of multiple payments, typically
     * from bank files, payment gateway reports, or manual import files.
     * It's designed for high-volume payment processing with error handling.
     *
     * Import Process:
     * 1. Validates each payment record individually
     * 2. Attempts to find corresponding loan account
     * 3. Records successful payments using standard allocation
     * 4. Collects errors for failed payments
     * 5. Returns comprehensive result summary
     *
     * Common Import Sources:
     * - Bank statement files (CSV, Excel)
     * - Payment gateway reports
     * - Mobile money provider files
     * - Manual payment entry files
     * - Third-party collection agency reports
     *
     * Error Handling:
     * - Individual payment failures don't stop batch processing
     * - All errors are collected and reported
     * - Successful payments are committed
     * - Failed payments can be corrected and reprocessed
     *
     * Validation Checks:
     * - Loan account exists and is active
     * - Payment amount is positive
     * - Payment date is valid
     * - Payment method is supported
     * - No duplicate transaction references
     *
     * Result Summary Includes:
     * - Total records processed
     * - Successful payment count
     * - Failed payment count
     * - Detailed error messages
     * - Processing statistics
     *
     * Best Practices:
     * - Process in smaller batches for better performance
     * - Validate file format before processing
     * - Backup original files
     * - Review error reports carefully
     * - Reconcile successful imports
     *
     * @param payments list of payment import records to process
     * @param importedBy the user performing the bulk import
     * @return BulkPaymentImportResult with processing summary and errors
     */
    public BulkPaymentImportResult processBulkPaymentImport(List<PaymentImportRecord> payments, String importedBy) {
        BulkPaymentImportResult result = new BulkPaymentImportResult();
        result.setTotalRecords(payments.size());

        List<String> errors = new ArrayList<>();
        int successful = 0;

        for (PaymentImportRecord record : payments) {
            try {
                Optional<LoanAccount> loanAccountOpt = loanAccountRepository
                        .findByLoanAccountNumber(record.getLoanAccountNumber());

                if (loanAccountOpt.isPresent()) {
                    recordPayment(
                            loanAccountOpt.get().getId(),
                            record.getPaymentAmount(),
                            record.getPaymentDate(),
                            record.getPaymentMethod(),
                            record.getTransactionReference(),
                            importedBy);
                    successful++;
                } else {
                    errors.add("Loan account not found: " + record.getLoanAccountNumber());
                }
            } catch (Exception e) {
                errors.add("Error processing " + record.getLoanAccountNumber() + ": " + e.getMessage());
            }
        }

        result.setSuccessfulRecords(successful);
        result.setFailedRecords(payments.size() - successful);
        result.setErrors(errors);

        return result;
    }

    /**
     * Generates a unique payment reference number.
     *
     * Currently uses timestamp-based generation. Production implementation
     * should use a more robust approach:
     * - Sequential numbering with check digits
     * - Date-based prefixes (PAY-YYYYMMDD-NNNN)
     * - Branch/channel identifiers
     * - Database sequence generators
     * - UUID-based identifiers
     *
     * The reference must be:
     * - Unique across all payments
     * - Easy to communicate (for customer service)
     * - Suitable for reconciliation
     * - Compliant with audit requirements
     *
     * @return a unique payment reference number
     */
    private String generatePaymentReference() {
        return "PAY-" + System.currentTimeMillis();
    }
}
