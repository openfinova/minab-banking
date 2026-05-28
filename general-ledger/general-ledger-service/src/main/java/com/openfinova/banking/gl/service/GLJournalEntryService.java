package com.openfinova.banking.gl.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.common.lib.model.SupportedCurrency;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLJournalEntry;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.repository.GLAccountRepository;
import com.openfinova.banking.gl.repository.GLJournalEntryRepository;
import com.openfinova.banking.gl.repository.GLTransactionRepository;

/**
 * Implementation of GLJournalEntryService providing comprehensive journal entry management.
 * This service handles the complete lifecycle of journal entries including creation, validation,
 * retrieval, and analysis operations.
 *
 * Key responsibilities:
 * - Journal entry validation and business rule enforcement
 * - Entry retrieval by various criteria (transaction, account, date ranges)
 * - Balance calculations and audit trail support
 * - Multi-currency entry handling and base currency conversion
 * - Integration with GL transactions and accounts
 */
@Service
@Transactional
public class GLJournalEntryService {

    private static final Logger logger = LoggerFactory.getLogger(GLJournalEntryService.class);

    private final GLJournalEntryRepository journalEntryRepository;
    private final GLAccountRepository accountRepository;
    private final GLTransactionRepository transactionRepository;

    /**
     * Constructor for dependency injection.
     *
     * @param journalEntryRepository the journal entry repository
     * @param accountRepository the GL account repository
     * @param transactionRepository the GL transaction repository
     */
    public GLJournalEntryService(GLJournalEntryRepository journalEntryRepository, GLAccountRepository accountRepository,
            GLTransactionRepository transactionRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.accountRepository = accountRepository;
        this.transactionRepository = transactionRepository;
    }

