package com.openfinova.banking.gl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.gl.api.dto.PostTransactionCommand;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.api.entity.GLTransactionStatus;
import com.openfinova.banking.gl.api.entity.GLTransactionType;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.entity.FiscalPeriod;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.entity.GLTransactionSequence;
import com.openfinova.banking.gl.mapper.GLEntityMapper;
import com.openfinova.banking.gl.repository.FiscalPeriodRepository;
import com.openfinova.banking.gl.repository.GLAccountRepository;
import com.openfinova.banking.gl.repository.GLTransactionRepository;
import com.openfinova.banking.gl.repository.GLTransactionSequenceRepository;

/**
 * Orchestrates the fiscal period close workflow.
 *
 * This service sits at the top of the dependency tree and owns the full
 * sequence of operations required to close a fiscal period:
 *
 *   1. Pre-close validation (balance consistency, pending approvals, sequence integrity)
 *   2. Currency revaluation for all foreign-currency accounts
 *   3. Daily balance snapshot synchronisation
 *   4. <b>Year-end only:</b> Closing journal entries (P&amp;L → Retained Earnings)
 *   5. Status change (delegates the actual close to {@link FiscalPeriodService})
 *   6. <b>Year-end only:</b> Post-close trial balance validation (P&amp;L accounts must be zero)
 *
 * Separating orchestration from domain logic breaks the circular dependency that
 * would otherwise exist between {@link FiscalPeriodService}, {@link GLTransactionService},
 * and {@link RevaluationService}.
 *
 * Dependency hierarchy (acyclic):
 * <pre>
 * FiscalPeriodWorkflowService
 *   ├── FiscalPeriodService   (domain / metadata / status change only)
 *   ├── RevaluationService    → GLTransactionService → FiscalPeriodService
 *   ├── BalanceService
 *   └── GLTransactionService  → FiscalPeriodService
 * </pre>
 */
