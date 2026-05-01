package com.openfinova.banking.gl.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.gl.api.dto.AccountReconciliationResult;
import com.openfinova.banking.gl.api.dto.BalanceReconciliationReport;
import com.openfinova.banking.gl.api.dto.DailyBalanceSnapshot;
import com.openfinova.banking.gl.api.dto.FiscalPeriodDTO;
import com.openfinova.banking.gl.api.dto.GLAccountBalance;
import com.openfinova.banking.gl.api.dto.GLAccountDTO;
import com.openfinova.banking.gl.api.dto.GLJournalEntryDTO;
import com.openfinova.banking.gl.api.dto.GLTransactionDTO;
import com.openfinova.banking.gl.api.dto.PostTransactionCommand;
import com.openfinova.banking.gl.api.dto.SnapshotsComplianceReport;
import com.openfinova.banking.gl.api.dto.ValidationResult;
import com.openfinova.banking.gl.api.entity.FiscalPeriodStatus;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;

/**
 * Façade interface for General Ledger operations.
 * This interface defines the contract for GL operations used by other modules.
 * Implementation resides in general-ledger module to follow Module Contract
 * Pattern.
 *
 * Note: Implementation types are kept internal to GL module.
 * Callers should depend on this interface and let Spring inject the concrete
 * implementation.
 *
 * Used by:
 * - Account module: Balance inquiries, account validation, reconciliation
 * - TP module: Transaction posting, fiscal period validation, reversals
 */
public interface GeneralLedgerService {

    /**
     * Retrieves a GL account by its unique identifier.
     * Used by: Account module (account validation), TP module (transaction posting)
     *
     * @param id the account ID
     * @return optional GL account DTO
     */
    Optional<GLAccountDTO> getAccountById(UUID id);

    /**
     * Retrieves a GL account by its internal code.
     * Used by: Account module (account mapping), TP module (clearing accounts)
     *
     * @param code the account code
     * @return optional GL account DTO
     */
    Optional<GLAccountDTO> findByCode(String code);

    /**
     * Checks if a GL account is active and available for posting.
     * Used by: Account module (validation), TP module (transaction posting)
     *
     * @param accountId the account ID
     * @return true if account is active for posting
     */
    boolean isAccountActiveForPosting(UUID accountId);

    /**
     * Validates if a GL account is eligible for posting.
     * Used by: Account module (validation), TP module (transaction posting)
     *
     * @param accountId the account ID
     * @throws IllegalArgumentException if account is not eligible
     */
    void validateAccountForPosting(UUID accountId);

    /**
     * Gets all active accounts that can accept journal entries.
     * Used by: Account module (account setup), TP module (account mapping)
     *
     * @return list of postable GL account DTOs
     */
    List<GLAccountDTO> getPostableAccounts();

    /**
     * Resolves an operational GL account by type.
     * Currency conversion is handled at the GL posting level.
     * Used by: TP module (fee and contra account resolution)
     *
     * @param type the operational GL account type
     * @return the operational GL account ID
     */
    UUID getOperationalGLAccount(OperationalGLAccountType type);

    /**
     * Resolves an operational GL account by type.
     * Used by: TP module (fee and contra account resolution)
     *
     * @param operationalGLAccountType the operational type (enum name as string)
     * @return the operational GL account ID
     */
    UUID getOperationalGLAccount(String operationalGLAccountType);

    /**
     * Gets accounts by type with optional status filter.
     * Used by: Account module (account categorization), TP module (clearing
     * accounts)
     *
     * @param accountType the GL account type (enum name as string)
     * @param status      optional account status (enum name as string)
     * @return list of GL account DTOs
     */
    List<GLAccountDTO> getAccountsByType(String accountType, String status);

    /**
     * Gets accounts by type with optional status filter.
     * Used by: Account module (account categorization), TP module (clearing
     * accounts)
     *
     * @param accountType the GL account type
     * @param status      optional account status
     * @return list of GL account DTOs
     */
    List<GLAccountDTO> getAccountsByType(GLAccountType accountType, GLAccountStatus status);

