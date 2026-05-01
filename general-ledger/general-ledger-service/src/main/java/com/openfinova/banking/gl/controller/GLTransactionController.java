package com.openfinova.banking.gl.controller;

import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.GLTransactionResponse;
import com.openfinova.banking.gl.api.dto.PostTransactionCommand;
import com.openfinova.banking.gl.api.dto.PostTransactionRequest;
import com.openfinova.banking.gl.api.dto.ReverseTransactionRequest;
import com.openfinova.banking.gl.api.dto.TransactionValidationResponse;
import com.openfinova.banking.gl.api.entity.GLApprovalRole;
import com.openfinova.banking.gl.entity.GLTransaction;
import com.openfinova.banking.gl.mapper.GLTransactionMapper;
import com.openfinova.banking.gl.service.GLTransactionService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gl/transactions")
@Tag(name = "GL Transaction Management", description = "APIs for posting and managing GL transactions")
public class GLTransactionController {

    private static final Logger log = LoggerFactory.getLogger(GLTransactionController.class);

    private final GLTransactionService transactionService;
    private final GLTransactionMapper transactionMapper;

    public GLTransactionController(GLTransactionService transactionService, GLTransactionMapper transactionMapper) {
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('gl:post')")
    @Operation(summary = "Submit a GL transaction for approval", description = "Creates a draft GL transaction and submits it into the maker-checker "
            + "approval workflow. The transaction is NOT posted immediately; a separate "
            + "approver must approve it via the /approvals endpoint.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Transaction accepted and pending approval", content = @Content(schema = @Schema(implementation = GLTransactionResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid transaction data or unbalanced entries"),
            @ApiResponse(responseCode = "409", description = "Duplicate reference ID") })
    public ResponseEntity<GLTransactionResponse> postTransaction(@Valid @RequestBody PostTransactionRequest request,
            @Parameter(description = "Role of the submitter (maker). Defaults to ACCOUNTANT.") @RequestParam(defaultValue = "ACCOUNTANT") GLApprovalRole submitterRole) {

        log.info("Submitting GL transaction for approval, reference: {}", request.getReferenceId());

        // Map request → PostTransactionCommand
        List<PostTransactionCommand.JournalEntryCommand> entryCmds = request.getEntries().stream().map(e -> {
            PostTransactionCommand.JournalEntryCommand cmd = new PostTransactionCommand.JournalEntryCommand(
                    e.getAccountId(),
                    e.getDebitAmount(),
                    e.getCreditAmount(),
                    e.getDescription(),
                    request.getTransactionDate());
            cmd.setCurrency(e.getCurrency());
            cmd.setExchangeRate(e.getExchangeRate());
            return cmd;
        }).toList();

        PostTransactionCommand command = new PostTransactionCommand(
                request.getReferenceId(),
                request.getDescription(),
                request.getTransactionDate(),
                request.getCurrency(),
                request.getPostedBy(),
                entryCmds);

        // Step 1 — persist as DRAFT (validates business rules, no transaction number yet, no balance updates).
        GLTransaction draft = transactionService.createDraftTransaction(command, request.getPostedBy());

        // Step 2 — submit the draft into the maker-checker approval workflow.
        //           The transaction moves to PENDING_APPROVAL; it will only be posted
        //           to the GL once a separate approver (checker) approves it.
        transactionService.submitTransactionForApproval(draft.getId(), request.getPostedBy(), submitterRole);

        // Re-read the saved state so the response reflects the current PENDING_APPROVAL status.
        GLTransaction pending = transactionService.getTransactionById(draft.getId()).orElse(draft);

        log.info("GL transaction {} submitted for approval (ID: {})", pending.getReferenceId(), pending.getId());

        // 202 Accepted: the request has been accepted for processing (approval pending).
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(transactionMapper.toResponse(pending));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get transaction by ID", description = "Retrieves a specific GL transaction by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(schema = @Schema(implementation = GLTransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<GLTransactionResponse> getTransactionById(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID id) {

        log.info("Fetching GL transaction with ID: {}", id);

        return transactionService.getTransactionById(id).map(transaction -> {
            log.info("Found GL transaction: {}", transaction.getReferenceId());
            return ResponseEntity.ok(transactionMapper.toResponse(transaction));
        }).orElseGet(() -> {
            log.warn("GL transaction not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/reference/{referenceId}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get transaction by reference ID", description = "Retrieves a GL transaction by its external reference ID")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Transaction found", content = @Content(schema = @Schema(implementation = GLTransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<GLTransactionResponse> getTransactionByReference(
            @Parameter(description = "External reference ID", required = true, example = "TXN-2024-001") @PathVariable String referenceId) {

        log.info("Fetching GL transaction with reference: {}", referenceId);

        return transactionService.getTransactionByReference(referenceId).map(transaction -> {
            log.info("Found GL transaction with ID: {}", transaction.getId());
            return ResponseEntity.ok(transactionMapper.toResponse(transaction));
        }).orElseGet(() -> {
            log.warn("GL transaction not found with reference: {}", referenceId);
            return ResponseEntity.notFound().build();
        });
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Reverse a transaction", description = "Creates a reversal transaction with contra-entries")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Transaction reversed successfully", content = @Content(schema = @Schema(implementation = GLTransactionResponse.class))),
            @ApiResponse(responseCode = "404", description = "Transaction not found"),
            @ApiResponse(responseCode = "400", description = "Transaction cannot be reversed") })
    public ResponseEntity<GLTransactionResponse> reverseTransaction(
            @Parameter(description = "Transaction ID to reverse", required = true) @PathVariable UUID id,
            @RequestBody ReverseTransactionRequest request) {

        log.info("Reversing GL transaction with ID: {}", id);

        GLTransaction reversalTransaction = transactionService
                .reverseTransaction(id, request.getReason(), request.getReversedBy());

        log.info("Successfully reversed GL transaction. Reversal ID: {}", reversalTransaction.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(transactionMapper.toResponse(reversalTransaction));
    }

    @GetMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Validate transaction balance", description = "Checks if a transaction's debits equal credits")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Validation result"),
            @ApiResponse(responseCode = "404", description = "Transaction not found") })
    public ResponseEntity<TransactionValidationResponse> validateTransactionBalance(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID id) {

        log.info("Validating balance for transaction ID: {}", id);

        boolean isBalanced = transactionService.validateTransactionBalance(id);

        TransactionValidationResponse response = new TransactionValidationResponse();
        response.setTransactionId(id);
        response.setBalanced(isBalanced);

        log.info("Transaction {} balance validation: {}", id, isBalanced ? "PASSED" : "FAILED");

        return ResponseEntity.ok(response);
    }
}