@Service
@Transactional
public class FiscalPeriodWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(FiscalPeriodWorkflowService.class);

    private final FiscalPeriodService fiscalPeriodService;
    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final RevaluationService revaluationService;
    private final BalanceService balanceService;
    private final GLTransactionService glTransactionService;
    private final GLAccountService glAccountService;
    private final GLAccountRepository glAccountRepository;
    private final OperationalGLAccountService operationalGLAccountService;
    private final GLTransactionRepository glTransactionRepository;
    private final GLTransactionSequenceRepository sequenceRepository;

    public FiscalPeriodWorkflowService(FiscalPeriodService fiscalPeriodService,
            FiscalPeriodRepository fiscalPeriodRepository, RevaluationService revaluationService,
            BalanceService balanceService, GLTransactionService glTransactionService, GLAccountService glAccountService,
            GLAccountRepository glAccountRepository, OperationalGLAccountService operationalGLAccountService,
            GLTransactionRepository glTransactionRepository, GLTransactionSequenceRepository sequenceRepository) {
        this.fiscalPeriodService = fiscalPeriodService;
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.revaluationService = revaluationService;
        this.balanceService = balanceService;
        this.glTransactionService = glTransactionService;
        this.glAccountService = glAccountService;
        this.glAccountRepository = glAccountRepository;
        this.operationalGLAccountService = operationalGLAccountService;
        this.glTransactionRepository = glTransactionRepository;
        this.sequenceRepository = sequenceRepository;
    }

    /**
     * Closes an active fiscal period.
     *
     * <p>This method performs the following operations in order:
     * <ol>
     *   <li>Validates the period exists and is not already closed</li>
     *   <li>Validates balance consistency</li>
     *   <li>Validates no pending-approval transactions exist</li>
     *   <li>Validates transaction number sequence completeness</li>
     *   <li>Performs currency revaluation for all foreign-currency accounts</li>
     *   <li>Synchronises daily balance snapshots for the entire period</li>
     *   <li>Generates and posts P&L closing journal entries</li>
     *   <li>Delegates the actual status change to {@link FiscalPeriodService#markClosed}</li>
     *   <li>Validates the post-closing trial balance</li>
     * </ol>
     *
     * @param periodId the UUID of the fiscal period to close
     * @param closedBy the user performing the close
     * @param reason   the business reason for closing the period
     * @throws IllegalArgumentException if the fiscal period does not exist
     * @throws IllegalStateException    if any pre-close validation fails
     */
    public void closePeriod(UUID periodId, String closedBy, String reason) {
        logger.info("Closing fiscal period: {} by {}", periodId, closedBy);

        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal period not found: " + periodId));

        if (period.isClosed()) {
            logger.warn("Fiscal period is already closed: {}", periodId);
            return;
        }

        // Capture old state for audit trail (passed through to markClosed)
        Map<String, Object> oldValues = Map.of(
                "status",
                period.getStatus().toString(),
                "startDate",
                period.getStartDate().toString(),
                "endDate",
                period.getEndDate().toString(),
                "name",
                period.getName());

        // --- Step 1: balance consistency ---
        boolean balancesConsistent = balanceService.validateAllBalancesConsistency(period.getEndDate());
        if (!balancesConsistent) {
            throw new IllegalStateException("Cannot close fiscal period with inconsistent balances: " + periodId);
        }

        // --- Step 1b: accounting equation (Assets = Liabilities + Equity) ---
        glTransactionService.validateAccountingEquation(period.getEndDate());

        // --- Step 2: no pending-approval transactions ---
        logger.info("Validating no pending approval transactions for period: {}", period.getName());
        List<GLTransaction> pendingApprovals = glTransactionRepository.findByStatusAndTransactionDateBetween(
                GLTransactionStatus.PENDING_APPROVAL,
                period.getStartDate(),
                period.getEndDate());
        if (!pendingApprovals.isEmpty()) {
            String errorMessage = String.format(
                    "Cannot close fiscal period with %d transactions pending approval. "
                            + "Please approve or reject all pending transactions before closing the period.",
                    pendingApprovals.size());
            logger.error("Period close validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // --- Step 3: transaction number sequence integrity ---
        logger.info("Validating transaction number sequence for period: {}", period.getName());
        List<String> sequenceErrors = validatePeriodSequence(periodId);
        if (!sequenceErrors.isEmpty()) {
            String errorMessage = "Cannot close fiscal period with sequence gaps or errors: "
                    + String.join("; ", sequenceErrors);
            logger.error("Period close validation failed: {}", errorMessage);
            throw new IllegalStateException(errorMessage);
        }

        // --- Step 4: currency revaluation ---
        logger.info("Performing automatic currency revaluation for period close: {}", periodId);
        try {
            revaluationService.performRevaluation(period.getEndDate());
        } catch (Exception e) {
            logger.error("Currency revaluation failed during period close: {}", periodId, e);
            throw new IllegalStateException(
                    "Cannot close fiscal period due to revaluation failure: " + e.getMessage(),
                    e);
        }

        // --- Step 5: synchronise daily balance snapshots ---
        balanceService.synchronizeDailySnapshots(period.getStartDate(), period.getEndDate());

        // --- Step 6: closing journal entries (year-end period only) ---
        // Revenue and Expense accounts are temporary: they accumulate throughout the
        // fiscal year and must only be zeroed out — and the P&L transferred to
        // Retained Earnings — when the LAST period of the year is closed.
        // Running closing entries on every monthly close would zero P&L after Period 1,
        // leaving Period 2 starting from zero and corrupting the year-to-date income
        // statement and every cumulative balance query (getBalanceAtDate) for the rest
        // of the year.
        UUID correlationId = UUID.randomUUID();
        boolean isYearEnd = isYearEndPeriod(period);
        if (isYearEnd) {
            logger.info("Year-end period detected — generating P&L closing entries for: {}", period.getName());
            GLTransaction closingTransaction = null;
            try {
                closingTransaction = generateClosingEntries(period, correlationId, closedBy);
                if (closingTransaction != null) {
                    logger.info(
                            "Closing entries posted successfully. Transaction: {}",
                            closingTransaction.getReferenceId());
                }
            } catch (Exception e) {
                logger.error("Failed to generate closing entries for period: {}", periodId, e);
                throw new IllegalStateException(
                        "Cannot close fiscal period due to closing entry failure: " + e.getMessage(),
                        e);
            }
        } else {
            logger.info("Non-year-end period close — P&L closing entries skipped for: {}", period.getName());
        }

        // --- Step 7: status change (delegates to FiscalPeriodService) ---
        fiscalPeriodService.markClosed(periodId, closedBy, reason, oldValues, correlationId);
        logger.info("Fiscal period closed successfully: {}", periodId);

        // --- Step 8: post-close trial balance validation (year-end only, non-fatal) ---
        // This check asserts that all P&L accounts are at zero after the closing entries,
        // so it only makes sense at year-end when those entries have actually been posted.
        if (isYearEnd) {
            logger.info("Validating post-closing trial balance for period: {}", period.getName());
            try {
                validatePostClosingTrialBalance(period);
            } catch (Exception e) {
                logger.error("Post-closing trial balance validation failed for period: {}", periodId, e);
                logger.warn("Period closed but post-closing validation found issues. Manual review required.");
            }
        }
    }

    // -------------------------------------------------------------------------
    // Private helpers (orchestration detail not exposed as public API)
    // -------------------------------------------------------------------------

    /**
     * Determines whether the given period is the last period of its fiscal year.
     *
     * <p>P&amp;L closing entries and the post-close trial balance assertion are only
     * meaningful — and only safe — at year-end.  Closing Revenue/Expense accounts
     * mid-year would zero out the year-to-date balances after every monthly close,
     * leaving subsequent periods starting from zero and corrupting cumulative
     * balance queries for the remainder of the year.
     *
     * @param period the period about to be closed
     * @return {@code true} if this period has the highest period number in its fiscal year
     */
    private boolean isYearEndPeriod(FiscalPeriod period) {
        if (period.getFiscalYear() == null || period.getPeriodNumber() == null) {
            return false;
        }
        return fiscalPeriodRepository.findMaxPeriodNumberByFiscalYear(period.getFiscalYear())
                .map(max -> period.getPeriodNumber().equals(max)).orElse(false);
    }

    /**
     * Generates and posts closing entries to zero out P&L accounts and transfer
     * net income/loss to retained earnings.
     */
    private GLTransaction generateClosingEntries(FiscalPeriod period, UUID correlationId, String closedBy) {
        logger.info("Generating closing entries for fiscal period: {}", period.getName());

        LocalDate periodEndDate = period.getEndDate();
        List<PostTransactionCommand.JournalEntryCommand> closingEntries = new ArrayList<>();
        List<GLAccount> allAccounts = glAccountRepository.findAll();

        BigDecimal totalRevenueBalance = BigDecimal.ZERO;
        BigDecimal totalExpenseBalance = BigDecimal.ZERO;

        // Debit REVENUE accounts to zero them out (revenues carry credit balances)
        for (GLAccount account : allAccounts) {
            if (account.getType() == GLAccountType.REVENUE && account.isActive()) {
                BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), periodEndDate);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    totalRevenueBalance = totalRevenueBalance.add(balance);
                    closingEntries.add(
                            new PostTransactionCommand.JournalEntryCommand(
                                    account.getId(),
                                    balance.abs(), // Debit
                                    BigDecimal.ZERO,
                                    "Period closing entry - " + period.getName(),
                                    periodEndDate));
                    logger.debug("Closing REVENUE account: {} with balance: {}", account.getCode(), balance);
                }
            }
        }

        // Credit EXPENSE accounts to zero them out (expenses carry debit balances)
        for (GLAccount account : allAccounts) {
            if (account.getType() == GLAccountType.EXPENSE && account.isActive()) {
                BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), periodEndDate);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    totalExpenseBalance = totalExpenseBalance.add(balance);
                    closingEntries.add(
                            new PostTransactionCommand.JournalEntryCommand(
                                    account.getId(),
                                    BigDecimal.ZERO,
                                    balance.abs(), // Credit
                                    "Period closing entry - " + period.getName(),
                                    periodEndDate));
                    logger.debug("Closing EXPENSE account: {} with balance: {}", account.getCode(), balance);
                }
            }
        }

        if (closingEntries.isEmpty()) {
            logger.info("No P&L balances to close for period: {}", period.getName());
            return null;
        }

        BigDecimal netIncome = totalRevenueBalance.subtract(totalExpenseBalance);

        UUID retainedEarningsAccountId = operationalGLAccountService
                .getOperationalGLAccount(OperationalGLAccountType.RETAINED_EARNINGS);
        GLAccount retainedEarningsAccount = glAccountRepository.findById(retainedEarningsAccountId).orElseThrow(
                () -> new IllegalStateException(
                        "Retained Earnings account not found: " + retainedEarningsAccountId
                                + ". Please configure RETAINED_EARNINGS operational account mapping."));

        if (netIncome.compareTo(BigDecimal.ZERO) > 0) {
            // Net income (profit) — credit retained earnings
            closingEntries.add(
                    new PostTransactionCommand.JournalEntryCommand(
                            retainedEarningsAccount.getId(),
                            BigDecimal.ZERO,
                            netIncome,
                            "Net income transfer - " + period.getName(),
                            periodEndDate));
            logger.info("Net income of {} transferred to retained earnings", netIncome);
        } else if (netIncome.compareTo(BigDecimal.ZERO) < 0) {
            // Net loss — debit retained earnings
            closingEntries.add(
                    new PostTransactionCommand.JournalEntryCommand(
                            retainedEarningsAccount.getId(),
                            netIncome.abs(),
                            BigDecimal.ZERO,
                            "Net loss transfer - " + period.getName(),
                            periodEndDate));
            logger.info("Net loss of {} transferred to retained earnings", netIncome.abs());
        }

        PostTransactionCommand closingCommand = new PostTransactionCommand(
                GLTransactionType.PERIOD_CLOSING.generateReferenceId(period.getName() + "-" + periodEndDate.toString()),
                "Period closing entry for " + period.getName() + " - Net Income/Loss: " + netIncome + " "
                        + retainedEarningsAccount.getCurrency(),
                periodEndDate,
                retainedEarningsAccount.getCurrency(),
                closedBy,
                closingEntries);

        // Note: posted while period is still OPEN to pass fiscal-period validation in GLTransactionService
        GLTransaction closingTransaction = GLEntityMapper.toEntity(closingCommand, glAccountService);
        GLTransaction postedTransaction = glTransactionService.postTransaction(closingTransaction);

        logger.info(
                "Closing entries posted successfully. Transaction ID: {}, Net Income/Loss: {}",
                postedTransaction.getId(),
                netIncome);
        return postedTransaction;
    }

    /**
     * Validates that all P&L accounts have zero balance and that the trial balance
     * remains balanced after period-close entries have been posted.
     */
    private void validatePostClosingTrialBalance(FiscalPeriod period) {
        logger.debug("Validating post-closing trial balance for period: {}", period.getName());

        LocalDate periodEndDate = period.getEndDate();
        List<GLAccount> allAccounts = glAccountRepository.findAll();

        List<String> nonZeroPLAccounts = new ArrayList<>();

        for (GLAccount account : allAccounts) {
            if ((account.getType() == GLAccountType.REVENUE || account.getType() == GLAccountType.EXPENSE)
                    && account.isActive()) {
                BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), periodEndDate);
                if (balance.compareTo(BigDecimal.ZERO) != 0) {
                    nonZeroPLAccounts.add(account.getCode() + ": " + balance);
                    logger.warn(
                            "P&L account {} still has non-zero balance after close: {}",
                            account.getCode(),
                            balance);
                }
            }
        }

        if (!nonZeroPLAccounts.isEmpty()) {
            throw new IllegalStateException(
                    "Post-closing validation failed: P&L accounts have non-zero balances: "
                            + String.join(", ", nonZeroPLAccounts));
        }

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (GLAccount account : allAccounts) {
            if (account.isActive()) {
                BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), periodEndDate);
                boolean isDebitBalance;
                switch (account.getType()) {
                    case ASSET:
                    case EXPENSE:
                        isDebitBalance = balance.compareTo(BigDecimal.ZERO) >= 0;
                        break;
                    default: // LIABILITY, EQUITY, REVENUE
                        isDebitBalance = balance.compareTo(BigDecimal.ZERO) < 0;
                        break;
                }
                if (isDebitBalance) {
                    totalDebits = totalDebits.add(balance.abs());
                } else {
                    totalCredits = totalCredits.add(balance.abs());
                }
            }
        }

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException(
                    "Post-closing trial balance is not balanced! Debits: " + totalDebits + ", Credits: "
                            + totalCredits);
        }

        logger.info("Post-closing trial balance validation passed. Debits: {}, Credits: {}", totalDebits, totalCredits);
    }

    /**
     * Validates the transaction number sequence for a fiscal period.
     * Ensures gapless, duplicate-free numbering for regulatory compliance.
     */
    private List<String> validatePeriodSequence(UUID periodId) {
        List<String> errors = new ArrayList<>();
        try {
            FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                    .orElseThrow(() -> new IllegalStateException("Period not found: " + periodId));

            List<GLTransaction> transactions = glTransactionRepository.findAll().stream()
                    .filter(t -> t.getTransactionNumber() != null).filter(GLTransaction::isPosted).filter(t -> {
                        LocalDate txDate = t.getTransactionDate();
                        return !txDate.isBefore(period.getStartDate()) && !txDate.isAfter(period.getEndDate());
                    }).sorted((t1, t2) -> t1.getTransactionNumber().compareTo(t2.getTransactionNumber())).toList();

            if (transactions.isEmpty()) {
                return errors;
            }

            Long expectedNumber = 1L;
            for (GLTransaction transaction : transactions) {
                if (!transaction.getTransactionNumber().equals(expectedNumber)) {
                    errors.add(
                            String.format(
                                    "Gap in sequence: expected %d, found %d (ref: %s)",
                                    expectedNumber,
                                    transaction.getTransactionNumber(),
                                    transaction.getReferenceId()));
                }
                expectedNumber++;
            }

            long uniqueCount = transactions.stream().map(GLTransaction::getTransactionNumber).distinct().count();
            if (uniqueCount != transactions.size()) {
                errors.add("Duplicate transaction numbers detected");
            }

            Optional<GLTransactionSequence> sequenceOpt = sequenceRepository.findByFiscalPeriodId(periodId);
            if (sequenceOpt.isPresent()) {
                long actualCount = transactions.size();
                long sequenceValue = sequenceOpt.get().getLastAssignedNumber();
                if (sequenceValue != actualCount) {
                    errors.add(
                            String.format(
                                    "Sequence mismatch: sequence shows %d, database has %d posted transactions",
                                    sequenceValue,
                                    actualCount));
                }
            } else if (!transactions.isEmpty()) {
                errors.add("No sequence record found despite having posted transactions");
            }

            if (errors.isEmpty()) {
                logger.info(
                        "Period {} sequence validation passed: {} transactions, all sequential",
                        period.getName(),
                        transactions.size());
            }
        } catch (Exception e) {
            logger.error("Error during period sequence validation: {}", e.getMessage(), e);
            errors.add("Validation error: " + e.getMessage());
        }
        return errors;
    }
}
