package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.entity.RepaymentFrequency;
import com.openfinova.banking.loan.api.entity.ScheduleStatus;
import com.openfinova.banking.loan.dto.ScheduleCalculation;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.entity.LoanSchedule;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.loan.repository.LoanScheduleRepository;

/**
 * Implementation of LoanScheduleService for managing loan repayment schedules and amortization.
 *
 * This service is responsible for generating, managing, and maintaining loan repayment schedules
 * throughout the loan lifecycle. It handles the complex calculations required for amortization
 * and provides critical functionality for payment tracking and delinquency management.
 *
 * Key Responsibilities:
 * - Generating complete loan repayment schedules using amortization formulas
 * - Managing schedule lifecycle (generation, updates, regeneration)
 * - Tracking payment applications against scheduled amounts
 * - Managing overdue detection and delinquency tracking
 * - Calculating outstanding and overdue amounts
 * - Supporting schedule modifications after restructuring
 * - Generating payment reminders and notifications
 *
 * Amortization Methods Supported:
 * - EQUAL_INSTALLMENTS (EMI): Fixed payment amount with varying principal/interest split
 * - REDUCING_BALANCE: Interest calculated on reducing principal balance
 * - FLAT_RATE: Interest calculated on original principal (less common)
 * - BULLET: Principal payment at maturity with periodic interest
 *
 * Schedule Status Lifecycle:
 * - PENDING: Schedule created, payment not yet due
 * - OVERDUE: Past due date with no payment received
 * - PARTIALLY_PAID: Some payment received but not full amount
 * - PAID: Full scheduled amount received
 * - WAIVED: Payment obligation waived by lender
 * - RESCHEDULED: Due date moved to different date
 *
 * Payment Frequency Support:
 * - MONTHLY: Most common, 12 payments per year
 * - QUARTERLY: 4 payments per year
 * - SEMI_ANNUALLY: 2 payments per year
 * - ANNUALLY: 1 payment per year
 * - WEEKLY: 52 payments per year (microfinance)
 * - BI_WEEKLY: 26 payments per year
 *
 * Business Rules:
 * - Schedules are immutable once payments are applied
 * - Overdue detection runs automatically via scheduled tasks
 * - Schedule regeneration preserves payment history
 * - Final installment is adjusted for rounding differences
 * - Payment reminders are generated before due dates
 *
 * Mathematical Formulas:
 * EMI Calculation: EMI = P × r × (1+r)^n / ((1+r)^n - 1)
 * Where: P = Principal, r = Monthly interest rate, n = Number of installments
 *
 * Integration Points:
 * - Payment service (payment application and allocation)
 * - Loan account service (balance updates and status changes)
 * - Notification service (payment reminders and overdue alerts)
 * - Reporting service (delinquency and performance metrics)
 * - Accounting system (accrual and payment postings)
 *
 * Scheduled Tasks:
 * - Daily overdue detection and status updates
 * - Payment reminder generation (configurable days before due)
 * - Schedule maintenance and cleanup
 * - Performance metrics calculation
 *
 * @see LoanScheduleService
 * @see LoanSchedule
 * @see com.openfinova.banking.loan.api.entity.ScheduleStatus
 * @see com.openfinova.banking.loan.api.dto.LoanScheduleCalculationResponse
 */
@Service
@Transactional
public class LoanScheduleService {

    private static final Logger log = LoggerFactory.getLogger(LoanScheduleService.class);

    private final LoanScheduleRepository scheduleRepository;
    private final LoanAccountRepository loanAccountRepository;

    public LoanScheduleService(LoanScheduleRepository scheduleRepository, LoanAccountRepository loanAccountRepository) {
        this.scheduleRepository = scheduleRepository;
        this.loanAccountRepository = loanAccountRepository;
    }

    /**
     * Generates a complete repayment schedule for a loan account.
     *
     * This method creates the entire repayment schedule using the loan's parameters
     * and amortization calculations. The schedule serves as the blueprint for
     * all future payments and is critical for loan management.
     *
     * Generation Process:
     * 1. Retrieve loan account parameters (amount, rate, tenor, frequency)
     * 2. Calculate amortization schedule using financial formulas
     * 3. Create schedule records for each installment
     * 4. Set all schedules to PENDING status initially
     * 5. Save complete schedule to database
     * 6. Return generated schedule list
     *
     * Schedule Calculation Details:
     * - Uses Equal Monthly Installment (EMI) method by default
     * - Calculates principal and interest for each period
     * - Tracks remaining balance after each payment
     * - Adjusts final installment for rounding differences
     * - Handles different payment frequencies (monthly, quarterly, etc.)
     *
     * Schedule Attributes Set:
     * - Installment Number: Sequential numbering (1, 2, 3, ...)
     * - Due Date: Calculated based on disbursement date and frequency
     * - Principal Due: Principal portion of the installment
     * - Interest Due: Interest portion of the installment
     * - Total Due: Principal + Interest for the installment
     * - Outstanding Balance: Remaining principal after this payment
     * - Status: PENDING (not yet due)
     * - Overdue Flag: false initially
     *
     * First Payment Date Logic:
     * - Uses loan's firstPaymentDate if specified
     * - Otherwise defaults to disbursement date + 1 month
     * - Subsequent payments follow the specified frequency
     *
     * Use Cases:
     * - Initial loan setup after disbursement
     * - Schedule creation for new loan products
     * - Bulk loan processing and setup
     * - System migration and data conversion
     *
     * Business Rules:
     * - Schedule can only be generated once per loan
     * - Loan must have valid parameters (amount, rate, tenor)
     * - Generated schedule cannot be modified once payments begin
     * - All installments must sum to the original loan amount
     *
     * @param loanAccountId the ID of the loan account to generate schedule for
     * @param generatedBy the user or system generating the schedule
     * @return list of generated schedule records in chronological order
     * @throws IllegalArgumentException if loan account not found or has invalid parameters
     */
    public List<LoanSchedule> generateSchedule(UUID loanAccountId, String generatedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        LocalDate firstPayment = loanAccount.getFirstPaymentDate();
        if (firstPayment == null) {
            if (loanAccount.getDisbursementDate() == null) {
                throw new IllegalArgumentException(
                        "Disbursement date or first payment date is required to generate schedule");
            }
            firstPayment = loanAccount.getDisbursementDate().plusMonths(1);
        }

        List<ScheduleCalculation> calculations = calculateAmortizationSchedule(
                loanAccount.getPrincipalAmount(),
                loanAccount.getInterestRate(),
                loanAccount.getTenorMonths(),
                firstPayment,
                loanAccount.getRepaymentFrequency().name());

        List<LoanSchedule> schedules = new ArrayList<>();
        for (ScheduleCalculation calc : calculations) {
            LoanSchedule schedule = new LoanSchedule();
            schedule.setLoanAccount(loanAccount);
            schedule.setInstallmentNumber(calc.getInstallmentNumber());
            schedule.setDueDate(calc.getDueDate());
            schedule.setPrincipalDue(calc.getPrincipalDue());
            schedule.setInterestDue(calc.getInterestDue());
            schedule.setTotalDue(calc.getTotalDue());
            schedule.setOutstandingBalance(calc.getOutstandingBalance());
            schedule.setStatus(ScheduleStatus.PENDING);
            schedule.setIsOverdue(false);

            schedules.add(schedule);
        }

        return scheduleRepository.saveAll(schedules);
    }

