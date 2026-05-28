package com.openfinova.banking.customer.account.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.account.api.dto.AccountBalanceView;
import com.openfinova.banking.customer.account.api.dto.BalanceHistoryResponse;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import com.openfinova.banking.customer.account.api.entity.LimitType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountTransaction;
import com.openfinova.banking.customer.account.entity.GLAccountMapping;
import com.openfinova.banking.customer.account.repository.AccountLimitRepository;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import com.openfinova.banking.customer.account.repository.AccountTransactionRepository;
import com.openfinova.banking.customer.account.repository.GLAccountMappingRepository;
import com.openfinova.banking.gl.api.GeneralLedgerService;
import com.openfinova.banking.setup.api.DateTimeService;

import jakarta.persistence.EntityNotFoundException;

/**
 * Implementation of AccountBalanceService for managing customer account balances.
 *
 * This service provides comprehensive balance management including:
 * - Real-time balance calculations from GL accounts
 * - Historical balance reconstruction from transaction history
 * - Balance trend analysis with statistical calculations
 * - Consistency validation between customer and GL accounts
 *
 * Balance Calculation Strategy:
 * - Current balances are maintained in the Account entity for performance
 * - Historical balances are calculated by working backwards from current balance
 * - GL account balances can be used to refresh/validate customer account balances
 * - Transaction history is used for point-in-time and trend analysis
 *
 * AVAILABLE BALANCE CALCULATION:
 * Available balance = Ledger Balance - Administrative Holds - Transaction Reservations - Pending Debits
 *
 * This service centralizes balance calculations considering:
 * - AccountHolds (long-lived administrative holds) via AccountHoldService
 * - Denormalized short-lived transaction reservations on Account.transactionReservedAmount
 *
 * This ensures consistent balance calculations across the system.
 *
 * MODULE BOUNDARIES:
 * - Uses GeneralLedgerFacade to access GL module functionality (not internal services)
 * - Maintains proper separation of concerns between Account and GL modules
 */
@Service
@Transactional
public class AccountBalanceService {

    private static final Logger logger = LoggerFactory.getLogger(AccountBalanceService.class);

    private final AccountRepository accountRepository;
    private final AccountLimitRepository accountLimitRepository;
    private final GLAccountMappingRepository glAccountMappingRepository;
    private final GeneralLedgerService generalLedgerFacade;
    private final AccountHoldService accountHoldService;
    private final AccountTransactionRepository accountTransactionRepository;
    private final DateTimeService dateTimeService;

