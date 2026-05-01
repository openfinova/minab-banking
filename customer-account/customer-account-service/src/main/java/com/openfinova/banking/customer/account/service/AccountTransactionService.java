package com.openfinova.banking.customer.account.service;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountTransaction;
import com.openfinova.banking.customer.account.repository.AccountRepository;
import com.openfinova.banking.customer.account.repository.AccountTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/*
 * Implementation of AccountTransactionService for recording and retrieving
 * account-level transactions (memo posts).
 *
 * PURPOSE AND SCOPE
 *
 * This service manages the customer-facing transaction layer, which serves as the
 * "statement view" or "memo post" records that customers see in their account history.
 * These transactions are separate from the underlying General Ledger (GL) transactions
 * and provide a simplified, user-friendly representation of account activity.
 *
 * ARCHITECTURAL CONTEXT
 *
 * In a dual-ledger banking architecture, there are two distinct transaction layers:
 *
 * - Account Transactions (this service): Customer-facing records that appear on statements
 *   and in transaction history. These are created immediately when an operation occurs
 *   (e.g., ATM withdrawal, transfer request).
 *
 * - GL Transactions: The authoritative accounting records that maintain the double-entry
 *   bookkeeping system. These are created during the posting process and may occur
 *   asynchronously.
 *
 * TRANSACTION LIFECYCLE
 *
 * 1. Recording (PENDING): Transaction is created via recordTransaction() with status
 *    "PENDING". This happens immediately when a customer initiates an action.
 *
 * 2. GL Posting: The transaction is posted to the General Ledger by another service
 *    (typically GLTransactionService or AccountFacade).
 *
 * 3. Linking (POSTED): Once GL posting succeeds, updateGLTransactionLink() is called
 *    to link the account transaction to its GL transaction and mark it as "POSTED".
 *
 * FUTURE ENHANCEMENTS
 *
 * - Add transaction reversal/correction capabilities
 * - Support for transaction attachments (receipts, documents)
 * - Enhanced status workflow (AUTHORIZED, CLEARED, SETTLED, REVERSED)
 * - Transaction categorization and tagging
 * - Real-time event publishing for transaction state changes
 */