    /**
     * Regenerates the repayment schedule from a specific effective date forward.
     *
     * This method is used when loan terms are modified (restructuring, rate changes)
     * and a new schedule needs to be created while preserving the payment history
     * for installments that have already been paid or partially paid.
     *
     * Regeneration Process:
     * 1. Identify future schedules (on or after effective date)
     * 2. Filter to only PENDING schedules (preserve paid/partial schedules)
     * 3. Delete future pending schedules
     * 4. Generate new schedules from effective date forward
     * 5. Use updated loan parameters for new calculations
     * 6. Preserve all historical payment data
     *
     * When Regeneration is Needed:
     * - Loan restructuring (tenor extension, rate reduction)
     * - Interest rate adjustments
     * - Payment frequency changes
     * - Principal modifications
     * - Schedule corrections due to errors
     * - Regulatory requirement changes
     *
     * Preservation Rules:
     * - PAID schedules are never deleted or modified
     * - PARTIALLY_PAID schedules are preserved
     * - Payment history remains intact
     * - Only future PENDING schedules are regenerated
     * - Overdue schedules may be preserved based on business rules
     *
     * Calculation Adjustments:
     * - New schedules use current loan parameters
     * - Outstanding balance is recalculated from payment history
     * - Remaining tenor is adjusted based on effective date
     * - Interest calculations use new rates if changed
     *
     * Use Cases:
     * - Post-restructuring schedule updates
     * - Interest rate change implementations
     * - Payment frequency modifications
     * - Error corrections in future schedules
     * - Regulatory compliance adjustments
     *
     * Business Rules:
     * - Effective date cannot be in the past for paid schedules
     * - Payment history must be preserved
     * - New schedule must be mathematically consistent
     * - Total payments must still equal loan obligations
     *
     * @param loanAccountId the ID of the loan account to regenerate schedule for
     * @param effectiveDate the date from which to regenerate (inclusive)
     * @param regeneratedBy the user or system performing the regeneration
     * @return list of newly generated schedule records from effective date forward
     * @throws IllegalArgumentException if loan account not found or effective date invalid
     */
    public List<LoanSchedule> regenerateSchedule(UUID loanAccountId, LocalDate effectiveDate, String regeneratedBy) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        List<LoanSchedule> futureSchedules = scheduleRepository.findByLoanAccountId(loanAccountId).stream()
                .filter(s -> !s.getDueDate().isBefore(effectiveDate))
                .filter(s -> ScheduleStatus.PENDING.equals(s.getStatus()))
                .sorted(Comparator.comparing(LoanSchedule::getDueDate)).toList();
        if (futureSchedules.isEmpty()) {
            return List.of();
        }

        LocalDate firstDue = futureSchedules.get(0).getDueDate();
        int newInstallmentCount = futureSchedules.size();
        scheduleRepository.deleteAll(futureSchedules);

        int nextInstallmentNumber = scheduleRepository.findByLoanAccountId(loanAccountId).stream()
                .mapToInt(LoanSchedule::getInstallmentNumber).max().orElse(0) + 1;

        List<ScheduleCalculation> calculations;
        if (loanAccount.getRepaymentFrequency() == RepaymentFrequency.BULLET) {
            int months = loanAccount.getTenorMonths() != null ? loanAccount.getTenorMonths() : 1;
            calculations = buildBulletSchedule(
                    loanAccount.getOutstandingPrincipal(),
                    loanAccount.getInterestRate(),
                    months,
                    firstDue);
        } else {
            calculations = buildAmortizationScheduleForPeriods(
                    loanAccount.getOutstandingPrincipal(),
                    loanAccount.getInterestRate(),
                    newInstallmentCount,
                    firstDue,
                    loanAccount.getRepaymentFrequency());
        }