    /**
     * Constructs a new AccountBalanceService with the necessary dependencies.
     *
     * @param accountRepository the repository for managing account entities
     * @param glAccountMappingRepository the repository for managing general ledger mappings
     * @param generalLedgerService the service for interacting with the general ledger
     * @param accountHoldService the service for managing account holds
     * @param accountTransactionRepository the repository for accessing transaction records
     * @param dateTimeService the service providing date and time utilities
     */
    public AccountBalanceService(AccountRepository accountRepository, AccountLimitRepository accountLimitRepository,
            GLAccountMappingRepository glAccountMappingRepository, GeneralLedgerService generalLedgerService,
            AccountHoldService accountHoldService, AccountTransactionRepository accountTransactionRepository,
            DateTimeService dateTimeService) {
        this.accountRepository = accountRepository;
        this.accountLimitRepository = accountLimitRepository;
        this.glAccountMappingRepository = glAccountMappingRepository;
        this.generalLedgerFacade = generalLedgerService;
        this.accountHoldService = accountHoldService;
        this.accountTransactionRepository = accountTransactionRepository;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Retrieves the current balance views for all accounts belonging to a specific user.
     *
     * @param userProfileId the unique identifier of the user profile
     * @return a list of account balance views containing current ledger and available balances
     */
    @Transactional(readOnly = true)
    public List<AccountBalanceView> getBalancesForUser(UUID userProfileId) {
        logger.debug("Getting balances for user: {}", userProfileId);

        List<Account> accounts = accountRepository.findAllByPrimaryUserProfileId(userProfileId);

        return accounts.stream().map(this::buildBalanceView).toList();
    }

    /**
     * Reconstructs the historical balance of a specific account as of the end of a given date.
     * The available balance is set equal to the ledger balance since holds are not tracked historically.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @param asOfDate the date for which the balance should be calculated
     * @return the reconstructed historical account balance view
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public AccountBalanceView getBalanceAsOfDate(UUID customerAccountId, LocalDate asOfDate) {
        logger.debug("Getting balance for account {} as of date {}", customerAccountId, asOfDate);

        Account account = accountRepository.findById(customerAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + customerAccountId));

        // For historical balance, we need to calculate from transactions up to that date
        LocalDateTime endOfDay = asOfDate.atTime(LocalTime.MAX);

        AccountBalanceView view = new AccountBalanceView();
        view.setAccountId(account.getId());
        view.setAccountNumber(account.getAccountNumber());
        view.setCurrency(account.getCurrency());
        view.setLastUpdated(endOfDay);

        // Calculate historical balance from transactions
        BigDecimal historicalBalance = calculateHistoricalBalance(customerAccountId, endOfDay);
        view.setCurrentBalance(historicalBalance);
        view.setAvailableBalance(historicalBalance); // No holds in historical view
        view.setPendingCredits(BigDecimal.ZERO);
        view.setPendingDebits(BigDecimal.ZERO);
        view.setReservedAmount(BigDecimal.ZERO);

        return view;
    }

    /**
     * Refreshes the balance of a specific account by recalculating it from the general ledger
     * and subtracting any reserved amounts. Updates the ledger and available balance on the account entity.
     *
     * @param customerAccountId the unique identifier of the customer account to refresh
     * @throws IllegalArgumentException if the account is not found
     */
    public void refreshBalanceView(UUID customerAccountId) {
        logger.debug("Refreshing balance view for account: {}", customerAccountId);

        Account account = accountRepository.findById(customerAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + customerAccountId));

        // Recalculate balance from GL accounts
        BigDecimal glBalance = calculateBalanceFromGLAccounts(customerAccountId);

        // Update account balances
        account.setLedgerBalance(glBalance);

        // Recalculate available balance
        BigDecimal reservedAmount = calculateReservedAmount(customerAccountId);
        account.setAvailableBalance(glBalance.subtract(reservedAmount));

        accountRepository.save(account);

        logger.info(
                "Refreshed balance for account {}: ledger={}, available={}",
                customerAccountId,
                glBalance,
                account.getAvailableBalance());
    }

    /**
     * Iterates through all accounts in the system and triggers a balance refresh for each one.
     * Any errors encountered during individual account refreshes are logged but do not stop the process.
     *
     * @return the total number of accounts successfully refreshed
     */
    public int refreshAllBalanceViews() {
        logger.info("Starting refresh of all balance views");

        List<Account> allAccounts = accountRepository.findAll();
        int refreshedCount = 0;

        for (Account account : allAccounts) {
            try {
                refreshBalanceView(account.getId());
                refreshedCount++;
            } catch (Exception e) {
                logger.error("Failed to refresh balance for account {}: {}", account.getId(), e.getMessage());
            }
        }

        logger.info("Completed refresh of {} balance views", refreshedCount);
        return refreshedCount;
    }

