package com.openfinova.banking.gl.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.GLJournalEntryResponse;
import com.openfinova.banking.gl.entity.GLJournalEntry;
import com.openfinova.banking.gl.mapper.GLTransactionMapper;
import com.openfinova.banking.gl.service.GLJournalEntryService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/v1/gl/journal-entries")
@Tag(name = "GL Journal Entries", description = "APIs for querying journal entries")
public class GLJournalEntryController {

    private static final Logger log = LoggerFactory.getLogger(GLJournalEntryController.class);

    private final GLJournalEntryService journalEntryService;
    private final GLTransactionMapper transactionMapper;

    public GLJournalEntryController(GLJournalEntryService journalEntryService, GLTransactionMapper transactionMapper) {
        this.journalEntryService = journalEntryService;
        this.transactionMapper = transactionMapper;
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get journal entry by ID", description = "Retrieves a specific journal entry by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Journal entry found", content = @Content(schema = @Schema(implementation = GLJournalEntryResponse.class))),
            @ApiResponse(responseCode = "404", description = "Journal entry not found") })
    public ResponseEntity<GLJournalEntryResponse> getEntryById(
            @Parameter(description = "Journal entry ID", required = true) @PathVariable UUID id) {

        log.info("Fetching journal entry with ID: {}", id);

        return journalEntryService.getEntryById(id).map(entry -> {
            log.info("Found journal entry for account: {}", entry.getAccount().getCode());
            return ResponseEntity.ok(transactionMapper.toJournalEntryResponse(entry));
        }).orElseGet(() -> {
            log.warn("Journal entry not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/transaction/{transactionId}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get entries by transaction", description = "Retrieves all journal entries for a specific transaction")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Entries retrieved successfully") })
    public ResponseEntity<List<GLJournalEntryResponse>> getEntriesByTransaction(
            @Parameter(description = "Transaction ID", required = true) @PathVariable UUID transactionId) {

        log.info("Fetching journal entries for transaction: {}", transactionId);

        List<GLJournalEntry> entries = journalEntryService.getEntriesByTransaction(transactionId);
        List<GLJournalEntryResponse> response = entries.stream().map(transactionMapper::toJournalEntryResponse)
                .toList();

        log.info("Retrieved {} journal entries for transaction: {}", response.size(), transactionId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get entries by account", description = "Retrieves all journal entries for a specific account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Entries retrieved successfully") })
    public ResponseEntity<List<GLJournalEntryResponse>> getEntriesByAccount(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId) {

        log.info("Fetching journal entries for account: {}", accountId);

        List<GLJournalEntry> entries = journalEntryService.getEntriesByAccount(accountId);
        List<GLJournalEntryResponse> response = entries.stream().map(transactionMapper::toJournalEntryResponse)
                .toList();

        log.info("Retrieved {} journal entries for account: {}", response.size(), accountId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}/date-range")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get entries by account and date range", description = "Retrieves journal entries for an account within a date range")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Entries retrieved successfully") })
    public ResponseEntity<List<GLJournalEntryResponse>> getEntriesByAccountAndDateRange(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Start date", required = true, example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Fetching journal entries for account {} from {} to {}", accountId, startDate, endDate);

        List<GLJournalEntry> entries = journalEntryService
                .getEntriesByAccountAndDateRange(accountId, startDate, endDate);
        List<GLJournalEntryResponse> response = entries.stream().map(transactionMapper::toJournalEntryResponse)
                .toList();

        log.info("Retrieved {} journal entries for account {} in date range", response.size(), accountId);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}/date")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get entries by account and date", description = "Retrieves journal entries for an account on a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Entries retrieved successfully") })
    public ResponseEntity<List<GLJournalEntryResponse>> getEntriesByAccountAndDate(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Transaction date", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Fetching journal entries for account {} on {}", accountId, date);

        List<GLJournalEntry> entries = journalEntryService.getEntriesByAccountAndDate(accountId, date);
        List<GLJournalEntryResponse> response = entries.stream().map(transactionMapper::toJournalEntryResponse)
                .toList();

        log.info("Retrieved {} journal entries for account {} on {}", response.size(), accountId, date);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/account/{accountId}/currency/{currency}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get entries by account, date range, and currency", description = "Retrieves journal entries filtered by currency")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Entries retrieved successfully") })
    public ResponseEntity<List<GLJournalEntryResponse>> getEntriesByAccountDateRangeAndCurrency(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID accountId,
            @Parameter(description = "Currency code", required = true, example = "USD") @PathVariable String currency,
            @Parameter(description = "Start date", required = true, example = "2024-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date", required = true, example = "2024-01-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info(
                "Fetching journal entries for account {} in {} from {} to {}",
                accountId,
                currency,
                startDate,
                endDate);

        List<GLJournalEntry> entries = journalEntryService
                .getEntriesByAccountDateRangeAndCurrency(accountId, startDate, endDate, currency);
        List<GLJournalEntryResponse> response = entries.stream().map(transactionMapper::toJournalEntryResponse)
                .toList();

        log.info("Retrieved {} journal entries for account {} in {}", response.size(), accountId, currency);

        return ResponseEntity.ok(response);
    }
}
