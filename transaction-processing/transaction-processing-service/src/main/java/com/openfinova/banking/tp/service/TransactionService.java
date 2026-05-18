package com.openfinova.banking.tp.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.context.ApplicationEventPublisher;
import com.openfinova.banking.tp.api.event.TransactionCompletedEvent;

import com.openfinova.banking.customer.account.api.CustomerAccountService;
import com.openfinova.banking.customer.account.api.entity.GLAccountMappingType;
import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.exchangerate.api.ExchangeRateService;
import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.gl.api.GeneralLedgerService;
import com.openfinova.banking.gl.api.dto.GLTransactionDTO;
import com.openfinova.banking.gl.api.dto.PostTransactionCommand;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tp.api.dto.BatchProcessingConfig;
import com.openfinova.banking.tp.api.dto.BatchTransactionResult;
import com.openfinova.banking.tp.api.dto.TransactionResponse;
import com.openfinova.banking.tp.api.entity.RefundType;
import com.openfinova.banking.tp.api.entity.ReservationStatus;
import com.openfinova.banking.tp.api.entity.ReservationType;
import com.openfinova.banking.tp.api.entity.TransactionStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.BalanceReservation;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.entity.TransactionEvent;
import com.openfinova.banking.tp.entity.TransactionRequest;
import com.openfinova.banking.tp.mapper.TransactionMapper;
import com.openfinova.banking.tp.repository.TransactionRepository;
import com.openfinova.banking.tp.repository.TransactionRequestRepository;
import com.openfinova.banking.tp.repository.TransactionSpecifications;

/**
 * Implementation of TransactionService with comprehensive lifecycle management,
 * compensation workflows, and audit trails.
 */
@Service("transactionServiceImpl")
@Transactional
public class TransactionService {

    private static final Logger logger = LoggerFactory.getLogger(TransactionService.class);

    private final TransactionRepository transactionRepository;
    private final TransactionRequestRepository transactionRequestRepository;
    private final FeeManagementService feeManagementService;
    private final VelocityLimitService velocityLimitService;
    private final BalanceReservationService balanceReservationService;
    private final GeneralLedgerService generalLedgerService;
    private final ExchangeRateService exchangeRateService;
    private final CustomerAccountService customerAccountService;
    private final CompensationWorkflowService compensationWorkflowService;
    private final DateTimeService dateTimeService;
    private final TransactionMapper transactionMapper;
    private final ApplicationEventPublisher eventPublisher;

    public TransactionService(TransactionRepository transactionRepository,
            TransactionRequestRepository transactionRequestRepository, FeeManagementService feeManagementService,
            VelocityLimitService velocityLimitService, BalanceReservationService balanceReservationService,
            GeneralLedgerService generalLedgerService, ExchangeRateService exchangeRateService,
            CustomerAccountService customerAccountService, CustomerInfoService customerInfoService,
            CompensationWorkflowService compensationWorkflowService, DateTimeService dateTimeService,
            TransactionMapper transactionMapper, ApplicationEventPublisher eventPublisher) {
        this.transactionRepository = transactionRepository;
        this.transactionRequestRepository = transactionRequestRepository;
        this.feeManagementService = feeManagementService;
        this.velocityLimitService = velocityLimitService;
        this.balanceReservationService = balanceReservationService;
        this.generalLedgerService = generalLedgerService;
        this.exchangeRateService = exchangeRateService;
        this.customerAccountService = customerAccountService;
        this.compensationWorkflowService = compensationWorkflowService;
        this.dateTimeService = dateTimeService;
        this.transactionMapper = transactionMapper;
        this.eventPublisher = eventPublisher;
    }

    public Transaction initiateTransaction(TransactionRequest request) {
        logger.info("Initiating transaction for request: {}", request.getIdempotencyKey());

        // Check for idempotency - return existing transaction if found
        var existingTransaction = transactionRepository.findByTransactionKey(request.getIdempotencyKey());
        if (existingTransaction.isPresent()) {
            logger.info("Found existing transaction for idempotency key: {}", request.getIdempotencyKey());
            return existingTransaction.get();
        }

        // Validate the request
        validateTransactionRequest(request);

        // Persist the request first
        TransactionRequest savedRequest = transactionRequestRepository.save(request);

        // Create and persist the transaction
        Transaction transaction = new Transaction(savedRequest);

        // Record initiation event
        recordTransactionEvent(
                transaction,
                "TRANSACTION_INITIATED",
                TransactionStatus.INITIATED,
                "Transaction initiated with request ID: " + savedRequest.getId());

        Transaction savedTransaction = transactionRepository.save(transaction);

        logger.info("Transaction initiated successfully: {}", savedTransaction.getId());
        return savedTransaction;
    }

    public Transaction processTransaction(UUID transactionId) {
        logger.info("Processing transaction: {}", transactionId);

        Transaction transaction = getTransactionById(transactionId);

        // Validate transaction state
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new IllegalStateException(
                    String.format(
                            "Transaction %s is in invalid state for processing: %s",
                            transactionId,
                            transaction.getStatus()));
        }

