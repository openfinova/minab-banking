package com.openfinova.banking.gl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.exchangerate.api.ExchangeRateService;
import com.openfinova.banking.gl.api.dto.DailyBalanceSnapshot;
import com.openfinova.banking.gl.api.dto.GLAccountBalance;
import com.openfinova.banking.gl.api.dto.TrialBalance;
import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLDailyBalance;
import com.openfinova.banking.gl.entity.GLJournalEntry;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.repository.GLAccountRepository;
import com.openfinova.banking.gl.repository.GLDailyBalanceRepository;
import com.openfinova.banking.gl.repository.GLJournalEntryRepository;
import com.openfinova.banking.gl.repository.GLTransactionRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Implementation of BalanceService with comprehensive balance management,
 * synchronization capabilities, and fiscal period integration.
 */
@Service
@Transactional
public class BalanceService {

    private static final Logger logger = LoggerFactory.getLogger(BalanceService.class);

    @Value("${app.base-currency:EUR}")
    private String baseCurrency;

    private final GLAccountRepository glAccountRepository;
    private final GLJournalEntryRepository glJournalEntryRepository;
    private final GLDailyBalanceRepository glDailyBalanceRepository;
    private final GLTransactionRepository transactionRepository;
    private final ExchangeRateService exchangeRateService;
    private final DateTimeService dateTimeService;

    public BalanceService(GLAccountRepository glAccountRepository, GLJournalEntryRepository glJournalEntryRepository,
            GLDailyBalanceRepository glDailyBalanceRepository, GLTransactionRepository transactionRepository,
            ExchangeRateService exchangeRateService, DateTimeService dateTimeService) {
        this.glAccountRepository = glAccountRepository;
        this.glJournalEntryRepository = glJournalEntryRepository;
        this.glDailyBalanceRepository = glDailyBalanceRepository;
        this.transactionRepository = transactionRepository;
        this.exchangeRateService = exchangeRateService;
        this.dateTimeService = dateTimeService;
    }

    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public BigDecimal getCurrentBalance(UUID accountId) {
        logger.debug("Getting current balance for GL account: {}", accountId);
        return calculateBalanceAtDate(accountId, dateTimeService.today());
    }

    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public BigDecimal getBalanceAtDate(UUID accountId, LocalDate date) {
        return calculateBalanceAtDate(accountId, date);
    }

    /**
     * Utility method to calculate balance for an account at a specific date.
     * This method implements an optimized balance calculation strategy:
     * 1. First attempts to use the most recent daily balance snapshot as a base
     * 2. Calculates incremental changes from the snapshot date to the target date
     * 3. Falls back to full journal entry calculation if no snapshots are available
     *
     * @param accountId The UUID of the GL account
     * @param targetDate The date for which to calculate the balance (null means current date)
     * @return The calculated balance as of the target date
     * @throws IllegalArgumentException if the account is not found or the target date is invalid
     */
    private BigDecimal calculateBalanceAtDate(UUID accountId, LocalDate targetDate) {
        if (targetDate == null) {
            throw new IllegalArgumentException("Target date cannot be null");
        }

        // Verify account exists
        if (!glAccountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("GLAccount", accountId);
        }

        // Try to get the most recent daily balance before or on the target date
        Optional<GLDailyBalance> latestDailyBalance = glDailyBalanceRepository
                .findFirstByGlAccount_IdAndBalanceDateBeforeOrderByBalanceDateDesc(accountId, targetDate);

        if (latestDailyBalance.isPresent()) {
            LocalDate baseDate = latestDailyBalance.get().getBalanceDate();
            BigDecimal baseBalance = latestDailyBalance.get().getClosingBalance();

            // If the snapshot is for the exact target date, return it directly
            if (baseDate.equals(targetDate)) {
                return baseBalance;
            }

            // Calculate incremental balance from the day after the snapshot to the target date
            BigDecimal incrementalBalance = calculateBalanceFromEntries(accountId, baseDate.plusDays(1), targetDate);
            return baseBalance.add(incrementalBalance);
        } else {
            // No daily balance available, calculate from all journal entries up to target date
            return calculateBalanceFromEntries(accountId, null, targetDate);
        }
    }

    public Optional<GLDailyBalance> getAccountLatestDailyBalance(UUID accountId) {
        return glDailyBalanceRepository.findFirstByGlAccount_IdOrderByBalanceDateDesc(accountId);
    }

    @PreAuthorize("hasAnyAuthority('gl:approve', 'service:gl:write')")

