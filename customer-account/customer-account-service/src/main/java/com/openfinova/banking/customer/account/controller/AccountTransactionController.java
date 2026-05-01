package com.openfinova.banking.customer.account.controller;

import java.time.LocalDateTime;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.customer.account.api.dto.AccountTransactionResponse;
import com.openfinova.banking.customer.account.api.dto.RecordTransactionRequest;
import com.openfinova.banking.customer.account.entity.AccountTransaction;
import com.openfinova.banking.customer.account.mapper.AccountTransactionMapper;
import com.openfinova.banking.customer.account.service.AccountTransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Transactions", description = "APIs for account transaction history")
/**
 * REST controller for account transaction operations.
 * Handles recording transactions, querying transaction history, and linking transactions to GL entries.
 */
public class AccountTransactionController {

    private static final Logger log = LoggerFactory.getLogger(AccountTransactionController.class);

    private final AccountTransactionService transactionService;
    private final AccountTransactionMapper transactionMapper;

    public AccountTransactionController(AccountTransactionService transactionService,
            AccountTransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping("/{id}/transactions")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Record transaction", description = "Records a new transaction on an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Transaction recorded successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data") })
    public ResponseEntity<AccountTransactionResponse> recordTransaction(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody RecordTransactionRequest request) {

        log.info("Recording transaction for account: {}", id);

        AccountTransaction transaction = transactionService.recordTransaction(
                id,
                request.getTransactionType(),
                request.getAmount(),
                request.getCurrency(),
                request.getTransactionDate(),
                request.getDescription(),
                request.getReferenceId());

        log.info("Successfully recorded transaction with ID: {}", transaction.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toResponse(transaction));
    }

    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get transaction history", description = "Retrieves transaction history for an account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction history retrieved successfully") })
    public ResponseEntity<Page<AccountTransactionResponse>> getTransactionHistory(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "From date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "To date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @PageableDefault(size = 50, sort = "transactionDate") Pageable pageable) {

        log.info("Fetching transaction history for account {} from {} to {}", id, fromDate, toDate);

        Page<AccountTransaction> transactions = transactionService
                .getTransactionHistory(id, fromDate, toDate, pageable);
        Page<AccountTransactionResponse> response = transactions.map(transactionMapper::toResponse);

        log.info("Retrieved {} transactions for account: {}", response.getTotalElements(), id);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions/{transactionId}")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get transaction by ID", description = "Retrieves a specific transaction by its ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Transaction found"),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<AccountTransactionResponse> getTransactionById(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID transactionId) {

        log.info("Fetching transaction with ID: {}", transactionId);

        AccountTransaction transaction = transactionService.getTransactionById(transactionId);

        log.info("Found transaction: {}", transactionId);

        return ResponseEntity.ok(transactionMapper.toResponse(transaction));
    }

    @PatchMapping("/transactions/{transactionId}/gl-link")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Update GL transaction link", description = "Links an account transaction to a GL transaction")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "GL link updated successfully"),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<Void> updateGLTransactionLink(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID transactionId,
            @Parameter(description = "GL transaction ID") @RequestParam UUID glTransactionId) {

        log.info("Updating GL link for transaction {} to GL transaction {}", transactionId, glTransactionId);

        transactionService.updateGLTransactionLink(transactionId, glTransactionId);

        log.info("Successfully updated GL link for transaction: {}", transactionId);

        return ResponseEntity.ok().build();
    }
}
