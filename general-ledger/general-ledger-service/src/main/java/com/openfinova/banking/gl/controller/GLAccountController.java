package com.openfinova.banking.gl.controller;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.CreateGLAccountRequest;
import com.openfinova.banking.gl.api.dto.GLAccountResponse;
import com.openfinova.banking.gl.api.dto.UpdateGLAccountRequest;
import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import com.openfinova.banking.gl.api.entity.GLAccountType;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.mapper.GLAccountMapper;
import com.openfinova.banking.gl.service.GLAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gl/accounts")
@Tag(name = "GL Account Management", description = "APIs for managing General Ledger accounts")
public class GLAccountController {

    private static final Logger log = LoggerFactory.getLogger(GLAccountController.class);

    private final GLAccountService accountService;
    private final GLAccountMapper accountMapper;

    public GLAccountController(GLAccountService accountService, GLAccountMapper accountMapper) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Create a new GL account", description = "Creates a new General Ledger account in the chart of accounts")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Account created successfully", content = @Content(schema = @Schema(implementation = GLAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "409", description = "Account with the same code already exists") })
    public ResponseEntity<GLAccountResponse> createAccount(@Valid @RequestBody CreateGLAccountRequest request) {

        log.info("Creating GL account with code: {}", request.getCode());

        GLAccount account = accountService.createAccount(request);
        GLAccountResponse response = accountMapper.toResponse(account);

        log.info("Successfully created GL account with ID: {} and code: {}", account.getId(), account.getCode());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get GL account by ID", description = "Retrieves a specific GL account by its unique identifier")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found", content = @Content(schema = @Schema(implementation = GLAccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<GLAccountResponse> getAccountById(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching GL account with ID: {}", id);

        return accountService.getAccountById(id).map(account -> {
            log.info("Found GL account: {}", account.getCode());
            return ResponseEntity.ok(accountMapper.toResponse(account));
        }).orElseGet(() -> {
            log.warn("GL account not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/code/{code}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get GL account by code", description = "Retrieves a specific GL account by its account code")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account found", content = @Content(schema = @Schema(implementation = GLAccountResponse.class))),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<GLAccountResponse> getAccountByCode(
            @Parameter(description = "Account code", required = true, example = "1000") @PathVariable String code) {

        log.info("Fetching GL account with code: {}", code);

        return accountService.findByCode(code).map(account -> {
            log.info("Found GL account with ID: {}", account.getId());
            return ResponseEntity.ok(accountMapper.toResponse(account));
        }).orElseGet(() -> {
            log.warn("GL account not found with code: {}", code);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "List / search GL accounts", description = "Returns a paginated, filterable view of the chart of accounts. "
            + "All filter parameters are optional and combinable. " + "Default sort is by account code ascending.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully") })
    public ResponseEntity<Page<GLAccountResponse>> listAccounts(
            @Parameter(description = "Filter by account type (e.g. ASSET, LIABILITY, EQUITY, REVENUE, EXPENSE)") @RequestParam(required = false) GLAccountType type,
            @Parameter(description = "Filter by account status (ACTIVE or INACTIVE)") @RequestParam(required = false) GLAccountStatus status,
            @Parameter(description = "Filter by currency code (e.g. USD, EUR)") @RequestParam(required = false) String currency,
            @Parameter(description = "Case-insensitive substring search on account name or code") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "code") Pageable pageable) {

        log.info(
                "Listing GL accounts: type={}, status={}, currency='{}', search='{}', page={}, size={}",
                type,
                status,
                currency,
                search,
                pageable.getPageNumber(),
                pageable.getPageSize());

        Page<GLAccount> accounts = accountService.filterAccounts(type, status, currency, search, pageable);
        Page<GLAccountResponse> response = accounts.map(accountMapper::toResponse);

        log.info(
                "Retrieved {} GL accounts out of {} total",
                response.getNumberOfElements(),
                response.getTotalElements());

        return ResponseEntity.ok(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Update GL account", description = "Updates an existing GL account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account updated successfully", content = @Content(schema = @Schema(implementation = GLAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input data"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<GLAccountResponse> updateAccount(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody UpdateGLAccountRequest request) {

        log.info("Updating GL account with ID: {}", id);

        return accountService.getAccountById(id).map(account -> {
            accountMapper.updateEntityFromRequest(account, request);

            // Handle parent update if provided
            if (request.getParentId() != null) {
                accountService.getAccountById(request.getParentId()).ifPresent(account::setParent);
            } else {
                account.setParent(null);
            }

            GLAccount updatedAccount = accountService.updateAccount(account);
            log.info("Successfully updated GL account: {}", updatedAccount.getCode());

            return ResponseEntity.ok(accountMapper.toResponse(updatedAccount));
        }).orElseGet(() -> {
            log.warn("GL account not found with ID: {}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @PostMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Deactivate GL account", description = "Deactivates a GL account (soft delete) with reason tracking for audit trail")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account deactivated successfully", content = @Content(schema = @Schema(implementation = GLAccountResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid reason or cannot deactivate account with active children"),
            @ApiResponse(responseCode = "404", description = "Account not found") })
    public ResponseEntity<GLAccountResponse> deactivateAccount(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Reason for deactivation", required = false) @RequestParam(required = false, defaultValue = "") String reason) {

        log.info("Deactivating GL account with ID: {} and reason: {}", id, reason);

        GLAccount deactivatedAccount = accountService.deactivateAccount(id, reason);
        log.info("Successfully deactivated GL account: {}", deactivatedAccount.getCode());

        return ResponseEntity.ok(accountMapper.toResponse(deactivatedAccount));
    }
}