@Service
@Transactional
public class AccountTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(AccountTransactionService.class);

    private final AccountTransactionRepository accountTransactionRepository;
    private final AccountRepository accountRepository;

    public AccountTransactionService(AccountTransactionRepository accountTransactionRepository,
            AccountRepository accountRepository) {
        this.accountTransactionRepository = accountTransactionRepository;
        this.accountRepository = accountRepository;
    }

    /*
     * Records a new transaction on a customer account.
     *
     * USAGE EXAMPLE
     *
     * // Record an ATM withdrawal
     * AccountTransaction txn = accountTransactionService.recordTransaction(
     *     accountId,
     *     AccountTransactionType.WITHDRAWAL,
     *     new BigDecimal("100.00"),
     *     "USD",
     *     LocalDateTime.now(), // Transaction occurred now
     *     "ATM Withdrawal - Main St Branch",
     *     "ATM-2024-001234"
     * );
     *
     * // Record a backdated correction
     * LocalDateTime correctionDate = LocalDateTime.of(2024, 1, 15, 0, 0);
     * AccountTransaction correction = accountTransactionService.recordTransaction(
     *     accountId,
     *     AccountTransactionType.ADJUSTMENT,
     *     new BigDecimal("50.00"),
     *     "USD",
     *     correctionDate, // Transaction date in the past
     *     "Balance correction for Jan 15",
     *     "CORR-2024-001"
     * );
     *
     * Later, after GL posting succeeds
     * accountTransactionService.updateGLTransactionLink(txn.getId(), glTransactionId);
     */
    public AccountTransaction recordTransaction(UUID accountId, AccountTransactionType transactionType,
            BigDecimal amount, String currency, LocalDateTime transactionDate, String description, String referenceId) {
        logger.info(
                "Recording transaction for account: {} type: {} amount: {} date: {}",
                accountId,
                transactionType,
                amount,
                transactionDate);

        // Validate account exists and load the entity for the foreign key relationship
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new IllegalArgumentException("Account not found: " + accountId));

        // Enforce positive amounts - direction is determined by transaction type
        if (amount == null || amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        // Use current time if no transaction date provided (though callers should provide it)
        LocalDateTime effectiveTransactionDate = transactionDate != null ? transactionDate : LocalDateTime.now();

        // Create the transaction entity using the constructor for required fields
        AccountTransaction transaction = new AccountTransaction(
                account,
                transactionType,
                amount,
                currency,
                effectiveTransactionDate);
        transaction.setDescription(description);
        transaction.setReferenceId(referenceId);
        transaction.setStatus("PENDING"); // Initial status before GL posting

        // Persist the transaction - createdAt and updatedAt are set automatically
        AccountTransaction savedTransaction = accountTransactionRepository.save(transaction);
        logger.info(
                "Transaction recorded successfully: {} with business date: {}",
                savedTransaction.getId(),
                savedTransaction.getTransactionDate());

        return savedTransaction;
    }

    /*
     * Links an account transaction to its corresponding GL transaction after successful posting.
     *
     * Two-Phase Transaction Recording
     * This method implements the second phase of transaction recording. The separation
     * between recording (recordTransaction) and linking (this method) allows for:
     * - Asynchronous GL Posting: The account transaction can be created immediately
     *   for customer visibility, while GL posting happens asynchronously
     * - Retry Logic: If GL posting fails, the account transaction remains in PENDING
     *   state and can be retried without creating duplicate records
     * - Audit Trail: The time gap between creation and posting is preserved in
     *   the database, providing valuable audit information
     * - Reconciliation: Unlinked transactions (PENDING status) can be easily
     *   identified for reconciliation processes
     *
     * IDEMPOTENCY
     * This method is idempotent - calling it multiple times with the same parameters
     * produces the same result. This is important for:
     * - Retry logic in distributed systems
     * - Recovery from partial failures
     * - Simplifying error handling in calling code
     *
     * USAGE EXAMPLE
     *
     * Step 1: Record the transaction (returns immediately)
     * AccountTransaction accountTxn = accountTransactionService.recordTransaction(
     *     accountId, AccountTransactionType.DEPOSIT, amount, "USD", LocalDateTime.now(), "Branch deposit", null
     * );
     *
     * Step 2: Post to GL (may be asynchronous)
     * GLTransaction glTxn = glTransactionService.postTransaction(...);
     *
     * Step 3: Link the two transactions
     * accountTransactionService.updateGLTransactionLink(accountTxn.getId(), glTxn.getId());
     *
     * Now the account transaction is marked as POSTED and linked to the GL
     */
    public void updateGLTransactionLink(UUID accountTransactionId, UUID glTransactionId) {
        logger.info("Linking account transaction: {} to GL transaction: {}", accountTransactionId, glTransactionId);

        // Load the account transaction - fail fast if it doesn't exist
        AccountTransaction transaction = accountTransactionRepository.findById(accountTransactionId).orElseThrow(
                () -> new IllegalArgumentException("Account transaction not found: " + accountTransactionId));

        // Update both the GL link and status atomically
        transaction.setGlTransactionId(glTransactionId);
        transaction.setStatus("POSTED"); // Transition from PENDING to POSTED

        // Persist the changes - this is idempotent
        accountTransactionRepository.save(transaction);
        logger.info("GL transaction link updated successfully");
    }

    /*
     * Retrieves the transaction history for an account within a specified date range.
     */
    @Transactional(readOnly = true)
    public Page<AccountTransaction> getTransactionHistory(UUID accountId, LocalDateTime fromDate, LocalDateTime toDate,
            Pageable pageable) {
        logger.debug(
                "Retrieving transaction history for account: {} from: {} to: {} page: {}",
                accountId,
                fromDate,
                toDate,
                pageable);

        if (fromDate == null || toDate == null) {
            throw new IllegalArgumentException("Date range is required");
        }

        if (fromDate.isAfter(toDate)) {
            throw new IllegalArgumentException("From date must be before or equal to date");
        }

        if (pageable == null) {
            pageable = PageRequest.of(0, 20, Sort.by("transactionDate").descending());
        }

        return accountTransactionRepository.findByAccountAndDateRange(accountId, fromDate, toDate, pageable);
    }

    /*
     * Retrieves a specific account transaction by its unique identifier.
     */
    @Transactional(readOnly = true)
    public AccountTransaction getTransactionById(UUID transactionId) {
        logger.debug("Retrieving transaction by ID: {}", transactionId);

        return accountTransactionRepository.findById(transactionId)
                .orElseThrow(() -> new ResourceNotFoundException("AccountTransaction", transactionId.toString()));
    }
}