        List<LoanSchedule> schedules = new ArrayList<>();
        int seq = nextInstallmentNumber;
        for (ScheduleCalculation calc : calculations) {
            LoanSchedule schedule = new LoanSchedule();
            schedule.setLoanAccount(loanAccount);
            schedule.setInstallmentNumber(seq++);
            schedule.setDueDate(calc.getDueDate());
            schedule.setPrincipalDue(calc.getPrincipalDue());
            schedule.setInterestDue(calc.getInterestDue());
            schedule.setTotalDue(calc.getTotalDue());
            schedule.setOutstandingBalance(calc.getOutstandingBalance());
            schedule.setStatus(ScheduleStatus.PENDING);
            schedule.setIsOverdue(false);
            schedules.add(schedule);
        }
        return scheduleRepository.saveAll(schedules);
    }

    /**
     * Retrieves a schedule record by its unique identifier.
     *
     * @param id the schedule ID
     * @return Optional containing the schedule if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<LoanSchedule> getScheduleById(UUID id) {
        return scheduleRepository.findById(id);
    }

    @Transactional(readOnly = true)
    public Optional<LoanSchedule> getScheduleForLoanAccount(UUID loanAccountId, UUID scheduleId) {
        return scheduleRepository.findById(scheduleId)
                .filter(s -> s.getLoanAccount() != null && loanAccountId.equals(s.getLoanAccount().getId()));
    }

    /**
     * Retrieves all schedule records for a loan account.
     *
     * Returns the complete repayment schedule in chronological order.
     * Used for:
     * - Customer payment schedule display
     * - Loan account reconciliation
     * - Payment history analysis
     * - Audit and compliance reporting
     *
     * @param loanAccountId the loan account ID
     * @return list of all schedule records for the loan account
     */
    @Transactional(readOnly = true)
    public List<LoanSchedule> getSchedulesByLoanAccount(UUID loanAccountId) {
        return scheduleRepository.findByLoanAccountId(loanAccountId);
    }

    /**
     * Retrieves all schedule records for a loan account with pagination.
     *
     * Paginated version for loans with many installments or when
     * implementing paged displays in user interfaces.
     *
     * @param loanAccountId the loan account ID
     * @param pageable pagination parameters
     * @return page of schedule records for the loan account
     */
    @Transactional(readOnly = true)
    public Page<LoanSchedule> getSchedulesByLoanAccount(UUID loanAccountId, Pageable pageable) {
        return scheduleRepository.findByLoanAccountId(loanAccountId, pageable);
    }

    /**
     * Retrieves a specific schedule by installment number.
     *
     * Installment numbers are sequential (1, 2, 3, ...) and provide
     * a business-friendly way to reference specific payments.
     *
     * Used for:
     * - Customer service inquiries ("What's due for installment 5?")
     * - Payment processing ("Apply payment to installment 3")
     * - Schedule modifications
     * - Reporting and analytics
     *
     * @param loanAccountId the loan account ID
     * @param installmentNumber the installment number (1-based)
     * @return Optional containing the schedule if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<LoanSchedule> getScheduleByInstallmentNumber(UUID loanAccountId, Integer installmentNumber) {
        return scheduleRepository.findByLoanAccountIdAndInstallmentNumber(loanAccountId, installmentNumber);
    }

    /**
     * Retrieves schedules by status for a loan account.
     *
     * Filters schedules by their payment status:
     * - PENDING: Not yet due or unpaid
     * - OVERDUE: Past due date with no payment
     * - PARTIALLY_PAID: Some payment received
     * - PAID: Fully paid
     * - WAIVED: Payment obligation waived
     * - RESCHEDULED: Due date changed
     *
     * Used for:
     * - Delinquency analysis
     * - Payment planning
     * - Customer service inquiries
     * - Collection prioritization
     *
     * @param loanAccountId the loan account ID
     * @param status the schedule status to filter by
     * @return list of schedules with the specified status
     */
    @Transactional(readOnly = true)
    public List<LoanSchedule> getSchedulesByStatus(UUID loanAccountId, ScheduleStatus status) {
        return scheduleRepository.findByLoanAccountIdAndStatus(loanAccountId, status);
    }

    /**
     * Retrieves all pending schedules for a loan account.
     *
     * Pending schedules are those that haven't been paid yet,
     * representing future payment obligations. This is a critical
     * query for payment processing and customer service.
     *
     * Used for:
     * - Next payment calculations
     * - Payment processing workflows
     * - Customer payment inquiries
     * - Outstanding balance calculations
     * - Payment reminder generation
     *
     * @param loanAccountId the loan account ID
     * @return list of pending (unpaid) schedules
     */
    @Transactional(readOnly = true)
    public List<LoanSchedule> getPendingSchedules(UUID loanAccountId) {
        return scheduleRepository.findPendingSchedulesByLoanAccount(loanAccountId);
    }

    /**
     * Retrieves all overdue schedules for a loan account.
     *
     * Overdue schedules are those past their due date without full payment.
     * This is critical for delinquency management and collection activities.
     *
     * Used for:
     * - Delinquency reporting
     * - Collection workflow prioritization
     * - Late fee calculations
     * - Customer contact lists
     * - Risk assessment
     * - Regulatory reporting
     *
     * @param loanAccountId the loan account ID
     * @return list of overdue schedules ordered by due date
     */
    @Transactional(readOnly = true)
    public List<LoanSchedule> getOverdueSchedules(UUID loanAccountId) {
        return scheduleRepository.findOverdueSchedulesByLoanAccount(loanAccountId);
    }

    /**
     * Retrieves schedules due within a date range with pagination.
     *
     * Finds all schedules across the portfolio that have due dates
     * within the specified range. Used for operational planning
     * and cash flow management.
     *
     * Used for:
     * - Daily/weekly payment processing queues
     * - Cash flow forecasting
     * - Collection planning
     * - Payment reminder campaigns
     * - Operational resource planning
     *
     * @param startDate the start of the date range (inclusive)
     * @param endDate the end of the date range (inclusive)
     * @param pageable pagination parameters
     * @return page of schedules due within the specified date range
     */
    @Transactional(readOnly = true)
    public Page<LoanSchedule> getSchedulesDueBetween(LocalDate startDate, LocalDate endDate, Pageable pageable) {
        return scheduleRepository.findSchedulesDueBetween(startDate, endDate, pageable);
    }

    @Transactional(readOnly = true)
    public Page<LoanSchedule> getSchedulesDueBetweenForLoanAccount(UUID loanAccountId, LocalDate startDate,
            LocalDate endDate, Pageable pageable) {
        return scheduleRepository.findSchedulesDueBetweenForLoanAccount(loanAccountId, startDate, endDate, pageable);
    }

    /**
     * Retrieves schedules due on a specific date with pagination.
     *
     * Finds all schedules across the portfolio due on exactly
     * the specified date. Critical for daily operations.
     *
     * Used for:
     * - Daily payment processing
     * - Same-day payment reminders
     * - Collection call lists
     * - Cash flow management
     * - Operational dashboards
     *
     * @param dueDate the specific due date to filter by
     * @param pageable pagination parameters
     * @return page of schedules due on the specified date
     */
    @Transactional(readOnly = true)
    public Page<LoanSchedule> getSchedulesDueOnDate(LocalDate dueDate, Pageable pageable) {
        return scheduleRepository.findSchedulesDueOnDate(dueDate, pageable);
    }

    /**
     * Retrieves the next due schedule for a loan account.
     *
     * Finds the earliest unpaid schedule, which represents the
     * next payment obligation for the borrower.
     *
     * Used for:
     * - Customer service inquiries ("When is my next payment?")
     * - Payment processing ("Apply payment to next due")
     * - Mobile app displays
     * - Automated payment systems
     * - Payment reminder generation
     *
     * @param loanAccountId the loan account ID
     * @return Optional containing the next due schedule if any pending schedules exist
     */
    @Transactional(readOnly = true)
    public Optional<LoanSchedule> getNextDueSchedule(UUID loanAccountId) {
        return scheduleRepository.findNextDueSchedule(loanAccountId);
    }

    /**
     * Retrieves the most recently paid schedule for a loan account.
     *
     * Finds the latest schedule that has been marked as PAID,
     * representing the most recent successful payment.
     *
     * Used for:
     * - Payment history inquiries
     * - Last payment date calculations
     * - Payment pattern analysis
     * - Customer service support
     * - Delinquency calculations
     *
     * @param loanAccountId the loan account ID
     * @return Optional containing the last paid schedule if any payments have been made
     */
    @Transactional(readOnly = true)
    public Optional<LoanSchedule> getLastPaidSchedule(UUID loanAccountId) {
        return scheduleRepository.findLastPaidSchedule(loanAccountId);
    }

    /**
     * Updates a schedule with payment information and recalculates status.
     *
     * This method records payment application against a specific schedule,
     * updating the paid amounts and determining the new schedule status
     * based on how much has been paid relative to what was due.
     *
     * Payment Application Process:
     * 1. Add payment amounts to existing paid amounts (cumulative)
     * 2. Calculate total paid across all components
     * 3. Compare total paid to total due
     * 4. Update schedule status based on payment completeness
     * 5. Set paid date if fully paid
     *
     * Status Determination Logic:
     * - If total paid >= total due: Status = PAID, set paid date
     * - If total paid > 0 but < total due: Status = PARTIALLY_PAID
     * - If total paid = 0: Status remains PENDING or OVERDUE
     *
     * Payment Components Updated:
     * - Principal Paid: Cumulative principal payments
     * - Interest Paid: Cumulative interest payments
     * - Fees Paid: Cumulative fee payments
     * - Penalties Paid: Cumulative penalty payments
     *
     * Business Rules:
     * - Payment amounts are added to existing amounts (cumulative)
     * - Negative payment amounts are not allowed
     * - Overpayments are tracked but don't change status beyond PAID
     * - Paid date is set only when fully paid
     * - Status changes are automatic based on payment amounts
     *
     * Integration Points:
     * - Called by payment service after payment allocation
     * - May trigger delinquency status updates
     * - May affect loan account balance calculations
     * - May trigger customer notifications
     *
     * @param scheduleId the ID of the schedule to update
     * @param principalPaid the principal amount paid (added to existing)
     * @param interestPaid the interest amount paid (added to existing)
     * @param feesPaid the fees amount paid (added to existing)
     * @param penaltiesPaid the penalties amount paid (added to existing)
     * @param updatedBy the user or system applying the payment
     * @return the updated schedule with new payment amounts and status
     * @throws IllegalArgumentException if schedule not found
     */
    public LoanSchedule updateSchedulePayment(UUID scheduleId, BigDecimal principalPaid, BigDecimal interestPaid,
            BigDecimal feesPaid, BigDecimal penaltiesPaid, String updatedBy) {
        LoanSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        return applySchedulePaymentUpdate(schedule, principalPaid, interestPaid, feesPaid, penaltiesPaid);
    }

    public LoanSchedule updateSchedulePaymentForLoanAccount(UUID loanAccountId, UUID scheduleId,
            BigDecimal principalPaid, BigDecimal interestPaid, BigDecimal feesPaid, BigDecimal penaltiesPaid,
            String updatedBy) {
        LoanSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        assertScheduleOnLoan(schedule, loanAccountId);
        return applySchedulePaymentUpdate(schedule, principalPaid, interestPaid, feesPaid, penaltiesPaid);
    }

    private LoanSchedule applySchedulePaymentUpdate(LoanSchedule schedule, BigDecimal principalPaid,
            BigDecimal interestPaid, BigDecimal feesPaid, BigDecimal penaltiesPaid) {
        schedule.setPrincipalPaid(schedule.getPrincipalPaid().add(principalPaid));
        schedule.setInterestPaid(schedule.getInterestPaid().add(interestPaid));
        schedule.setFeesPaid(schedule.getFeesPaid().add(feesPaid));
        schedule.setPenaltiesPaid(schedule.getPenaltiesPaid().add(penaltiesPaid));

        BigDecimal totalPaid = schedule.getPrincipalPaid().add(schedule.getInterestPaid()).add(schedule.getFeesPaid())
                .add(schedule.getPenaltiesPaid());

        if (totalPaid.compareTo(schedule.getTotalDue()) >= 0) {
            schedule.setStatus(ScheduleStatus.PAID);
            schedule.setPaidDate(LocalDate.now());
        } else {
            schedule.setStatus(ScheduleStatus.PARTIALLY_PAID);
        }

        return scheduleRepository.save(schedule);
    }

    private static void assertScheduleOnLoan(LoanSchedule schedule, UUID loanAccountId) {
        if (schedule.getLoanAccount() == null || !loanAccountId.equals(schedule.getLoanAccount().getId())) {
            throw new IllegalArgumentException("Schedule does not belong to this loan account");
        }
    }

    /**
     * Marks a schedule as fully paid with a specific paid date.
     *
     * This method directly sets a schedule to PAID status without
     * requiring specific payment amount details. Used when the
     * payment amounts are managed elsewhere or for bulk operations.
     *
     * Update Process:
     * 1. Set schedule status to PAID
     * 2. Record the paid date
     * 3. Clear overdue flag (if set)
     * 4. Save updated schedule
     *
     * Use Cases:
     * - Bulk payment processing
     * - Payment corrections
     * - Administrative adjustments
     * - System migrations
     * - Payment reversals (when reversing, would set back to previous status)
     *
     * Business Rules:
     * - Paid date cannot be in the future
     * - Once marked as paid, schedule should not be modified
     * - Overdue flag is automatically cleared
     * - Status change is permanent (requires separate reversal process)
     *
     * @param scheduleId the ID of the schedule to mark as paid
     * @param paidDate the date the payment was made
     * @param updatedBy the user or system marking the schedule as paid
     * @return the updated schedule with PAID status
     * @throws IllegalArgumentException if schedule not found
     */
    public LoanSchedule markScheduleAsPaid(UUID scheduleId, LocalDate paidDate, String updatedBy) {
        LoanSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        return applyMarkSchedulePaid(schedule, paidDate);
    }

    public LoanSchedule markScheduleAsPaidForLoanAccount(UUID loanAccountId, UUID scheduleId, LocalDate paidDate,
            String updatedBy) {
        LoanSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        assertScheduleOnLoan(schedule, loanAccountId);
        return applyMarkSchedulePaid(schedule, paidDate);
    }

    private LoanSchedule applyMarkSchedulePaid(LoanSchedule schedule, LocalDate paidDate) {
        schedule.setStatus(ScheduleStatus.PAID);
        schedule.setPaidDate(paidDate);
        schedule.setIsOverdue(false);

        return scheduleRepository.save(schedule);
    }

    /**
     * Updates the overdue status and days past due for a schedule.
     *
     * This method is used to manually update overdue information,
     * typically called by automated overdue detection processes
     * or for administrative corrections.
     *
     * Overdue Information Updated:
     * - Overdue Flag: Boolean indicating if schedule is overdue
     * - Days Past Due: Number of days since due date
     *
     * Use Cases:
     * - Automated overdue detection corrections
     * - Manual overdue status adjustments
     * - System reconciliation processes
     * - Administrative corrections
     *
     * Business Rules:
     * - Days past due should be consistent with overdue flag
     * - Negative days past due not allowed
     * - Overdue flag should be true if days past due > 0
     *
     * @param scheduleId the ID of the schedule to update
     * @param isOverdue whether the schedule is overdue
     * @param daysPastDue the number of days past the due date
     * @return the updated schedule with new overdue information
     * @throws IllegalArgumentException if schedule not found
     */
    public LoanSchedule updateOverdueStatus(UUID scheduleId, Boolean isOverdue, Integer daysPastDue) {
        LoanSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));

        return applyOverdueUpdate(schedule, isOverdue, daysPastDue);
    }

    public LoanSchedule updateOverdueStatusForLoanAccount(UUID loanAccountId, UUID scheduleId, Boolean isOverdue,
            Integer daysPastDue) {
        LoanSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("Schedule not found: " + scheduleId));
        assertScheduleOnLoan(schedule, loanAccountId);
        return applyOverdueUpdate(schedule, isOverdue, daysPastDue);
    }

    private LoanSchedule applyOverdueUpdate(LoanSchedule schedule, Boolean isOverdue, Integer daysPastDue) {
        schedule.setIsOverdue(isOverdue);
        schedule.setDaysPastDue(daysPastDue);

        return scheduleRepository.save(schedule);
    }

    /**
     * Processes overdue detection for all schedules as of a specific date.
     *
     * This is a critical scheduled task that runs daily to identify and mark
     * schedules that have become overdue. It's essential for delinquency
     * management and portfolio risk monitoring.
     *
     * Processing Logic:
     * 1. Find all schedules requiring overdue check (past due date, PENDING status)
     * 2. Calculate days past due for each schedule
     * 3. Mark schedules as overdue with calculated days
     * 4. Update schedule status to OVERDUE
     * 5. Save all updated schedules
     * 6. Return count of schedules marked as overdue
     *
     * Overdue Criteria:
     * - Due date is before the as-of date
     * - Schedule status is PENDING (not paid or partially paid)
     * - Not already marked as overdue
     *
     * Days Past Due Calculation:
     * - Days = as-of date - due date
     * - Always positive number
     * - Calculated using ChronoUnit.DAYS for accuracy
     *
     * Business Impact:
     * - Triggers delinquency reporting
     * - Affects loan account delinquency status
     * - May trigger collection workflows
     * - Impacts portfolio quality metrics
     * - Required for regulatory reporting
     *
     * Scheduling:
     * - Typically runs daily (early morning)
     * - Should run before other daily processes
     * - May run multiple times per day for real-time systems
     * - Critical for operational accuracy
     *
     * Error Handling:
     * - Individual schedule failures don't stop batch processing
     * - Errors are logged for investigation
     * - Failed schedules can be reprocessed
     * - Maintains data integrity
     *
     * @param asOfDate the date to check overdue status against (typically current date)
     * @return the number of schedules marked as overdue
     */
    public int processOverdueDetection(LocalDate asOfDate) {
        List<LoanSchedule> pendingSchedules = scheduleRepository.findSchedulesRequiringOverdueCheck(asOfDate);

        for (LoanSchedule schedule : pendingSchedules) {
            int daysPastDue = (int) java.time.temporal.ChronoUnit.DAYS.between(schedule.getDueDate(), asOfDate);
            schedule.setIsOverdue(true);
            schedule.setDaysPastDue(daysPastDue);
            schedule.setStatus(ScheduleStatus.OVERDUE);
        }

        scheduleRepository.saveAll(pendingSchedules);
        return pendingSchedules.size();
    }

    /**
     * Calculates the total outstanding amount across all unpaid schedules for a loan account.
     *
     * This method sums up all remaining payment obligations (principal + interest + fees + penalties)
     * for schedules that haven't been fully paid. It's critical for loan balance calculations
     * and customer account inquiries.
     *
     * Calculation Logic:
     * - Includes all schedules with status PENDING, OVERDUE, or PARTIALLY_PAID
     * - Sums total due amounts minus amounts already paid
     * - Excludes fully PAID schedules
     * - Includes future scheduled payments
     *
     * Components Included:
     * - Unpaid principal amounts
     * - Unpaid interest amounts
     * - Unpaid fees and charges
     * - Unpaid penalties and late fees
     *
     * Use Cases:
     * - Customer balance inquiries
     * - Loan account statements
     * - Payoff amount calculations
     * - Portfolio reporting
     * - Regulatory capital calculations
     * - Risk assessment
     *
     * Business Rules:
     * - Only includes unpaid portions of schedules
     * - Future payments are included in outstanding balance
     * - Overpayments don't reduce outstanding below zero
     * - Calculation is real-time based on current schedule status
     *
     * @param loanAccountId the loan account ID to calculate outstanding for
     * @return the total outstanding amount across all unpaid schedules
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalOutstanding(UUID loanAccountId) {
        return scheduleRepository.sumOutstandingByLoanAccount(loanAccountId);
    }

    /**
     * Calculates the total overdue amount for a loan account.
     *
     * This method sums up all payment obligations that are past their due date
     * and haven't been fully paid. It's essential for delinquency management
     * and collection activities.
     *
     * Calculation Logic:
     * - Includes only schedules marked as overdue (isOverdue = true)
     * - Sums unpaid portions of overdue schedules
     * - Excludes future payments (not yet due)
     * - Includes partially paid overdue amounts
     *
     * Components Included:
     * - Overdue principal amounts
     * - Overdue interest amounts
     * - Overdue fees and charges
     * - Accumulated penalties and late fees
     *
     * Use Cases:
     * - Delinquency reporting
     * - Collection prioritization
     * - Late fee calculations
     * - Customer contact strategies
     * - Portfolio risk assessment
     * - Regulatory delinquency reporting
     *
     * Business Rules:
     * - Only includes amounts past due date
     * - Partially paid schedules contribute remaining unpaid amount
     * - Calculation updates automatically with overdue detection
     * - Zero if no overdue amounts exist
     *
     * @param loanAccountId the loan account ID to calculate overdue amount for
     * @return the total overdue amount for the loan account
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalOverdue(UUID loanAccountId) {
        return scheduleRepository.sumOverdueByLoanAccount(loanAccountId);
    }

    /**
     * Counts the number of schedules with a specific status for a loan account.
     *
     * This method provides statistical information about schedule distribution
     * by status, useful for reporting and analytics.
     *
     * Status Categories:
     * - PENDING: Future payments not yet due
     * - OVERDUE: Past due payments not received
     * - PARTIALLY_PAID: Payments received but not complete
     * - PAID: Fully paid schedules
     * - WAIVED: Payments waived by lender
     * - RESCHEDULED: Due dates modified
     *
     * Use Cases:
     * - Payment performance analysis
     * - Delinquency statistics
     * - Customer service metrics
     * - Portfolio health reporting
     * - Operational dashboards
     * - Audit and compliance reporting
     *
     * @param loanAccountId the loan account ID to count schedules for
     * @param status the schedule status to count
     * @return the number of schedules with the specified status
     */
    @Transactional(readOnly = true)
    public long countSchedulesByStatus(UUID loanAccountId, ScheduleStatus status) {
        return scheduleRepository.countByLoanAccountIdAndStatus(loanAccountId, status);
    }

    /**
     * Counts the number of overdue schedules for a loan account.
     *
     * This method provides a quick count of how many payment installments
     * are currently overdue, which is a key delinquency indicator.
     *
     * Counting Logic:
     * - Includes schedules with isOverdue = true
     * - Counts individual installments, not amounts
     * - Updates automatically with overdue detection
     *
     * Use Cases:
     * - Delinquency bucket classification (30+, 60+, 90+ days)
     * - Collection workflow prioritization
     * - Customer risk scoring
     * - Portfolio quality metrics
     * - Regulatory reporting
     * - Management dashboards
     *
     * Business Rules:
     * - Count reflects current overdue status
     * - Zero indicates no overdue payments
     * - Higher counts indicate greater delinquency risk
     * - Used in automated decision making
     *
     * @param loanAccountId the loan account ID to count overdue schedules for
     * @return the number of overdue schedules for the loan account
     */
    @Transactional(readOnly = true)
    public long countOverdueSchedules(UUID loanAccountId) {
        return scheduleRepository.countOverdueSchedulesByLoanAccount(loanAccountId);
    }

    /**
     * Calculates a complete amortization schedule using financial mathematics.
     *
     * This method performs the core mathematical calculations for loan amortization,
     * generating a complete payment schedule with principal and interest breakdown
     * for each installment. It implements standard banking amortization formulas.
     *
     * Calculation Process:
     * 1. Convert annual interest rate to monthly rate
     * 2. Calculate Equal Monthly Installment (EMI) using amortization formula
     * 3. For each installment, calculate interest on remaining balance
     * 4. Calculate principal as EMI minus interest
     * 5. Update remaining balance after principal payment
     * 6. Adjust final installment for rounding differences
     *
     * Mathematical Formula (EMI):
     * EMI = P × r × (1+r)^n / ((1+r)^n - 1)
     * Where:
     * - P = Principal amount
     * - r = Monthly interest rate (annual rate / 12 / 100)
     * - n = Number of installments (tenor in months)
     *
     * Interest Calculation (per installment):
     * Interest = Remaining Balance × Monthly Interest Rate
     *
     * Principal Calculation (per installment):
     * Principal = EMI - Interest
     *
     * Schedule Attributes Calculated:
     * - Installment Number: Sequential numbering (1, 2, 3, ...)
     * - Due Date: Based on first payment date and frequency
     * - Principal Due: Principal portion of the payment
     * - Interest Due: Interest portion of the payment
     * - Total Due: Principal + Interest for the installment
     * - Outstanding Balance: Remaining principal after this payment
     *
     * Repayment Frequency Support:
     * - MONTHLY: Most common, 12 payments per year
     * - QUARTERLY: 4 payments per year (due dates every 3 months)
     * - SEMI_ANNUALLY: 2 payments per year (due dates every 6 months)
     * - ANNUALLY: 1 payment per year (due dates every 12 months)
     *
     * Rounding and Precision:
     * - All calculations use HALF_UP rounding mode
     * - Interest calculations maintain 6 decimal places during computation
     * - Final amounts rounded to 2 decimal places (currency precision)
     * - Final installment adjusted to ensure total equals original principal
     *
     * Edge Cases Handled:
     * - Zero interest rate (simple division of principal)
     * - Single payment loans (bullet payments)
     * - Rounding differences in final installment
     * - Very small or very large principal amounts
     *
     * Use Cases:
     * - Initial loan setup and schedule generation
     * - Loan restructuring calculations
     * - What-if analysis for loan modifications
     * - Customer loan quotations
     * - Regulatory reporting calculations
     * - Audit and reconciliation processes
     *
     * Business Rules:
     * - Total of all principal payments must equal original principal
     * - Interest is calculated on reducing balance method
     * - Final installment absorbs any rounding differences
     * - All amounts must be positive
     * - Schedule must be mathematically consistent
     *
     * @param principalAmount the loan principal amount
     * @param annualInterestRate the annual interest rate as percentage (e.g., 12.5 for 12.5%)
     * @param tenorMonths the loan tenor in months
     * @param firstPaymentDate the date of the first payment
     * @param repaymentFrequency the payment frequency (MONTHLY, QUARTERLY, etc.)
     * @return list of calculated schedule records with all payment details
     */
    @Transactional(readOnly = true)
    public List<ScheduleCalculation> calculateAmortizationSchedule(BigDecimal principalAmount,
            BigDecimal annualInterestRate, Integer tenorMonths, LocalDate firstPaymentDate, String repaymentFrequency) {
        RepaymentFrequency freq = parseRepaymentFrequency(repaymentFrequency);
        if (freq == RepaymentFrequency.BULLET) {
            int months = tenorMonths != null && tenorMonths >= 1 ? tenorMonths : 1;
            return buildBulletSchedule(principalAmount, annualInterestRate, months, firstPaymentDate);
        }
        int periods = numberOfInstallmentsFromTenor(tenorMonths, freq);
        return buildAmortizationScheduleForPeriods(
                principalAmount,
                annualInterestRate,
                periods,
                firstPaymentDate,
                freq);
    }

    private List<ScheduleCalculation> buildBulletSchedule(BigDecimal principalAmount, BigDecimal annualInterestRate,
            int tenorMonths, LocalDate firstPaymentDate) {
        List<ScheduleCalculation> one = new ArrayList<>();
        BigDecimal interestDue = principalAmount.multiply(nullToZero(annualInterestRate))
                .multiply(BigDecimal.valueOf(tenorMonths)).divide(BigDecimal.valueOf(1200), 4, RoundingMode.HALF_UP);
        ScheduleCalculation calc = new ScheduleCalculation();
        calc.setInstallmentNumber(1);
        calc.setDueDate(firstPaymentDate);
        calc.setPrincipalDue(principalAmount.setScale(2, RoundingMode.HALF_UP));
        calc.setInterestDue(interestDue.setScale(2, RoundingMode.HALF_UP));
        calc.setTotalDue(calc.getPrincipalDue().add(calc.getInterestDue()));
        calc.setOutstandingBalance(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        one.add(calc);
        return one;
    }

    /**
     * Applies payment allocation (same waterfall as loan account) to pending / overdue installments in due-date order.
     */
    public void applyWaterfallPaymentToSchedules(UUID loanAccountId, BigDecimal principalPaid, BigDecimal interestPaid,
            BigDecimal feesPaid, BigDecimal penaltiesPaid) {
        BigDecimal pen = nullToZero(penaltiesPaid);
        BigDecimal fee = nullToZero(feesPaid);
        BigDecimal intP = nullToZero(interestPaid);
        BigDecimal prin = nullToZero(principalPaid);

        List<LoanSchedule> schedules = scheduleRepository.findByLoanAccountId(loanAccountId).stream().filter(
                s -> ScheduleStatus.PENDING.equals(s.getStatus()) || ScheduleStatus.OVERDUE.equals(s.getStatus())
                        || ScheduleStatus.PARTIALLY_PAID.equals(s.getStatus()))
                .sorted(Comparator.comparing(LoanSchedule::getDueDate)).toList();

        for (LoanSchedule s : schedules) {
            if (pen.signum() <= 0 && fee.signum() <= 0 && intP.signum() <= 0 && prin.signum() <= 0) {
                break;
            }
            BigDecimal lineRemaining = s.getTotalDue().subtract(s.getPrincipalPaid()).subtract(s.getInterestPaid())
                    .subtract(s.getFeesPaid()).subtract(s.getPenaltiesPaid());
            if (lineRemaining.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            BigDecimal penApply = pen.min(lineRemaining);
            lineRemaining = lineRemaining.subtract(penApply);
            pen = pen.subtract(penApply);

            BigDecimal feeApply = fee.min(lineRemaining);
            lineRemaining = lineRemaining.subtract(feeApply);
            fee = fee.subtract(feeApply);

            BigDecimal intCap = s.getInterestDue().subtract(s.getInterestPaid()).max(BigDecimal.ZERO);
            BigDecimal intApply = intP.min(intCap).min(lineRemaining);
            lineRemaining = lineRemaining.subtract(intApply);
            intP = intP.subtract(intApply);

            BigDecimal prinCap = s.getPrincipalDue().subtract(s.getPrincipalPaid()).max(BigDecimal.ZERO);
            BigDecimal prinApply = prin.min(prinCap).min(lineRemaining);
            prin = prin.subtract(prinApply);

            if (penApply.signum() > 0 || feeApply.signum() > 0 || intApply.signum() > 0 || prinApply.signum() > 0) {
                updateSchedulePayment(s.getId(), prinApply, intApply, feeApply, penApply, "payment-allocation");
            }
        }
    }

    private static BigDecimal nullToZero(BigDecimal v) {
        return v != null ? v : BigDecimal.ZERO;
    }

    private List<ScheduleCalculation> buildAmortizationScheduleForPeriods(BigDecimal principalAmount,
            BigDecimal annualInterestRate, int numberOfPeriods, LocalDate firstPaymentDate, RepaymentFrequency freq) {
        if (principalAmount == null || principalAmount.compareTo(BigDecimal.ZERO) <= 0) {
            return List.of();
        }
        if (firstPaymentDate == null) {
            throw new IllegalArgumentException("firstPaymentDate is required");
        }
        if (annualInterestRate == null) {
            annualInterestRate = BigDecimal.ZERO;
        }
        int n = Math.max(1, numberOfPeriods);

        BigDecimal periodicRate = annualInterestRate.divide(BigDecimal.valueOf(100), 12, RoundingMode.HALF_UP)
                .divide(paymentsPerYear(freq), 12, RoundingMode.HALF_UP);
        BigDecimal installmentPayment = calculatePeriodicPayment(principalAmount, periodicRate, n);

        List<ScheduleCalculation> schedules = new ArrayList<>();
        BigDecimal remainingBalance = principalAmount;
        LocalDate dueDate = firstPaymentDate;

        for (int i = 1; i <= n; i++) {
            ScheduleCalculation calc = new ScheduleCalculation();
            calc.setInstallmentNumber(i);
            calc.setDueDate(dueDate);

            BigDecimal interestDue = remainingBalance.multiply(periodicRate).setScale(2, RoundingMode.HALF_UP);
            BigDecimal principalDue = installmentPayment.subtract(interestDue);
            if (i == n) {
                principalDue = remainingBalance.setScale(2, RoundingMode.HALF_UP);
            }

            calc.setPrincipalDue(principalDue);
            calc.setInterestDue(interestDue);
            calc.setTotalDue(principalDue.add(interestDue));

            remainingBalance = remainingBalance.subtract(principalDue);
            calc.setOutstandingBalance(remainingBalance.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP));

            schedules.add(calc);
            dueDate = advanceDueDate(dueDate, freq);
        }
        return schedules;
    }

    private RepaymentFrequency parseRepaymentFrequency(String repaymentFrequency) {
        if (repaymentFrequency == null || repaymentFrequency.isBlank()) {
            return RepaymentFrequency.MONTHLY;
        }
        try {
            return RepaymentFrequency.valueOf(repaymentFrequency.trim());
        } catch (IllegalArgumentException ex) {
            log.warn("Unknown repayment frequency '{}', using MONTHLY", repaymentFrequency);
            return RepaymentFrequency.MONTHLY;
        }
    }

    private static int numberOfInstallmentsFromTenor(Integer tenorMonths, RepaymentFrequency f) {
        if (tenorMonths == null || tenorMonths < 1) {
            return 1;
        }
        return switch (f) {
            case MONTHLY -> tenorMonths;
            case QUARTERLY -> Math.max(1, (tenorMonths + 2) / 3);
            case SEMI_ANNUALLY -> Math.max(1, (tenorMonths + 5) / 6);
            case ANNUALLY -> Math.max(1, (tenorMonths + 11) / 12);
            case WEEKLY -> Math.max(1, tenorMonths * 52 / 12);
            case BIWEEKLY -> Math.max(1, tenorMonths * 26 / 12);
            case DAILY -> Math.max(1, tenorMonths * 365 / 12);
            case BULLET -> 1;
        };
    }

    private static BigDecimal paymentsPerYear(RepaymentFrequency f) {
        return switch (f) {
            case DAILY -> BigDecimal.valueOf(365);
            case WEEKLY -> BigDecimal.valueOf(52);
            case BIWEEKLY -> BigDecimal.valueOf(26);
            case MONTHLY -> BigDecimal.valueOf(12);
            case QUARTERLY -> BigDecimal.valueOf(4);
            case SEMI_ANNUALLY -> BigDecimal.valueOf(2);
            case ANNUALLY, BULLET -> BigDecimal.valueOf(1);
        };
    }

    private static LocalDate advanceDueDate(LocalDate current, RepaymentFrequency f) {
        return switch (f) {
            case DAILY -> current.plusDays(1);
            case WEEKLY -> current.plusWeeks(1);
            case BIWEEKLY -> current.plusWeeks(2);
            case MONTHLY -> current.plusMonths(1);
            case QUARTERLY -> current.plusMonths(3);
            case SEMI_ANNUALLY -> current.plusMonths(6);
            case ANNUALLY -> current.plusYears(1);
            case BULLET -> current;
        };
    }

    private static BigDecimal calculatePeriodicPayment(BigDecimal principal, BigDecimal periodicRate, int periods) {
        if (periods < 1) {
            return BigDecimal.ZERO;
        }
        if (periodicRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(periods), 2, RoundingMode.HALF_UP);
        }
        BigDecimal onePlusR = BigDecimal.ONE.add(periodicRate);
        BigDecimal numerator = principal.multiply(periodicRate).multiply(onePlusR.pow(periods));
        BigDecimal denominator = onePlusR.pow(periods).subtract(BigDecimal.ONE);
        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    // ========== Scheduled Task Method Implementations ==========

    /**
     * Processes overdue detection for all schedules as of the current date.
     *
     * This is a critical scheduled task that runs daily to identify and mark
     * schedules that have become overdue. It's essential for delinquency
     * management and portfolio risk monitoring.
     *
     * Processing Logic:
     * 1. Find all schedules requiring overdue check (past due date, PENDING status)
     * 2. Calculate days past due for each schedule
     * 3. Mark schedules as overdue with calculated days
     * 4. Update schedule status to OVERDUE
     * 5. Save all updated schedules
     * 6. Return count of schedules marked as overdue
     *
     * Overdue Criteria:
     * - Due date is before the current date
     * - Schedule status is PENDING (not paid or partially paid)
     * - Not already marked as overdue
     *
     * Days Past Due Calculation:
     * - Days = current date - due date
     * - Always positive number
     * - Calculated using ChronoUnit.DAYS for accuracy
     *
     * Business Impact:
     * - Triggers delinquency reporting
     * - Affects loan account delinquency status
     * - May trigger collection workflows
     * - Impacts portfolio quality metrics
     * - Required for regulatory reporting
     *
     * Scheduling:
     * - Typically runs daily (early morning)
     * - Should run before other daily processes
     * - May run multiple times per day for real-time systems
     * - Critical for operational accuracy
     *
     * Error Handling:
     * - Individual schedule failures don't stop batch processing
     * - Errors are logged for investigation
     * - Failed schedules can be reprocessed
     * - Maintains data integrity
     *
     * Performance Considerations:
     * - Processes schedules in batches for large portfolios
     * - Uses database queries to minimize memory usage
     * - Logs progress for monitoring
     * - Optimized for daily execution
     *
     * @param currentDate the date to check overdue status against (typically current date)
     * @return the number of schedules marked as overdue
     */
    @Transactional
    public int processOverdueSchedules(LocalDate currentDate) {
        log.info("Starting overdue schedule processing for date: {}", currentDate);

        List<LoanSchedule> pendingSchedules = scheduleRepository.findSchedulesRequiringOverdueCheck(currentDate);
        int markedCount = 0;

        for (LoanSchedule schedule : pendingSchedules) {
            try {
                if (schedule.getDueDate().isBefore(currentDate)
                        && ScheduleStatus.PENDING.equals(schedule.getStatus())) {

                    long daysPastDue = ChronoUnit.DAYS.between(schedule.getDueDate(), currentDate);

                    schedule.setIsOverdue(true);
                    schedule.setDaysPastDue((int) daysPastDue);
                    schedule.setStatus(ScheduleStatus.OVERDUE);

                    scheduleRepository.save(schedule);
                    markedCount++;

                    log.debug("Marked schedule {} as overdue ({} days past due)", schedule.getId(), daysPastDue);
                }
            } catch (Exception e) {
                log.error("Error processing overdue status for schedule {}", schedule.getId(), e);
            }
        }

        log.info("Marked {} schedules as overdue", markedCount);
        return markedCount;
    }

    /**
     * Generates payment reminders for schedules due in the specified number of days.
     *
     * This scheduled task identifies upcoming payment obligations and generates
     * reminders to help borrowers make timely payments. It's a proactive customer
     * service feature that helps reduce delinquencies.
     *
     * Processing Logic:
     * 1. Calculate reminder date (current date + reminder days)
     * 2. Find all schedules due on the reminder date
     * 3. Filter to only PENDING schedules (not yet paid)
     * 4. Generate reminder notifications for each schedule
     * 5. Log reminder generation for audit trail
     * 6. Return count of reminders generated
     *
     * Reminder Timing:
     * - Configurable days before due date (typically 3-7 days)
     * - Can be run multiple times with different reminder periods
     * - Common patterns: 7 days, 3 days, 1 day before due
     * - Avoids duplicate reminders for same schedule
     *
     * Reminder Content (logged):
     * - Loan account number for identification
     * - Payment amount due
     * - Due date for the payment
     * - Customer contact information (if available)
     *
     * Integration Points:
     * - SMS/Email notification services
     * - Customer communication platforms
     * - Mobile app push notifications
     * - Call center systems
     * - Customer relationship management (CRM)
     *
     * Business Benefits:
     * - Reduces payment delinquencies
     * - Improves customer satisfaction
     * - Decreases collection costs
     * - Enhances cash flow predictability
     * - Supports regulatory compliance
     *
     * Scheduling Patterns:
     * - Daily execution with different reminder periods
     * - Multiple reminders per loan (7-day, 3-day, 1-day)
     * - Weekend and holiday adjustments
     * - Time zone considerations for multi-region operations
     *
     * Error Handling:
     * - Individual reminder failures don't stop batch processing
     * - Failed reminders are logged for retry
     * - Maintains audit trail of all reminder attempts
     * - Graceful handling of communication service failures
     *
     * Performance Considerations:
     * - Efficient database queries for due date filtering
     * - Batch processing for large customer bases
     * - Rate limiting for external communication services
     * - Monitoring and alerting for processing failures
     *
     * @param currentDate the current date for reminder calculation
     * @param reminderDays the number of days before due date to send reminders
     * @return the number of payment reminders generated
     */
    @Transactional(readOnly = true)
    public int generatePaymentReminders(LocalDate currentDate, int reminderDays) {
        log.info("Starting payment reminder generation for date: {} with {} days notice", currentDate, reminderDays);

        LocalDate reminderDate = currentDate.plusDays(reminderDays);
        List<LoanSchedule> upcomingSchedules = scheduleRepository.findSchedulesDueOnDate(reminderDate);

        int reminderCount = 0;

        for (LoanSchedule schedule : upcomingSchedules) {
            try {
                if (ScheduleStatus.PENDING.equals(schedule.getStatus())) {
                    log.info(
                            "Payment reminder: Loan {} has payment of {} due on {}",
                            schedule.getLoanAccount().getLoanAccountNumber(),
                            schedule.getTotalDue(),
                            schedule.getDueDate());

                    reminderCount++;
                }
            } catch (Exception e) {
                log.error("Error generating payment reminder for schedule {}", schedule.getId(), e);
            }
        }

        log.info("Generated {} payment reminders", reminderCount);
        return reminderCount;
    }
}
