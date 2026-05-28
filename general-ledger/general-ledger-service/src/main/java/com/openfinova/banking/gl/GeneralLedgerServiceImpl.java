package com.openfinova.banking.gl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.openfinova.banking.gl.api.GeneralLedgerService;
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
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.mapper.GLEntityMapper;
import com.openfinova.banking.gl.service.BalanceService;
import com.openfinova.banking.gl.service.FiscalPeriodService;
import com.openfinova.banking.gl.service.GLAccountService;
import com.openfinova.banking.gl.service.GLJournalEntryService;
import com.openfinova.banking.gl.service.GLSnapshotService;
import com.openfinova.banking.gl.service.GLTransactionService;
import com.openfinova.banking.gl.service.OperationalGLAccountService;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Facade for General Ledger operations used by Account and TP modules.
 * This facade provides a simplified interface for inter-module communication,
 * acting as a proxy to hide the complexity of the underlying GL services.
 * It delegates all GL requests to the appropriate GL services and converts
 * entities to DTOs to prevent entity leakage to external modules.
 * Used by:
 * - Account module: Balance inquiries, account validation, reconciliation
 * - TP module: Transaction posting, fiscal period validation, reversals
 */
@Service
public class GeneralLedgerServiceImpl implements GeneralLedgerService {

    private final GLAccountService glAccountService;
    private final BalanceService balanceService;
    private final GLSnapshotService glSnapshotService;
    private final GLTransactionService glTransactionService;
    private final FiscalPeriodService fiscalPeriodService;
    private final GLJournalEntryService glJournalEntryService;
    private final OperationalGLAccountService operationalGLAccountService;
    private final DateTimeService dateTimeService;