    /**
     * Retrieves all journal entries for a specific transaction.
     *
     * @param transactionId The UUID of the transaction.
     * @return List of journal entries.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    @Transactional(readOnly = true)
    public List<GLJournalEntry> getEntriesByTransaction(UUID transactionId) {
        logger.debug("Getting journal entries for transaction: {}", transactionId);

        Optional<GLTransaction> transaction = transactionRepository.findById(transactionId);
        if (transaction.isEmpty()) {
            logger.warn("Transaction not found: {}", transactionId);
            return List.of();
        }

        return transaction.get().getJournalEntries();
    }

    /**
     * Retrieves all journal entries for a specific account.
     *
     * @param accountId The UUID of the account.
     * @return List of journal entries for the account.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    @Transactional(readOnly = true)
    public List<GLJournalEntry> getEntriesByAccount(UUID accountId) {
        logger.debug("Getting journal entries for account: {}", accountId);

        Optional<GLAccount> account = accountRepository.findById(accountId);
        if (account.isEmpty()) {
            logger.warn("Account not found: {}", accountId);
            return List.of();
        }

        // Get all posted entries for this account
        return journalEntryRepository.findEntriesByAccount(accountId);
    }

    /**
     * Validates if a journal entry leg is well-formed.
     *
     * @param entry The entry to validate.
     * @return true if the entry is valid.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    public boolean validateEntry(GLJournalEntry entry) {
        logger.debug("Validating journal entry: {}", entry);

        try {
            // Basic null checks
            if (entry == null) {
                logger.warn("Journal entry is null");
                return false;
            }

            // Account validation
            if (entry.getAccount() == null) {
                logger.warn("Journal entry missing account");
                return false;
            }

            // Transaction validation
            if (entry.getTransaction() == null) {
                logger.warn("Journal entry missing transaction");
                return false;
            }

            // Amount validation - exactly one of debit or credit must be non-zero
            boolean hasDebit = entry.getDebitAmount() != null && entry.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = entry.getCreditAmount() != null
                    && entry.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;

            if (hasDebit && hasCredit) {
                logger.warn("Journal entry has both debit and credit amounts: {}", entry);
                return false;
            }

            if (!hasDebit && !hasCredit) {
                logger.warn("Journal entry has no debit or credit amount: {}", entry);
                return false;
            }

            // Value date validation
            if (entry.getValueDate() == null) {
                logger.warn("Journal entry missing value date");
                return false;
            }

            // Currency validation
            if (entry.getCurrency() == null || entry.getCurrency().trim().isEmpty()) {
                logger.warn("Journal entry missing currency");
                return false;
            }

            if (entry.getCurrency().length() != 3) {
                logger.warn("Journal entry currency must be 3 characters: {}", entry.getCurrency());
                return false;
            }

            // Exchange rate validation
            if (entry.getExchangeRate() == null || entry.getExchangeRate().compareTo(BigDecimal.ZERO) <= 0) {
                logger.warn("Journal entry has invalid exchange rate: {}", entry.getExchangeRate());
                return false;
            }

            // Base currency amounts validation
            if (entry.getBaseDebitAmount() == null || entry.getBaseCreditAmount() == null) {
                logger.warn("Journal entry missing base currency amounts");
                return false;
            }

            // Validate base amounts are consistent with original amounts and exchange rate
            // Use tolerance-based comparison to handle FX rounding differences
            if (hasDebit) {
                BigDecimal expectedBaseDebit = entry.getDebitAmount().multiply(entry.getExchangeRate());
                // Use transaction currency (not entry currency) for tolerance because baseDebitAmount
                // is in the transaction's base currency. Entry may be in EUR, but base amount is in USD.
                // Tolerance must match the precision of the TARGET currency (transaction currency).
                BigDecimal tolerance = getToleranceForCurrency(entry.getTransaction().getCurrency());
                BigDecimal difference = entry.getBaseDebitAmount().subtract(expectedBaseDebit).abs();

                if (difference.compareTo(tolerance) > 0) {
                    logger.warn(
                            "Base debit amount {} differs from expected {} by {} (tolerance: {})",
                            entry.getBaseDebitAmount(),
                            expectedBaseDebit,
                            difference,
                            tolerance);
                    return false;
                }
                if (entry.getBaseCreditAmount().compareTo(BigDecimal.ZERO) != 0) {
                    logger.warn("Base credit amount should be zero when debit amount is set");
                    return false;
                }
            }

            if (hasCredit) {
                BigDecimal expectedBaseCredit = entry.getCreditAmount().multiply(entry.getExchangeRate());
                BigDecimal tolerance = getToleranceForCurrency(entry.getTransaction().getCurrency());
                BigDecimal difference = entry.getBaseCreditAmount().subtract(expectedBaseCredit).abs();

                if (difference.compareTo(tolerance) > 0) {
                    logger.warn(
                            "Base credit amount {} differs from expected {} by {} (tolerance: {})",
                            entry.getBaseCreditAmount(),
                            expectedBaseCredit,
                            difference,
                            tolerance);
                    return false;
                }
                if (entry.getBaseDebitAmount().compareTo(BigDecimal.ZERO) != 0) {
                    logger.warn("Base debit amount should be zero when credit amount is set");
                    return false;
                }
            }

            // Line number validation
            if (entry.getLineNumber() == null || entry.getLineNumber() <= 0) {
                logger.warn("Journal entry has invalid line number: {}", entry.getLineNumber());
                return false;
            }

            // Account status validation
            if (!entry.getAccount().isActive()) {
                logger.warn("Journal entry references inactive account: {}", entry.getAccount().getCode());
                return false;
            }

            // Currency consistency validation
            if (!entry.getCurrency().equals(entry.getAccount().getCurrency())) {
                logger.warn(
                        "Journal entry currency {} does not match account currency {}",
                        entry.getCurrency(),
                        entry.getAccount().getCurrency());
                return false;
            }

            logger.debug("Journal entry validation passed: {}", entry);
            return true;

        } catch (Exception e) {
            logger.error("Error validating journal entry: {}", e.getMessage(), e);
            return false;
        }
    }

    /**
     * Retrieves journal entries for a specific account within a date range.
     *
     * @param accountId The UUID of the account.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @return List of journal entries for the account in the date range.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    @Transactional(readOnly = true)
    public List<GLJournalEntry> getEntriesByAccountAndDateRange(UUID accountId, LocalDate startDate,
            LocalDate endDate) {
        logger.debug("Getting journal entries for account: {} from {} to {}", accountId, startDate, endDate);

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        return journalEntryRepository.findEntriesByAccountAndDateRange(accountId, startDate, endDate);
    }

    /**
     * Retrieves journal entries for a specific account and date.
     *
     * @param accountId The UUID of the account.
     * @param date The transaction date.
     * @return List of journal entries for the account on the date.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    @Transactional(readOnly = true)
    public List<GLJournalEntry> getEntriesByAccountAndDate(UUID accountId, LocalDate date) {
        logger.debug("Getting journal entries for account: {} on date: {}", accountId, date);

        if (date == null) {
            throw new IllegalArgumentException("Date is required");
        }

        return journalEntryRepository.findEntriesByAccountAndDate(accountId, date);
    }

    /**
     * Retrieves journal entries for a specific account, date range, and currency.
     *
     * @param accountId The UUID of the account.
     * @param startDate The start date (inclusive).
     * @param endDate The end date (inclusive).
     * @param currency The currency code.
     * @return List of journal entries matching the criteria.
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('gl:read')")
    public List<GLJournalEntry> getEntriesByAccountDateRangeAndCurrency(UUID accountId, LocalDate startDate,
            LocalDate endDate, String currency) {
        logger.debug(
                "Getting journal entries for account: {} from {} to {} in currency: {}",
                accountId,
                startDate,
                endDate,
                currency);

        if (startDate == null || endDate == null) {
            throw new IllegalArgumentException("Start date and end date are required");
        }

        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }

        if (currency == null || currency.trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }

        return journalEntryRepository.findEntriesByAccountDateRangeAndCurrency(accountId, startDate, endDate, currency);
    }

    /**
     * Retrieves a specific journal entry by its ID.
     *
     * @param entryId The UUID of the journal entry.
     * @return Optional containing the journal entry if found.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")
    @Transactional(readOnly = true)
    public Optional<GLJournalEntry> getEntryById(UUID entryId) {
        logger.debug("Getting journal entry by ID: {}", entryId);

        if (entryId == null) {
            return Optional.empty();
        }

        return journalEntryRepository.findById(entryId);
    }

    /**
     * Creates a new journal entry with validation.
     *
     * @param entry The journal entry to create.
     * @return The created and persisted journal entry.
     * @throws IllegalArgumentException if validation fails.
     */
    public GLJournalEntry createEntry(GLJournalEntry entry) {
        logger.info("Creating new journal entry: {}", entry);

        if (!validateEntry(entry)) {
            throw new IllegalArgumentException("Journal entry validation failed");
        }

        // Ensure the transaction exists and is not posted
        if (entry.getTransaction().isPosted()) {
            throw new IllegalStateException("Cannot add entries to posted transaction");
        }

        // Ensure the account exists and is active
        Optional<GLAccount> account = accountRepository.findById(entry.getAccount().getId());
        if (account.isEmpty() || !account.get().isActive()) {
            throw new IllegalArgumentException("Account not found or inactive: " + entry.getAccount().getId());
        }

        GLJournalEntry savedEntry = journalEntryRepository.save(entry);
        logger.info("Successfully created journal entry with ID: {}", savedEntry.getId());

        return savedEntry;
    }

