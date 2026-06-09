package com.openfinova.banking.customer.account.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.openfinova.banking.customer.account.api.dto.AccountRelationshipResponse;
import com.openfinova.banking.customer.account.api.dto.AccountResponse;
import com.openfinova.banking.customer.account.api.dto.AccountTransactionResponse;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import com.openfinova.banking.customer.account.entity.Account;
import com.openfinova.banking.customer.account.entity.AccountRelationship;
import com.openfinova.banking.customer.account.entity.AccountTransaction;
import com.openfinova.banking.customer.account.entity.AccountTransactionSearchCriteria;
import com.openfinova.banking.customer.account.mapper.AccountMapper;
import com.openfinova.banking.customer.account.mapper.AccountRelationshipMapper;
import com.openfinova.banking.customer.account.mapper.AccountTransactionMapper;
import com.openfinova.banking.customer.account.service.AccountRelationshipService;
import com.openfinova.banking.customer.account.service.AccountService;
import com.openfinova.banking.customer.account.service.AccountTransactionService;
import com.openfinova.banking.setup.api.DateTimeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Self-service account APIs for the authenticated customer. Identity is resolved in the service
 * layer from the JWT subject — callers never supply a user profile id. Requires
 * {@code account:read:own}.
 */

@RestController
@RequestMapping("/api/v1/accounts/me")
@Tag(name = "My Accounts", description = "Self-service account, relationship, and transaction access for the authenticated user")
public class CustomerAccountSelfServiceController {
    private static final Logger log = LoggerFactory.getLogger(CustomerAccountSelfServiceController.class);
    private final AccountService accountService;
    private final AccountMapper accountMapper;
    private final AccountRelationshipService relationshipService;
    private final AccountRelationshipMapper relationshipMapper;
    private final AccountTransactionService transactionService;
    private final AccountTransactionMapper transactionMapper;
    private final DateTimeService dateTimeService;

    public CustomerAccountSelfServiceController(AccountService accountService, AccountMapper accountMapper,
            AccountRelationshipService relationshipService, AccountRelationshipMapper relationshipMapper,
            AccountTransactionService transactionService, AccountTransactionMapper transactionMapper,
            DateTimeService dateTimeService) {
        this.accountService = accountService;
        this.accountMapper = accountMapper;
        this.relationshipService = relationshipService;
        this.relationshipMapper = relationshipMapper;
        this.transactionService = transactionService;
        this.transactionMapper = transactionMapper;
        this.dateTimeService = dateTimeService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('account:read:own')")
    @Operation(summary = "List my accounts", description = "Retrieves all accounts where the authenticated user is the primary holder")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Missing permission account:read:own") })
    public ResponseEntity<List<AccountResponse>> listMyAccounts() {
        log.info("Fetching accounts for authenticated user");
        List<Account> accounts = accountService.getMyAccounts();
        List<AccountResponse> response = accounts.stream().map(accountMapper::toResponse).toList();
        log.info("Found {} accounts for authenticated user", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/relationships")
    @PreAuthorize("hasAuthority('account:read:own')")
    @Operation(summary = "List my account relationships", description = "Retrieves all accounts where the authenticated user has any relationship")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Relationships retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Missing permission account:read:own") })
    public ResponseEntity<List<AccountRelationshipResponse>> listMyRelationships() {
        log.info("Fetching account relationships for authenticated user");
        List<AccountRelationship> relationships = relationshipService.getMyRelationships();
        List<AccountRelationshipResponse> response = relationships.stream()
                .map(relationship -> relationshipMapper.toResponse(relationship, dateTimeService.now())).toList();
        log.info("Found {} account relationships for authenticated user", response.size());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/transactions")
    @PreAuthorize("hasAuthority('account:read:own')")
    @Operation(summary = "Search my transactions", description = "Paginated search across all accounts owned by the authenticated user")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Transactions retrieved successfully"),
            @ApiResponse(responseCode = "403", description = "Missing permission account:read:own") })
    public ResponseEntity<Page<AccountTransactionResponse>> searchMyTransactions(
            @Parameter(description = "From date (inclusive)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime fromDate,
            @Parameter(description = "To date (inclusive)") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime toDate,
            @Parameter(description = "Optional account filter") @RequestParam(required = false) UUID accountId,
            @Parameter(description = "Transaction type filter") @RequestParam(required = false) AccountTransactionType transactionType,
            @Parameter(description = "Status filter") @RequestParam(required = false) String status,
            @Parameter(description = "Search description or reference") @RequestParam(required = false) String search,
            @PageableDefault(size = 20, sort = "transactionDate") Pageable pageable) {
        log.info("Searching transactions for authenticated user");
        AccountTransactionSearchCriteria criteria = new AccountTransactionSearchCriteria();
        criteria.setFromDate(fromDate);
        criteria.setToDate(toDate);
        criteria.setAccountId(accountId);
        criteria.setTransactionType(transactionType);
        criteria.setStatus(status);
        criteria.setSearch(search);
        Page<AccountTransaction> transactions = transactionService.searchMyTransactions(criteria, pageable);
        Page<AccountTransactionResponse> response = transactions.map(transactionMapper::toResponse);
        log.info("Retrieved {} transactions for authenticated user", response.getTotalElements());
        return ResponseEntity.ok(response);
    }

}