    public void recalculateBalance(UUID accountId) {
        logger.info("Recalculating balance for GL account: {}", accountId);

        GLAccount account = glAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("GL Account not found: %s", accountId)));

        // Calculate balance from all journal entries
        BigDecimal recalculatedBalance = calculateBalanceFromEntries(accountId, null, dateTimeService.today());

        // Store the recalculated balance in GLDailyBalance for today
        LocalDate today = dateTimeService.today();
        GLDailyBalance dailyBalance = glDailyBalanceRepository.findDailyBalanceByAccountAndDate(accountId, today)
                .orElse(new GLDailyBalance(account, today));

        dailyBalance.setClosingBalance(recalculatedBalance);
        glDailyBalanceRepository.save(dailyBalance);

        logger.info("Recalculated balance for account: {} = {}", accountId, recalculatedBalance);
    }

    /**
     * Generates a trial balance report as of a specific date.
     *
     * A trial balance is a fundamental accounting report that lists all general ledger accounts
     * and their balances at a specific point in time. It serves as a key control mechanism
     * to ensure the accounting equation (Assets = Liabilities + Equity) remains balanced.
     *
     * Key characteristics of a trial balance:
     * - Lists all accounts with their debit or credit balances
     * - Total debits must equal total credits (fundamental accounting principle)
     * - Provides a snapshot of the financial position at a specific date
     * - Used to verify the mathematical accuracy of the double-entry bookkeeping system
     * - Serves as the foundation for preparing financial statements
     *
     * Account balance presentation rules:
     * - Asset accounts: Normal debit balance, shown in debit column when positive
     * - Liability accounts: Normal credit balance, shown in credit column when positive
     * - Equity accounts: Normal credit balance, shown in credit column when positive
     * - Revenue accounts: Normal credit balance, shown in credit column when positive
     * - Expense accounts: Normal debit balance, shown in debit column when positive
     * - Contra accounts: Opposite of their related account's normal balance
     *
     * Sign convention (applied by {@code calculateBalanceFromEntries}):
     * A POSITIVE balance always means the account carries a balance in its normal
     * direction (debit for Assets/Expenses, credit for Liabilities/Equity/Revenue).
     * A NEGATIVE balance always means an abnormal balance (the opposite of normal).
     * The column-placement logic below relies on this invariant.
     *
     * If an account has an abnormal balance (e.g., a credit-balance Asset), it appears
     * in the opposite column to maintain the mathematical integrity of the trial balance.
     *
     * @param asOfDate The date for which to generate the trial balance
     * @return TrialBalance containing all account balances and totals
     * @throws IllegalArgumentException if asOfDate is null or invalid
     */
    @PreAuthorize("hasAuthority('gl:read')")

    @Transactional(readOnly = true)
    public TrialBalance getTrialBalance(LocalDate asOfDate) {
        logger.info("Generating trial balance as of: {}", asOfDate);

        // ── Query 1 of 4: all accounts ────────────────────────────────────────────
        List<GLAccount> allAccounts = glAccountRepository.findAll();

        // ── Query 2 of 4: latest snapshot ≤ asOfDate, one row per account ────────
        Map<UUID, GLDailyBalance> latestSnapshots = glDailyBalanceRepository
                .findLatestSnapshotsForAllAccountsAsOf(asOfDate).stream()
                .collect(Collectors.toMap(db -> db.getGlAccount().getId(), db -> db));

        // Partition accounts into three buckets:
        //  exactHit       – snapshot IS asOfDate  → use closingBalance directly
        //  needsIncrement – snapshot exists BUT before asOfDate → base + incremental entries
        //  noSnapshot     – no snapshot at all    → full entry scan
        List<UUID> needsIncrementIds = new ArrayList<>();
        List<UUID> noSnapshotIds = new ArrayList<>();
        LocalDate earliestBase = asOfDate; // tracks the oldest snapshot date in needsIncrement

        for (GLAccount account : allAccounts) {
            GLDailyBalance snap = latestSnapshots.get(account.getId());
            if (snap == null) {
                noSnapshotIds.add(account.getId());
            } else if (!snap.getBalanceDate().isEqual(asOfDate)) {
                needsIncrementIds.add(account.getId());
                if (snap.getBalanceDate().isBefore(earliestBase)) {
                    earliestBase = snap.getBalanceDate();
                }
            }
            // else: exact hit – nothing to add
        }

        // ── Query 3 of 4 (conditional): incremental entries after earliest snapshot ─
        // Key: account UUID → list of journal entries after that account's snapshot date.
        // We over-fetch from the global earliest base; entries are filtered per account
        // in the balance calculation loop below.
        Map<UUID, List<GLJournalEntry>> incrementalEntries = new HashMap<>();
        if (!needsIncrementIds.isEmpty()) {
            glJournalEntryRepository
                    .findEntriesForAccountsInDateRange(needsIncrementIds, earliestBase.plusDays(1), asOfDate).forEach(
                            je -> incrementalEntries.computeIfAbsent(je.getAccount().getId(), k -> new ArrayList<>())
                                    .add(je));
        }

        // ── Query 4 of 4 (conditional): full entry history for snapshot-less accounts ─
        Map<UUID, List<GLJournalEntry>> fullHistoryEntries = new HashMap<>();
        if (!noSnapshotIds.isEmpty()) {
            glJournalEntryRepository.findEntriesForAccountsUpToDate(noSnapshotIds, asOfDate).forEach(
                    je -> fullHistoryEntries.computeIfAbsent(je.getAccount().getId(), k -> new ArrayList<>()).add(je));
        }

        // ── In-memory balance computation ─────────────────────────────────────────
        TrialBalance trialBalance = new TrialBalance();
        trialBalance.setAsOfDate(asOfDate);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;
        List<GLAccountBalance> accountBalances = new ArrayList<>();

        for (GLAccount account : allAccounts) {
            UUID id = account.getId();
            GLDailyBalance snap = latestSnapshots.get(id);

            BigDecimal balance;
            if (snap != null && snap.getBalanceDate().isEqual(asOfDate)) {
                // Exact snapshot hit: use stored closing balance directly.
                balance = snap.getClosingBalance();
            } else if (snap != null) {
                // Snapshot exists but is before asOfDate.
                // closingBalance is already signed (positive = normal direction).
                // The raw incremental (Σdebits − Σcredits) has the same sign as the
                // calculateBalanceFromEntries result, so we apply the same convention.
                BigDecimal rawIncrement = BigDecimal.ZERO;
                LocalDate snapDate = snap.getBalanceDate();
                for (GLJournalEntry je : incrementalEntries.getOrDefault(id, List.of())) {
                    // Filter: only entries strictly after the account's own snapshot date.
                    if (je.getValueDate().isAfter(snapDate)) {
                        rawIncrement = rawIncrement.add(je.getBaseDebitAmount()).subtract(je.getBaseCreditAmount());
                    }
                }
                BigDecimal signedIncrement = account.getNormalBalance() == BalanceType.CREDIT ? rawIncrement.negate()
                        : rawIncrement;
                balance = snap.getClosingBalance().add(signedIncrement);
            } else {
                // No snapshot: sum all entries up to asOfDate.
                BigDecimal raw = BigDecimal.ZERO;
                for (GLJournalEntry je : fullHistoryEntries.getOrDefault(id, List.of())) {
                    raw = raw.add(je.getBaseDebitAmount()).subtract(je.getBaseCreditAmount());
                }
                balance = account.getNormalBalance() == BalanceType.CREDIT ? raw.negate() : raw;
            }

            GLAccountBalance accountBalance = new GLAccountBalance();
            accountBalance.setAccountId(id);
            accountBalance.setAccountCode(account.getCode());
            accountBalance.setAccountName(account.getName());
            accountBalance.setBalance(balance);

            // Place the balance in the correct trial-balance column.
            // Post fix-#1, balance is always signed relative to the account's normal direction:
            //   positive  → balance is in the NORMAL direction   → goes in the normal column
            //   negative  → balance is in the ABNORMAL direction → goes in the opposite column
            if (account.getAccountType().isDebitNormal()) {
                // Debit-normal accounts (Assets, Expenses)
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    // Normal: debits > credits → Debit column
                    accountBalance.setDebitAmount(balance);
                    accountBalance.setCreditAmount(BigDecimal.ZERO);
                    totalDebits = totalDebits.add(balance);
                } else {
                    // Abnormal: credits > debits → Credit column
                    accountBalance.setDebitAmount(BigDecimal.ZERO);
                    accountBalance.setCreditAmount(balance.negate());
                    totalCredits = totalCredits.add(balance.negate());
                }
            } else {
                // Credit-normal accounts (Liabilities, Equity, Revenue)
                if (balance.compareTo(BigDecimal.ZERO) >= 0) {
                    // Normal: credits > debits → Credit column
                    accountBalance.setDebitAmount(BigDecimal.ZERO);
                    accountBalance.setCreditAmount(balance);
                    totalCredits = totalCredits.add(balance);
                } else {
                    // Abnormal: debits > credits → Debit column
                    accountBalance.setDebitAmount(balance.negate());
                    accountBalance.setCreditAmount(BigDecimal.ZERO);
                    totalDebits = totalDebits.add(balance.negate());
                }
            }

            accountBalances.add(accountBalance);
        }

        trialBalance.setAccountBalances(accountBalances);
        trialBalance.setTotalDebits(totalDebits);
        trialBalance.setTotalCredits(totalCredits);
        trialBalance.setIsBalanced(totalDebits.compareTo(totalCredits) == 0);

        logger.info(
                "Trial balance generated: Debits={}, Credits={}, Balanced={}",
                totalDebits,
                totalCredits,
                trialBalance.getIsBalanced());

        return trialBalance;
    }

    /**
     * Synchronizes daily balance snapshots for a range of dates.
     *
     * This method ensures that daily balance snapshots exist for all accounts
     * within the specified date range. It's particularly useful for:
     * - Backfilling missing snapshots after system downtime
     * - Ensuring data consistency across date ranges
     * - Preparing for month-end or year-end closing procedures
     * - Supporting audit requirements for historical balance verification
     *
     * The synchronization process:
     * 1. Iterates through each date in the specified range
     * 2. Identifies accounts missing snapshots for each date
     * 3. Creates missing snapshots using real-time balance calculations
     * 4. Validates the consistency of newly created snapshots
     * 5. Logs any errors or inconsistencies found during the process
     *
     * Performance considerations:
     * - Large date ranges may require significant processing time
     * - Consider running during off-peak hours for extensive synchronization
     * - The method is transactional to ensure data consistency
     * - Failed snapshot creation for individual accounts won't affect others
     *
     * @param startDate The beginning date of the synchronization range (inclusive)
     * @param endDate The ending date of the synchronization range (inclusive)
     * @return List of DailyBalance objects representing newly created snapshots
     * @throws IllegalArgumentException if startDate is after endDate or either date is null
     * @throws RuntimeException if critical synchronization errors occur
     */
    public List<DailyBalanceSnapshot> synchronizeDailySnapshots(LocalDate startDate, LocalDate endDate) {
        // Validate parameters
        if (startDate == null) {
            throw new IllegalArgumentException("Start date cannot be null");
        }
        if (endDate == null) {
            throw new IllegalArgumentException("End date cannot be null");
        }
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        logger.info("Synchronizing daily balance snapshots from {} to {}", startDate, endDate);

        List<DailyBalanceSnapshot> createdSnapshots = new ArrayList<>();
        int totalCreated = 0;

        // Iterate through each date in the range
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            // Identify accounts missing snapshots for this date
            List<UUID> missingAccountIds = getAccountsMissingSnapshots(currentDate);

            // Create snapshots only for accounts with missing snapshots
            for (UUID accountId : missingAccountIds) {
                try {
                    DailyBalanceSnapshot dailyBalanceSnapshot = createDailySnapshot(accountId, currentDate);
                    createdSnapshots.add(dailyBalanceSnapshot);
                    totalCreated++;
                } catch (Exception e) {
                    logger.error(
                            "Failed to create daily snapshot for account: {} on date: {}",
                            accountId,
                            currentDate,
                            e);
                }
            }

            currentDate = currentDate.plusDays(1);
        }

        logger.info("Snapshot synchronization completed. Total snapshots created: {}", totalCreated);
        return createdSnapshots;
    }

    /**
     * Removes every persisted snapshot for {@code date} for all accounts, then rebuilds from journal activity.
     * Used when snapshots may be missing or stale relative to postings (period close, operational repair).
     */
    public void recreateSnapshotsForDate(LocalDate date) {
        if (date == null) {
            throw new IllegalArgumentException("Date cannot be null");
        }
        int deleted = glDailyBalanceRepository.deleteByBalanceDateBetween(date, date);
        logger.info("Cleared {} GLDailyBalance row(s) on {} — rebuilding snapshots from journals", deleted, date);
        synchronizeDailySnapshots(date, date);
    }

    /**
     * Creates daily balance snapshots for an accounts on a specific date.
     *
     * @param date The date for which to create daily balance snapshots
     * @return List of successfully created DailyBalance snapshots
     * @throws IllegalArgumentException if date is null
     * @see #createDailySnapshot(UUID, LocalDate) for individual account snapshot creation
     */
    public DailyBalanceSnapshot createDailySnapshot(UUID accountId, LocalDate date) {
        logger.debug("Creating daily balance snapshot for account: {} on date: {}", accountId, date);

        // Check if snapshot already exists
        Optional<GLDailyBalance> existingSnapshot = glDailyBalanceRepository
                .findDailyBalanceByAccountAndDate(accountId, date);

        if (existingSnapshot.isPresent()) {
            logger.debug("Daily balance snapshot already exists for account: {} on date: {}", accountId, date);
            return convertToDTO(existingSnapshot.get());
        }

        GLAccount account = glAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("GL Account not found: " + accountId));

        // Calculate opening balance (closing balance of previous day)
        BigDecimal openingBalance = getBalanceAtDate(accountId, date.minusDays(1));

        // Calculate activity for the day
        GLAccountBalance dayActivity = getAccountActivity(accountId, date);

        // Build the daily balance record and derive the closing balance using addActivity(),
        // which correctly applies debit-normal vs credit-normal sign convention.
        GLDailyBalance dailyBalanceEntity = new GLDailyBalance();
        dailyBalanceEntity.setGlAccount(account);
        dailyBalanceEntity.setBalanceDate(date);
        dailyBalanceEntity.setOpeningBalance(openingBalance);
        // Seed closingBalance at the opening so addActivity() adjusts from the correct base.
        dailyBalanceEntity.setClosingBalance(openingBalance);
        // addActivity() applies the correct sign convention and accumulates
        // totalDebits + totalCredits as side effects.
        dailyBalanceEntity.addActivity(dayActivity.getDebitAmount(), dayActivity.getCreditAmount());
        // addActivity() increments transactionCount by 1 (one aggregate call here);
        // override with the actual entry count for the day.
        dailyBalanceEntity.setTransactionCount(dayActivity.getTransactionCount());

        GLDailyBalance savedDailyBalance = glDailyBalanceRepository.save(dailyBalanceEntity);

        logger.debug(
                "Created daily balance snapshot: {} for account: {} on date: {}",
                savedDailyBalance.getClosingBalance(),
                accountId,
                date);

        return convertToDTO(savedDailyBalance);
    }

    /**
     * Retrieves the balance history for a specific account within a date range.
     *
     * @param accountId The UUID of the GL account for which to retrieve balance history
     * @param startDate The beginning date of the history range (inclusive)
     * @param endDate The ending date of the history range (inclusive)
     * @return List of DailyBalance objects representing the account's balance history,
     *         ordered chronologically by date
     * @throws IllegalArgumentException if accountId is null, or if startDate is after endDate
     * @see #createDailySnapshot(UUID, LocalDate) for creating missing snapshots
     * @see DailyBalanceSnapshot for details on the returned balance information
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public List<DailyBalanceSnapshot> getBalanceHistory(UUID accountId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting balance history for account: {} from {} to {}", accountId, startDate, endDate);

        List<GLDailyBalance> dailyBalances = glDailyBalanceRepository
                .findDailyBalancesByAccountAndDateRange(accountId, startDate, endDate);

        return dailyBalances.stream().map(this::convertToDTO).toList();
    }

    /**
     * Validates the consistency of balance data between daily snapshots and real-time calculations.
     *
     * This method performs a critical data integrity check by comparing the stored daily balance
     * snapshot against a real-time calculation from journal entries. This validation is essential
     * for maintaining data accuracy and identifying potential synchronization issues.
     *
     * The validation process:
     * 1. Retrieves the daily balance snapshot for the specified account and date
     * 2. Performs a real-time balance calculation using all journal entries up to the date
     * 3. Compares the two values for exact equality
     * 4. Logs any discrepancies found for investigation
     *
     * Common causes of inconsistency:
     * - Journal entries posted after snapshot creation
     * - Data corruption or incomplete transactions
     * - System errors during snapshot generation
     * - Manual adjustments not reflected in snapshots
     * - Concurrent transaction processing issues
     *
     * This validation should be performed regularly as part of:
     * - Daily reconciliation procedures
     * - Month-end closing processes
     * - Audit preparation activities
     * - Data quality monitoring
     *
     * @param accountId The UUID of the GL account to validate
     * @param asOfDate The specific date for which to validate balance consistency
     * @return true if the snapshot balance matches the calculated balance, false otherwise
     * @throws IllegalArgumentException if accountId is null or account doesn't exist
     * @see #validateAllBalancesConsistency(LocalDate) for validating all accounts at once
     * @see #createDailySnapshot(UUID, LocalDate) for creating accurate snapshots
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public boolean validateBalanceConsistency(UUID accountId, LocalDate asOfDate) {
        logger.debug("Validating balance consistency for account: {} as of: {}", accountId, asOfDate);

        // Validate parameters
        if (accountId == null) {
            throw new IllegalArgumentException("Account ID cannot be null");
        }

        if (!glAccountRepository.existsById(accountId)) {
            throw new ResourceNotFoundException("GLAccount", accountId);
        }

        // Get balance from daily snapshot
        Optional<GLDailyBalance> dailyBalance = glDailyBalanceRepository
                .findDailyBalanceByAccountAndDate(accountId, asOfDate);

        if (dailyBalance.isEmpty()) {
            logger.warn("No daily balance snapshot found for account: {} on date: {}", accountId, asOfDate);
            return false;
        }

        // Calculate real-time balance
        BigDecimal realTimeBalance = calculateBalanceFromEntries(accountId, null, asOfDate);

        // Compare balances
        BigDecimal snapshotBalance = dailyBalance.get().getClosingBalance();
        boolean isConsistent = realTimeBalance.compareTo(snapshotBalance) == 0;

        if (!isConsistent) {
            logger.warn(
                    "Balance inconsistency detected for account: {} on date: {}. Snapshot: {}, Real-time: {}",
                    accountId,
                    asOfDate,
                    snapshotBalance,
                    realTimeBalance);
        }

        return isConsistent;
    }

    /**
     * Validates the consistency of balance data between daily snapshots and real-time calculations for all GL accounts
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public boolean validateAllBalancesConsistency(LocalDate asOfDate) {
        logger.info("Validating balance consistency for all accounts as of: {}", asOfDate);

        List<GLAccount> allAccounts = glAccountRepository.findAll();
        boolean allConsistent = true;

        for (GLAccount account : allAccounts) {
            boolean isConsistent = validateBalanceConsistency(account.getId(), asOfDate);
            if (!isConsistent) {
                allConsistent = false;
            }
        }

        logger.info(
                "Balance consistency validation completed for date: {}. All consistent: {}",
                asOfDate,
                allConsistent);
        return allConsistent;
    }

    @Transactional(readOnly = true)
    public List<UUID> getAccountsMissingSnapshots(LocalDate date) {
        return glDailyBalanceRepository.findAccountsMissingSnapshotsForDate(date);
    }

    @Transactional(readOnly = true)
    public LocalDate getLatestSnapshotDate() {
        return glDailyBalanceRepository.findLatestSnapshotDate().orElse(null);
    }

    /**
     * Retrieves the balance change for a specific account between two dates.
     *
     * This method calculates the net change in an account's balance over a specified period
     * by comparing the balance at the end of the period with the balance at the beginning.
     * The calculation uses the day before the start date as the baseline to capture the
     * complete change during the specified period.
     *
     * The balance change calculation:
     * - Starting balance: Balance as of (startDate - 1 day)
     * - Ending balance: Balance as of endDate
     * - Change = Ending balance - Starting balance
     *
     * A positive change indicates:
     * - For asset/expense accounts: An increase in the account balance
     * - For liability/equity/revenue accounts: An increase in the account balance
     *
     * A negative change indicates:
     * - For asset/expense accounts: A decrease in the account balance
     * - For liability/equity/revenue accounts: A decrease in the account balance
     *
     * Use cases:
     * - Period-over-period analysis
     * - Performance measurement
     * - Variance analysis
     * - Financial reporting and analytics
     * - Budget vs. actual comparisons
     *
     * @param accountId The UUID of the GL account for which to calculate balance change
     * @param startDate The beginning date of the period (inclusive)
     * @param endDate The ending date of the period (inclusive)
     * @return GLAccountBalance containing the calculated balance change
     * @throws IllegalArgumentException if accountId is null, account doesn't exist,
     *         or if startDate is after endDate
     * @see #getBalanceAtDate(UUID, LocalDate) for individual balance calculations
     * @see #getAccountActivity(UUID, LocalDate) for daily activity details
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public GLAccountBalance getBalanceChange(UUID accountId, LocalDate startDate, LocalDate endDate) {
        BigDecimal startBalance = getBalanceAtDate(accountId, startDate.minusDays(1));
        BigDecimal endBalance = getBalanceAtDate(accountId, endDate);

        GLAccountBalance balanceChange = new GLAccountBalance();
        balanceChange.setAccountId(accountId);
        balanceChange.setBalance(endBalance.subtract(startBalance));

        return balanceChange;
    }

    /**
     * Retrieves the daily activity (debits and credits) for a specific account on a given date.
     *
     * This method calculates the total debit and credit amounts for all journal entries
     * posted to a specific GL account on a particular date. It provides detailed insight
     * into the account's daily transaction activity, which is essential for:
     *
     * - Daily balance reconciliation
     * - Transaction volume analysis
     * - Account activity monitoring
     * - Audit trail verification
     * - Daily snapshot creation
     *
     * The returned GLAccountBalance contains:
     * - Total debit amount for the day
     * - Total credit amount for the day
     * - Transaction count (number of journal entries)
     * - Account identifier
     *
     * Note: This method only considers journal entries with an exact date match.
     * It does not include entries from previous or subsequent dates.
     *
     * @param accountId The UUID of the GL account for which to retrieve daily activity
     * @param activityDate The specific date for which to calculate account activity
     * @return GLAccountBalance containing the day's debit/credit totals and transaction count
     * @throws IllegalArgumentException if accountId is null or activityDate is null
     * @see #createDailySnapshot(UUID, LocalDate) for creating daily balance snapshots
     * @see #getBalanceChange(UUID, LocalDate, LocalDate) for period balance changes
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public GLAccountBalance getAccountActivity(UUID accountId, LocalDate activityDate) {
        logger.debug("Getting account activity for account: {} on date: {}", accountId, activityDate);

        List<GLJournalEntry> dayEntries = glJournalEntryRepository.findEntriesByAccountAndDate(accountId, activityDate);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (GLJournalEntry entry : dayEntries) {
            // Accumulate base-currency amounts (locked in at posting time) to avoid
            // numeric mixing across entries denominated in different currencies (bug #9).
            totalDebits = totalDebits.add(entry.getBaseDebitAmount());
            totalCredits = totalCredits.add(entry.getBaseCreditAmount());
        }

        GLAccountBalance activity = new GLAccountBalance();
        activity.setAccountId(accountId);
        activity.setDebitAmount(totalDebits);
        activity.setCreditAmount(totalCredits);
        activity.setTransactionCount(dayEntries.size());

        return activity;
    }

    /**
     * Retrieves the balance of a specific account converted to the base currency as of a given date.
     *
     * This method provides multi-currency support by converting account balances from their
     * native currency to the system's base currency using historical exchange rates. This is
     * essential for consolidated financial reporting, multi-currency trial balances, and
     * cross-currency analysis.
     *
     * The conversion process:
     * 1. Retrieves the account balance in its native currency
     * 2. Checks if conversion is needed (account currency vs. base currency)
     * 3. If conversion is required, fetches the appropriate exchange rate for the date
     * 4. Applies the exchange rate to convert the balance to base currency
     *
     * @param accountId The UUID of the GL account for balance retrieval
     * @param date The date for which to calculate the balance and apply exchange rates
     * @return The account balance converted to base currency using historical exchange rates
     * @throws IllegalArgumentException if accountId is null or account doesn't exist
     * @throws RuntimeException if exchange rate cannot be retrieved for the specified date
     * @see exchangeRateService#getExchangeRate(String, String, LocalDate) for exchange rate details
     * @see #getBalanceAtDate(UUID, LocalDate) for native currency balance calculation
     */
    @Transactional(readOnly = true)
    public BigDecimal getBalanceInBaseCurrency(UUID accountId, LocalDate date) {
        GLAccount account = glAccountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("GL Account not found: " + accountId));

        BigDecimal balance = getBalanceAtDate(accountId, date);

        // If account is already in base currency, return as-is
        if (baseCurrency.equals(account.getCurrency())) {
            return balance;
        }

        // Convert to base currency using exchange rate
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(account.getCurrency(), baseCurrency, date);
        return balance.multiply(exchangeRate);
    }

    /**
     * Retrieves the balance change for a specific account in a particular currency between two dates.
     *
     * This method calculates the net change in an account's balance for transactions conducted
     * in a specific currency over a defined period. Unlike the general balance change method,
     * this focuses exclusively on transactions in the specified currency, making it essential
     * for multi-currency account analysis and currency-specific reporting.
     *
     * Calculation process:
     * 1. Retrieves all journal entries for the account within the date range and currency
     * 2. Sums all debit amounts in the specified currency
     * 3. Sums all credit amounts in the specified currency
     * 4. Calculates net change as (Total Debits - Total Credits)
     *
     * @param accountId The UUID of the GL account for which to calculate currency-specific balance change
     * @param currency The ISO currency code (e.g., "USD", "EUR", "GBP") to filter transactions
     * @param startDate The beginning date of the analysis period (inclusive)
     * @param endDate The ending date of the analysis period (inclusive)
     * @return GLAccountBalance containing currency-specific debit/credit totals and net change
     * @throws IllegalArgumentException if accountId is null, currency is null/empty,
     *         or if startDate is after endDate
     * @see #getBalanceChange(UUID, LocalDate, LocalDate) for total balance change across all currencies
     * @see #getBalanceInBaseCurrency(UUID, LocalDate) for base currency conversion
     */
    @Transactional(readOnly = true)
    public GLAccountBalance getBalanceChangeByCurrency(UUID accountId, String currency, LocalDate startDate,
            LocalDate endDate) {
        List<GLJournalEntry> entries = glJournalEntryRepository
                .findEntriesByAccountDateRangeAndCurrency(accountId, startDate, endDate, currency);

        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (GLJournalEntry entry : entries) {
            totalDebits = totalDebits.add(entry.getDebitAmount());
            totalCredits = totalCredits.add(entry.getCreditAmount());
        }

        GLAccountBalance balanceChange = new GLAccountBalance();
        balanceChange.setAccountId(accountId);
        balanceChange.setDebitAmount(totalDebits);
        balanceChange.setCreditAmount(totalCredits);
        balanceChange.setBalance(totalDebits.subtract(totalCredits));

        return balanceChange;
    }

    /**
     * Calculates balance from journal entries with optional date range.
     *
     * This private helper method consolidates balance calculation logic for two use cases:
     * 1. Range-based calculation: When fromDate is non-null, sums entries between fromDate and toDate
     * 2. All-time calculation: When fromDate is null, sums all entries up to toDate
     *
     * The raw accumulation uses the debit-normal formula: Σ(debits) − Σ(credits).
     * For credit-normal accounts (Liabilities, Equity, Revenue) the result is
     * negated so that a healthy credit balance is returned as a positive number.
     *
     * Multi-currency: every journal entry stores pre-computed {@code baseDebitAmount}
     * and {@code baseCreditAmount} (transaction-currency amount × exchange rate locked
     * in at posting time).  This method accumulates those base-currency fields so that
     * balances are always expressed in the system base currency, avoiding the numeric
     * mixing that would occur if raw {@code debitAmount}/{@code creditAmount} were summed
     * across entries denominated in different currencies (bug #9).
     *
     * @param accountId The UUID of the GL account
     * @param fromDate The start date (null means all-time from beginning)
     * @param toDate The end date (inclusive)
     * @return The signed balance in base currency, positive when the account carries
     *         a balance in its normal direction (debit for Assets/Expenses, credit for
     *         Liabilities/Equity/Revenue)
     */
    private BigDecimal calculateBalanceFromEntries(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        List<GLJournalEntry> entries;

        if (fromDate != null) {
            entries = glJournalEntryRepository.findEntriesByAccountAndDateRange(accountId, fromDate, toDate);
        } else {
            entries = glJournalEntryRepository.findEntriesByAccountUpToDate(accountId, toDate);
        }

        BigDecimal balance = BigDecimal.ZERO;

        for (GLJournalEntry entry : entries) {
            // Use the base-currency amounts that were locked in at posting time.
            // entry.getDebitAmount() / entry.getCreditAmount() are in the entry's own
            // transaction currency and must NOT be summed across mixed-currency entries.
            balance = balance.add(entry.getBaseDebitAmount()).subtract(entry.getBaseCreditAmount());
        }

        // Apply sign convention: credit-normal accounts (Liabilities, Equity, Revenue)
        // accumulate credits, so the raw debits-minus-credits result is negative for a
        // healthy balance.  Negate to return a positive value in the normal direction.
        GLAccount account = glAccountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("GLAccount", accountId));
        return account.getNormalBalance() == BalanceType.CREDIT ? balance.negate() : balance;
    }

    /**
     * Gets the current balance for a specific GL account with detailed information.
     *
     * This method implements an optimized balance retrieval strategy aligned with
     * the daily snapshot scheduler pattern:
     * 1. The scheduler runs at 1 AM daily and creates snapshots for the PREVIOUS day
     * 2. Calculate balance using optimized snapshot+incremental strategy:
     *    - Uses yesterday's snapshot as base (guaranteed to exist from 1 AM scheduler)
     *    - Calculates incremental changes from yesterday to today
     *
     * This ensures current, accurate balances while leveraging the daily snapshot cache
     * created by the scheduler.
     *
     * @param accountId the UUID of the account
     * @return an Optional containing the account balance if found, empty otherwise
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public Optional<GLAccountBalance> getAccountBalance(UUID accountId) {
        logger.debug("Getting account balance for: {}", accountId);

        Optional<GLAccount> accountOpt = glAccountRepository.findById(accountId);
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }

        GLAccount account = accountOpt.get();
        LocalDate today = dateTimeService.today();

        // Calculate balance using optimized strategy that leverages yesterday's snapshot
        // (created by 1 AM scheduler). This method will:
        // 1. Get yesterday's snapshot as base (guaranteed to exist)
        // 2. Calculate incremental changes from yesterday to today
        logger.debug(
                "Calculating balance for account: {} using yesterday's snapshot + incremental to today",
                accountId);

        BigDecimal currentBalance = calculateBalanceAtDate(accountId, today);

        GLAccountBalance balance = new GLAccountBalance(
                accountId,
                account.getCode(),
                account.getName(),
                currentBalance,
                BigDecimal.ZERO,
                currentBalance,
                BigDecimal.ZERO,
                BigDecimal.ZERO,
                0);
        logger.debug("Current balance for account: {} as of {}: {}", accountId, today, currentBalance);
        return Optional.of(balance);
    }

    /**
     * Gets the current balance for a GL account by its code.
     *
     * @param accountCode the account code
     * @return an Optional containing the account balance if found, empty otherwise
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public Optional<GLAccountBalance> getAccountBalanceByCode(String accountCode) {
        logger.debug("Getting account balance by code: {}", accountCode);

        Optional<GLAccount> accountOpt = glAccountRepository.findByCode(accountCode);
        if (accountOpt.isEmpty()) {
            return Optional.empty();
        }

        return getAccountBalance(accountOpt.get().getId());
    }

    /**
     * Gets balances for multiple GL accounts in a single operation.
     *
     * @param accountIds list of account UUIDs
     * @return a map of account ID to account balance
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public Map<UUID, GLAccountBalance> getAccountBalances(List<UUID> accountIds) {
        logger.debug("Getting account balances for {} accounts", accountIds.size());

        Map<UUID, GLAccountBalance> balances = new HashMap<>();

        for (UUID accountId : accountIds) {
            Optional<GLAccountBalance> balance = getAccountBalance(accountId);
            balance.ifPresent(b -> balances.put(accountId, b));
        }

        return balances;
    }

    /**
     * Generates a trial balance report for specific account types only.
     *
     * @param asOfDate the date for which to generate the trial balance
     * @param accountTypes list of account types to include in the report
     * @return a filtered trial balance report
     */
    @PreAuthorize("hasAuthority('gl:read')")

    @Transactional(readOnly = true)
    public TrialBalance getTrialBalanceByType(LocalDate asOfDate, List<GLAccountType> accountTypes) {
        logger.info("Generating trial balance by type as of: {} for types: {}", asOfDate, accountTypes);

        List<GLAccountBalance> accountBalances = new ArrayList<>();
        BigDecimal totalDebits = BigDecimal.ZERO;
        BigDecimal totalCredits = BigDecimal.ZERO;

        for (GLAccountType type : accountTypes) {
            List<GLAccount> accountsOfType = glAccountRepository.findByType(type);

            for (GLAccount account : accountsOfType) {
                Optional<GLAccountBalance> balanceOpt = getAccountBalance(account.getId());
                if (balanceOpt.isPresent()) {
                    GLAccountBalance balance = balanceOpt.get();

                    // Only include accounts with non-zero balances
                    if (balance.getCurrentBalance().compareTo(BigDecimal.ZERO) != 0) {
                        accountBalances.add(balance);

                        // Add to appropriate total based on normal balance type
                        if (account.getNormalBalance() == BalanceType.DEBIT) {
                            if (balance.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
                                totalDebits = totalDebits.add(balance.getCurrentBalance());
                            } else {
                                totalCredits = totalCredits.add(balance.getCurrentBalance().abs());
                            }
                        } else {
                            if (balance.getCurrentBalance().compareTo(BigDecimal.ZERO) > 0) {
                                totalCredits = totalCredits.add(balance.getCurrentBalance());
                            } else {
                                totalDebits = totalDebits.add(balance.getCurrentBalance().abs());
                            }
                        }
                    }
                }
            }
        }

        boolean isBalanced = totalDebits.compareTo(totalCredits) == 0;

        return new TrialBalance(asOfDate, accountBalances, totalDebits, totalCredits, isBalanced);
    }

    /**
     * Gets the closing balance for an account on a specific date.
     *
     * @param accountId the UUID of the account
     * @param date the specific date
     * @return an Optional containing the closing balance if available
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public Optional<BigDecimal> getClosingBalance(UUID accountId, LocalDate date) {
        logger.debug("Getting closing balance for account: {} on date: {}", accountId, date);

        Optional<GLDailyBalance> dailyBalance = glDailyBalanceRepository
                .findDailyBalanceByAccountAndDate(accountId, date);

        return dailyBalance.map(GLDailyBalance::getClosingBalance);
    }

    /**
     * Calculates the balance for an account as of a specific date and time.
     * This method uses daily balance snapshots when available and falls back to
     * transaction-based calculation if needed.
     *
     * @param accountId the UUID of the account
     * @param asOfDate the specific date and time
     * @return the calculated balance as of the specified date/time
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public BigDecimal calculateBalanceAsOf(UUID accountId, LocalDateTime asOfDate) {
        logger.debug("Calculating balance as of: {} for account: {}", asOfDate, accountId);

        LocalDate date = asOfDate.toLocalDate();

        // Try to get the daily balance for the specific date
        Optional<GLDailyBalance> dailyBalance = glDailyBalanceRepository
                .findDailyBalanceByAccountAndDate(accountId, date);

        if (dailyBalance.isPresent()) {
            return dailyBalance.get().getClosingBalance();
        }

        // If no daily balance exists for the date, try to get the latest before that date
        Optional<GLDailyBalance> latestBefore = glDailyBalanceRepository
                .findFirstByGlAccount_IdAndBalanceDateBeforeOrderByBalanceDateDesc(accountId, date);

        if (latestBefore.isPresent()) {
            return latestBefore.get().getClosingBalance();
        }

        // If no daily balances exist, return zero
        return BigDecimal.ZERO;
    }

    // Helper method to convert GLDailyBalance to DailyBalance DTO
    private DailyBalanceSnapshot convertToDTO(GLDailyBalance entity) {
        DailyBalanceSnapshot dto = new DailyBalanceSnapshot();
        dto.setAccountId(entity.getGlAccount().getId());
        dto.setBalanceDate(entity.getBalanceDate());
        dto.setOpeningBalance(entity.getOpeningBalance());
        dto.setDebitAmount(entity.getDebitAmount());
        dto.setCreditAmount(entity.getCreditAmount());
        dto.setClosingBalance(entity.getClosingBalance());
        dto.setTransactionCount(entity.getTransactionCount());
        return dto;
    }

    /**
     * Calculates the total debit amount for an account within a date range.
     *
     * @param accountId The UUID of the account.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return The total debit amount.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public BigDecimal getTotalDebitsForAccount(UUID accountId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating total debits for account: {} from {} to {}", accountId, startDate, endDate);

        List<GLJournalEntry> entries = glJournalEntryRepository
                .findEntriesByAccountAndDateRange(accountId, startDate, endDate);

        return entries.stream().filter(GLJournalEntry::isDebit).map(GLJournalEntry::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total credit amount for an account within a date range.
     *
     * @param accountId The UUID of the account.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return The total credit amount.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public BigDecimal getTotalCreditsForAccount(UUID accountId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating total credits for account: {} from {} to {}", accountId, startDate, endDate);

        List<GLJournalEntry> entries = glJournalEntryRepository
                .findEntriesByAccountAndDateRange(accountId, startDate, endDate);

        return entries.stream().filter(GLJournalEntry::isCredit).map(GLJournalEntry::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the net balance (debits - credits) for an account within a date range.
     *
     * @param accountId The UUID of the account.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return The net balance amount.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public BigDecimal getNetBalanceForAccount(UUID accountId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Calculating net balance for account: {} from {} to {}", accountId, startDate, endDate);

        BigDecimal totalDebits = getTotalDebitsForAccount(accountId, startDate, endDate);
        BigDecimal totalCredits = getTotalCreditsForAccount(accountId, startDate, endDate);

        return totalDebits.subtract(totalCredits);
    }

    /**
     * Counts the number of journal entries for an account within a date range.
     *
     * @param accountId The UUID of the account.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return The count of journal entries.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public long countEntriesForAccount(UUID accountId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Counting journal entries for account: {} from {} to {}", accountId, startDate, endDate);

        List<GLJournalEntry> entries = glJournalEntryRepository
                .findEntriesByAccountAndDateRange(accountId, startDate, endDate);
        return entries.size();
    }

    /**
     * Calculates the total debit amount for an account within a date range, filtered by currency.
     *
     * @param accountId The UUID of the account.
     * @param currency The currency code to filter by.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return The total debit amount in the specified currency.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalDebitsByCurrency(UUID accountId, String currency, LocalDate startDate,
            LocalDate endDate) {
        logger.debug(
                "Calculating total debits for account: {} in currency: {} from {} to {}",
                accountId,
                currency,
                startDate,
                endDate);

        List<GLJournalEntry> entries = glJournalEntryRepository
                .findEntriesByAccountDateRangeAndCurrency(accountId, startDate, endDate, currency);

        return entries.stream().filter(GLJournalEntry::isDebit).map(GLJournalEntry::getDebitAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the total credit amount for an account within a date range, filtered by currency.
     *
     * @param accountId The UUID of the account.
     * @param currency The currency code to filter by.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return The total credit amount in the specified currency.
     */
    @Transactional(readOnly = true)
    public BigDecimal getTotalCreditsByCurrency(UUID accountId, String currency, LocalDate startDate,
            LocalDate endDate) {
        logger.debug(
                "Calculating total credits for account: {} in currency: {} from {} to {}",
                accountId,
                currency,
                startDate,
                endDate);

        List<GLJournalEntry> entries = glJournalEntryRepository
                .findEntriesByAccountDateRangeAndCurrency(accountId, startDate, endDate, currency);

        return entries.stream().filter(GLJournalEntry::isCredit).map(GLJournalEntry::getCreditAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Calculates the exchange gain or loss for a specific transaction.
     *
     * @param transactionId The UUID of the transaction.
     * @return The calculated gain (positive) or loss (negative).
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateExchangeGainLoss(UUID transactionId) {
        logger.debug("Calculating exchange gain/loss for transaction: {}", transactionId);

        Optional<GLTransaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            logger.warn("Transaction not found: {}", transactionId);
            return BigDecimal.ZERO;
        }

        GLTransaction transaction = transactionOpt.get();

        // If transaction is in base currency, no gain/loss
        if (baseCurrency.equals(transaction.getCurrency())) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalGainLoss = BigDecimal.ZERO;

        for (GLJournalEntry entry : transaction.getJournalEntries()) {
            // Calculate gain/loss for each entry
            BigDecimal entryAmount = entry.isDebit() ? entry.getDebitAmount() : entry.getCreditAmount();
            BigDecimal baseAmount = entry.isDebit() ? entry.getBaseDebitAmount() : entry.getBaseCreditAmount();

            // Get current exchange rate
            BigDecimal currentRate = exchangeRateService
                    .getExchangeRate(entry.getCurrency(), baseCurrency, dateTimeService.today());

            // Calculate what the base amount would be at current rate
            BigDecimal currentBaseAmount = entryAmount.multiply(currentRate);

            // Calculate gain/loss (difference between recorded and current base amounts)
            BigDecimal entryGainLoss = currentBaseAmount.subtract(baseAmount);

            // For credit entries, reverse the sign
            if (entry.isCredit()) {
                entryGainLoss = entryGainLoss.negate();
            }

            totalGainLoss = totalGainLoss.add(entryGainLoss);
        }

        logger.debug("Exchange gain/loss for transaction {}: {} {}", transactionId, totalGainLoss, baseCurrency);

        return totalGainLoss;
    }

    /**
     * Gets the base (functional) currency of the ledger.
     *
     * @return The 3-letter ISO currency code.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    public String getBaseCurrency() {
        return baseCurrency;
    }

    /**
     * Converts an amount from a transaction currency to the base currency.
     *
     * @param amount    The amount in transaction currency.
     * @param currency  The transaction currency code.
     * @param valueDate The date for picking the exchange rate.
     * @return The amount in base currency.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public BigDecimal convertToBaseCurrency(BigDecimal amount, String currency, LocalDate valueDate) {
        logger.debug("Converting {} {} to {} on date: {}", amount, currency, baseCurrency, valueDate);

        if (amount == null || amount.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        // If already in base currency, return as-is
        if (baseCurrency.equals(currency)) {
            return amount;
        }

        // Get exchange rate and convert
        BigDecimal exchangeRate = exchangeRateService.getExchangeRate(currency, baseCurrency, valueDate);
        BigDecimal convertedAmount = amount.multiply(exchangeRate);

        logger.debug(
                "Converted {} {} to {} {} (rate: {})",
                amount,
                currency,
                convertedAmount,
                baseCurrency,
                exchangeRate);

        return convertedAmount;
    }

}