    /**
     * Gets accounts by currency.
     * Used by: Account module (multi-currency), TP module (currency-specific
     * accounts)
     *
     * @param currency the currency code
     * @return list of GL account DTOs
     */
    List<GLAccountDTO> getAccountsByCurrency(String currency);

    /**
     * Searches accounts by name or code.
     * Used by: Account module (account lookup), TP module (account discovery)
     *
     * @param searchTerm the search term
     * @return list of matching GL account DTOs
     */
    List<GLAccountDTO> searchAccounts(String searchTerm);

    /**
     * Posts a complete transaction containing multiple journal entries.
     * Used by: TP module (transaction posting), Account module (interest posting)
     *
     * @param command the post transaction command
     * @return the posted GL transaction DTO
     */
    GLTransactionDTO postTransaction(PostTransactionCommand command);

    /**
     * Retrieves a transaction by its unique ID.
     * Used by: TP module (transaction lookup), Account module (transaction inquiry)
     *
     * @param id the transaction ID
     * @return optional GL transaction DTO
     */
    Optional<GLTransactionDTO> getTransactionById(UUID id);

    /**
     * Retrieves a transaction by its external reference ID.
     * Used by: TP module (idempotency checks), Account module (transaction
     * matching)
     *
     * @param referenceId the external reference ID
     * @return optional GL transaction DTO
     */
    Optional<GLTransactionDTO> getTransactionByReference(String referenceId);

    /**
     * Validates that a transaction is balanced and adheres to GL rules.
     * Used by: TP module (transaction validation), Account module (posting
     * validation)
     *
     * @param transactionId the transaction ID
     * @throws IllegalArgumentException if validation fails
     */
    void validateTransaction(UUID transactionId);

    /**
     * Reverses an entire transaction by creating contra-entries.
     * Used by: TP module (transaction reversals), Account module (error
     * corrections)
     *
     * @param transactionId the transaction ID to reverse
     * @param reason        the reversal reason
     * @param reversedBy    the user performing the reversal
     * @return the reversal GL transaction DTO
     */
    GLTransactionDTO reverseTransaction(UUID transactionId, String reason, String reversedBy);

    /**
     * Validates that all journal entries in a transaction are balanced.
     * Used by: TP module (posting validation), Account module (transaction
     * validation)
     *
     * @param transactionId the transaction ID
     * @return true if transaction is balanced
     */
    boolean validateTransactionBalance(UUID transactionId);

    /**
     * Gets the current balance of a GL account.
     * Used by: Account module (balance inquiry), TP module (balance validation)
     *
     * @param accountId the account ID
     * @return the current balance
     */
    BigDecimal getCurrentBalance(UUID accountId);

    /**
     * Gets the historical balance of an account as of a specific date.
     * Used by: Account module (balance history), TP module (reconciliation)
     *
     * @param accountId the account ID
     * @param date      the date
     * @return the balance at the specified date
     */
    BigDecimal getBalanceAtDate(UUID accountId, LocalDate date);

    /**
     * Gets the current balance for a specific GL account with detailed information.
     * Used by: Account module (detailed balance), TP module (balance inquiry)
     *
     * @param accountId the account ID
     * @return optional GL account balance DTO
     */
    Optional<GLAccountBalance> getAccountBalance(UUID accountId);

    /**
     * Gets the current balance for a GL account by its code.
     * Used by: Account module (balance lookup), TP module (account balance)
     *
     * @param accountCode the account code
     * @return optional GL account balance DTO
     */
    Optional<GLAccountBalance> getAccountBalanceByCode(String accountCode);

    /**
     * Gets balances for multiple GL accounts in a single operation.
     * Used by: Account module (batch balance inquiry), TP module (multi-account
     * validation)
     */
    Map<UUID, GLAccountBalance> getAccountBalances(List<UUID> accountIds);

    /**
     * Validates balance consistency between snapshot and real-time calculation.
     * Used by: Account module (balance validation), TP module (reconciliation)
     */
    boolean validateBalanceConsistency(UUID accountId, LocalDate asOfDate);