    /**
     * Retrieves the daily balance history for an account within a specified date range,
     * including a statistical trend analysis of the balance changes.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     * @return a comprehensive response containing daily balance entries and a trend analysis
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public BalanceHistoryResponse getBalanceHistory(UUID customerAccountId, LocalDate startDate, LocalDate endDate) {
        logger.debug("Getting balance history for account {} from {} to {}", customerAccountId, startDate, endDate);

        // Verify account exists
        accountRepository.findById(customerAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + customerAccountId));

        BalanceHistoryResponse response = new BalanceHistoryResponse(customerAccountId, startDate, endDate);

        // Build daily balance history
        List<BalanceHistoryResponse.BalanceHistoryEntry> history = buildBalanceHistory(
                customerAccountId,
                startDate,
                endDate);
        response.setBalanceHistory(history);

        // Calculate trend analysis
        BalanceHistoryResponse.BalanceTrendAnalysis trendAnalysis = calculateTrendAnalysis(history);
        response.setTrendAnalysis(trendAnalysis);

        return response;
    }

    /**
     * Retrieves a detailed balance view for an account, including the breakdown of its mapped
     * general ledger component balances and their respective weights.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @return a detailed account balance view with component breakdown
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public AccountBalanceView getDetailedBalance(UUID customerAccountId) {
        logger.debug("Getting detailed balance for account: {}", customerAccountId);

        Account account = accountRepository.findById(customerAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + customerAccountId));

        AccountBalanceView view = buildBalanceView(account);

        // Add GL component breakdown
        List<GLAccountMapping> mappings = glAccountMappingRepository
                .findByCustomerAccountIdAndIsActiveTrue(customerAccountId);

        List<AccountBalanceView.GLComponentBalance> components = new ArrayList<>();
        for (GLAccountMapping mapping : mappings) {
            // Use facade to get GL account information
            generalLedgerFacade.getAccountById(mapping.getGlAccountId()).ifPresent(glAccount -> {
                // Get current balance from facade
                BigDecimal balance = generalLedgerFacade.getAccountBalance(glAccount.getId())
                        .map(b -> b.getCurrentBalance()).orElse(BigDecimal.ZERO);

                AccountBalanceView.GLComponentBalance component = new AccountBalanceView.GLComponentBalance();
                component.setGlAccountId(glAccount.getId());
                component.setMappingType(mapping.getMappingType().name());
                component.setBalance(balance);
                component.setWeight(mapping.getWeight());
                components.add(component);
            });
        }

        view.setComponents(components);
        view.setGlAccountCount(components.size());

        return view;
    }

    /**
     * Validates that the ledger balance stored on the account entity matches the calculated
     * balance derived from the associated general ledger accounts, allowing for small rounding differences.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @return true if the balances are consistent, false if an inconsistency is detected or the account is not found
     */
    @Transactional(readOnly = true)
    public boolean validateBalanceConsistency(UUID customerAccountId) {
        logger.debug("Validating balance consistency for account: {}", customerAccountId);

        Account account = accountRepository.findById(customerAccountId).orElse(null);
        if (account == null) {
            return false;
        }

        BigDecimal accountBalance = account.getLedgerBalance();
        BigDecimal glBalance = calculateBalanceFromGLAccounts(customerAccountId);

        // Allow for small rounding differences (0.01)
        BigDecimal difference = accountBalance.subtract(glBalance).abs();
        boolean consistent = difference.compareTo(new BigDecimal("0.01")) <= 0;

        if (!consistent) {
            logger.warn(
                    "Balance inconsistency detected for account {}: account={}, GL={}, diff={}",
                    customerAccountId,
                    accountBalance,
                    glBalance,
                    difference);
        }

        return consistent;
    }

    /**
     * Retrieves the current available balance for a specific account.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @return the available balance amount
     * @throws EntityNotFoundException if the account is not found
     */
    @Transactional(readOnly = true)
    public BigDecimal getAvailableBalance(UUID customerAccountId) {
        logger.debug("Getting available balance for account: {}", customerAccountId);

        Account account = accountRepository.findById(customerAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + customerAccountId));