    public GeneralLedgerServiceImpl(GLAccountService glAccountService, BalanceService balanceService,
            GLSnapshotService glSnapshotService, GLTransactionService glTransactionService,
            FiscalPeriodService fiscalPeriodService, GLJournalEntryService glJournalEntryService,
            OperationalGLAccountService operationalGLAccountService, DateTimeService dateTimeService) {
        this.glAccountService = glAccountService;
        this.balanceService = balanceService;
        this.glSnapshotService = glSnapshotService;
        this.glTransactionService = glTransactionService;
        this.fiscalPeriodService = fiscalPeriodService;
        this.glJournalEntryService = glJournalEntryService;
        this.operationalGLAccountService = operationalGLAccountService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Retrieves a GL account by its unique identifier.
     * Used by: Account module (account validation), TP module (transaction posting)
     */
    @Override
    public Optional<GLAccountDTO> getAccountById(UUID id) {
        return glAccountService.getAccountById(id).map(GLEntityMapper::toDTO);
    }

    /**
     * Retrieves a GL account by its internal code.
     * Used by: Account module (account mapping), TP module (clearing accounts)
     */
    @Override
    public Optional<GLAccountDTO> findByCode(String code) {
        return glAccountService.findByCode(code).map(GLEntityMapper::toDTO);
    }

    /**
     * Checks if a GL account is active and available for posting.
     * Used by: Account module (validation), TP module (transaction posting)
     */
    @Override
    public boolean isAccountActiveForPosting(UUID accountId) {
        return glAccountService.isAccountActiveForPosting(accountId);
    }

    /**
     * Validates if a GL account is eligible for posting.
     * Used by: Account module (validation), TP module (transaction posting)
     */
    @Override
    public void validateAccountForPosting(UUID accountId) {
        glAccountService.validateAccountForPosting(accountId);
    }

    /**
     * Gets all active accounts that can accept journal entries.
     * Used by: Account module (account setup), TP module (account mapping)
     */
    @Override
    public List<GLAccountDTO> getPostableAccounts() {
        return GLEntityMapper.toAccountDTOList(glAccountService.getPostableAccounts());
    }

    /**
     * Resolves an operational GL account by type.
     * Currency conversion is handled at the GL posting level.
     * Used by: TP module (fee and contra account resolution)
     */
    @Override
    public UUID getOperationalGLAccount(OperationalGLAccountType type) {
        return operationalGLAccountService.getOperationalGLAccount(type);
    }

    @Override
    public UUID getOperationalGLAccount(String operationalGLAccountType) {
        return operationalGLAccountService.getOperationalGLAccount(operationalGLAccountType);
    }

    @Override
    public List<GLAccountDTO> getAccountsByType(String accountType, String status) {
        return GLEntityMapper.toAccountDTOList(glAccountService.getAccountsByType(accountType, status));
    }

    /**
     * Gets accounts by type with optional status filter.
     * Used by: Account module (account categorization), TP module (clearing accounts)
     */
    @Override
    public List<GLAccountDTO> getAccountsByType(GLAccountType accountType, GLAccountStatus status) {
        return GLEntityMapper.toAccountDTOList(glAccountService.getAccountsByType(accountType, status));
    }

    /**
     * Gets accounts by currency.
     * Used by: Account module (multi-currency), TP module (currency-specific accounts)
     */
    @Override
    public List<GLAccountDTO> getAccountsByCurrency(String currency) {
        return GLEntityMapper.toAccountDTOList(glAccountService.getAccountsByCurrency(currency));
    }

    /**
     * Searches accounts by name or code.
     * Used by: Account module (account lookup), TP module (account discovery)
     */
    @Override
    public List<GLAccountDTO> searchAccounts(String searchTerm) {
        return GLEntityMapper.toAccountDTOList(glAccountService.searchAccounts(searchTerm));
    }

    /**
     * Posts a complete transaction containing multiple journal entries.
     * Used by: TP module (transaction posting), Account module (interest posting)
     * This method accepts a command object with the transaction data, converts it to an entity,
     * posts it through the service layer, and returns the result as a DTO.
     */
    @Override
    public GLTransactionDTO postTransaction(PostTransactionCommand command) {
        // Convert command to entity
        GLTransaction transaction = GLEntityMapper.toEntity(command, glAccountService);

        // Post through service layer
        GLTransaction postedTransaction = glTransactionService.postTransaction(transaction);

        // Convert result back to DTO
        return GLEntityMapper.toDTO(postedTransaction);
    }

    /**
     * Retrieves a transaction by its unique ID.
     * Used by: TP module (transaction lookup), Account module (transaction inquiry)
     */
    @Override
    public Optional<GLTransactionDTO> getTransactionById(UUID id) {
        return glTransactionService.getTransactionById(id).map(GLEntityMapper::toDTO);
    }

    /**
     * Retrieves a transaction by its external reference ID.
     * Used by: TP module (idempotency checks), Account module (transaction matching)
     */
    @Override
    public Optional<GLTransactionDTO> getTransactionByReference(String referenceId) {
        return glTransactionService.getTransactionByReference(referenceId).map(GLEntityMapper::toDTO);
    }

    /**
     * Validates that a transaction is balanced and adheres to GL rules.
     * Used by: TP module (transaction validation), Account module (posting validation)
     * Note: Validation is done by ID to avoid entity exposure.
     */
    @Override
    public void validateTransaction(UUID transactionId) {
        glTransactionService.getTransactionById(transactionId).ifPresent(glTransactionService::validateTransaction);
    }

    /**
     * Reverses an entire transaction by creating contra-entries.
     * Used by: TP module (transaction reversals), Account module (error corrections)
     */
    @Override
    public GLTransactionDTO reverseTransaction(UUID transactionId, String reason, String reversedBy) {
        return GLEntityMapper.toDTO(glTransactionService.reverseTransaction(transactionId, reason, reversedBy));
    }

    /**
     * Validates that all journal entries in a transaction are balanced.
     * Used by: TP module (posting validation), Account module (transaction validation)
     */
    @Override
    public boolean validateTransactionBalance(UUID transactionId) {
        return glTransactionService.validateTransactionBalance(transactionId);
    }

    /**
     * Gets the current balance of a GL account.
     * Used by: Account module (balance inquiry), TP module (balance validation)
     */
    @Override
    public BigDecimal getCurrentBalance(UUID accountId) {
        return balanceService.getCurrentBalance(accountId);
    }

    /**
     * Gets the historical balance of an account as of a specific date.
     * Used by: Account module (balance history), TP module (reconciliation)
     */
    @Override
    public BigDecimal getBalanceAtDate(UUID accountId, LocalDate date) {
        return balanceService.getBalanceAtDate(accountId, date);
    }

    /**
     * Gets the current balance for a specific GL account with detailed information.
     * Used by: Account module (detailed balance), TP module (balance inquiry)
     */
    @Override
    public Optional<GLAccountBalance> getAccountBalance(UUID accountId) {
        return balanceService.getAccountBalance(accountId);
    }

    /**
     * Gets the current balance for a GL account by its code.
     * Used by: Account module (balance lookup), TP module (account balance)
     */
    @Override
    public Optional<GLAccountBalance> getAccountBalanceByCode(String accountCode) {
        return balanceService.getAccountBalanceByCode(accountCode);
    }

    /**
     * Gets balances for multiple GL accounts in a single operation.
     * Used by: Account module (batch balance inquiry), TP module (multi-account validation)
     */
    @Override
    public Map<UUID, GLAccountBalance> getAccountBalances(List<UUID> accountIds) {
        return balanceService.getAccountBalances(accountIds);
    }

    /**
     * Validates balance consistency between snapshot and real-time calculation.
     * Used by: Account module (balance validation), TP module (reconciliation)
     */
    @Override
    public boolean validateBalanceConsistency(UUID accountId, LocalDate asOfDate) {
        return balanceService.validateBalanceConsistency(accountId, asOfDate);
    }

    /**
     * Validates balance consistency for all accounts on a specific date.
     * Used by: Account module (system validation), TP module (end-of-day reconciliation)
     */
    @Override
    public boolean validateAllBalancesConsistency(LocalDate asOfDate) {
        return balanceService.validateAllBalancesConsistency(asOfDate);
    }

    /**
     * Triggers a recalculation of account balances from transaction history.
     * Used by: Account module (balance correction), TP module (error recovery)
     */
    @Override
    public void recalculateBalance(UUID accountId) {
        balanceService.recalculateBalance(accountId);
    }

    /**
     * Calculates the net change in account balance over a period.
     * Used by: Account module (balance analysis), TP module (activity reporting)
     */
    @Override
    public GLAccountBalance getBalanceChange(UUID accountId, LocalDate startDate, LocalDate endDate) {
        return balanceService.getBalanceChange(accountId, startDate, endDate);
    }

    /**
     * Gets account activity summary for a specific date.
     * Used by: Account module (activity inquiry), TP module (transaction analysis)
     */
    @Override
    public GLAccountBalance getAccountActivity(UUID accountId, LocalDate activityDate) {
        return balanceService.getAccountActivity(accountId, activityDate);
    }

    /**
     * Gets the balance history for a specific account over a date range.
     * Used by: Account module (balance trends), TP module (historical analysis)
     */
    @Override
    public List<DailyBalanceSnapshot> getBalanceHistory(UUID accountId, LocalDate startDate, LocalDate endDate) {
        return balanceService.getBalanceHistory(accountId, startDate, endDate);
    }

    /**
     * Converts an amount from a transaction currency to the base currency.
     * Used by: Account module (multi-currency balances), TP module (currency conversion)
     */
    @Override
    public BigDecimal convertToBaseCurrency(BigDecimal amount, String currency, LocalDate valueDate) {
        return balanceService.convertToBaseCurrency(amount, currency, valueDate);
    }

    /**
     * Gets the base (functional) currency of the ledger.
     * Used by: Account module (currency operations), TP module (base currency conversion)
     */
    @Override
    public String getBaseCurrency() {
        return balanceService.getBaseCurrency();
    }

    /**
     * Calculates the total debit amount for an account within a date range.
     * Used by: Account module (activity analysis), TP module (transaction reporting)
     */
    @Override
    public BigDecimal getTotalDebitsForAccount(UUID accountId, LocalDate startDate, LocalDate endDate) {
        return balanceService.getTotalDebitsForAccount(accountId, startDate, endDate);
    }

    /**
     * Calculates the total credit amount for an account within a date range.
     * Used by: Account module (activity analysis), TP module (transaction reporting)
     */
    @Override
    public BigDecimal getTotalCreditsForAccount(UUID accountId, LocalDate startDate, LocalDate endDate) {
        return balanceService.getTotalCreditsForAccount(accountId, startDate, endDate);
    }

    /**
     * Calculates the net balance (debits - credits) for an account within a date range.
     * Used by: Account module (net activity), TP module (balance calculation)
     */
    @Override
    public BigDecimal getNetBalanceForAccount(UUID accountId, LocalDate startDate, LocalDate endDate) {
        return balanceService.getNetBalanceForAccount(accountId, startDate, endDate);
    }

    /**
     * Counts the number of journal entries for an account within a date range.
     * Used by: Account module (activity counting), TP module (transaction volume)
     */
    @Override
    public long countEntriesForAccount(UUID accountId, LocalDate startDate, LocalDate endDate) {
        return balanceService.countEntriesForAccount(accountId, startDate, endDate);
    }

    /**
     * Validates if a journal entry is well-formed.
     * Used by: Account module (entry validation), TP module (posting validation)
     * Note: Validation is done by ID to avoid entity exposure.
     */
    @Override
    public boolean validateEntry(UUID entryId) {
        return glJournalEntryService.getEntryById(entryId).map(glJournalEntryService::validateEntry).orElse(false);
    }

    /**
     * Retrieves all journal entries for a specific transaction.
     * Used by: Account module (transaction details), TP module (entry inquiry)
     */
    @Override
    public List<GLJournalEntryDTO> getEntriesByTransaction(UUID transactionId) {
        return GLEntityMapper.toEntryDTOList(glJournalEntryService.getEntriesByTransaction(transactionId));
    }

    /**
     * Retrieves all journal entries for a specific account.
     * Used by: Account module (account activity), TP module (account inquiry)
     */
    @Override
    public List<GLJournalEntryDTO> getEntriesByAccount(UUID accountId) {
        return GLEntityMapper.toEntryDTOList(glJournalEntryService.getEntriesByAccount(accountId));
    }

    /**
     * Retrieves journal entries for a specific account within a date range.
     * Used by: Account module (activity history), TP module (transaction history)
     */
    @Override
    public List<GLJournalEntryDTO> getEntriesByAccountAndDateRange(UUID accountId, LocalDate startDate,
            LocalDate endDate) {
        return GLEntityMapper
                .toEntryDTOList(glJournalEntryService.getEntriesByAccountAndDateRange(accountId, startDate, endDate));
    }

    /**
     * Retrieves journal entries for a specific account and date.
     * Used by: Account module (daily activity), TP module (date-specific inquiry)
     */
    @Override
    public List<GLJournalEntryDTO> getEntriesByAccountAndDate(UUID accountId, LocalDate date) {
        return GLEntityMapper.toEntryDTOList(glJournalEntryService.getEntriesByAccountAndDate(accountId, date));
    }

    /**
     * Retrieves a specific journal entry by its ID.
     * Used by: Account module (entry details), TP module (entry lookup)
     */
    @Override
    public Optional<GLJournalEntryDTO> getEntryById(UUID entryId) {
        return glJournalEntryService.getEntryById(entryId).map(GLEntityMapper::toDTO);
    }

    /**
     * Retrieves the active fiscal period for a given date.
     * Used by: Account module (period validation), TP module (posting validation)
     */
    @Override
    public Optional<FiscalPeriodDTO> findActivePeriod(LocalDate date) {
        return fiscalPeriodService.findActivePeriod(date).map(GLEntityMapper::toDTO);
    }

    /**
     * Retrieves the fiscal period that contains a specific date.
     * Used by: Account module (period inquiry), TP module (date validation)
     */
    @Override
    public Optional<FiscalPeriodDTO> getFiscalPeriodForDate(LocalDate date) {
        return fiscalPeriodService.getFiscalPeriodForDate(date).map(GLEntityMapper::toDTO);
    }

    /**
     * Validates if posting is allowed for a specific date based on fiscal period rules.
     * Used by: Account module (posting validation), TP module (transaction validation)
     */
    @Override
    public boolean isPostingAllowedForDate(LocalDate postingDate) {
        return fiscalPeriodService.isPostingAllowedForDate(postingDate);
    }

    /**
     * Retrieves fiscal periods filtered by their status.
     * Used by: Account module (period management), TP module (period inquiry)
     */
    @Override
    public List<FiscalPeriodDTO> getFiscalPeriodsByStatus(FiscalPeriodStatus status) {
        return GLEntityMapper.toPeriodDTOList(fiscalPeriodService.getFiscalPeriodsByStatus(status));
    }

    /**
     * Performs balance reconciliation for a specific account and date.
     * Used by: Account module (balance validation), TP module (transaction reconciliation)
     */
    @Override
    public AccountReconciliationResult performBalanceReconciliationForAccount(UUID accountId, LocalDate date) {
        return glSnapshotService.performBalanceReconciliationForAccount(accountId, date);
    }

    /**
     * Performs balance reconciliation for a specific period.
     * Used by: Account module (period reconciliation), TP module (batch reconciliation)
     */
    @Override
    public BalanceReconciliationReport performBalanceReconciliationForPeriod(LocalDate startDate, LocalDate endDate) {
        return glSnapshotService.performBalanceReconciliationForPeriod(startDate, endDate);
    }

    /**
     * Validates snapshot data integrity for a specific date.
     * Used by: Account module (data validation), TP module (integrity checks)
     */
    @Override
    public ValidationResult validateSnapshotIntegrity(LocalDate date) {
        return glSnapshotService.validateSnapshotIntegrity(date);
    }

    /**
     * Generates compliance report for regulatory requirements.
     * Used by: Account module (compliance reporting), TP module (audit reporting)
     */
    @Override
    public SnapshotsComplianceReport generateComplianceReport(LocalDate startDate, LocalDate endDate) {
        return glSnapshotService.generateComplianceReport(startDate, endDate);
    }

    /**
     * Calculates the balance for an account as of a specific date and time.
     * Used by: Account module (precise balance), TP module (real-time balance)
     */
    @Override
    public BigDecimal calculateBalanceAsOf(UUID accountId, LocalDateTime asOfDate) {
        return balanceService.calculateBalanceAsOf(accountId, asOfDate);
    }

    /**
     * Gets the closing balance for an account on a specific date.
     * Used by: Account module (end-of-day balance), TP module (settlement balance)
     */
    @Override
    public Optional<BigDecimal> getClosingBalance(UUID accountId, LocalDate date) {
        return balanceService.getClosingBalance(accountId, date);
    }

    @Override
    public List<GLJournalEntryDTO> getJournalEntriesByTransaction(UUID transactionId) {
        return getEntriesByTransaction(transactionId);
    }

    @Override
    public List<GLJournalEntryDTO> getJournalEntriesByAccount(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        return GLEntityMapper
                .toEntryDTOList(glJournalEntryService.getEntriesByAccountAndDateRange(accountId, fromDate, toDate));
    }

    @Override
    public Optional<GLJournalEntryDTO> getJournalEntryById(UUID entryId) {
        return glJournalEntryService.getEntryById(entryId).map(GLEntityMapper::toDTO);
    }

    @Override
    public List<DailyBalanceSnapshot> getDailyBalances(UUID accountId, LocalDate fromDate, LocalDate toDate) {
        return getBalanceHistory(accountId, fromDate, toDate);
    }

    @Override
    public Optional<FiscalPeriodDTO> getCurrentFiscalPeriod() {
        return findActivePeriod(dateTimeService.today());
    }

    @Override
    public boolean isFiscalPeriodOpen(UUID periodId) {
        return fiscalPeriodService.isFiscalPeriodOpen(periodId);
    }

    @Override
    public Optional<FiscalPeriodDTO> getFiscalPeriodById(UUID id) {
        return fiscalPeriodService.getFiscalPeriodById(id).map(GLEntityMapper::toDTO);
    }
}
