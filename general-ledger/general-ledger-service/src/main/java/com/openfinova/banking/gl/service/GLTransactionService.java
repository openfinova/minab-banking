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
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.gl.api.dto.BulkPostTransactionResponse;
import com.openfinova.banking.gl.api.dto.GLTransactionDTO;
import com.openfinova.banking.gl.api.dto.PostTransactionCommand;
import com.openfinova.banking.gl.api.dto.ValidationError;
import com.openfinova.banking.gl.api.entity.BalanceType;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import com.openfinova.banking.gl.api.entity.GLTransactionSource;
import com.openfinova.banking.gl.api.entity.GLTransactionType;
import com.openfinova.banking.gl.api.entity.ReconciliationStatus;
import com.openfinova.banking.gl.entity.FiscalPeriod;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.GLJournalEntry;
import com.openfinova.banking.gl.entity.GLReconciliation;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.mapper.GLEntityMapper;
import com.openfinova.banking.gl.repository.GLReconciliationRepository;
import com.openfinova.banking.gl.repository.GLRevaluationDetailRepository;
import com.openfinova.banking.gl.repository.GLTransactionRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Implementation of GLTransactionService providing comprehensive transaction management.
 * This service handles the complete lifecycle of GL transactions including creation, validation,
 * posting, reversal, and balance validation.
 *
 * Key responsibilities:
 * - Transaction lifecycle management (create, post, reverse)
 * - Transaction-level validation and business rule enforcement
 * - Balance validation across all journal entries in a transaction
 * - Integration with journal entries and GL accounts
 */
@Service
@Transactional
public class GLTransactionService {

    private static final Logger logger = LoggerFactory.getLogger(GLTransactionService.class);

    // Error message constants for consistent exception handling
    private static final String ERR_NO_ENTRIES = "Transaction must have at least one journal entry";
    private static final String ERR_NOT_BALANCED = "Transaction is not balanced";
    private static final String ERR_ENTRY_NO_ACCOUNT = "Journal entry must have an account";
    private static final String ERR_INACTIVE_ACCOUNT = "Cannot post to inactive account";
    private static final String ERR_PARENT_ACCOUNT = "Cannot post directly to a parent (header) account — post to a leaf account";
    private static final String ERR_VALUE_DATE_CLOSED = "Journal entry valueDate falls in a closed or locked fiscal period";
    private static final String ERR_CURRENCY_MISMATCH = "Journal entry currency does not match account currency";
    private static final String ERR_BOTH_DEBIT_CREDIT = "Journal entry cannot have both debit and credit amounts";
    private static final String ERR_MISSING_AMOUNT = "Journal entry must have either debit or credit amount";
    private static final String ERR_ALREADY_POSTED = "Transaction is already posted";
    private static final String ERR_NORMAL_BALANCE = "Journal entry would violate normal balance for account";
    private static final BigDecimal BALANCE_TOLERANCE = new BigDecimal("0.01");

    private final GLTransactionRepository transactionRepository;
    private final AuditService auditService;
    private final GLAccountService accountService;
    private final BalanceService balanceService;
    private final FiscalPeriodService fiscalPeriodService;
    private final GLRevaluationDetailRepository revaluationDetailRepository;
    private final GLReconciliationRepository reconciliationRepository;
    private final TransactionNumberingService numberingService;
    private final ApprovalWorkflowService approvalWorkflowService;
    private final DateTimeService dateTimeService;