        return account.getAvailableBalance();
    }

    /**
     * Calculates the balance trends for an account over a specified number of days looking back from today.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @param days the number of days to analyze
     * @return a response containing the balance history and trend analysis
     */
    @Transactional(readOnly = true)
    public BalanceHistoryResponse calculateBalanceTrends(UUID customerAccountId, int days) {
        logger.debug("Calculating balance trends for account {} over {} days", customerAccountId, days);

        LocalDate endDate = dateTimeService.today();
        LocalDate startDate = endDate.minusDays(days);

        return getBalanceHistory(customerAccountId, startDate, endDate);
    }

    /**
     * Directly applies delta adjustments to an account's ledger and available balances.
     *
     * @param accountId the unique identifier of the customer account to update
     * @param ledgerDelta the amount to add to the ledger balance
     * @param availableDelta the amount to add to the available balance
     * @throws IllegalArgumentException if the account is not found
     */
    public void updateBalances(UUID accountId, BigDecimal ledgerDelta, BigDecimal availableDelta) {
        logger.debug(
                "Updating balances for account {}: ledger delta={}, available delta={}",
                accountId,
                ledgerDelta,
                availableDelta);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        BigDecimal newLedgerBalance = account.getLedgerBalance().add(ledgerDelta);
        BigDecimal newAvailableBalance = account.getAvailableBalance().add(availableDelta);

        account.setLedgerBalance(newLedgerBalance);
        account.setAvailableBalance(newAvailableBalance);

        accountRepository.save(account);

        logger.info(
                "Updated balances for account {}: ledger={}, available={}",
                accountId,
                newLedgerBalance,
                newAvailableBalance);
    }

    /**
     * Constructs a standard AccountBalanceView object from an account entity, including
     * its current available balance and calculated reserved amounts.
     *
     * @param account the account entity
     * @return a populated account balance view
     */
    private AccountBalanceView buildBalanceView(Account account) {
        AccountBalanceView view = new AccountBalanceView();
        view.setAccountId(account.getId());
        view.setAccountNumber(account.getAccountNumber());
        view.setCurrentBalance(account.getLedgerBalance());
        view.setAvailableBalance(account.getAvailableBalance());
        view.setCurrency(account.getCurrency());
        view.setLastUpdated(dateTimeService.now());

        // Calculate pending amounts (simplified - would need transaction status in real implementation)
        view.setPendingCredits(BigDecimal.ZERO);
        view.setPendingDebits(BigDecimal.ZERO);

        BigDecimal reservedAmount = calculateReservedAmount(account);
        view.setReservedAmount(reservedAmount);

        return view;
    }

    /**
     * Calculates the total amount reserved/held for an account.
     *
     * CENTRALIZED BALANCE CALCULATION:
     * This method combines both types of holds to provide a complete picture:
     * 1. AccountHolds - Long-lived administrative holds (court orders, fraud, etc.)
     * 2. BalanceReservations - Denormalized on account.transactionReservedAmount
     *
     * This ensures available balance calculations are consistent across the system.
     *
     * @param account the account entity
     * @return total amount held/reserved (administrative holds + transaction reservations)
     */
    private BigDecimal calculateReservedAmount(Account account) {
        UUID customerAccountId = account.getId();
        logger.debug("Calculating total reserved amount for account: {}", customerAccountId);

        BigDecimal administrativeHolds = accountHoldService.getTotalHoldAmount(customerAccountId);
        BigDecimal transactionReservations = account.getTransactionReservedAmount() != null
                ? account.getTransactionReservedAmount()
                : BigDecimal.ZERO;
        BigDecimal totalReserved = administrativeHolds
                .add(transactionReservations != null ? transactionReservations : BigDecimal.ZERO);

        logger.debug(
                "Account {} reserved amounts - Administrative holds: {}, Transaction reservations: {}, Total: {}",
                customerAccountId,
                administrativeHolds,
                transactionReservations,
                totalReserved);

        return totalReserved;
    }

    private BigDecimal calculateReservedAmount(UUID customerAccountId) {
        Account account = accountRepository.findById(customerAccountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + customerAccountId));
        return calculateReservedAmount(account);
    }

    /**
     * Updates the denormalized transaction-reserved snapshot for the given account and refreshes
     * available balance using the current administrative hold total.
     */
    public void syncTransactionReservedAmount(UUID accountId, BigDecimal reservedAmount) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));
        BigDecimal normalizedReservedAmount = reservedAmount != null ? reservedAmount.max(BigDecimal.ZERO)
                : BigDecimal.ZERO;
        account.setTransactionReservedAmount(normalizedReservedAmount);

        BigDecimal administrativeHolds = accountHoldService.getTotalHoldAmount(accountId);
        account.setAvailableBalance(
                account.getLedgerBalance().subtract(administrativeHolds.add(normalizedReservedAmount)));
        accountRepository.save(account);
    }

    /**
     * Calculates the total balance for an account by aggregating the balances of its mapped
     * general ledger accounts, taking mapping weights into consideration.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @return the calculated balance from the general ledger
     */
    private BigDecimal calculateBalanceFromGLAccounts(UUID customerAccountId) {
        List<GLAccountMapping> mappings = glAccountMappingRepository
                .findByCustomerAccountIdAndIsActiveTrue(customerAccountId);

        // Use arrays to allow modification in lambda (workaround for effectively final)
        final BigDecimal[] totalBalance = { BigDecimal.ZERO };
        final int[] totalWeight = { 0 };

        for (GLAccountMapping mapping : mappings) {
            // Use facade to get GL account information
            generalLedgerFacade.getAccountById(mapping.getGlAccountId()).ifPresent(glAccount -> {
                // Get current balance from facade
                BigDecimal glBalance = generalLedgerFacade.getAccountBalance(glAccount.getId())
                        .map(b -> b.getCurrentBalance()).orElse(BigDecimal.ZERO);
                int weight = mapping.getWeight();

                totalBalance[0] = totalBalance[0].add(glBalance.multiply(BigDecimal.valueOf(weight)));
                totalWeight[0] += weight;
            });
        }

        // Calculate weighted average if there are multiple mappings
        if (totalWeight[0] > 0 && mappings.size() > 1) {
            return totalBalance[0].divide(BigDecimal.valueOf(totalWeight[0]), 4, RoundingMode.HALF_UP);
        }

        return totalBalance[0];
    }

    /**
     * Reconstructs the historical balance of an account at a specific point in time by taking the current
     * balance and reversing the effects of all posted or completed transactions that occurred after that time.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @param asOfDateTime the target date and time for the historical balance
     * @return the calculated historical balance
     */
    private BigDecimal calculateHistoricalBalance(UUID customerAccountId, LocalDateTime asOfDateTime) {
        // Get current balance
        Account account = accountRepository.findById(customerAccountId).orElse(null);
        if (account == null) {
            return BigDecimal.ZERO;
        }

        BigDecimal currentBalance = account.getLedgerBalance();
        LocalDateTime now = dateTimeService.now();

        // Stream all transactions after asOfDateTime (fetch size hint avoids loading all into memory)
        try (Stream<AccountTransaction> stream = accountTransactionRepository
                .streamByAccountAndDateRange(customerAccountId, asOfDateTime, now)) {
            return stream.filter(tx -> "POSTED".equals(tx.getStatus()) || "COMPLETED".equals(tx.getStatus())).reduce(
                    currentBalance,
                    (balance, tx) -> isCredit(tx.getTransactionType()) ? balance.subtract(tx.getAmount())
                            : balance.add(tx.getAmount()),
                    (a, b) -> a);
        }
    }

    /**
     * Builds a list of daily balance history entries for an account over a specific date range,
     * applying transaction changes sequentially to determine the balance at the end of each day.
     *
     * @param customerAccountId the unique identifier of the customer account
     * @param startDate the start date of the period
     * @param endDate the end date of the period
     * @return a list of daily balance history entries
     */
    private List<BalanceHistoryResponse.BalanceHistoryEntry> buildBalanceHistory(UUID customerAccountId,
            LocalDate startDate, LocalDate endDate) {

        List<BalanceHistoryResponse.BalanceHistoryEntry> history = new ArrayList<>();
        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        // Stream all transactions in the date range (fetch size hint avoids loading all into memory)
        List<AccountTransaction> transactions;
        try (Stream<AccountTransaction> stream = accountTransactionRepository
                .streamByAccountAndDateRange(customerAccountId, startDateTime, endDateTime)) {
            transactions = stream.toList();
        }

        // Get the starting balance (balance before the start date)
        BigDecimal startingBalance = calculateHistoricalBalance(customerAccountId, startDateTime);

        // Group transactions by date
        Map<LocalDate, List<AccountTransaction>> transactionsByDate = new HashMap<>();
        for (AccountTransaction transaction : transactions) {
            LocalDate txDate = transaction.getTransactionDate().toLocalDate();
            transactionsByDate.computeIfAbsent(txDate, k -> new ArrayList<>()).add(transaction);
        }

        // Build daily balance entries
        BigDecimal runningBalance = startingBalance;
        LocalDate currentDate = startDate;

        while (!currentDate.isAfter(endDate)) {
            // Calculate balance change for this day
            List<AccountTransaction> dayTransactions = transactionsByDate.getOrDefault(currentDate, new ArrayList<>());
            BigDecimal dayChange = BigDecimal.ZERO;

            for (AccountTransaction tx : dayTransactions) {
                // Only include posted/completed transactions
                if ("POSTED".equals(tx.getStatus()) || "COMPLETED".equals(tx.getStatus())) {
                    BigDecimal txAmount = tx.getAmount();

                    // Apply transaction based on type (credit increases balance, debit decreases)
                    if (isCredit(tx.getTransactionType())) {
                        dayChange = dayChange.add(txAmount);
                    } else {
                        dayChange = dayChange.subtract(txAmount);
                    }
                }
            }

            // Update running balance
            runningBalance = runningBalance.add(dayChange);

            // Create history entry
            BalanceHistoryResponse.BalanceHistoryEntry entry = new BalanceHistoryResponse.BalanceHistoryEntry(
                    currentDate,
                    runningBalance);
            entry.setChange(dayChange);

            // Add change reason if there were transactions
            if (!dayTransactions.isEmpty()) {
                entry.setChangeReason(dayTransactions.size() + " transaction(s)");
            }

            history.add(entry);
            currentDate = currentDate.plusDays(1);
        }

        return history;
    }

    /**
     * Determines if a transaction type represents a credit (increases balance).
     *
     * @param type the transaction type to evaluate
     * @return true if the transaction type is a credit, false if it is a debit
     */
    private boolean isCredit(AccountTransactionType type) {
        return switch (type) {
            case DEPOSIT, TRANSFER_IN, INTEREST_CREDIT, ADJUSTMENT -> true;
            case WITHDRAWAL, TRANSFER_OUT, FEE, INTEREST_CHARGE -> false;
        };
    }

    /**
     * Performs a statistical trend analysis on a set of historical balance entries,
     * calculating averages, maximums, minimums, and overall trend direction.
     *
     * @param history the list of historical balance entries to analyze
     * @return an analysis object detailing balance trends
     */
    private BalanceHistoryResponse.BalanceTrendAnalysis calculateTrendAnalysis(
            List<BalanceHistoryResponse.BalanceHistoryEntry> history) {

        BalanceHistoryResponse.BalanceTrendAnalysis analysis = new BalanceHistoryResponse.BalanceTrendAnalysis();

        if (history.isEmpty()) {
            return analysis;
        }

        // Calculate statistics
        BigDecimal sum = BigDecimal.ZERO;
        BigDecimal min = history.get(0).getBalance();
        BigDecimal max = history.get(0).getBalance();

        for (BalanceHistoryResponse.BalanceHistoryEntry entry : history) {
            BigDecimal balance = entry.getBalance();
            sum = sum.add(balance);

            if (balance.compareTo(min) < 0) {
                min = balance;
            }
            if (balance.compareTo(max) > 0) {
                max = balance;
            }
        }

        BigDecimal average = sum.divide(BigDecimal.valueOf(history.size()), 4, RoundingMode.HALF_UP);

        analysis.setAverageBalance(average);
        analysis.setMinimumBalance(min);
        analysis.setMaximumBalance(max);

        // Calculate total change
        if (history.size() > 1) {
            BigDecimal firstBalance = history.get(0).getBalance();
            BigDecimal lastBalance = history.get(history.size() - 1).getBalance();
            BigDecimal totalChange = lastBalance.subtract(firstBalance);

            analysis.setTotalChange(totalChange);

            BigDecimal avgDailyChange = totalChange
                    .divide(BigDecimal.valueOf(history.size() - 1), 4, RoundingMode.HALF_UP);
            analysis.setAverageDailyChange(avgDailyChange);

            // Determine trend
            if (totalChange.compareTo(BigDecimal.ZERO) > 0) {
                analysis.setTrend("INCREASING");
            } else if (totalChange.compareTo(BigDecimal.ZERO) < 0) {
                analysis.setTrend("DECREASING");
            } else {
                analysis.setTrend("STABLE");
            }
        } else {
            analysis.setTotalChange(BigDecimal.ZERO);
            analysis.setAverageDailyChange(BigDecimal.ZERO);
            analysis.setTrend("STABLE");
        }

        return analysis;
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('account:read', 'service:account:read')")
    public boolean hasSufficientBalance(UUID accountId, BigDecimal amount) {
        return accountRepository.findById(accountId).map(account -> hasSufficientFunds(account, amount)).orElse(false);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAnyAuthority('account:read', 'service:account:read')")
    public boolean hasSufficientBalanceUnderLock(UUID accountId, BigDecimal amount) {
        return accountRepository.findByIdWithLock(accountId).map(account -> hasSufficientFunds(account, amount))
                .orElse(false);
    }

    private boolean hasSufficientFunds(Account account, BigDecimal amount) {
        BigDecimal available = account.getAvailableBalance();
        BigDecimal overdraftLimit = getOverdraftLimit(account.getId());
        BigDecimal effectiveLimit = available.add(overdraftLimit);
        return effectiveLimit.compareTo(amount) >= 0;
    }

    private BigDecimal getOverdraftLimit(UUID accountId) {
        return accountLimitRepository.findActiveEffectiveLimitsByAccount(accountId, dateTimeService.instant()).stream()
                .filter(l -> l.getLimitType() == LimitType.OVERDRAFT_LIMIT)
                .map(l -> l.getMaxAmount() != null ? l.getMaxAmount() : BigDecimal.ZERO).max(BigDecimal::compareTo)
                .orElse(BigDecimal.ZERO);
    }
}