    /**
     * Updates an existing journal entry with validation.
     *
     * @param entry The journal entry to update.
     * @return The updated journal entry.
     * @throws IllegalArgumentException if validation fails or entry not found.
     */
    public GLJournalEntry updateEntry(GLJournalEntry entry) {
        logger.info("Updating journal entry: {}", entry);

        if (entry.getId() == null) {
            throw new IllegalArgumentException("Journal entry ID is required for update");
        }

        Optional<GLJournalEntry> existingEntry = journalEntryRepository.findById(entry.getId());
        if (existingEntry.isEmpty()) {
            throw new ResourceNotFoundException("GLJournalEntry", entry.getId());
        }

        // Check if the transaction is posted
        if (existingEntry.get().getTransaction().isPosted()) {
            throw new IllegalStateException("Cannot update entries in posted transaction");
        }

        if (!validateEntry(entry)) {
            throw new IllegalArgumentException("Journal entry validation failed");
        }

        GLJournalEntry updatedEntry = journalEntryRepository.save(entry);
        logger.info("Successfully updated journal entry with ID: {}", updatedEntry.getId());

        return updatedEntry;
    }

    /**
     * Deletes a journal entry by ID.
     * Only allowed for entries in non-posted transactions.
     *
     * @param entryId The UUID of the journal entry to delete.
     * @throws IllegalStateException if the entry cannot be deleted.
     */
    public void deleteEntry(UUID entryId) {
        logger.info("Deleting journal entry: {}", entryId);

        Optional<GLJournalEntry> entry = journalEntryRepository.findById(entryId);
        if (entry.isEmpty()) {
            throw new ResourceNotFoundException("GLJournalEntry", entryId);
        }

        // Check if the transaction is posted
        if (entry.get().getTransaction().isPosted()) {
            throw new IllegalStateException("Cannot delete entries from posted transaction");
        }

        journalEntryRepository.deleteById(entryId);
        logger.info("Successfully deleted journal entry: {}", entryId);
    }

    /**
     * Get the FX conversion tolerance for a given base currency.
     * Uses ISO 4217 decimal places to determine appropriate tolerance.
     * For example: USD (2 decimals) = 0.01, JPY (0 decimals) = 1.00, KWD (3 decimals) = 0.001
     *
     * @param baseCurrency The base currency code (e.g., "USD", "EUR", "JPY")
     * @return BigDecimal tolerance based on the currency's smallest unit
     */
    private BigDecimal getToleranceForCurrency(String baseCurrency) {
        try {
            return SupportedCurrency.fromCode(baseCurrency).getFxTolerance();
        } catch (IllegalArgumentException e) {
            logger.warn("Unsupported currency: {}, using default tolerance 0.01", baseCurrency);
            return new BigDecimal("0.01");
        }
    }
}