        try {
            // Step 1: Velocity and limit checks
            performVelocityChecks(transaction);

            // Step 2: Fee calculation
            calculateAndApplyFees(transaction);

            // Step 3: Balance reservation
            createBalanceReservation(transaction);

            // Step 4: Transition to PENDING_RESERVATION
            transaction.transitionTo(TransactionStatus.PENDING_RESERVATION, "Balance reservation created");

            // Step 5: External authorization (simulated)
            performExternalAuthorization(transaction);

            // Step 6: Transition to AUTHORIZED
            transaction.transitionTo(TransactionStatus.AUTHORIZED, "External authorization completed");

            // Step 7: GL posting handoff
            initiateGLPosting(transaction);

            // Step 8: Transition to POSTED
            transaction.transitionTo(TransactionStatus.POSTED, "GL posting completed");

            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.info("Transaction processed successfully: {}", transactionId);
            return savedTransaction;

        } catch (Exception e) {
            logger.error("Transaction processing failed for transaction: {}", transactionId, e);
            return failTransaction(transactionId, e.getMessage(), "PROCESSING_ERROR");
        }
    }

    public Transaction processMultiCurrencyTransaction(UUID transactionId, String sourceAccountCurrency,
            String destinationAccountCurrency) {
        logger.info(
                "Processing multi-currency transaction: {} with currencies {} -> {}",
                transactionId,
                sourceAccountCurrency,
                destinationAccountCurrency);

        Transaction transaction = getTransactionById(transactionId);

        // Validate transaction state
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new IllegalStateException(
                    String.format(
                            "Transaction %s is in invalid state for processing: %s",
                            transactionId,
                            transaction.getStatus()));
        }

        try {
            // Step 1: Multi-currency validation and exchange rate checks
            validateMultiCurrencyTransaction(transaction, sourceAccountCurrency, destinationAccountCurrency);

            // Step 2: Process currency conversion if needed
            processCurrencyConversion(transaction, sourceAccountCurrency, destinationAccountCurrency);

            // Step 3: Continue with standard transaction processing
            // Velocity and limit checks
            performVelocityChecks(transaction);

            // Step 4: Fee calculation (including FX fees)
            calculateAndApplyFees(transaction);

            // Step 5: Balance reservation
            createBalanceReservation(transaction);

            // Step 6: Transition to PENDING_RESERVATION
            transaction
                    .transitionTo(TransactionStatus.PENDING_RESERVATION, "Multi-currency balance reservation created");

            // Step 7: External authorization (simulated)
            performExternalAuthorization(transaction);

            // Step 8: Transition to AUTHORIZED
            transaction.transitionTo(TransactionStatus.AUTHORIZED, "Multi-currency external authorization completed");

            // Step 9: GL posting handoff
            initiateGLPosting(transaction);

            // Step 10: Transition to POSTED
            transaction.transitionTo(TransactionStatus.POSTED, "Multi-currency GL posting completed");

            Transaction savedTransaction = transactionRepository.save(transaction);
            logger.info("Multi-currency transaction processed successfully: {}", transactionId);
            return savedTransaction;

        } catch (Exception e) {
            logger.error("Multi-currency transaction processing failed for transaction: {}", transactionId, e);
            return failTransaction(transactionId, e.getMessage(), "MULTI_CURRENCY_PROCESSING_ERROR");
        }
    }

    public BigDecimal calculateTotalAmountInCurrency(UUID transactionId, String targetCurrency) {
        logger.debug("Calculating total amount in currency {} for transaction: {}", targetCurrency, transactionId);

        exchangeRateService.validateCurrencyCode(targetCurrency);

        Transaction transaction = getTransactionById(transactionId);
        TransactionRequest request = transaction.getRequest();

        // Convert principal amount
        BigDecimal principalInTarget = exchangeRateService.convertCurrency(
                request.getAmount(),
                request.getCurrency(),
                targetCurrency,
                transaction.getValueDate(),
                RateType.SPOT);

        // Convert fee amount if present
        BigDecimal feeInTarget = BigDecimal.ZERO;
        if (transaction.getFeeAmount() != null && transaction.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            feeInTarget = exchangeRateService.convertCurrency(
                    transaction.getFeeAmount(),
                    transaction.getCurrency(),
                    targetCurrency,
                    transaction.getValueDate(),
                    RateType.SPOT);
        }

        return principalInTarget.add(feeInTarget);
    }

    public boolean isMultiCurrencyTransaction(UUID transactionId, String sourceAccountCurrency,
            String destinationAccountCurrency) {
        Transaction transaction = getTransactionById(transactionId);
        String transactionCurrency = transaction.getRequest().getCurrency();

        return !transactionCurrency.equals(sourceAccountCurrency)
                || !transactionCurrency.equals(destinationAccountCurrency)
                || !sourceAccountCurrency.equals(destinationAccountCurrency);
    }

    public BigDecimal getEffectiveExchangeRate(UUID transactionId, String sourceCurrency, String targetCurrency) {
        if (sourceCurrency.equals(targetCurrency)) {
            return BigDecimal.ONE;
        }

        Transaction transaction = getTransactionById(transactionId);
        return exchangeRateService.getExchangeRate(sourceCurrency, targetCurrency, transaction.getValueDate());
    }

    public Transaction processTransactionWithApproval(UUID transactionId, List<String> approverIds) {
        logger.info("Processing transaction with approval: {} requiring approvers: {}", transactionId, approverIds);

        Transaction transaction = getTransactionById(transactionId);

        // Validate transaction state
        if (transaction.getStatus() != TransactionStatus.INITIATED) {
            throw new IllegalStateException(
                    String.format(
                            "Transaction %s is in invalid state for approval processing: %s",
                            transactionId,
                            transaction.getStatus()));
        }

        // Record approval requirement
        recordTransactionEvent(
                transaction,
                "APPROVAL_REQUIRED",
                transaction.getStatus(),
                "Transaction requires approval from: " + String.join(", ", approverIds));

        // For now, simulate approval process - in real implementation this would integrate with approval workflow
        recordTransactionEvent(
                transaction,
                "APPROVAL_GRANTED",
                transaction.getStatus(),
                "All required approvals obtained");

        // Continue with standard processing
        return processTransaction(transactionId);
    }

    public List<Transaction> processTransactionBatch(List<UUID> transactionIds) {
        logger.info("Processing transaction batch of size: {}", transactionIds.size());

        return transactionIds.stream().map(this::processTransaction).toList();
    }

    public List<Transaction> initiateTransactionBatch(List<TransactionRequest> requests) {
        logger.info("Initiating transaction batch of size: {}", requests.size());

        if (requests == null || requests.isEmpty()) {
            return new ArrayList<>();
        }

        if (requests.size() > BatchProcessingConfig.MAX_BATCH_SIZE) {
            throw new IllegalArgumentException(
                    "Batch size exceeds maximum allowed: " + BatchProcessingConfig.MAX_BATCH_SIZE);
        }

        List<Transaction> results = new ArrayList<>();

        for (TransactionRequest request : requests) {
            try {
                Transaction transaction = initiateTransaction(request);
                results.add(transaction);
            } catch (Exception e) {
                logger.error(
                        "Failed to initiate transaction in batch for request {}: {}",
                        request.getIdempotencyKey(),
                        e.getMessage());
                // Continue with other transactions - don't fail the entire batch
                // In a more sophisticated implementation, you might want to collect errors
            }
        }

        logger.info("Successfully initiated {} out of {} transactions in batch", results.size(), requests.size());

        return results;
    }

    public BatchTransactionResult processTransactionBatchAdvanced(List<UUID> transactionIds,
            BatchProcessingConfig config) {
        logger.info("Processing advanced transaction batch of size: {} with config: {}", transactionIds.size(), config);

        if (config != null) {
            config.validate();
        } else {
            config = new BatchProcessingConfig();
        }

        BatchTransactionResult result = new BatchTransactionResult(
                "BATCH-" + UUID.randomUUID().toString().substring(0, 8),
                transactionIds.size());

        List<TransactionResponse> successfulResults = new ArrayList<>();
        Map<UUID, String> failedResults = new HashMap<>();

        // Process transactions with configured batch size
        int batchSize = config.getBatchSize();
        List<List<UUID>> batches = partitionList(transactionIds, batchSize);

        for (List<UUID> batch : batches) {
            for (UUID transactionId : batch) {
                try {
                    Transaction processedTransaction = processTransaction(transactionId);
                    successfulResults.add(transactionMapper.toResponse(processedTransaction));
                } catch (Exception e) {
                    logger.error(
                            "Failed to process transaction {} in advanced batch: {}",
                            transactionId,
                            e.getMessage());
                    failedResults.put(transactionId, e.getMessage());

                    if (config.isFailFast()) {
                        logger.warn("Failing fast due to transaction processing error");
                        break;
                    }
                }
            }

            if (config.isFailFast() && !failedResults.isEmpty()) {
                break;
            }
        }

        result.setSuccessfulResults(successfulResults);
        result.setFailedResults(failedResults);
        result.setSuccessfulTransactions(successfulResults.size());
        result.setFailedTransactions(failedResults.size());
        result.markProcessingComplete();

        logger.info("Advanced batch processing completed: {}", result);

        return result;
    }

    public List<Transaction> completeTransactionBatch(List<UUID> transactionIds) {
        logger.info("Completing transaction batch of size: {}", transactionIds.size());

        return transactionIds.stream().map(this::completeTransaction).toList();
    }

    public List<Transaction> failTransactionBatch(List<UUID> transactionIds, String reason, String errorCode) {
        logger.info("Failing transaction batch of size: {} with reason: {}", transactionIds.size(), reason);

        return transactionIds.stream().map(id -> failTransaction(id, reason, errorCode)).toList();
    }

    public TransactionStatus getTransactionStatus(UUID transactionId) {
        Transaction transaction = getTransactionById(transactionId);
        return transaction.getStatus();
    }

    public List<TransactionEvent> getTransactionHistory(UUID transactionId) {
        Transaction transaction = getTransactionById(transactionId);
        return transaction.getEvents();
    }

    /**
     * Resolves an existing transaction by idempotency key (external facade / idempotency helpers).
     */
    @Transactional(readOnly = true)
    public Optional<Transaction> findExistingTransaction(String idempotencyKey) {
        return transactionRepository.findByTransactionKey(idempotencyKey);
    }

    /**
     * Paginated admin search across TP transactions with optional filters.
     */
    @Transactional(readOnly = true)
    public Page<TransactionResponse> searchTransactions(UUID accountId, TransactionStatus status,
            TransactionType transactionType, LocalDate fromTransactionDate, LocalDate toTransactionDate,
            String currency, BigDecimal minAmount, BigDecimal maxAmount, String referenceContains, Pageable pageable) {

        Specification<Transaction> spec = TransactionSpecifications.adminSearch(
                accountId,
                status,
                transactionType,
                fromTransactionDate,
                toTransactionDate,
                currency,
                minAmount,
                maxAmount,
                referenceContains);

        Page<Transaction> page = transactionRepository.findAll(spec, pageable);
        List<UUID> ids = page.getContent().stream().map(Transaction::getId).toList();
        if (ids.isEmpty()) {
            return new PageImpl<>(List.of(), pageable, page.getTotalElements());
        }

        Map<UUID, Transaction> byId = transactionRepository.findByIdInWithAllRelations(ids).stream()
                .collect(Collectors.toMap(Transaction::getId, Function.identity()));

        List<TransactionResponse> content = ids.stream().map(byId::get).filter(Objects::nonNull)
                .map(transactionMapper::toResponse).toList();

        return new PageImpl<>(content, pageable, page.getTotalElements());
    }

    public boolean isTransactionDuplicate(TransactionRequest request) {
        return transactionRepository.findByTransactionKey(request.getIdempotencyKey()).isPresent();
    }

    public Transaction completeTransaction(UUID transactionId) {
        logger.info("Completing transaction: {}", transactionId);

        Transaction transaction = getTransactionById(transactionId);

        if (transaction.getStatus() != TransactionStatus.POSTED) {
            throw new IllegalStateException(
                    String.format(
                            "Transaction %s cannot be completed from state: %s",
                            transactionId,
                            transaction.getStatus()));
        }

        // Confirm balance reservations
        confirmBalanceReservations(transaction);

        // Record completion event
        recordTransactionEvent(
                transaction,
                "TRANSACTION_COMPLETED",
                TransactionStatus.POSTED,
                "Transaction completed successfully");

        Transaction savedTransaction = transactionRepository.save(transaction);

        // Process post-transaction operations
        processPostTransactionOperations(savedTransaction);
        
        eventPublisher.publishEvent(new TransactionCompletedEvent(
                savedTransaction.getId(),
                savedTransaction.getSourceAccountId(),
                savedTransaction.getPrincipalAmount(),
                savedTransaction.getCurrency(),
                savedTransaction.getTransactionType().name()
        ));

        logger.info("Transaction completed successfully: {}", transactionId);
        return savedTransaction;
    }

    public Transaction failTransaction(UUID transactionId, String reason, String errorCode) {
        logger.warn("Failing transaction: {} with reason: {}", transactionId, reason);

        Transaction transaction = getTransactionById(transactionId);

        if (transaction.isTerminal()) {
            logger.warn("Attempted to fail already terminal transaction: {}", transactionId);
            return transaction;
        }

        // Record failure event with error details
        recordTransactionEvent(transaction, "TRANSACTION_FAILED", TransactionStatus.FAILED, reason, errorCode);

        // Transition to FAILED state
        transaction.transitionTo(TransactionStatus.FAILED, reason);

        // Release any balance reservations
        releaseBalanceReservations(transaction);

        // Reverse GL transaction if it was posted
        if (transaction.getGlTransactionId() != null) {
            try {
                logger.info(
                        "Reversing GL transaction {} for failed TP transaction: {}",
                        transaction.getGlTransactionId(),
                        transactionId);
                reverseTransactionInGL(transaction.getGlTransactionId(), "TP Transaction failed: " + reason);

                recordTransactionEvent(
                        transaction,
                        "GL_REVERSAL_COMPLETED",
                        transaction.getStatus(),
                        "GL transaction reversed due to failure");
            } catch (Exception e) {
                logger.error(
                        "Failed to reverse GL transaction {} for failed TP transaction {}: {}",
                        transaction.getGlTransactionId(),
                        transactionId,
                        e.getMessage());

                recordTransactionEvent(
                        transaction,
                        "GL_REVERSAL_FAILED",
                        transaction.getStatus(),
                        "GL reversal failed: " + e.getMessage(),
                        "GL_REVERSAL_ERROR");

                // Don't fail the transaction failure process due to GL reversal issues
                // This should be handled by a separate reconciliation process
            }
        }

        // Initiate compensation workflow if transaction was authorized
        if (wasTransactionAuthorized(transaction)) {
            try {
                logger.info("Initiating compensation workflow for failed transaction: {}", transactionId);
                UUID workflowId = compensationWorkflowService.startCompensation(transactionId);

                recordTransactionEvent(
                        transaction,
                        "COMPENSATION_WORKFLOW_INITIATED",
                        transaction.getStatus(),
                        "Compensation workflow started with ID: " + workflowId);

                logger.info("Compensation workflow {} initiated for transaction: {}", workflowId, transactionId);
            } catch (Exception e) {
                logger.error(
                        "Failed to initiate compensation workflow for transaction {}: {}",
                        transactionId,
                        e.getMessage());

                recordTransactionEvent(
                        transaction,
                        "COMPENSATION_WORKFLOW_FAILED",
                        transaction.getStatus(),
                        "Failed to initiate compensation workflow: " + e.getMessage(),
                        "COMPENSATION_INITIATION_ERROR");

                // Don't fail the transaction failure process due to compensation workflow issues
                // The workflow can be manually initiated later if needed
            }
        } else {
            logger.debug("Skipping compensation workflow for transaction {} - not authorized", transactionId);
        }

        Transaction savedTransaction = transactionRepository.save(transaction);
        logger.info("Transaction failed and compensation initiated: {}", transactionId);
        return savedTransaction;
    }

    /**
     * Finds transactions in non-terminal status that have been in progress longer than the given
     * timeout and fails them with reason "Reservation timeout exceeded". Releases their balance
     * reservations and triggers compensation if applicable. Called by TPOperationsScheduler.
     *
     * @param timeoutMinutes only transactions with processingStartedAt older than this are failed
     * @return number of transactions failed
     */
    public int failTimedOutTransactions(int timeoutMinutes) {
        LocalDateTime cutoff = dateTimeService.now().minusMinutes(timeoutMinutes);
        List<TransactionStatus> inProgress = List
                .of(TransactionStatus.INITIATED, TransactionStatus.PENDING_RESERVATION, TransactionStatus.AUTHORIZED);
        List<Transaction> timedOut = transactionRepository
                .findByStatusInAndProcessingStartedAtBefore(inProgress, cutoff);
        for (Transaction tx : timedOut) {
            try {
                logger.warn(
                        "Failing timed-out transaction: {} (processing started at {})",
                        tx.getId(),
                        tx.getProcessingStartedAt());
                failTransaction(tx.getId(), "Reservation timeout exceeded", "TIMEOUT");
            } catch (Exception e) {
                logger.error("Failed to fail timed-out transaction {}: {}", tx.getId(), e.getMessage());
            }
        }
        if (!timedOut.isEmpty()) {
            logger.info("Failed {} timed-out transaction(s) (cutoff: {} min ago)", timedOut.size(), timeoutMinutes);
        }
        return timedOut.size();
    }

    @Transactional(readOnly = true)
    public Transaction getTransactionById(UUID id) {
        return transactionRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + id));
    }

    // Private helper methods

    private void validateMultiCurrencyTransaction(Transaction transaction, String sourceAccountCurrency,
            String destinationAccountCurrency) {
        TransactionRequest request = transaction.getRequest();

        // Validate all currencies are supported
        exchangeRateService.validateCurrencyCode(request.getCurrency());
        exchangeRateService.validateCurrencyCode(sourceAccountCurrency);
        exchangeRateService.validateCurrencyCode(destinationAccountCurrency);

        // Check if exchange rates are available for the transaction date
        if (!request.getCurrency().equals(sourceAccountCurrency)) {
            if (!exchangeRateService.exchangeRateExists(
                    request.getCurrency(),
                    sourceAccountCurrency,
                    transaction.getValueDate(),
                    RateType.SPOT)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Exchange rate not available for %s to %s on %s",
                                request.getCurrency(),
                                sourceAccountCurrency,
                                transaction.getValueDate()));
            }
        }

        if (!request.getCurrency().equals(destinationAccountCurrency)) {
            if (!exchangeRateService.exchangeRateExists(
                    request.getCurrency(),
                    destinationAccountCurrency,
                    transaction.getValueDate(),
                    RateType.SPOT)) {
                throw new IllegalArgumentException(
                        String.format(
                                "Exchange rate not available for %s to %s on %s",
                                request.getCurrency(),
                                destinationAccountCurrency,
                                transaction.getValueDate()));
            }
        }

        recordTransactionEvent(
                transaction,
                "MULTI_CURRENCY_VALIDATION_PASSED",
                transaction.getStatus(),
                "Multi-currency validation completed successfully");
    }

    private void processCurrencyConversion(Transaction transaction, String sourceAccountCurrency,
            String destinationAccountCurrency) {
        TransactionRequest request = transaction.getRequest();

        // If all currencies are the same, no conversion needed
        if (sourceAccountCurrency.equals(destinationAccountCurrency)
                && sourceAccountCurrency.equals(request.getCurrency())) {
            return;
        }

        // Convert transaction amount to source account currency for debit
        BigDecimal sourceAmount = exchangeRateService.convertCurrency(
                request.getAmount(),
                request.getCurrency(),
                sourceAccountCurrency,
                transaction.getValueDate(),
                RateType.SPOT);

        // Convert transaction amount to destination account currency for credit
        BigDecimal destinationAmount = exchangeRateService.convertCurrency(
                request.getAmount(),
                request.getCurrency(),
                destinationAccountCurrency,
                transaction.getValueDate(),
                RateType.SPOT);

        // Store conversion details in transaction metadata
        if (request.getMetadata() == null) {
            request.setMetadata(new HashMap<>());
        }

        request.getMetadata().put("sourceAmount", sourceAmount);
        request.getMetadata().put("sourceCurrency", sourceAccountCurrency);
        request.getMetadata().put("destinationAmount", destinationAmount);
        request.getMetadata().put("destinationCurrency", destinationAccountCurrency);
        request.getMetadata().put("originalAmount", request.getAmount());
        request.getMetadata().put("originalCurrency", request.getCurrency());

        // Calculate FX fee via exchange-rate facade; tier multiplier from TP
        var tier = feeManagementService.getCustomerTierForTransaction(transaction);
        double tierMultiplier = tier != null ? tier.getFeeMultiplier() : 1.0;
        BigDecimal fxFee = exchangeRateService
                .calculateFXFee(request.getAmount(), sourceAccountCurrency, destinationAccountCurrency, tierMultiplier);

        if (fxFee.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal currentFee = transaction.getFeeAmount() != null ? transaction.getFeeAmount() : BigDecimal.ZERO;
            transaction.setFeeAmount(currentFee.add(fxFee));

            request.getMetadata().put("fxFee", fxFee);
            request.getMetadata().put("fxFeeDescription", "Foreign exchange conversion fee");
        }

        recordTransactionEvent(
                transaction,
                "CURRENCY_CONVERSION_COMPLETED",
                transaction.getStatus(),
                String.format(
                        "Currency conversion completed: %s %s -> %s %s / %s %s",
                        request.getAmount(),
                        request.getCurrency(),
                        sourceAmount,
                        sourceAccountCurrency,
                        destinationAmount,
                        destinationAccountCurrency));
    }

    private void validateTransactionRequest(TransactionRequest request) {
        if (request.getAmount() == null || request.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Transaction amount must be positive");
        }

        if (request.getSourceAccountId() == null && request.getDestinationAccountId() == null) {
            throw new IllegalArgumentException("At least one account (source or destination) must be specified");
        }

        if (request.getTransactionType() == null) {
            throw new IllegalArgumentException("Transaction type is required");
        }

        if (request.getCurrency() == null || request.getCurrency().trim().isEmpty()) {
            throw new IllegalArgumentException("Currency is required");
        }

        // Validate accounts using AccountFacade
        try {
            if (request.getSourceAccountId() != null) {
                validateAccountForTransaction(request.getSourceAccountId(), request, true);
            }

            if (request.getDestinationAccountId() != null) {
                validateAccountForTransaction(request.getDestinationAccountId(), request, false);
            }
        } catch (Exception e) {
            logger.error("Account validation failed for transaction request: {}", e.getMessage());
            throw new IllegalArgumentException("Account validation failed: " + e.getMessage(), e);
        }
    }

    private void validateAccountForTransaction(UUID accountId, TransactionRequest request, boolean isSourceAccount) {
        // Basic account validation
        if (!customerAccountService.accountExists(accountId)) {
            throw new IllegalArgumentException(
                    (isSourceAccount ? "Source" : "Destination") + " account not found: " + accountId);
        }

        if (!customerAccountService.isAccountActive(accountId)) {
            throw new IllegalArgumentException(
                    (isSourceAccount ? "Source" : "Destination") + " account is not active: " + accountId);
        }

        if (!customerAccountService.isAccountEligibleForTransaction(accountId, request.getTransactionType())) {
            throw new IllegalArgumentException(
                    (isSourceAccount ? "Source" : "Destination") + " account is not eligible for transaction type: "
                            + request.getTransactionType());
        }

        // Customer validation using cached service
        // Note: Customer ID validation would need to be done at a higher level
        // since IAccountFacade doesn't expose customer associations
        // The calling module should validate customer eligibility before calling this method

        // Validate sufficient balance for debit transactions
        if (isSourceAccount && isDebitTransaction(request.getTransactionType())) {
            if (!customerAccountService.hasSufficientBalance(accountId, request.getAmount())) {
                throw new IllegalArgumentException("Insufficient balance in source account: " + accountId);
            }
        }
    }

    private boolean isDebitTransaction(TransactionType transactionType) {
        return transactionType == TransactionType.P2P || transactionType == TransactionType.CASH_OUT
                || transactionType == TransactionType.BILL_PAYMENT
                || transactionType == TransactionType.MERCHANT_PURCHASE;
    }

    private void performVelocityChecks(Transaction transaction) {
        logger.debug("Performing velocity checks for transaction: {}", transaction.getId());

        // Check velocity limits for source account
        if (transaction.getSourceAccountId() != null) {
            boolean velocityCheckPassed = velocityLimitService.checkLimits(
                    transaction.getSourceAccountId(),
                    transaction.getTransactionType(),
                    transaction.getPrincipalAmount(),
                    transaction.getCurrency());

            if (!velocityCheckPassed) {
                throw new RuntimeException("Velocity limit exceeded for source account");
            }
        }

        recordTransactionEvent(
                transaction,
                "VELOCITY_CHECK_PASSED",
                transaction.getStatus(),
                "Velocity limits validated successfully");
    }

    private void calculateAndApplyFees(Transaction transaction) {
        logger.debug("Calculating fees for transaction: {}", transaction.getId());

        BigDecimal feeAmount = feeManagementService.calculateFees(transaction);
        transaction.setEstimatedFeeAmount(feeAmount);
        transaction.setFeeAmount(feeAmount); // Set actual fee for GL revenue recognition
        transaction.setFeeCalculationAt(dateTimeService.now());

        recordTransactionEvent(transaction, "FEE_CALCULATED", transaction.getStatus(), "Fee calculated: " + feeAmount);
    }

    private void createBalanceReservation(Transaction transaction) {
        logger.debug("Creating balance reservation for transaction: {}", transaction.getId());

        if (transaction.getSourceAccountId() != null) {
            BigDecimal totalAmount = transaction.getTotalAmount();

            try {
                // Use BalanceReservationService with full transaction context
                UUID reservationId = balanceReservationService.reserveBalanceForTransaction(
                        transaction,
                        transaction.getSourceAccountId(),
                        totalAmount,
                        ReservationType.DEBIT_HOLD);

                logger.info("Created balance reservation {} for transaction {}", reservationId, transaction.getId());

                recordTransactionEvent(
                        transaction,
                        "BALANCE_RESERVED",
                        transaction.getStatus(),
                        "Balance reserved: " + totalAmount + " for account: " + transaction.getSourceAccountId());
            } catch (Exception e) {
                logger.error(
                        "Failed to create balance reservation for transaction {}: {}",
                        transaction.getId(),
                        e.getMessage());
                throw new RuntimeException("Balance reservation failed: " + e.getMessage(), e);
            }
        }
    }

    private void performExternalAuthorization(Transaction transaction) {
        logger.debug("Performing external authorization for transaction: {}", transaction.getId());

        // Simulate external authorization process
        // In real implementation, this would call external payment gateway or authorization service

        // For now, we'll simulate success
        transaction.setGatewayTransactionId("GTW-" + UUID.randomUUID().toString().substring(0, 8));

        recordTransactionEvent(
                transaction,
                "EXTERNAL_AUTHORIZATION_COMPLETED",
                transaction.getStatus(),
                "External authorization completed with gateway ID: " + transaction.getGatewayTransactionId());
    }

    private void initiateGLPosting(Transaction transaction) {
        logger.debug("Initiating GL posting for transaction: {}", transaction.getId());

        try {
            // Validate fiscal period before posting
            Optional<com.openfinova.banking.gl.api.dto.FiscalPeriodDTO> period = generalLedgerService
                    .getFiscalPeriodForDate(transaction.getValueDate());
            if (period.isEmpty() || !generalLedgerService.isFiscalPeriodOpen(period.get().getId())) {
                throw new IllegalStateException("Posting not allowed for date: " + transaction.getValueDate());
            }

            // Post transaction to GL synchronously with retry logic
            UUID glTransactionId = postTransactionToGL(transaction);

            transaction.setGlTransactionId(glTransactionId);
            transaction.setGlReferenceNumber("GL-" + glTransactionId.toString().substring(0, 8));

            recordTransactionEvent(
                    transaction,
                    "GL_POSTING_COMPLETED",
                    transaction.getStatus(),
                    "GL posting completed with GL transaction ID: " + glTransactionId);

            // Create customer-facing AccountTransactions after successful GL posting
            createAccountTransactions(transaction);

        } catch (Exception e) {
            logger.error("GL posting failed for transaction: {}", transaction.getId(), e);

            // Record the failure event
            recordTransactionEvent(
                    transaction,
                    "GL_POSTING_FAILED",
                    transaction.getStatus(),
                    "GL posting failed: " + e.getMessage(),
                    "GL_POSTING_ERROR");

            throw new RuntimeException("GL posting failed: " + e.getMessage(), e);
        }
    }

    /**
     * Posts a transaction to the General Ledger with retry logic.
     * Attempts up to 3 times with exponential backoff for retryable errors.
     *
     * @param transaction the TP transaction to post to GL
     * @return the GL transaction ID
     * @throws RuntimeException if posting fails after all retry attempts
     */
    private UUID postTransactionToGL(Transaction transaction) {
        logger.debug("Posting transaction {} to GL", transaction.getId());

        int maxAttempts = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            try {
                // Create a PostTransactionCommand from the TP transaction
                PostTransactionCommand command = buildGLTransactionCommand(transaction);

                // Post to GL using facade
                GLTransactionDTO postedTransaction = generalLedgerService.postTransaction(command);

                logger.info(
                        "Successfully posted transaction {} to GL with ID: {}",
                        transaction.getId(),
                        postedTransaction.getId());

                return postedTransaction.getId();

            } catch (Exception e) {
                lastException = e;
                attempt++;

                logger.error(
                        "GL posting attempt {} failed for transaction {}: {}",
                        attempt,
                        transaction.getId(),
                        e.getMessage());

                // Check if this is a retryable error and we have attempts left
                if (isRetryableGLError(e) && attempt < maxAttempts) {
                    logger.warn(
                            "Retryable GL error for transaction {}, attempt {}/{}: {}",
                            transaction.getId(),
                            attempt,
                            maxAttempts,
                            e.getMessage());

                    // Exponential backoff: wait 1s, 2s, 4s
                    try {
                        Thread.sleep(1000L * (1L << (attempt - 1)));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("GL posting interrupted", ie);
                    }
                } else {
                    logger.error(
                            "Non-retryable GL error or max attempts reached for transaction {}: {}",
                            transaction.getId(),
                            e.getMessage());
                    break;
                }
            }
        }

        throw new RuntimeException("GL posting failed after " + maxAttempts + " attempts", lastException);
    }

    public void reverseTransactionInGL(UUID glTransactionId, String reason) {
        logger.debug("Reversing GL transaction: {} with reason: {}", glTransactionId, reason);

        int maxAttempts = 3;
        int attempt = 0;
        Exception lastException = null;

        while (attempt < maxAttempts) {
            try {
                generalLedgerService.reverseTransaction(glTransactionId, reason, "SYSTEM");
                logger.info("Successfully reversed GL transaction: {}", glTransactionId);
                return;

            } catch (Exception e) {
                lastException = e;
                attempt++;

                logger.error(
                        "GL reversal attempt {} failed for transaction {}: {}",
                        attempt,
                        glTransactionId,
                        e.getMessage());

                if (isRetryableGLError(e) && attempt < maxAttempts) {
                    logger.warn(
                            "Retryable GL reversal error for transaction {}, attempt {}/{}: {}",
                            glTransactionId,
                            attempt,
                            maxAttempts,
                            e.getMessage());

                    // Exponential backoff: wait 2s, 4s, 8s
                    try {
                        Thread.sleep(2000L * (1L << (attempt - 1)));
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RuntimeException("GL reversal interrupted", ie);
                    }
                } else {
                    logger.error(
                            "Non-retryable GL reversal error or max attempts reached for transaction {}: {}",
                            glTransactionId,
                            e.getMessage());
                    break;
                }
            }
        }

        throw new RuntimeException(
                "GL reversal failed after " + maxAttempts + " attempts: "
                        + (lastException != null ? lastException.getMessage() : "Unknown error"));
    }

    private boolean isRetryableGLError(Exception e) {
        // Define which GL errors are retryable
        String message = e.getMessage() != null ? e.getMessage().toLowerCase() : "";

        // Retryable errors: temporary network issues, timeouts, service unavailable
        return message.contains("timeout") || message.contains("connection") || message.contains("service unavailable")
                || message.contains("temporary") || e instanceof java.net.SocketTimeoutException
                || e instanceof java.net.ConnectException;
    }

    /**
     * Builds a GL transaction command with proper journal entries for posting to the General Ledger.
     *
    * This method resolves the appropriate GL accounts for customer accounts using AccountFacade
     * and creates balanced journal entries following double-entry bookkeeping principles.
     *
     * For a typical P2P transaction with fees:
     * - DEBIT: Source customer's PRIMARY_BALANCE GL account (principal + fee)
     * - CREDIT: Destination customer's PRIMARY_BALANCE GL account (principal only)
     * - CREDIT: Bank's FEE_COLLECTION GL account (fee amount)
     *
     * @param transaction the TP transaction to convert to GL posting
     * @return PostTransactionCommand ready for GL posting
     * @throws IllegalStateException if required GL account mappings are not found
     */
    private PostTransactionCommand buildGLTransactionCommand(Transaction transaction) {
        logger.debug("Building GL transaction command for TP transaction: {}", transaction.getId());

        try {
            List<PostTransactionCommand.JournalEntryCommand> entries = new ArrayList<>();

            // Handle P2P transactions (source and destination both present)
            if (transaction.getSourceAccountId() != null && transaction.getDestinationAccountId() != null) {

                // 1. Resolve source customer account's PRIMARY_BALANCE GL account
                UUID sourceGLAccountId = resolveGLAccountForCustomerAccount(
                        transaction.getSourceAccountId(),
                        GLAccountMappingType.PRIMARY_BALANCE);

                // 2. Resolve destination customer account's PRIMARY_BALANCE GL account
                UUID destinationGLAccountId = resolveGLAccountForCustomerAccount(
                        transaction.getDestinationAccountId(),
                        GLAccountMappingType.PRIMARY_BALANCE);

                // 3. Create DEBIT entry - reduce source customer's balance (Asset/Liability account)
                PostTransactionCommand.JournalEntryCommand debitEntry = new PostTransactionCommand.JournalEntryCommand(
                        sourceGLAccountId,
                        transaction.getTotalAmount(), // debit amount (principal + fee)
                        BigDecimal.ZERO, // credit amount
                        "Debit from account for TP transaction: " + transaction.getIdempotencyKey(),
                        transaction.getValueDate());
                entries.add(debitEntry);

                // 4. Create CREDIT entry - increase destination customer's balance (Asset/Liability account)
                PostTransactionCommand.JournalEntryCommand creditEntry = new PostTransactionCommand.JournalEntryCommand(
                        destinationGLAccountId,
                        BigDecimal.ZERO, // debit amount
                        transaction.getPrincipalAmount(), // credit amount (principal only, without fees)
                        "Credit to account for TP transaction: " + transaction.getIdempotencyKey(),
                        transaction.getValueDate());
                entries.add(creditEntry);

                // 5. Fee revenue recognition: credit FEE_INCOME when fee > 0
                BigDecimal feeForGL = getFeeAmountForGL(transaction);
                if (feeForGL.compareTo(BigDecimal.ZERO) > 0) {
                    UUID feeGLAccountId = generalLedgerService
                            .getOperationalGLAccount(OperationalGLAccountType.FEE_INCOME.name());
                    PostTransactionCommand.JournalEntryCommand feeEntry = new PostTransactionCommand.JournalEntryCommand(
                            feeGLAccountId,
                            BigDecimal.ZERO,
                            feeForGL,
                            "Fee income for TP transaction: " + transaction.getIdempotencyKey(),
                            transaction.getValueDate());
                    entries.add(feeEntry);
                }

            } else if (transaction.getSourceAccountId() != null) {
                // Debit-only transaction (e.g., cash withdrawal, external payment)
                // Need to determine the contra account based on transaction type

                UUID sourceGLAccountId = resolveGLAccountForCustomerAccount(
                        transaction.getSourceAccountId(),
                        GLAccountMappingType.PRIMARY_BALANCE);

                // Create DEBIT entry - reduce customer's balance (principal + fee)
                PostTransactionCommand.JournalEntryCommand debitEntry = new PostTransactionCommand.JournalEntryCommand(
                        sourceGLAccountId,
                        transaction.getTotalAmount(),
                        BigDecimal.ZERO,
                        "Debit for TP transaction: " + transaction.getIdempotencyKey(),
                        transaction.getValueDate());
                entries.add(debitEntry);

                // Determine contra account based on transaction type or metadata
                UUID contraGLAccountId = resolveContraAccountForDebitOnly(transaction);
                BigDecimal feeForGL = getFeeAmountForGL(transaction);
                BigDecimal principalAmount = transaction.getPrincipalAmount();

                // CREDIT contra for principal only (fee revenue recognized separately)
                PostTransactionCommand.JournalEntryCommand contraEntry = new PostTransactionCommand.JournalEntryCommand(
                        contraGLAccountId,
                        BigDecimal.ZERO,
                        principalAmount,
                        "Contra entry for TP transaction: " + transaction.getIdempotencyKey(),
                        transaction.getValueDate());
                entries.add(contraEntry);

                // Fee revenue recognition: credit FEE_INCOME when fee > 0
                if (feeForGL.compareTo(BigDecimal.ZERO) > 0) {
                    UUID feeGLAccountId = generalLedgerService
                            .getOperationalGLAccount(OperationalGLAccountType.FEE_INCOME.name());
                    PostTransactionCommand.JournalEntryCommand feeEntry = new PostTransactionCommand.JournalEntryCommand(
                            feeGLAccountId,
                            BigDecimal.ZERO,
                            feeForGL,
                            "Fee income for TP transaction: " + transaction.getIdempotencyKey(),
                            transaction.getValueDate());
                    entries.add(feeEntry);
                }

            } else if (transaction.getDestinationAccountId() != null) {
                // Credit-only transaction (e.g., cash deposit, external receipt)
                // Need to determine the contra account based on transaction type

                UUID destinationGLAccountId = resolveGLAccountForCustomerAccount(
                        transaction.getDestinationAccountId(),
                        GLAccountMappingType.PRIMARY_BALANCE);

                // Determine contra account based on transaction type or metadata
                UUID contraGLAccountId = resolveContraAccountForCreditOnly(transaction);

                // Create DEBIT entry - contra account (cash, clearing, etc.)
                PostTransactionCommand.JournalEntryCommand contraEntry = new PostTransactionCommand.JournalEntryCommand(
                        contraGLAccountId,
                        transaction.getTotalAmount(),
                        BigDecimal.ZERO,
                        "Contra entry for TP transaction: " + transaction.getIdempotencyKey(),
                        transaction.getValueDate());
                entries.add(contraEntry);

                // Create CREDIT entry - increase customer's balance
                PostTransactionCommand.JournalEntryCommand creditEntry = new PostTransactionCommand.JournalEntryCommand(
                        destinationGLAccountId,
                        BigDecimal.ZERO,
                        transaction.getTotalAmount(),
                        "Credit for TP transaction: " + transaction.getIdempotencyKey(),
                        transaction.getValueDate());
                entries.add(creditEntry);
            }

            // Validate that we have at least 2 entries for balanced double-entry bookkeeping
            if (entries.size() < 2) {
                logger.warn(
                        "Transaction {} has only {} journal entries - may be unbalanced",
                        transaction.getId(),
                        entries.size());
            }

            // Create the PostTransactionCommand
            PostTransactionCommand command = new PostTransactionCommand(
                    transaction.getId().toString(), // referenceId
                    "TP Transaction: " + transaction.getIdempotencyKey(), // description
                    transaction.getTransactionDate(), // transactionDate
                    transaction.getCurrency(), // currency
                    "TP_SERVICE", // createdBy
                    entries // journal entries
            );

            logger.debug(
                    "Built GL transaction command with {} entries for TP transaction: {}",
                    entries.size(),
                    transaction.getId());

            return command;

        } catch (Exception e) {
            logger.error(
                    "Failed to build GL transaction command for TP transaction {}: {}",
                    transaction.getId(),
                    e.getMessage());
            throw new RuntimeException("GL transaction command building failed: " + e.getMessage(), e);
        }
    }

    /**
     * Returns the fee amount to use for GL revenue recognition.
     * Uses feeAmount when set (e.g. after FX add-on); otherwise estimatedFeeAmount so revenue is never missed.
     */
    private static BigDecimal getFeeAmountForGL(Transaction transaction) {
        if (transaction.getFeeAmount() != null && transaction.getFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            return transaction.getFeeAmount();
        }
        if (transaction.getEstimatedFeeAmount() != null
                && transaction.getEstimatedFeeAmount().compareTo(BigDecimal.ZERO) > 0) {
            return transaction.getEstimatedFeeAmount();
        }
        return BigDecimal.ZERO;
    }

    /**
     * Resolves the GL account ID for a customer account based on the mapping type.
     *
     * This method looks up the GLAccountMapping for the given customer account and mapping type
     * to find the corresponding GL account where the transaction should be posted.
     *
     * @param customerAccountId the customer account ID
     * @param mappingType the type of GL account mapping (e.g., PRIMARY_BALANCE, FEE_COLLECTION)
     * @return the GL account UUID
     * @throws IllegalStateException if no mapping is found
     */
    private UUID resolveGLAccountForCustomerAccount(UUID customerAccountId, GLAccountMappingType mappingType) {
        logger.debug(
                "Resolving GL account for customer account: {} with mapping type: {}",
                customerAccountId,
                mappingType);

        UUID glAccountId = customerAccountService.getGLAccountIdForType(customerAccountId, mappingType);

        if (glAccountId == null) {
            throw new IllegalStateException(
                    String.format(
                            "No %s GL account mapping found for customer account: %s. "
                                    + "Please ensure the customer account has proper GL account mappings configured.",
                            mappingType,
                            customerAccountId));
        }

        logger.debug(
                "Resolved GL account: {} for customer account: {} (type: {})",
                glAccountId,
                customerAccountId,
                mappingType);

        return glAccountId;
    }

    /**
     * Resolves the contra GL account for debit-only transactions.
     *
     * For debit-only transactions (e.g., cash withdrawal, external payment), we need to determine
     * which operational account to credit. This method examines the transaction metadata or type
     * to determine the appropriate contra account.
     *
     * Common scenarios:
     * - Cash withdrawal → CASH_VAULT
     * - External payment → EXTERNAL_CLEARING
     * - ATM withdrawal → ATM_CASH
     * - Default → SUSPENSE (for unclassified transactions)
     *
     * @param transaction the transaction to analyze
     * @return the contra GL account UUID
     */
    private UUID resolveContraAccountForDebitOnly(Transaction transaction) {
        logger.debug("Resolving contra account for debit-only transaction: {}", transaction.getId());

        // Check transaction metadata for hints about the transaction type
        TransactionRequest request = transaction.getRequest();
        Map<String, Object> metadata = request.getMetadata();

        OperationalGLAccountType contraType = OperationalGLAccountType.SUSPENSE; // Default

        if (metadata != null) {
            String transactionType = (String) metadata.get("transactionType");
            String channel = (String) metadata.get("channel");

            // Determine contra account based on transaction type or channel
            if ("CASH_WITHDRAWAL".equalsIgnoreCase(transactionType)) {
                contraType = OperationalGLAccountType.CASH_VAULT;
            } else if ("ATM_WITHDRAWAL".equalsIgnoreCase(transactionType) || "ATM".equalsIgnoreCase(channel)) {
                contraType = OperationalGLAccountType.ATM_CASH;
            } else if ("EXTERNAL_PAYMENT".equalsIgnoreCase(transactionType)
                    || "WIRE_TRANSFER".equalsIgnoreCase(transactionType)) {
                contraType = OperationalGLAccountType.EXTERNAL_CLEARING;
            } else {
                logger.debug(
                        "No specific transaction type found, using SUSPENSE for transaction: {}",
                        transaction.getId());
            }
        } else {
            logger.debug("No metadata found, using SUSPENSE for transaction: {}", transaction.getId());
        }

        UUID contraGLAccountId = generalLedgerService.getOperationalGLAccount(contraType.name());

        logger.debug(
                "Resolved contra account: {} (type: {}) for debit-only transaction: {}",
                contraGLAccountId,
                contraType,
                transaction.getId());

        return contraGLAccountId;
    }

    /**
     * Resolves the contra GL account for credit-only transactions.
     *
     * For credit-only transactions (e.g., cash deposit, external receipt), we need to determine
     * which operational account to debit. This method examines the transaction metadata or type
     * to determine the appropriate contra account.
     *
     * Common scenarios:
     * - Cash deposit → CASH_VAULT
     * - External receipt → EXTERNAL_CLEARING
     * - ATM deposit → ATM_CASH
     * - Default → SUSPENSE (for unclassified transactions)
     *
     * @param transaction the transaction to analyze
     * @return the contra GL account UUID
     */
    private UUID resolveContraAccountForCreditOnly(Transaction transaction) {
        logger.debug("Resolving contra account for credit-only transaction: {}", transaction.getId());

        // Check transaction metadata for hints about the transaction type
        TransactionRequest request = transaction.getRequest();
        Map<String, Object> metadata = request.getMetadata();

        OperationalGLAccountType contraType = OperationalGLAccountType.SUSPENSE; // Default

        if (metadata != null) {
            String transactionType = (String) metadata.get("transactionType");
            String channel = (String) metadata.get("channel");

            // Determine contra account based on transaction type or channel
            if ("CASH_DEPOSIT".equalsIgnoreCase(transactionType)) {
                contraType = OperationalGLAccountType.CASH_VAULT;
            } else if ("ATM_DEPOSIT".equalsIgnoreCase(transactionType) || "ATM".equalsIgnoreCase(channel)) {
                contraType = OperationalGLAccountType.ATM_CASH;
            } else if ("EXTERNAL_RECEIPT".equalsIgnoreCase(transactionType)
                    || "WIRE_RECEIPT".equalsIgnoreCase(transactionType)
                    || "INCOMING_WIRE".equalsIgnoreCase(transactionType)) {
                contraType = OperationalGLAccountType.EXTERNAL_CLEARING;
            } else {
                logger.debug(
                        "No specific transaction type found, using SUSPENSE for transaction: {}",
                        transaction.getId());
            }
        } else {
            logger.debug("No metadata found, using SUSPENSE for transaction: {}", transaction.getId());
        }

        UUID contraGLAccountId = generalLedgerService.getOperationalGLAccount(contraType.name());

        logger.debug(
                "Resolved contra account: {} (type: {}) for credit-only transaction: {}",
                contraGLAccountId,
                contraType,
                transaction.getId());

        return contraGLAccountId;
    }

    private void confirmBalanceReservations(Transaction transaction) {
        logger.debug("Confirming balance reservations for transaction: {}", transaction.getId());

        for (BalanceReservation reservation : transaction.getReservations()) {
            if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                try {
                    balanceReservationService.confirmReservation(reservation.getId());
                    reservation.setStatus(ReservationStatus.CONVERTED);
                } catch (Exception e) {
                    logger.error(
                            "Failed to confirm reservation {} for transaction {}: {}",
                            reservation.getId(),
                            transaction.getId(),
                            e.getMessage());
                    throw new RuntimeException("Failed to confirm balance reservation: " + e.getMessage(), e);
                }
            }
        }

        recordTransactionEvent(
                transaction,
                "RESERVATIONS_CONFIRMED",
                transaction.getStatus(),
                "All balance reservations confirmed");
    }

    private void releaseBalanceReservations(Transaction transaction) {
        logger.debug("Releasing balance reservations for transaction: {}", transaction.getId());

        for (BalanceReservation reservation : transaction.getReservations()) {
            if (reservation.getStatus() == ReservationStatus.ACTIVE) {
                try {
                    balanceReservationService.releaseReservation(reservation.getId());
                    reservation.setStatus(ReservationStatus.RELEASED);
                } catch (Exception e) {
                    logger.error(
                            "Failed to release reservation {} for transaction {}: {}",
                            reservation.getId(),
                            transaction.getId(),
                            e.getMessage());
                    // Continue with other reservations even if one fails
                }
            }
        }

        recordTransactionEvent(
                transaction,
                "RESERVATIONS_RELEASED",
                transaction.getStatus(),
                "All balance reservations released");
    }

    private boolean wasTransactionAuthorized(Transaction transaction) {
        return transaction.getEvents().stream().anyMatch(event -> event.getNewStatus() == TransactionStatus.AUTHORIZED);
    }

    private void recordTransactionEvent(Transaction transaction, String eventType, TransactionStatus status,
            String context) {
        recordTransactionEvent(transaction, eventType, status, context, null);
    }

    private void recordTransactionEvent(Transaction transaction, String eventType, TransactionStatus status,
            String context, String errorCode) {
        TransactionEvent event = new TransactionEvent();
        event.setTransaction(transaction);
        event.setEventType(eventType);
        event.setEventSequence(transaction.getEvents().size() + 1);
        event.setPreviousStatus(transaction.getStatus());
        event.setNewStatus(status);
        event.setCreatedBy("SYSTEM");

        if (context != null) {
            event.setEventData(Map.of("context", context));
        }

        if (errorCode != null) {
            event.setErrorCode(errorCode);
            event.setErrorMessage(context);
        }

        transaction.getEvents().add(event);

        logger.debug("Recorded transaction event: {} for transaction: {}", eventType, transaction.getId());
    }

    // Helper method for batch processing
    private <T> List<List<T>> partitionList(List<T> list, int batchSize) {
        List<List<T>> partitions = new ArrayList<>();
        for (int i = 0; i < list.size(); i += batchSize) {
            partitions.add(list.subList(i, Math.min(i + batchSize, list.size())));
        }
        return partitions;
    }

    /**
     * Processes post-transaction operations after successful transaction completion.
     * This includes notifications, analytics updates, and other non-critical operations.
     *
     * @param transaction the completed transaction
     */
    private void processPostTransactionOperations(Transaction transaction) {
        logger.debug("Processing post-transaction operations for transaction: {}", transaction.getId());

        try {
            // Update velocity limit usage for the account
            if (transaction.getSourceAccountId() != null) {
                velocityLimitService.incrementUsage(
                        transaction.getSourceAccountId(),
                        transaction.getTransactionType(),
                        transaction.getTotalAmount(),
                        transaction.getCurrency());
                logger.debug("Updated velocity limit usage for account: {}", transaction.getSourceAccountId());
            }

            // Record successful transaction event for analytics
            recordTransactionEvent(
                    transaction,
                    "POST_TRANSACTION_OPERATIONS_COMPLETED",
                    transaction.getStatus(),
                    "Post-transaction operations completed successfully");

            // Additional post-transaction operations can be added here:
            // - Send transaction confirmation notifications
            // - Update customer transaction history
            // - Trigger fraud detection analysis
            // - Update account statistics
            // - Generate transaction receipts

            logger.debug("Post-transaction operations completed for transaction: {}", transaction.getId());

        } catch (Exception e) {
            // Log the error but don't fail the transaction
            // Post-transaction operations are non-critical
            logger.error(
                    "Error during post-transaction operations for transaction {}: {}",
                    transaction.getId(),
                    e.getMessage());

            recordTransactionEvent(
                    transaction,
                    "POST_TRANSACTION_OPERATIONS_FAILED",
                    transaction.getStatus(),
                    "Post-transaction operations failed: " + e.getMessage(),
                    "POST_TRANSACTION_ERROR");
        }
    }

    // ==================== Refund Transaction Methods ====================

    public Transaction initiateFullRefund(UUID originalTransactionId, String reason, String initiatedBy) {
        logger.info("Initiating full refund for transaction: {}", originalTransactionId);

        Transaction originalTransaction = getTransactionById(originalTransactionId);

        // Validate transaction is refundable
        validateTransactionRefundable(originalTransaction);

        // Calculate full refund amount (principal + fees)
        BigDecimal refundAmount = originalTransaction.getTotalAmount();

        // Check if already fully refunded
        BigDecimal remainingRefundable = getRemainingRefundableAmount(originalTransactionId);
        if (remainingRefundable.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalStateException("Transaction has already been fully refunded: " + originalTransactionId);
        }

        // Use remaining refundable amount in case of previous partial refunds
        refundAmount = remainingRefundable;

        logger.info("Full refund amount: {} for transaction: {}", refundAmount, originalTransactionId);

        return createRefundTransaction(originalTransaction, refundAmount, reason, initiatedBy, RefundType.FULL);
    }

    public Transaction initiatePartialRefund(UUID originalTransactionId, BigDecimal refundAmount, String reason,
            String initiatedBy) {
        logger.info("Initiating partial refund of {} for transaction: {}", refundAmount, originalTransactionId);

        if (refundAmount == null || refundAmount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new IllegalArgumentException("Refund amount must be positive");
        }

        Transaction originalTransaction = getTransactionById(originalTransactionId);

        // Validate transaction is refundable
        validateTransactionRefundable(originalTransaction);

        // Check remaining refundable amount
        BigDecimal remainingRefundable = getRemainingRefundableAmount(originalTransactionId);

        if (refundAmount.compareTo(remainingRefundable) > 0) {
            throw new IllegalStateException(
                    String.format(
                            "Refund amount %s exceeds remaining refundable amount %s for transaction: %s",
                            refundAmount,
                            remainingRefundable,
                            originalTransactionId));
        }

        logger.info(
                "Partial refund amount: {} (remaining: {}) for transaction: {}",
                refundAmount,
                remainingRefundable,
                originalTransactionId);

        return createRefundTransaction(originalTransaction, refundAmount, reason, initiatedBy, RefundType.PARTIAL);
    }

    public BigDecimal getTotalRefundedAmount(UUID originalTransactionId) {
        logger.debug("Getting total refunded amount for transaction: {}", originalTransactionId);

        List<Transaction> refunds = getRefundTransactions(originalTransactionId);

        BigDecimal totalRefunded = refunds.stream().filter(t -> t.getStatus() == TransactionStatus.POSTED)
                .map(Transaction::getPrincipalAmount).reduce(BigDecimal.ZERO, BigDecimal::add);

        logger.debug("Total refunded amount: {} for transaction: {}", totalRefunded, originalTransactionId);

        return totalRefunded;
    }

    public BigDecimal getRemainingRefundableAmount(UUID originalTransactionId) {
        logger.debug("Getting remaining refundable amount for transaction: {}", originalTransactionId);

        Transaction originalTransaction = getTransactionById(originalTransactionId);
        BigDecimal originalAmount = originalTransaction.getTotalAmount();
        BigDecimal totalRefunded = getTotalRefundedAmount(originalTransactionId);

        BigDecimal remaining = originalAmount.subtract(totalRefunded);

        logger.debug("Remaining refundable amount: {} for transaction: {}", remaining, originalTransactionId);

        return remaining.max(BigDecimal.ZERO);
    }

    public boolean isTransactionRefundable(UUID transactionId) {
        logger.debug("Checking if transaction is refundable: {}", transactionId);

        try {
            Transaction transaction = getTransactionById(transactionId);

            // Transaction must be posted (successfully completed)
            if (transaction.getStatus() != TransactionStatus.POSTED) {
                logger.debug("Transaction {} is not refundable - status: {}", transactionId, transaction.getStatus());
                return false;
            }

            // Transaction must not be a refund itself
            if (transaction.getTransactionType() == TransactionType.REFUND) {
                logger.debug("Transaction {} is not refundable - already a refund", transactionId);
                return false;
            }

            // Check if there's remaining refundable amount
            BigDecimal remaining = getRemainingRefundableAmount(transactionId);
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                logger.debug("Transaction {} is not refundable - no remaining amount", transactionId);
                return false;
            }

            logger.debug("Transaction {} is refundable", transactionId);
            return true;

        } catch (Exception e) {
            logger.error("Error checking refundability for transaction {}: {}", transactionId, e.getMessage());
            return false;
        }
    }

    public List<Transaction> getRefundTransactions(UUID originalTransactionId) {
        logger.debug("Getting refund transactions for original transaction: {}", originalTransactionId);

        // Find all transactions that reference this transaction as the original
        List<Transaction> allTransactions = transactionRepository.findAll();

        List<Transaction> refunds = allTransactions.stream()
                .filter(t -> t.getTransactionType() == TransactionType.REFUND).filter(t -> {
                    TransactionRequest request = t.getRequest();
                    if (request.getMetadata() != null) {
                        Object originalTxnId = request.getMetadata().get("originalTransactionId");
                        return originalTransactionId.toString().equals(String.valueOf(originalTxnId));
                    }
                    return false;
                }).toList();

        logger.debug(
                "Found {} refund transactions for original transaction: {}",
                refunds.size(),
                originalTransactionId);

        return refunds;
    }

    /**
     * Validates that a transaction is eligible for refund.
     *
     * @param transaction the transaction to validate
     * @throws IllegalStateException if the transaction is not refundable
     */
    private void validateTransactionRefundable(Transaction transaction) {
        if (transaction.getStatus() != TransactionStatus.POSTED) {
            throw new IllegalStateException(
                    String.format(
                            "Transaction %s cannot be refunded - status: %s",
                            transaction.getId(),
                            transaction.getStatus()));
        }

        if (transaction.getTransactionType() == TransactionType.REFUND) {
            throw new IllegalStateException(
                    String.format("Cannot refund a refund transaction: %s", transaction.getId()));
        }

        // Validate that the original transaction has a source account (where funds came from)
        if (transaction.getSourceAccountId() == null) {
            throw new IllegalStateException(
                    String.format("Transaction %s cannot be refunded - no source account", transaction.getId()));
        }
    }

    /**
     * Creates a refund transaction for an original transaction.
     *
     * @param originalTransaction the original transaction to refund
     * @param refundAmount the amount to refund
     * @param reason the reason for the refund
     * @param initiatedBy who initiated the refund
     * @param refundType FULL or PARTIAL
     * @return the created refund transaction
     */
    private Transaction createRefundTransaction(Transaction originalTransaction, BigDecimal refundAmount, String reason,
            String initiatedBy, RefundType refundType) {
        logger.info(
                "Creating {} refund transaction for original transaction: {}",
                refundType,
                originalTransaction.getId());

        // Create refund transaction request
        TransactionRequest refundRequest = new TransactionRequest();
        refundRequest.setIdempotencyKey(
                "REFUND-" + originalTransaction.getId() + "-" + UUID.randomUUID().toString().substring(0, 8));
        refundRequest.setTransactionType(TransactionType.REFUND);
        refundRequest.setAmount(refundAmount);
        refundRequest.setCurrency(originalTransaction.getCurrency());

        // For refund: destination is the original source (return funds to original payer)
        refundRequest.setDestinationAccountId(originalTransaction.getSourceAccountId());
        refundRequest.setSourceAccountId(null); // No source for refund (funds come from bank)

        // Store refund metadata
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("originalTransactionId", originalTransaction.getId().toString());
        metadata.put("originalTransactionKey", originalTransaction.getIdempotencyKey());
        metadata.put("refundType", refundType.name());
        metadata.put("refundReason", reason);
        metadata.put("initiatedBy", initiatedBy);
        metadata.put("originalAmount", originalTransaction.getTotalAmount());
        metadata.put("transactionType", "REFUND");
        refundRequest.setMetadata(metadata);
        refundRequest.setCreatedBy(initiatedBy != null && !initiatedBy.isBlank() ? initiatedBy : "SYSTEM");

        // Initiate the refund transaction
        Transaction refundTransaction = initiateTransaction(refundRequest);

        logger.info(
                "Created refund transaction {} for original transaction {}",
                refundTransaction.getId(),
                originalTransaction.getId());

        // Process the refund transaction immediately
        try {
            refundTransaction = processTransaction(refundTransaction.getId());
            logger.info("Processed refund transaction: {}", refundTransaction.getId());
        } catch (Exception e) {
            logger.error("Failed to process refund transaction {}: {}", refundTransaction.getId(), e.getMessage());
            throw new RuntimeException("Refund processing failed: " + e.getMessage(), e);
        }

        return refundTransaction;
    }

    /**
     * Creates customer-facing AccountTransactions after successful GL posting.
     * This method is called from initiateGLPosting() after the GL transaction is created.
     * It creates AccountTransaction records that customers see in their statements.
     *
     * @param transaction the TP transaction that was posted to GL
     */
    private void createAccountTransactions(Transaction transaction) {
        logger.debug("Creating customer-facing AccountTransactions for transaction: {}", transaction.getId());

        UUID glTransactionId = transaction.getGlTransactionId();
        if (glTransactionId == null) {
            logger.warn(
                    "Cannot create AccountTransactions - GL transaction ID is null for transaction: {}",
                    transaction.getId());
            return;
        }

        LocalDateTime txDate = LocalDateTime.of(transaction.getTransactionDate(), java.time.LocalTime.now());

        try {
            // Create AccountTransaction for source account (debit)
            if (transaction.getSourceAccountId() != null) {
                String debitType = mapToAccountTransactionType(transaction.getTransactionType(), true);
                String debitDescription = buildAccountTransactionDescription(transaction, true);

                UUID sourceAccountTxId = customerAccountService.recordAndLinkAccountTransaction(
                        transaction.getSourceAccountId(),
                        debitType,
                        transaction.getTotalAmount(), // Principal + fees
                        transaction.getCurrency(),
                        txDate,
                        debitDescription,
                        transaction.getIdempotencyKey(),
                        glTransactionId);

                logger.info(
                        "Created source AccountTransaction: {} for TP transaction: {}",
                        sourceAccountTxId,
                        transaction.getId());

                recordTransactionEvent(
                        transaction,
                        "ACCOUNT_TRANSACTION_CREATED",
                        transaction.getStatus(),
                        "Source AccountTransaction created: " + sourceAccountTxId);
            }

            // Create AccountTransaction for destination account (credit)
            if (transaction.getDestinationAccountId() != null) {
                String creditType = mapToAccountTransactionType(transaction.getTransactionType(), false);
                String creditDescription = buildAccountTransactionDescription(transaction, false);

                UUID destAccountTxId = customerAccountService.recordAndLinkAccountTransaction(
                        transaction.getDestinationAccountId(),
                        creditType,
                        transaction.getPrincipalAmount(), // Principal only (no fees)
                        transaction.getCurrency(),
                        txDate,
                        creditDescription,
                        transaction.getIdempotencyKey(),
                        glTransactionId);

                logger.info(
                        "Created destination AccountTransaction: {} for TP transaction: {}",
                        destAccountTxId,
                        transaction.getId());

                recordTransactionEvent(
                        transaction,
                        "ACCOUNT_TRANSACTION_CREATED",
                        transaction.getStatus(),
                        "Destination AccountTransaction created: " + destAccountTxId);
            }

        } catch (Exception e) {
            logger.error(
                    "Failed to create AccountTransactions for transaction {}: {}",
                    transaction.getId(),
                    e.getMessage(),
                    e);
            // Don't fail the transaction - AccountTransactions can be created later via reconciliation
            recordTransactionEvent(
                    transaction,
                    "ACCOUNT_TRANSACTION_CREATION_FAILED",
                    transaction.getStatus(),
                    "Failed to create AccountTransactions: " + e.getMessage(),
                    "ACCOUNT_TX_ERROR");
        }
    }

    /**
     * Maps TP TransactionType to AccountTransactionType for customer-facing display.
     *
     * @param tpType the TP transaction type
     * @param isSource true if this is the source account (debit), false for destination (credit)
     * @return the AccountTransactionType as a string
     */
    private String mapToAccountTransactionType(TransactionType tpType, boolean isSource) {
        if (isSource) {
            // Debit transactions (money leaving the account)
            return switch (tpType) {
                case P2P, TRANSFER -> "TRANSFER_OUT";
                case CASH_OUT -> "WITHDRAWAL";
                case BILL_PAYMENT -> "FEE"; // Or could be a new type like BILL_PAYMENT
                case MERCHANT_PURCHASE -> "FEE"; // Or could be a new type like PURCHASE
                case REFUND -> "ADJUSTMENT"; // Refund source (rare case)
                default -> "ADJUSTMENT";
            };
        } else {
            // Credit transactions (money entering the account)
            return switch (tpType) {
                case P2P, TRANSFER -> "TRANSFER_IN";
                case CASH_IN, DEPOSIT -> "DEPOSIT";
                case REFUND -> "ADJUSTMENT"; // Refund destination (common case)
                default -> "ADJUSTMENT";
            };
        }
    }

    /**
     * Builds a customer-friendly description for AccountTransaction.
     *
     * @param transaction the TP transaction
     * @param isSource true if this is the source account, false for destination
     * @return customer-friendly description
     */
    private String buildAccountTransactionDescription(Transaction transaction, boolean isSource) {
        TransactionType type = transaction.getTransactionType();
        TransactionRequest request = transaction.getRequest();

        if (isSource) {
            // Descriptions for debit (money leaving)
            return switch (type) {
                case P2P, TRANSFER -> {
                    String destAccountId = transaction.getDestinationAccountId() != null
                            ? transaction.getDestinationAccountId().toString().substring(0, 8)
                            : "unknown";
                    yield "Transfer to account " + destAccountId;
                }
                case CASH_OUT -> "ATM Withdrawal";
                case BILL_PAYMENT -> {
                    String payee = request.getMetadata() != null && request.getMetadata().containsKey("payee")
                            ? String.valueOf(request.getMetadata().get("payee"))
                            : "Bill Payment";
                    yield payee;
                }
                case MERCHANT_PURCHASE -> {
                    String merchant = request.getMetadata() != null && request.getMetadata().containsKey("merchant")
                            ? String.valueOf(request.getMetadata().get("merchant"))
                            : "Purchase";
                    yield merchant;
                }
                case REFUND -> "Refund reversal";
                default -> "Transaction";
            };
        } else {
            // Descriptions for credit (money entering)
            return switch (type) {
                case P2P, TRANSFER -> {
                    String sourceAccountId = transaction.getSourceAccountId() != null
                            ? transaction.getSourceAccountId().toString().substring(0, 8)
                            : "unknown";
                    yield "Transfer from account " + sourceAccountId;
                }
                case CASH_IN, DEPOSIT -> "Deposit";
                case REFUND -> {
                    String originalTxId = request.getMetadata() != null
                            && request.getMetadata().containsKey("originalTransactionId")
                                    ? String.valueOf(request.getMetadata().get("originalTransactionId")).substring(0, 8)
                                    : "unknown";
                    yield "Refund for transaction " + originalTxId;
                }
                default -> "Transaction";
            };
        }
    }
}
