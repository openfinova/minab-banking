package com.openfinova.banking.customer.account.controller;

import com.openfinova.banking.customer.account.api.dto.*;
import com.openfinova.banking.customer.account.api.entity.AccountProductType;
import com.openfinova.banking.customer.account.api.entity.AccountStatus;
import com.openfinova.banking.customer.account.mapper.AccountMapper;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountSearchCriteria;
import com.openfinova.banking.customer.account.service.AccountService;
import com.openfinova.banking.identity.api.principal.CallerContextResolver;
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
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Management", description = "APIs for managing customer accounts")
/**
 * REST controller for customer account lifecycle management.
 * Handles account creation, retrieval, status updates, closure, batch operations, and dormancy detection.
 */
public class AccountController {

    private static final Logger log = LoggerFactory.getLogger(AccountController.class);

    private final AccountService accountService;
    private final AccountMapper accountMapper;

    public AccountController(AccountService accountService, AccountMapper accountMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Create account", description = "Creates a new customer account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data") })
    public ResponseEntity<AccountResponse> createAccount(Authentication authentication,
            @Valid @RequestBody CreateAccountRequest request) {

        log.info("Creating account for user: {}", request.getPrimaryUserProfileId());

        String createdBy = CallerContextResolver.resolveUsername(authentication);

        Account account = accountService.createAccount(
                request.getPrimaryUserProfileId(),
                request.getProductType(),
                request.getCurrency(),
                request.getAccountNumber(),
                createdBy);

        log.info("Successfully created account with ID: {}", account.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(accountMapper.toResponse(account));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get account by ID", description = "Retrieves a specific account by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found", content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<AccountResponse> getAccountById(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching account with ID: {}", id);

        return accountService.getAccountById(id).map(account -> {
            log.info("Found account: {}", account.getAccountNumber());
            return ResponseEntity.ok(accountMapper.toResponse(account));
        }).orElseGet(() -> {
            log.warn("Account not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/number/{accountNumber}")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get account by number", description = "Retrieves a specific account by its account number")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found", content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<AccountResponse> getAccountByNumber(
            @Parameter(description = "Account number", required = true) @PathVariable String accountNumber) {

        log.info("Fetching account with number: {}", accountNumber);

        return accountService.getAccountByNumber(accountNumber).map(account -> {
            log.info("Found account with ID: {}", account.getId());
            return ResponseEntity.ok(accountMapper.toResponse(account));
        }).orElseGet(() -> {
            log.warn("Account not found with number: {}", accountNumber);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/iban/{iban}")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get account by IBAN", description = "Retrieves a specific account by its IBAN")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found", content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<AccountResponse> getAccountByIban(
            @Parameter(description = "IBAN", required = true) @PathVariable String iban) {

        log.info("Fetching account with IBAN: {}", iban);

        return accountService.getAccountByIban(iban).map(account -> {
            log.info("Found account with ID: {}", account.getId());
            return ResponseEntity.ok(accountMapper.toResponse(account));
        }).orElseGet(() -> {
            log.warn("Account not found with IBAN: {}", iban);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/user/{userProfileId}")
    @PreAuthorize("hasAuthority('account:read:own')")
    @Operation(summary = "Get user's accounts", description = "Retrieves all accounts owned by a specific user")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully") })
    public ResponseEntity<List<AccountResponse>> getUserAccounts(
            @Parameter(description = "User profile ID", required = true) @PathVariable UUID userProfileId) {

        log.info("Fetching accounts for user: {}", userProfileId);

        List<Account> accounts = accountService.getAccountsByPrimaryUser(userProfileId);
        List<AccountResponse> response = accounts.stream().map(accountMapper::toResponse).toList();

        log.info("Found {} accounts for user: {}", response.size(), userProfileId);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Search accounts", description = "Search accounts with filters and pagination")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully") })
    public ResponseEntity<Page<AccountResponse>> searchAccounts(
            @Parameter(description = "Product type filter") @RequestParam(required = false) AccountProductType productType,
            @Parameter(description = "Status filter") @RequestParam(required = false) AccountStatus status,
            @Parameter(description = "User profile ID filter") @RequestParam(required = false) UUID primaryUserProfileId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {

        log.info("Searching accounts with filters");

        AccountSearchCriteria criteria = new AccountSearchCriteria();
        criteria.setProductType(productType);
        criteria.setStatus(status);
        criteria.setPrimaryUserProfileId(primaryUserProfileId);

        Page<Account> accounts = accountService.findAccountsWithFilters(criteria, pageable);
        Page<AccountResponse> response = accounts.map(accountMapper::toResponse);

        log.info("Found {} accounts", response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Update account status", description = "Updates the status of an account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Status updated successfully", content = @Content(schema = @Schema(implementation = AccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid status transition"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<AccountResponse> updateAccountStatus(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            Authentication authentication, @Valid @RequestBody UpdateAccountStatusRequest request) {

        String actor = authentication != null ? authentication.getName() : "system";
        log.info("Updating status of account {} to {} by {}", id, request.getNewStatus(), actor);

        accountService.updateAccountStatus(id, request.getNewStatus(), request.getReason(), actor);

        return accountService.getAccountById(id).map(account -> {
            log.info("Successfully updated status of account: {}", account.getAccountNumber());
            return ResponseEntity.ok(accountMapper.toResponse(account));
        }).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Close account", description = "Closes a customer account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Account closed successfully"),
            @ApiResponse(responseCode = "400", description = "Account cannot be closed"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<Void> closeAccount(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Reason for closure") @RequestParam String reason) {

        log.info("Closing account with ID: {}", id);

        try {
            accountService.closeAccount(id, reason);
            log.info("Successfully closed account: {}", id);
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            log.error("Failed to close account: {}", id, e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Validate account for transaction", description = "Validates if an account can perform a transaction")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Validation completed", content = @Content(schema = @Schema(implementation = ValidationResult.class))) })
    public ResponseEntity<ValidationResult> validateForTransaction(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Transaction amount") @RequestParam BigDecimal amount) {

        log.info("Validating account {} for transaction amount: {}", id, amount);

        ValidationResult result = accountService.validateAccountForTransaction(id, amount);

        log.info("Validation result for account {}: {}", id, result.isValid() ? "VALID" : "INVALID");

        return ResponseEntity.ok(result);
    }

    @PostMapping("/batch/status")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Batch update account status", description = "Updates status for multiple accounts")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Batch update completed") })
    public ResponseEntity<Map<UUID, String>> batchUpdateStatus(@Valid @RequestBody BatchStatusUpdateRequest request,
            Authentication authentication) {

        String actor = authentication != null ? authentication.getName() : "system";
        log.info("Batch updating status for {} accounts by {}", request.getAccountIds().size(), actor);

        Map<UUID, String> results = accountService
                .batchUpdateAccountStatus(request.getAccountIds(), request.getNewStatus(), request.getReason(), actor);

        long successCount = results.values().stream().filter(r -> r.equals("SUCCESS")).count();
        log.info(
                "Batch status update completed: {} successful, {} failed",
                successCount,
                results.size() - successCount);

        return ResponseEntity.ok(results);
    }

    @PostMapping("/batch/close")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Batch close accounts", description = "Closes multiple accounts")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Batch close completed") })
    public ResponseEntity<Map<UUID, String>> batchCloseAccounts(@Valid @RequestBody BatchCloseAccountsRequest request,
            Authentication authentication) {

        String actor = authentication != null ? authentication.getName() : "system";
        log.info("Batch closing {} accounts by {}", request.getAccountIds().size(), actor);

        Map<UUID, String> results = accountService
                .batchCloseAccounts(request.getAccountIds(), request.getReason(), actor);

        long successCount = results.values().stream().filter(r -> r.equals("SUCCESS")).count();
        log.info("Batch close completed: {} successful, {} failed", successCount, results.size() - successCount);

        return ResponseEntity.ok(results);
    }

    @GetMapping("/type/{productType}")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get accounts by product type", description = "Retrieves all accounts of a specific product type")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully") })
    public ResponseEntity<List<AccountResponse>> getAccountsByType(
            @Parameter(description = "Product type", required = true) @PathVariable AccountProductType productType) {

        log.info("Fetching accounts of type: {}", productType);

        List<Account> accounts = accountService.getAccountsOfType(productType);
        List<AccountResponse> response = accounts.stream().map(accountMapper::toResponse).toList();

        log.info("Found {} accounts of type: {}", response.size(), productType);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get accounts by status", description = "Retrieves all accounts with a specific status")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully") })
    public ResponseEntity<List<AccountResponse>> getAccountsByStatus(
            @Parameter(description = "Account status", required = true) @PathVariable AccountStatus status) {

        log.info("Fetching accounts with status: {}", status);

        List<Account> accounts = accountService.getAccountsByStatus(status);
        List<AccountResponse> response = accounts.stream().map(accountMapper::toResponse).toList();

        log.info("Found {} accounts with status: {}", response.size(), status);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/dormancy/process")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Process dormancy detection", description = "Marks inactive accounts as dormant")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Dormancy processing completed") })
    public ResponseEntity<Map<String, Integer>> processDormancyDetection(
            @Parameter(description = "Inactivity months threshold") @RequestParam(defaultValue = "12") int inactivityMonths) {

        log.info("Processing dormancy detection with {} months threshold", inactivityMonths);

        int count = accountService.processDormancyDetection(inactivityMonths);

        log.info("Marked {} accounts as dormant", count);

        return ResponseEntity.ok(Map.of("accountsMarkedDormant", count));
    }
}
