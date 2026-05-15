package com.openfinova.banking.tp.controller;

import com.openfinova.banking.tp.api.dto.RefundRequest;
import com.openfinova.banking.tp.api.dto.TransactionResponse;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.entity.TransactionEvent;
import com.openfinova.banking.tp.entity.TransactionRequest;
import com.openfinova.banking.tp.api.entity.TransactionStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.mapper.TransactionMapper;
import com.openfinova.banking.tp.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for customer-facing transaction operations.
 *
 * NOTE: This controller exposes only customer-facing endpoints.
 * Internal transaction processing methods (process, complete, fail, batch operations, etc.)
 * are called internally by TransactionService and should NOT be exposed via REST API.
 *
 * Customer-facing endpoints:
 * - POST /api/transactions - Initiate a new transaction
 * - GET /api/transactions/{id} - View transaction details
 * - GET /api/transactions/{id}/status - Check transaction status
 * - GET /api/transactions/{id}/history - View transaction event history
 */
@RestController
@RequestMapping("/api/v1/transactions")
@Tag(name = "Transaction Processing", description = "Customer-facing APIs for transaction management")
public class TransactionController {

    private static final Logger log = LoggerFactory.getLogger(TransactionController.class);

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;

    public TransactionController(TransactionService transactionService, TransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('payment:initiate')")
    @Operation(summary = "Initiate transaction", description = "Creates and initiates a new transaction. This is the main entry point for customers to start a transaction. The transaction will be processed asynchronously.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction initiated successfully", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid transaction request"),
            @ApiResponse(responseCode = "409", description = "Duplicate transaction (idempotency key already exists)") })
    public ResponseEntity<TransactionResponse> initiateTransaction(@Valid @RequestBody TransactionRequest request) {

        log.info(
                "Initiating transaction: type={}, amount={}, idempotencyKey={}",
                request.getTransactionType(),
                request.getAmount(),
                request.getIdempotencyKey());

        Transaction transaction = transactionService.initiateTransaction(request);

        log.info("Successfully initiated transaction with ID: {}", transaction.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toResponse(transaction));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('transaction:read')")
    @Operation(summary = "Search transactions", description = "Paginated admin search with optional filters (account, status, type, dates, currency, amount range, reference text)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Page of transactions", content = @Content(schema = @Schema(implementation = TransactionResponse.class))) })
    public ResponseEntity<Page<TransactionResponse>> searchTransactions(
            @Parameter(description = "Involves this account as source or destination") @RequestParam(required = false) UUID accountId,
            @Parameter(description = "Transaction status") @RequestParam(required = false) TransactionStatus status,
            @Parameter(description = "Transaction type (from request)") @RequestParam(required = false) TransactionType transactionType,
            @Parameter(description = "Minimum transaction date (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "Maximum transaction date (inclusive)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate,
            @Parameter(description = "ISO currency code") @RequestParam(required = false) String currency,
            @Parameter(description = "Minimum principal amount (request amount)") @RequestParam(required = false) BigDecimal minAmount,
            @Parameter(description = "Maximum principal amount (request amount)") @RequestParam(required = false) BigDecimal maxAmount,
            @Parameter(description = "Substring match on idempotency key, external ref, gateway id, or client reference") @RequestParam(required = false) String reference,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info(
                "Searching transactions: accountId={}, status={}, type={}, from={}, to={}",
                accountId,
                status,
                transactionType,
                fromDate,
                toDate);

        Page<TransactionResponse> page = transactionService.searchTransactions(
                accountId,
                status,
                transactionType,
                fromDate,
                toDate,
                currency,
                minAmount,
                maxAmount,
                reference,
                pageable);

        return ResponseEntity.ok(page);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('transaction:read')")
    @Operation(summary = "Get transaction details", description = "Retrieves complete transaction details by ID including amounts, fees, and current status")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction retrieved successfully", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<TransactionResponse> getTransaction(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID id) {

        log.info("Fetching transaction: {}", id);

        Transaction transaction = transactionService.getTransactionById(id);

        if (transaction == null) {
            log.warn("Transaction not found: {}", id);
            return ResponseEntity.notFound().build();
        }

        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @GetMapping("/{id}/status")
    @PreAuthorize("hasAuthority('transaction:read')")
    @Operation(summary = "Get transaction status", description = "Retrieves the current status of a transaction for real-time tracking. Useful for polling transaction progress.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<Map<String, TransactionStatus>> getTransactionStatus(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID id) {

        log.info("Fetching status for transaction: {}", id);

        TransactionStatus status = transactionService.getTransactionStatus(id);

        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/{id}/history")
    @PreAuthorize("hasAuthority('transaction:read')")
    @Operation(summary = "Get transaction history", description = "Retrieves the complete event history for a transaction, useful for auditing and customer support")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "History retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<List<TransactionEvent>> getTransactionHistory(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID id) {

        log.info("Fetching history for transaction: {}", id);

        List<TransactionEvent> history = transactionService.getTransactionHistory(id);

        log.info("Found {} events for transaction: {}", history.size(), id);

        return ResponseEntity.ok(history);
    }

    @PostMapping("/{id}/refund/full")
    @PreAuthorize("hasAuthority('transaction:write')")
    @Operation(summary = "Initiate full refund", description = "Initiates a full refund of a completed transaction, returning the entire amount (principal + fees) to the original source account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Full refund initiated successfully", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid refund request"),
            @ApiResponse(responseCode = "404", description = "Original transaction not found"),
            @ApiResponse(responseCode = "409", description = "Transaction already fully refunded or not refundable") })
    public ResponseEntity<TransactionResponse> initiateFullRefund(
            @Parameter(description = "Original transaction ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request) {

        log.info("Initiating full refund for transaction: {}", id);

        Transaction refundTransaction = transactionService.initiateFullRefund(
                id,
                request.getReason(),
                request.getInitiatedBy() != null ? request.getInitiatedBy() : "CUSTOMER");

        log.info("Successfully initiated full refund with transaction ID: {}", refundTransaction.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toResponse(refundTransaction));
    }

    @PostMapping("/{id}/refund/partial")
    @PreAuthorize("hasAuthority('transaction:write')")
    @Operation(summary = "Initiate partial refund", description = "Initiates a partial refund of a completed transaction, returning a specified amount to the original source account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Partial refund initiated successfully", content = @Content(schema = @Schema(implementation = TransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid refund request or amount exceeds refundable amount"),
            @ApiResponse(responseCode = "404", description = "Original transaction not found"),
            @ApiResponse(responseCode = "409", description = "Transaction not refundable") })
    public ResponseEntity<TransactionResponse> initiatePartialRefund(
            @Parameter(description = "Original transaction ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody RefundRequest request) {

        log.info("Initiating partial refund of {} for transaction: {}", request.getRefundAmount(), id);

        if (request.getRefundAmount() == null) {
            return ResponseEntity.badRequest().build();
        }

        Transaction refundTransaction = transactionService.initiatePartialRefund(
                id,
                request.getRefundAmount(),
                request.getReason(),
                request.getInitiatedBy() != null ? request.getInitiatedBy() : "CUSTOMER");

        log.info("Successfully initiated partial refund with transaction ID: {}", refundTransaction.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toResponse(refundTransaction));
    }

    @GetMapping("/{id}/refundable-amount")
    @PreAuthorize("hasAuthority('transaction:read')")
    @Operation(summary = "Get refundable amount", description = "Retrieves the remaining refundable amount for a transaction (original amount minus any refunds already processed)")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refundable amount retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<Map<String, Object>> getRefundableAmount(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID id) {

        log.info("Fetching refundable amount for transaction: {}", id);

        Transaction transaction = transactionService.getTransactionById(id);
        BigDecimal totalRefunded = transactionService.getTotalRefundedAmount(id);
        BigDecimal remainingRefundable = transactionService.getRemainingRefundableAmount(id);
        boolean isRefundable = transactionService.isTransactionRefundable(id);

        Map<String, Object> response = Map.of(
                "transactionId",
                id,
                "originalAmount",
                transaction.getTotalAmount(),
                "totalRefunded",
                totalRefunded,
                "remainingRefundable",
                remainingRefundable,
                "isRefundable",
                isRefundable,
                "currency",
                transaction.getCurrency());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{id}/refunds")
    @PreAuthorize("hasAuthority('transaction:read')")
    @Operation(summary = "Get refund transactions", description = "Retrieves all refund transactions associated with an original transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Refund transactions retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<List<TransactionResponse>> getRefundTransactions(
            @Parameter(description = "Original transaction ID", required = true) @PathVariable UUID id) {

        log.info("Fetching refund transactions for original transaction: {}", id);

        List<Transaction> refunds = transactionService.getRefundTransactions(id);

        List<TransactionResponse> responses = refunds.stream().map(transactionMapper::toResponse).toList();

        log.info("Found {} refund transactions for transaction: {}", responses.size(), id);

        return ResponseEntity.ok(responses);
    }
}