    /**
     * Constructor for dependency injection.
     *
     * @param transactionRepository the GL transaction repository
     * @param auditService the audit service for regulatory compliance audit logging
     * @param accountService the GL account service for account lookups
     * @param balanceService the balance service for balance-at-date and accounting equation
     * @param fiscalPeriodService the fiscal period service for period validation
     * @param revaluationDetailRepository the revaluation detail repository for dependency checking
     * @param reconciliationRepository the reconciliation repository for reversal enforcement
     * @param numberingService the transaction numbering service for gapless sequence generation
     * @param approvalWorkflowService the approval workflow service for maker-checker approval
     */
    public GLTransactionService(GLTransactionRepository transactionRepository, AuditService auditService,
            GLAccountService accountService, BalanceService balanceService, FiscalPeriodService fiscalPeriodService,
            GLRevaluationDetailRepository revaluationDetailRepository,
            GLReconciliationRepository reconciliationRepository, TransactionNumberingService numberingService,
            ApprovalWorkflowService approvalWorkflowService, DateTimeService dateTimeService) {
        this.transactionRepository = transactionRepository;
        this.auditService = auditService;
        this.accountService = accountService;
        this.balanceService = balanceService;
        this.fiscalPeriodService = fiscalPeriodService;
        this.revaluationDetailRepository = revaluationDetailRepository;
        this.reconciliationRepository = reconciliationRepository;
        this.numberingService = numberingService;
        this.approvalWorkflowService = approvalWorkflowService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Posts a complete transaction containing multiple journal entries.
     * Validates idempotency via referenceId and ensures debits equal credits.
     *
     * @param transaction The transaction to post.
     * @return The posted transaction.
     */
    @PreAuthorize("hasAnyAuthority('gl:post', 'service:gl:write')")

    public GLTransaction postTransaction(GLTransaction transaction) {
        logger.info("Posting transaction: {}", transaction.getReferenceId());

        // Validate the transaction before posting
        validateTransaction(transaction);

        // Check if transaction is already posted
        if (transaction.isPosted()) {
            throw new IllegalStateException(ERR_ALREADY_POSTED + ": " + transaction.getReferenceId());
        }

        // Determine fiscal period for transaction date
        FiscalPeriod fiscalPeriod = fiscalPeriodService.getFiscalPeriodForDate(transaction.getTransactionDate())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No fiscal period found for transaction date: " + transaction.getTransactionDate()));

        // Assign gapless transaction number (with pessimistic locking)
        Long transactionNumber = numberingService.getNextTransactionNumber(fiscalPeriod.getId());
        transaction.setTransactionNumber(transactionNumber);

        logger.debug(
                "Assigned transaction number {} to transaction {}",
                transactionNumber,
                transaction.getReferenceId());

        // Capture old status for audit
        String oldStatus = transaction.getStatus().toString();

        // Post the transaction
        transaction.approveAndPost(transaction.getCreatedBy(), dateTimeService.instant());

        GLTransaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Successfully posted transaction: {}", savedTransaction.getReferenceId());

        // Audit log: status change (PENDING -> POSTED)
        BigDecimal totalAmount = calculateTotalAmount(savedTransaction);
        Map<String, Object> oldValues = Map.of(
                "status",
                oldStatus,
                "referenceId",
                savedTransaction.getReferenceId(),
                "transactionNumber",
                savedTransaction.getTransactionNumber() != null ? savedTransaction.getTransactionNumber().toString()
                        : "pending");
        Map<String, Object> newValues = Map.of(
                "status",
                savedTransaction.getStatus().toString(),
                "referenceId",
                savedTransaction.getReferenceId(),
                "transactionNumber",
                savedTransaction.getTransactionNumber().toString());
        auditService.logAudit(
                GLEntityType.GL_TRANSACTION,
                savedTransaction.getId(),
                GLAuditAction.STATUS_CHANGE,
                savedTransaction.getCreatedBy(),
                oldValues,
                newValues,
                "Transaction posted",
                totalAmount,
                savedTransaction.getCurrency(),
                null, // no correlation ID for single transaction post
                null, // TODO: IP address from SecurityContext
                null // TODO: session ID from SecurityContext
        );

        return savedTransaction;
    }

    /**
     * Reverses an entire transaction by creating a set of contra-entries.
     *
     * @param transactionId The UUID of the transaction to reverse.
     * @param reason        The reason for reversal.
     * @param reversedBy    The user or system performing the reversal.
     * @return The reversal transaction.
     */
    @PreAuthorize("hasAnyAuthority('gl:post', 'service:gl:write')")

    public GLTransaction reverseTransaction(UUID transactionId, String reason, String reversedBy) {
        logger.info("Reversing transaction: {} by {}", transactionId, reversedBy);

        Optional<GLTransaction> originalTransactionOpt = transactionRepository.findById(transactionId);
        if (originalTransactionOpt.isEmpty()) {
            throw new ResourceNotFoundException("GLTransaction", transactionId);
        }

        GLTransaction originalTransaction = originalTransactionOpt.get();

        // Comprehensive reversal validation
        validateReversalAllowed(originalTransaction);

        // Calculate transaction amount for audit
        BigDecimal originalAmount = calculateTotalAmount(originalTransaction);

        // Audit log BEFORE reversal (as recommended in plan)
        Map<String, Object> oldValues = Map.of(
                "transactionId",
                transactionId.toString(),
                "status",
                originalTransaction.getStatus().toString(),
                "amount",
                originalAmount.toString(),
                "referenceId",
                originalTransaction.getReferenceId());

        // Create reversal transaction
        GLTransaction reversalTransaction = new GLTransaction(
                GLTransactionType.REVERSAL.generateReferenceId(originalTransaction.getReferenceId()),
                "Reversal of: " + reason,
                originalTransaction.getTransactionDate());

        // Create contra journal entries
        for (GLJournalEntry originalEntry : originalTransaction.getJournalEntries()) {
            GLJournalEntry reversalEntry = new GLJournalEntry();
            reversalEntry.setAccount(originalEntry.getAccount());
            reversalEntry.setValueDate(originalEntry.getValueDate());
            reversalEntry.setCurrency(originalEntry.getCurrency());
            reversalEntry.setExchangeRate(originalEntry.getExchangeRate());
            reversalEntry.setDescription("Reversal of: " + originalEntry.getDescription());

            // Reverse the amounts (debit becomes credit, credit becomes debit)
            if (originalEntry.isDebit()) {
                reversalEntry.setCreditAmount(originalEntry.getDebitAmount());
                reversalEntry.setBaseCreditAmount(originalEntry.getBaseDebitAmount());
            } else {
                reversalEntry.setDebitAmount(originalEntry.getCreditAmount());
                reversalEntry.setBaseDebitAmount(originalEntry.getBaseCreditAmount());
            }

            reversalTransaction.addGLJournalEntry(reversalEntry);
        }

        // Assign gapless transaction number to the reversal (mirrors postTransaction logic)
        FiscalPeriod reversalFiscalPeriod = fiscalPeriodService
                .getFiscalPeriodForDate(reversalTransaction.getTransactionDate()).orElseThrow(
                        () -> new IllegalStateException(
                                "No fiscal period found for reversal transaction date: "
                                        + reversalTransaction.getTransactionDate()));
        Long reversalTransactionNumber = numberingService.getNextTransactionNumber(reversalFiscalPeriod.getId());
        reversalTransaction.setTransactionNumber(reversalTransactionNumber);

        logger.debug("Assigned transaction number {} to reversal of {}", reversalTransactionNumber, transactionId);

        // Post the reversal and persist the final state (status + transactionNumber)
        reversalTransaction.approveAndPost(reversedBy, dateTimeService.instant());
        GLTransaction savedReversalTransaction = transactionRepository.save(reversalTransaction);

        // Mark original transaction as reversed
        originalTransaction.markReversed(savedReversalTransaction);
        transactionRepository.save(originalTransaction);

        logger.info(
                "Successfully reversed transaction: {} with reversal: {}",
                transactionId,
                savedReversalTransaction.getId());

        // Complete audit log for reversal (mandatory reason)
        Map<String, Object> newValues = Map.of(
                "reversalTransactionId",
                savedReversalTransaction.getId().toString(),
                "reversalReferenceId",
                savedReversalTransaction.getReferenceId(),
                "newStatus",
                "REVERSED");
        auditService.logAudit(
                GLEntityType.GL_TRANSACTION,
                transactionId,
                GLAuditAction.REVERSE,
                reversedBy,
                oldValues,
                newValues,
                reason, // Mandatory reason for REVERSE action
                originalAmount,
                originalTransaction.getCurrency(),
                null, // no correlation ID for single reversal
                null, // TODO: IP address from SecurityContext
                null // TODO: session ID from SecurityContext
        );

        return savedReversalTransaction;
    }

    /**
     * Retrieves a transaction by its unique ID.
     *
     * @param id The UUID of the transaction.
     * @return An Optional containing the transaction if found.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public Optional<GLTransaction> getTransactionById(UUID id) {
        logger.debug("Getting transaction by ID: {}", id);
        return transactionRepository.findById(id);
    }

    /**
     * Retrieves a transaction by its external reference ID.
     *
     * @param referenceId The external reference ID.
     * @return An Optional containing the transaction if found.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public Optional<GLTransaction> getTransactionByReference(String referenceId) {
        logger.debug("Getting transaction by reference: {}", referenceId);
        return transactionRepository.findByReferenceId(referenceId);
    }

    /**
     * Validates that a transaction is balanced and adheres to GL rules.
     *
     * @param transaction The transaction to validate.
     * @throws IllegalStateException if the transaction is invalid.
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    public void validateTransaction(GLTransaction transaction) {
        if (transaction == null) {
            throw new IllegalArgumentException("Transaction cannot be null");
        }

        logger.debug("Validating transaction: {}", transaction.getReferenceId());

        // Validate fiscal period - cannot post to closed or locked periods
        if (!fiscalPeriodService.isPostingAllowedForDate(transaction.getTransactionDate())) {
            throw new IllegalStateException(
                    "Cannot post transaction to closed fiscal period. Transaction date: "
                            + transaction.getTransactionDate() + ". Please verify the fiscal period status.");
        }

        if (transaction.getJournalEntries() == null || transaction.getJournalEntries().isEmpty()) {
            throw new IllegalStateException(ERR_NO_ENTRIES);
        }

        // Validate that transaction is balanced by checking journal entries directly
        List<GLJournalEntry> entries = transaction.getJournalEntries();

        BigDecimal totalDebits = entries.stream().filter(GLJournalEntry::isDebit)
                .map(GLJournalEntry::getBaseDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entries.stream().filter(GLJournalEntry::isCredit)
                .map(GLJournalEntry::getBaseCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalDebits.compareTo(totalCredits) != 0) {
            throw new IllegalStateException(ERR_NOT_BALANCED + ": debits=" + totalDebits + ", credits=" + totalCredits);
        }

        // Validate individual journal entries
        for (GLJournalEntry entry : entries) {
            GLAccount account = entry.getAccount();

            if (account == null) {
                throw new IllegalStateException(ERR_ENTRY_NO_ACCOUNT);
            }

            if (!account.isActive()) {
                throw new IllegalStateException(ERR_INACTIVE_ACCOUNT + ": " + account.getCode());
            }

            if (account.hasChildren()) {
                throw new IllegalStateException(ERR_PARENT_ACCOUNT + ": " + account.getCode());
            }

            // Validate that the value date (effective accounting date) is in an open period
            if (entry.getValueDate() != null && !fiscalPeriodService.isPostingAllowedForDate(entry.getValueDate())) {
                throw new IllegalStateException(
                        ERR_VALUE_DATE_CLOSED + ": valueDate=" + entry.getValueDate() + " on account "
                                + account.getCode());
            }

            // Validate that the entry currency matches the account's designated currency
            if (entry.getCurrency() != null && account.getCurrency() != null
                    && !entry.getCurrency().equals(account.getCurrency())) {
                throw new IllegalStateException(
                        ERR_CURRENCY_MISMATCH + ": entry currency=" + entry.getCurrency() + ", account currency="
                                + account.getCurrency() + " on account " + account.getCode());
            }

            // Validate amounts
            boolean hasDebit = entry.getDebitAmount() != null && entry.getDebitAmount().compareTo(BigDecimal.ZERO) > 0;
            boolean hasCredit = entry.getCreditAmount() != null
                    && entry.getCreditAmount().compareTo(BigDecimal.ZERO) > 0;

            if (hasDebit && hasCredit) {
                throw new IllegalStateException(ERR_BOTH_DEBIT_CREDIT);
            }

            if (!hasDebit && !hasCredit) {
                throw new IllegalStateException(ERR_MISSING_AMOUNT);
            }

            // Normal balance rule: posting must not push account into opposite balance (e.g. asset with credit balance)
            validateNormalBalance(entry, account, transaction.getTransactionDate());
        }

        logger.debug("Transaction validation passed: {}", transaction.getReferenceId());
    }

    /**
     * Validates that the journal entry would not push the account balance into the opposite
     * of its normal balance (e.g. asset with credit balance, liability with debit balance).
     * Uses balance at the day before value date so the check is consistent with posting order.
     */
    private void validateNormalBalance(GLJournalEntry entry, GLAccount account, LocalDate transactionDate) {
        LocalDate asOfDate = entry.getValueDate() != null ? entry.getValueDate() : transactionDate;
        BigDecimal currentBalance = balanceService.getBalanceAtDate(account.getId(), asOfDate);
        BigDecimal debitAmt = entry.getBaseDebitAmount() != null ? entry.getBaseDebitAmount() : BigDecimal.ZERO;
        BigDecimal creditAmt = entry.getBaseCreditAmount() != null ? entry.getBaseCreditAmount() : BigDecimal.ZERO;
        // Signed impact: debit-normal account balance increases with debit, decreases with credit; credit-normal opposite
        BigDecimal impact = account.getNormalBalance() == BalanceType.DEBIT ? debitAmt.subtract(creditAmt)
                : creditAmt.subtract(debitAmt);
        BigDecimal projectedBalance = currentBalance.add(impact);
        if (projectedBalance.compareTo(BALANCE_TOLERANCE.negate()) < 0) {
            throw new IllegalStateException(
                    ERR_NORMAL_BALANCE + " " + account.getCode() + ". Normal balance: " + account.getNormalBalance()
                            + ", current: " + currentBalance + ", entry impact: " + impact + ", projected: "
                            + projectedBalance + ". Use an adjusting entry or contra account.");
        }
    }

    /**
     * Validates the accounting equation (Assets = Liabilities + Equity) as of a given date.
     * Section totals use balance-sheet convention: contra accounts reduce their section total.
     * Call at period end or after batch posting to detect ledger inconsistencies.
     *
     * @param asOfDate date for balance snapshot
     * @throws IllegalStateException if |Assets - (Liabilities + Equity)| &gt; tolerance
     */
    @Transactional(readOnly = true)
    public void validateAccountingEquation(LocalDate asOfDate) {
        BigDecimal totalAssets = BigDecimal.ZERO;
        BigDecimal totalLiabilities = BigDecimal.ZERO;
        BigDecimal totalEquity = BigDecimal.ZERO;

        for (GLAccount account : accountService.getAccountsByType(GLAccountType.ASSET, GLAccountStatus.ACTIVE)) {
            BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), asOfDate);
            BigDecimal contribution = account.isContra() ? balance.negate() : balance;
            totalAssets = totalAssets.add(contribution);
        }
        for (GLAccount account : accountService.getAccountsByType(GLAccountType.LIABILITY, GLAccountStatus.ACTIVE)) {
            BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), asOfDate);
            BigDecimal contribution = account.isContra() ? balance.negate() : balance;
            totalLiabilities = totalLiabilities.add(contribution);
        }
        for (GLAccount account : accountService.getAccountsByType(GLAccountType.EQUITY, GLAccountStatus.ACTIVE)) {
            BigDecimal balance = balanceService.getBalanceAtDate(account.getId(), asOfDate);
            BigDecimal contribution = account.isContra() ? balance.negate() : balance;
            totalEquity = totalEquity.add(contribution);
        }

        BigDecimal liabPlusEquity = totalLiabilities.add(totalEquity);
        BigDecimal difference = totalAssets.subtract(liabPlusEquity);
        if (difference.abs().compareTo(BALANCE_TOLERANCE) > 0) {
            throw new IllegalStateException(
                    "Accounting equation violated as of " + asOfDate + ". Assets=" + totalAssets + ", Liabilities="
                            + totalLiabilities + ", Equity=" + totalEquity + ", Liabilities+Equity=" + liabPlusEquity
                            + ", difference=" + difference + ". Reconcile ledger or run trial balance.");
        }
        logger.debug(
                "Accounting equation validated as of {}: Assets={} = Liabilities+Equity={}",
                asOfDate,
                totalAssets,
                liabPlusEquity);
    }

    /**
     * Validates that all journal entries in a transaction are balanced.
     * Moved from GLJournalEntryService for better service boundaries.
     *
     * @param transactionId The UUID of the transaction.
     * @return true if the transaction is balanced (total debits = total credits).
     */
    @PreAuthorize("hasAnyAuthority('gl:read', 'service:gl:read')")

    @Transactional(readOnly = true)
    public boolean validateTransactionBalance(UUID transactionId) {
        logger.debug("Validating transaction balance for: {}", transactionId);

        Optional<GLTransaction> transactionOpt = transactionRepository.findById(transactionId);
        if (transactionOpt.isEmpty()) {
            logger.warn("Transaction not found: {}", transactionId);
            return false;
        }

        GLTransaction transaction = transactionOpt.get();
        List<GLJournalEntry> entries = transaction.getJournalEntries();

        if (entries.isEmpty()) {
            logger.warn("No journal entries found for transaction: {}", transactionId);
            return false;
        }

        BigDecimal totalDebits = entries.stream().filter(GLJournalEntry::isDebit)
                .map(GLJournalEntry::getBaseDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalCredits = entries.stream().filter(GLJournalEntry::isCredit)
                .map(GLJournalEntry::getBaseCreditAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        boolean isBalanced = totalDebits.compareTo(totalCredits) == 0;

        if (!isBalanced) {
            logger.warn(
                    "Transaction {} is not balanced: debits={}, credits={}",
                    transactionId,
                    totalDebits,
                    totalCredits);
        }

        return isBalanced;
    }

    /**
     * Calculate total transaction amount (sum of all debits) for audit logging.
     * Used for materiality analysis in regulatory reporting.
     *
     * @param transaction the transaction to calculate amount for
     * @return total debit amount (which equals total credit in a balanced transaction)
     */
    private BigDecimal calculateTotalAmount(GLTransaction transaction) {
        if (transaction == null || transaction.getJournalEntries() == null) {
            return BigDecimal.ZERO;
        }

        return transaction.getJournalEntries().stream().filter(GLJournalEntry::isDebit)
                .map(GLJournalEntry::getDebitAmount).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Validates a batch of transaction commands without posting them.
     * Useful for fail-fast validation before attempting to post any transactions.
     *
     * @param commands the list of transaction commands to validate
     * @return list of validation errors (empty if all are valid)
     */
    public List<ValidationError> validateTransactionBatch(List<PostTransactionCommand> commands) {
        logger.info("Validating batch of {} transactions", commands.size());

        List<ValidationError> errors = new ArrayList<>();

        for (int i = 0; i < commands.size(); i++) {
            PostTransactionCommand command = commands.get(i);

            try {
                // Convert command to entity for validation
                GLTransaction transaction = GLEntityMapper.toEntity(command, accountService);

                // Validate the transaction
                validateTransaction(transaction);

            } catch (Exception e) {
                logger.warn("Validation failed for transaction at index {}: {}", i, e.getMessage());
                errors.add(new ValidationError(i, command.getReferenceId(), e.getMessage(), determineErrorCode(e)));
            }
        }

        logger.info("Batch validation complete: {} errors found in {} transactions", errors.size(), commands.size());
        return errors;
    }

    /**
     * Posts multiple transactions in a single operation.
     * Processes transactions individually with detailed error reporting.
     * Uses a correlation ID to link all transactions in the batch for audit trail.
     *
     * @param commands list of transaction commands to post
     * @param correlationId correlation ID for tracking this batch across systems
     * @param validateFirst whether to validate all transactions before posting any
     * @return response containing successful transactions and validation errors
     */
    @Transactional
    public BulkPostTransactionResponse postTransactions(List<PostTransactionCommand> commands, UUID correlationId,
            boolean validateFirst) {

        logger.info("Posting bulk transactions: {} items with correlation ID: {}", commands.size(), correlationId);

        BulkPostTransactionResponse response = new BulkPostTransactionResponse(commands.size());

        // Optional: validate all first if requested (fail-fast approach)
        if (validateFirst) {
            List<ValidationError> preValidationErrors = validateTransactionBatch(commands);
            if (!preValidationErrors.isEmpty()) {
                logger.warn("Pre-validation failed: {} errors found", preValidationErrors.size());
                response.setValidationErrors(preValidationErrors);
                response.setSuccessCount(0);
                response.setFailureCount(preValidationErrors.size());
                return response;
            }
        }

        // Process each transaction individually
        for (int i = 0; i < commands.size(); i++) {
            PostTransactionCommand command = commands.get(i);

            try {
                // Convert command to entity
                GLTransaction transaction = GLEntityMapper.toEntity(command, accountService);

                // Validate (if not already validated)
                if (!validateFirst) {
                    validateTransaction(transaction);
                }

                // Check if already posted (idempotency)
                if (transaction.isPosted()) {
                    throw new IllegalStateException(ERR_ALREADY_POSTED + ": " + transaction.getReferenceId());
                }

                // Determine fiscal period for transaction date
                FiscalPeriod fiscalPeriod = fiscalPeriodService.getFiscalPeriodForDate(transaction.getTransactionDate())
                        .orElseThrow(
                                () -> new IllegalStateException(
                                        "No fiscal period found for transaction date: "
                                                + transaction.getTransactionDate()));

                // Assign gapless transaction number (with pessimistic locking)
                Long transactionNumber = numberingService.getNextTransactionNumber(fiscalPeriod.getId());
                transaction.setTransactionNumber(transactionNumber);

                // Capture old status for audit
                String oldStatus = transaction.getStatus().toString();

                // Post the transaction
                transaction.approveAndPost(transaction.getCreatedBy(), dateTimeService.instant());
                GLTransaction savedTransaction = transactionRepository.save(transaction);

                // Audit log with correlation ID
                BigDecimal totalAmount = calculateTotalAmount(savedTransaction);
                Map<String, Object> oldValues = Map.of(
                        "status",
                        oldStatus,
                        "referenceId",
                        savedTransaction.getReferenceId(),
                        "transactionNumber",
                        "pending");
                Map<String, Object> newValues = Map.of(
                        "status",
                        savedTransaction.getStatus().toString(),
                        "referenceId",
                        savedTransaction.getReferenceId(),
                        "transactionNumber",
                        savedTransaction.getTransactionNumber().toString(),
                        "batchIndex",
                        i,
                        "batchSize",
                        commands.size());
                auditService.logAudit(
                        GLEntityType.GL_TRANSACTION,
                        savedTransaction.getId(),
                        GLAuditAction.STATUS_CHANGE,
                        savedTransaction.getCreatedBy(),
                        oldValues,
                        newValues,
                        "Bulk transaction posted (batch " + (i + 1) + " of " + commands.size() + ")",
                        totalAmount,
                        savedTransaction.getCurrency(),
                        correlationId,
                        null, // TODO: IP address from SecurityContext
                        null // TODO: session ID from SecurityContext
                );

                // Convert to DTO and add to success list
                GLTransactionDTO dto = GLEntityMapper.toDTO(savedTransaction);
                response.addSuccessfulTransaction(dto);

                logger.debug(
                        "Successfully posted transaction {}/{}: {}",
                        i + 1,
                        commands.size(),
                        savedTransaction.getReferenceId());

            } catch (Exception e) {
                logger.error("Failed to post transaction at index {}: {}", i, e.getMessage(), e);
                response.addValidationError(
                        new ValidationError(i, command.getReferenceId(), e.getMessage(), determineErrorCode(e)));
            }
        }

        logger.info(
                "Bulk posting complete: {} successful, {} failed out of {} total",
                response.getSuccessCount(),
                response.getFailureCount(),
                response.getTotalSubmitted());

        return response;
    }

    /**
     * Extracts error code from exception message.
     * Returns the full message which contains both the error type and contextual details.
     */
    private String determineErrorCode(Exception e) {
        String message = e.getMessage();
        return message != null ? message : "UNKNOWN_ERROR";
    }

    // ========== Reversal Validation Methods ==========

    /**
     * Checks if a transaction is system-generated based on its reference ID.
     * System-generated transactions include:
     * - REVERSAL (REV-)
     * - PERIOD_CLOSING (CLOSING-)
     * - CURRENCY_REVALUATION (REVAL-)
     *
     * @param transaction the transaction to check
     * @return true if the transaction is system-generated
     */
    private boolean isSystemGeneratedTransaction(GLTransaction transaction) {
        return GLTransactionType.isSystemGeneratedReferenceId(transaction.getReferenceId());
    }

    /**
     * Validates that a transaction can be reversed.
     * Performs comprehensive checks including:
     * - Transaction must be posted
     * - Transaction must not already be reversed
     * - System-generated transactions cannot be reversed (CLOSING-, REVAL-)
     * - Reversal transactions cannot be reversed (prevents reversal-of-reversal)
     * - Fiscal period must allow posting (not CLOSED or LOCKED)
     * - Transaction must not have revaluation dependencies
     *
     * @param transaction the transaction to validate
     * @throws IllegalStateException if the transaction cannot be reversed
     */
    private void validateReversalAllowed(GLTransaction transaction) {
        UUID transactionId = transaction.getId();
        String referenceId = transaction.getReferenceId();

        // 1. Basic state validations
        if (!transaction.isPosted()) {
            throw new IllegalStateException("Only posted transactions can be reversed: " + transactionId);
        }

        if (transaction.isReversed()) {
            throw new IllegalStateException("Transaction is already reversed: " + transactionId);
        }

        // 2. System transaction protection - only user-initiated transactions can be reversed
        GLTransactionType transactionType = GLTransactionType.fromReferenceId(referenceId);

        if (transactionType.isSystemGenerated()) {
            throw new IllegalStateException(
                    "System-generated transactions cannot be reversed. " + "Transaction: " + transactionId
                            + ", Reference: " + referenceId + ", Type: " + transactionType + ". "
                            + "Only user-initiated transactions can be reversed.");
        }

        // 3. Fiscal period validation
        if (!fiscalPeriodService.isPostingAllowedForDate(transaction.getTransactionDate())) {
            throw new IllegalStateException(
                    "Cannot reverse transaction in a closed or locked fiscal period. " + "Transaction date: "
                            + transaction.getTransactionDate() + ". "
                            + "Please reopen the fiscal period before reversing.");
        }

        // 4. Revaluation dependency check
        if (hasRevaluationDependency(transactionId)) {
            throw new IllegalStateException(
                    "Cannot reverse transaction that has subsequent currency revaluation dependencies. "
                            + "Transaction: " + transactionId + ". "
                            + "Please reverse or adjust dependent revaluations first.");
        }

        // 5. Reconciliation check — reconciled transactions cannot be reversed
        if (isReconciled(transaction.getId())) {
            LocalDate reconciledOn = getReconciliationDate(transaction.getId()).orElse(null);
            String dateStr = reconciledOn != null ? reconciledOn.toString() : "unknown date";
            throw new IllegalStateException(
                    "Cannot reverse reconciled transaction. " + "Transaction: " + transaction.getId()
                            + " was reconciled on " + dateStr + ". " + "Create an adjusting journal entry instead.");
        }
    }

    /**
     * Returns whether the given transaction has been reconciled against an external statement.
     * Reconciled transactions cannot be reversed.
     */
    private boolean isReconciled(UUID transactionId) {
        return reconciliationRepository.findByTransactionIdAndStatus(transactionId, ReconciliationStatus.RECONCILED)
                .isPresent();
    }

    /**
     * Returns the reconciliation date for a transaction if it is reconciled.
     */
    private Optional<LocalDate> getReconciliationDate(UUID transactionId) {
        return reconciliationRepository.findByTransactionIdAndStatus(transactionId, ReconciliationStatus.RECONCILED)
                .map(GLReconciliation::getReconciliationDate);
    }

    /**
     * Checks if a transaction has revaluation dependencies.
     * A transaction has revaluation dependencies if it appears in any GLRevaluationDetail records,
     * meaning that a subsequent currency revaluation was performed on accounts affected by this transaction.
     *
     * @param transactionId the transaction ID to check
     * @return true if the transaction has revaluation dependencies
     */
    private boolean hasRevaluationDependency(UUID transactionId) {
        return !revaluationDetailRepository.findByJournalTransactionId(transactionId).isEmpty();
    }

    /**
     * Create a draft GL transaction for manual journal entries.
     * Draft transactions are not posted to the GL and do not affect balances.
     * They must be submitted for approval and approved before posting.
     *
     * @param command the transaction command
     * @param createdBy username of the creator (maker)
     * @return the created draft transaction
     */
    @PreAuthorize("hasAuthority('gl:post')")

    public GLTransaction createDraftTransaction(PostTransactionCommand command, String createdBy) {
        logger.info("Creating draft transaction: {}", command.getReferenceId());

        // Build transaction entity from command
        GLTransaction transaction = GLEntityMapper.toEntity(command, accountService);
        transaction.setStatus(com.openfinova.banking.gl.api.entity.GLTransactionStatus.DRAFT);

        // Set source (default to MANUAL_ENTRY if not specified)
        if (transaction.getSource() == null) {
            transaction.setSource(GLTransactionSource.MANUAL_ENTRY);
        }

        // Validate the transaction (business rules, balancing, etc.)
        validateTransaction(transaction);

        // Save draft (no transaction number assigned yet, no balance updates)
        GLTransaction savedTransaction = transactionRepository.save(transaction);

        logger.info(
                "Created draft transaction: {} with ID {}",
                savedTransaction.getReferenceId(),
                savedTransaction.getId());

        // Audit log: draft created
        BigDecimal totalAmount = calculateTotalAmount(savedTransaction);
        auditService.logAudit(
                GLEntityType.GL_TRANSACTION,
                savedTransaction.getId(),
                GLAuditAction.CREATE,
                createdBy,
                Map.of(),
                Map.of(
                        "status",
                        savedTransaction.getStatus().toString(),
                        "referenceId",
                        savedTransaction.getReferenceId(),
                        "source",
                        savedTransaction.getSource().toString()),
                "Draft transaction created",
                totalAmount,
                savedTransaction.getCurrency(),
                null,
                null,
                null);

        return savedTransaction;
    }

    /**
     * Submit a draft transaction for approval.
     * Transaction moves to PENDING_APPROVAL status.
     *
     * @param transactionId the transaction ID
     * @param submitterUsername username of the submitter
     * @param submitterRole role of the submitter
     * @throws IllegalStateException if transaction is not in DRAFT status
     * @throws SecurityException if submitter doesn't have authority
     */
    @PreAuthorize("hasAuthority('gl:post')")

    public void submitTransactionForApproval(UUID transactionId, String submitterUsername,
            GLApprovalRole submitterRole) {
        logger.info("Submitting transaction {} for approval by {}", transactionId, submitterUsername);

        GLTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // System-generated transactions bypass the maker-checker workflow: post immediately.
        if (transaction.isSystemGenerated()) {
            logger.info("System-generated transaction {} auto-posting directly", transactionId);
            postTransaction(transaction);
            return;
        }

        // Delegate to approval workflow service
        approvalWorkflowService.submitForApproval(transactionId, submitterUsername, submitterRole);

        // Audit log — re-read the saved state for accurate status
        GLTransaction savedTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        BigDecimal totalAmount = calculateTotalAmount(savedTransaction);
        auditService.logAudit(
                GLEntityType.GL_TRANSACTION,
                transactionId,
                GLAuditAction.STATUS_CHANGE,
                submitterUsername,
                Map.of("status", "DRAFT"),
                Map.of("status", "PENDING_APPROVAL", "submittedBy", submitterUsername),
                "Transaction submitted for approval",
                totalAmount,
                savedTransaction.getCurrency(),
                null,
                null,
                null);
    }

    /**
     * Approve and post a pending transaction.
     * If all required approvals are received, transaction is posted to GL and balances updated.
     *
     * @param transactionId the transaction ID
     * @param approverUsername username of the approver (checker)
     * @param approverRole role of the approver
     * @param comments optional approval comments
     * @param ipAddress IP address of approver (for audit)
     * @return true if transaction was approved and posted
     * @throws IllegalStateException if transaction is not pending approval
     * @throws SecurityException if approver doesn't have authority
     */
    @PreAuthorize("hasAuthority('gl:approve')")

    public boolean approveAndPostTransaction(UUID transactionId, String approverUsername, GLApprovalRole approverRole,
            String comments, String ipAddress) {
        logger.info("Approving transaction {} by {}", transactionId, approverUsername);

        // Delegate approval to workflow service
        boolean fullyApproved = approvalWorkflowService
                .approveTransaction(transactionId, approverUsername, approverRole, comments, ipAddress);

        if (fullyApproved) {
            // All required approvals received - post transaction
            GLTransaction transaction = transactionRepository.findById(transactionId)
                    .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

            // Determine fiscal period
            FiscalPeriod fiscalPeriod = fiscalPeriodService.getFiscalPeriodForDate(transaction.getTransactionDate())
                    .orElseThrow(
                            () -> new IllegalStateException(
                                    "No fiscal period found for transaction date: "
                                            + transaction.getTransactionDate()));

            // Assign transaction number (gapless sequence)
            Long transactionNumber = numberingService.getNextTransactionNumber(fiscalPeriod.getId());
            transaction.setTransactionNumber(transactionNumber);

            // Approve and post
            transaction.approveAndPost(approverUsername, dateTimeService.instant());
            transactionRepository.save(transaction);

            logger.info("Transaction {} fully approved and posted with number {}", transactionId, transactionNumber);

            // Audit log
            BigDecimal totalAmount = calculateTotalAmount(transaction);
            auditService.logAudit(
                    GLEntityType.GL_TRANSACTION,
                    transactionId,
                    GLAuditAction.STATUS_CHANGE,
                    approverUsername,
                    Map.of("status", "PENDING_APPROVAL"),
                    Map.of(
                            "status",
                            "POSTED",
                            "postedBy",
                            approverUsername,
                            "transactionNumber",
                            transactionNumber.toString()),
                    "Transaction approved and posted",
                    totalAmount,
                    transaction.getCurrency(),
                    null,
                    ipAddress,
                    null);

            return true;
        } else {
            logger.info("Transaction {} approved by {} but needs more approvals", transactionId, approverUsername);
            return false;
        }
    }

    /**
     * Reject a pending transaction.
     * Transaction moves to REJECTED status and will not be posted.
     *
     * @param transactionId the transaction ID
     * @param rejecterUsername username of the rejecter
     * @param rejecterRole role of the rejecter
     * @param reason rejection reason (required)
     * @param ipAddress IP address of rejecter (for audit)
     * @throws IllegalStateException if transaction is not pending approval
     * @throws SecurityException if rejecter doesn't have authority
     */
    @PreAuthorize("hasAuthority('gl:approve')")

    public void rejectTransaction(UUID transactionId, String rejecterUsername, GLApprovalRole rejecterRole,
            String reason, String ipAddress) {
        logger.info("Rejecting transaction {} by {}: {}", transactionId, rejecterUsername, reason);

        // Delegate to approval workflow service
        approvalWorkflowService.rejectTransaction(transactionId, rejecterUsername, rejecterRole, reason, ipAddress);

        // Audit log
        GLTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        BigDecimal totalAmount = calculateTotalAmount(transaction);
        auditService.logAudit(
                GLEntityType.GL_TRANSACTION,
                transactionId,
                GLAuditAction.STATUS_CHANGE,
                rejecterUsername,
                Map.of("status", "PENDING_APPROVAL"),
                Map.of("status", "REJECTED", "rejectedBy", rejecterUsername, "reason", reason),
                "Transaction rejected",
                totalAmount,
                transaction.getCurrency(),
                null,
                ipAddress,
                null);
    }

    /**
     * Cancel a draft transaction before submission.
     * Only the creator can cancel their own drafts.
     *
     * @param transactionId the transaction ID
     * @param username username of the person cancelling
     * @throws IllegalStateException if transaction is not in DRAFT status
     * @throws SecurityException if user is not the creator
     */
    public void cancelDraftTransaction(UUID transactionId, String username) {
        logger.info("Cancelling draft transaction {} by {}", transactionId, username);

        // Delegate to approval workflow service
        approvalWorkflowService.cancelTransaction(transactionId, username);

        // Audit log
        GLTransaction transaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found"));

        BigDecimal totalAmount = calculateTotalAmount(transaction);
        auditService.logAudit(
                GLEntityType.GL_TRANSACTION,
                transactionId,
                GLAuditAction.STATUS_CHANGE,
                username,
                Map.of("status", "DRAFT"),
                Map.of("status", "CANCELLED"),
                "Draft transaction cancelled",
                totalAmount,
                transaction.getCurrency(),
                null,
                null,
                null);
    }

    /**
     * Post a system-generated transaction without approval workflow.
     * Used for automated processes (period closing, revaluations, batch imports).
     *
     * @param command the transaction command
     * @param source the transaction source (must be system-generated)
     * @param systemUser the system user posting the transaction
     * @return the posted transaction
     * @throws IllegalArgumentException if source is not system-generated
     */
    public GLTransaction postSystemTransaction(PostTransactionCommand command, GLTransactionSource source,
            String systemUser) {
        if (!source.isSystemGenerated()) {
            throw new IllegalArgumentException(
                    "This method is only for system-generated transactions. Use approval workflow for manual entries.");
        }

        logger.info("Posting system transaction: {} with source {}", command.getReferenceId(), source);

        // Build transaction
        GLTransaction transaction = GLEntityMapper.toEntity(command, accountService);
        transaction.setSource(source);
        transaction.setStatus(com.openfinova.banking.gl.api.entity.GLTransactionStatus.POSTED);
        transaction.setPostedBy(systemUser);

        // Validate
        validateTransaction(transaction);

        // Determine fiscal period
        FiscalPeriod fiscalPeriod = fiscalPeriodService.getFiscalPeriodForDate(transaction.getTransactionDate())
                .orElseThrow(
                        () -> new IllegalStateException(
                                "No fiscal period found for transaction date: " + transaction.getTransactionDate()));

        // Assign transaction number
        Long transactionNumber = numberingService.getNextTransactionNumber(fiscalPeriod.getId());
        transaction.setTransactionNumber(transactionNumber);

        // Post transaction
        transaction.approveAndPost(systemUser, dateTimeService.instant());
        GLTransaction savedTransaction = transactionRepository.save(transaction);

        logger.info(
                "Posted system transaction: {} with number {}",
                savedTransaction.getReferenceId(),
                transactionNumber);

        // Audit log
        BigDecimal totalAmount = calculateTotalAmount(savedTransaction);
        auditService.logAudit(
                GLEntityType.GL_TRANSACTION,
                savedTransaction.getId(),
                GLAuditAction.CREATE,
                systemUser,
                Map.of(),
                Map.of(
                        "status",
                        "POSTED",
                        "source",
                        source.toString(),
                        "transactionNumber",
                        transactionNumber.toString()),
                "System transaction posted",
                totalAmount,
                savedTransaction.getCurrency(),
                null,
                null,
                null);

        return savedTransaction;
    }
}