    /**
     * Triggers a recalculation of account balances from transaction history.
     * Used by: Account module (balance correction), TP module (error recovery)
     */
    void recalculateBalance(UUID accountId);

    /**
     * Validates balance consistency for all accounts on a specific date.
     * Used by: Account module (system validation), TP module (end-of-day
     * reconciliation)
     */
    boolean validateAllBalancesConsistency(LocalDate asOfDate);

    /**
     * Calculates the net change in account balance over a period.
     * Used by: Account module (balance analysis), TP module (activity reporting)
     */
    GLAccountBalance getBalanceChange(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Gets daily balance records for an account.
     * Used by: Account module (balance history), TP module (reconciliation)
     *
     * @param accountId the account ID
     * @param fromDate  the start date (inclusive)
     * @param toDate    the end date (inclusive)
     * @return list of daily balance DTOs
     */
    List<DailyBalanceSnapshot> getDailyBalances(UUID accountId, LocalDate fromDate, LocalDate toDate);

    /**
     * Gets the current fiscal period.
     * Used by: TP module (posting period validation), Account module (period
     * inquiry)
     *
     * @return optional fiscal period DTO
     */
    Optional<FiscalPeriodDTO> getCurrentFiscalPeriod();

    /**
     * Gets a fiscal period by its ID.
     * Used by: TP module (period validation), Account module (period lookup)
     *
     * @param id the fiscal period ID
     * @return optional fiscal period DTO
     */
    Optional<FiscalPeriodDTO> getFiscalPeriodById(UUID id);

    /**
     * Gets the fiscal period for a specific date.
     * Used by: TP module (date-based period lookup), Account module (period
     * identification)
     *
     * @param date the date
     * @return optional fiscal period DTO
     */
    Optional<FiscalPeriodDTO> getFiscalPeriodForDate(LocalDate date);

    /**
     * Checks if a fiscal period is open for posting.
     * Used by: TP module (posting validation), Account module (period status check)
     *
     * @param periodId the fiscal period ID
     * @return true if period is open
     */
    boolean isFiscalPeriodOpen(UUID periodId);

    /**
     * Gets journal entries for a specific transaction.
     * Used by: TP module (entry inquiry), Account module (transaction details)
     *
     * @param transactionId the transaction ID
     * @return list of journal entry DTOs
     */
    List<GLJournalEntryDTO> getJournalEntriesByTransaction(UUID transactionId);

    /**
     * Gets journal entries for a GL account within a date range.
     * Used by: Account module (account activity), TP module (account
     * reconciliation)
     *
     * @param accountId the account ID
     * @param fromDate  the start date (inclusive)
     * @param toDate    the end date (inclusive)
     * @return list of journal entry DTOs
     */
    List<GLJournalEntryDTO> getJournalEntriesByAccount(UUID accountId, LocalDate fromDate, LocalDate toDate);

    /**
     * Finds a journal entry by its unique ID.
     * Used by: TP module (entry lookup), Account module (entry details)
     *
     * @param entryId the journal entry ID
     * @return optional journal entry DTO
     */
    Optional<GLJournalEntryDTO> getJournalEntryById(UUID entryId);

    /**
     * Gets the closing balance for an account on a specific date.
     * Used by: Account module (end-of-day balance), TP module (settlement balance)
     */
    Optional<BigDecimal> getClosingBalance(UUID accountId, LocalDate date);

    /**
     * Calculates the balance for an account as of a specific date and time.
     * Used by: Account module (precise balance), TP module (real-time balance)
     */
    BigDecimal calculateBalanceAsOf(UUID accountId, LocalDateTime asOfDate);

    /**
     * Gets account activity summary for a specific date.
     * Used by: Account module (activity inquiry), TP module (transaction analysis)
     */
    GLAccountBalance getAccountActivity(UUID accountId, LocalDate activityDate);

    /**
     * Gets the balance history for a specific account over a date range.
     * Used by: Account module (balance trends), TP module (historical analysis)
     */
    List<DailyBalanceSnapshot> getBalanceHistory(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Converts an amount from a transaction currency to the base currency.
     * Used by: Account module (multi-currency balances), TP module (currency
     * conversion)
     */
    BigDecimal convertToBaseCurrency(BigDecimal amount, String currency, LocalDate valueDate);

    /**
     * Gets the base (functional) currency of the ledger.
     * Used by: Account module (currency operations), TP module (base currency
     * conversion)
     */
    String getBaseCurrency();

    /**
     * Calculates the total debit amount for an account within a date range.
     * Used by: Account module (activity analysis), TP module (transaction
     * reporting)
     */
    BigDecimal getTotalDebitsForAccount(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculates the total credit amount for an account within a date range.
     * Used by: Account module (activity analysis), TP module (transaction
     * reporting)
     */
    BigDecimal getTotalCreditsForAccount(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Calculates the net balance (debits - credits) for an account within a date
     * range.
     * Used by: Account module (net activity), TP module (balance calculation)
     */
    BigDecimal getNetBalanceForAccount(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Counts the number of journal entries for an account within a date range.
     * Used by: Account module (activity counting), TP module (transaction volume)
     */
    long countEntriesForAccount(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Validates if a journal entry is well-formed.
     * Used by: Account module (entry validation), TP module (posting validation)
     * Note: Validation is done by ID to avoid entity exposure.
     */
    boolean validateEntry(UUID entryId);

    /**
     * Retrieves all journal entries for a specific transaction.
     * Used by: Account module (transaction details), TP module (entry inquiry)
     */
    List<GLJournalEntryDTO> getEntriesByTransaction(UUID transactionId);

    /**
     * Retrieves all journal entries for a specific account.
     * Used by: Account module (account activity), TP module (account inquiry)
     */
    List<GLJournalEntryDTO> getEntriesByAccount(UUID accountId);

    /**
     * Retrieves journal entries for a specific account within a date range.
     * Used by: Account module (activity history), TP module (transaction history)
     */
    List<GLJournalEntryDTO> getEntriesByAccountAndDateRange(UUID accountId, LocalDate startDate, LocalDate endDate);

    /**
     * Retrieves journal entries for a specific account and date.
     * Used by: Account module (daily activity), TP module (date-specific inquiry)
     */
    List<GLJournalEntryDTO> getEntriesByAccountAndDate(UUID accountId, LocalDate date);

    /**
     * Retrieves a specific journal entry by its ID.
     * Used by: Account module (entry details), TP module (entry lookup)
     */
    Optional<GLJournalEntryDTO> getEntryById(UUID entryId);

    /**
     * Retrieves the active fiscal period for a given date.
     * Used by: Account module (period validation), TP module (posting validation)
     */
    Optional<FiscalPeriodDTO> findActivePeriod(LocalDate date);

    /**
     * Validates if posting is allowed for a specific date based on fiscal period
     * rules.
     * Used by: Account module (posting validation), TP module (transaction
     * validation)
     */
    boolean isPostingAllowedForDate(LocalDate postingDate);

    /**
     * Retrieves fiscal periods filtered by their status.
     * Used by: Account module (period management), TP module (period inquiry)
     */
    List<FiscalPeriodDTO> getFiscalPeriodsByStatus(FiscalPeriodStatus status);

    /**
     * Performs balance reconciliation for a specific account and date.
     * Used by: Account module (balance validation), TP module (transaction
     * reconciliation)
     */
    AccountReconciliationResult performBalanceReconciliationForAccount(UUID accountId, LocalDate date);

    /**
     * Performs balance reconciliation for a specific period.
     * Used by: Account module (period reconciliation), TP module (batch
     * reconciliation)
     */
    BalanceReconciliationReport performBalanceReconciliationForPeriod(LocalDate startDate, LocalDate endDate);

    /**
     * Validates snapshot data integrity for a specific date.
     * Used by: Account module (data validation), TP module (integrity checks)
     */
    ValidationResult validateSnapshotIntegrity(LocalDate date);

    /**
     * Generates compliance report for regulatory requirements.
     * Used by: Account module (compliance reporting), TP module (audit reporting)
     */
    SnapshotsComplianceReport generateComplianceReport(LocalDate startDate, LocalDate endDate);
}
