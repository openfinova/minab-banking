package com.openfinova.banking.loan.controller;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.customer.account.api.dto.ValidationResult;
import com.openfinova.banking.loan.api.dto.LoanAccountBalanceUpdateRequest;
import com.openfinova.banking.loan.api.dto.LoanAccountBatchStatusUpdateRequest;
import com.openfinova.banking.loan.api.dto.LoanAccountCloseRequest;
import com.openfinova.banking.loan.api.dto.LoanAccountCreateRequest;
import com.openfinova.banking.loan.api.dto.LoanAccountDisburseRequest;
import com.openfinova.banking.loan.api.dto.LoanAccountResponse;
import com.openfinova.banking.loan.api.dto.LoanAccountRestructureMarkRequest;
import com.openfinova.banking.loan.api.dto.LoanAccountStatusUpdateRequest;
import com.openfinova.banking.loan.api.dto.LoanAccountTopUpRequest;
import com.openfinova.banking.loan.api.dto.LoanAccountWriteOffRequest;
import com.openfinova.banking.loan.api.dto.LoanStatementResponse;
import com.openfinova.banking.loan.api.entity.DelinquencyBucket;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.mapper.LoanAccountMapper;
import com.openfinova.banking.loan.service.LoanAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for managing loan accounts.
 * Provides user-facing operations for loan account information and management.
 */
@RestController
@RequestMapping("/api/v1/loan-accounts")
@Tag(name = "Loan Accounts", description = "Loan account management APIs")
public class LoanAccountController {

    private final LoanAccountService accountService;

    public LoanAccountController(LoanAccountService accountService) {
        this.accountService = accountService;
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get loan account by ID")
    public ResponseEntity<LoanAccountResponse> getLoanAccountById(@PathVariable UUID id) {
        return accountService.getLoanAccountById(id).map(LoanAccountMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{loanAccountNumber}")
    @Operation(summary = "Get loan account by account number")
    public ResponseEntity<LoanAccountResponse> getLoanAccountByNumber(@PathVariable String loanAccountNumber) {

        return accountService.getLoanAccountByNumber(loanAccountNumber).map(LoanAccountMapper::toResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all loan accounts for a customer")
    public ResponseEntity<Page<LoanAccountResponse>> getLoanAccountsByCustomer(@PathVariable UUID customerId,
            Pageable pageable) {

        Page<LoanAccount> accounts = accountService.getLoanAccountsByCustomer(customerId, pageable);
        return ResponseEntity.ok(accounts.map(LoanAccountMapper::toResponse));
    }

    @GetMapping("/customer/{customerId}/active")
    @Operation(summary = "Get active loan accounts for a customer")
    public ResponseEntity<List<LoanAccountResponse>> getActiveLoanAccountsByCustomer(@PathVariable UUID customerId) {

        List<LoanAccount> accounts = accountService.getActiveLoanAccountsByCustomer(customerId);
        List<LoanAccountResponse> responses = accounts.stream().map(LoanAccountMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get loan accounts by status")
    public ResponseEntity<Page<LoanAccountResponse>> getLoanAccountsByStatus(@PathVariable LoanStatus status,
            Pageable pageable) {

        Page<LoanAccount> accounts = accountService.getLoanAccountsByStatus(status, pageable);
        return ResponseEntity.ok(accounts.map(LoanAccountMapper::toResponse));
    }

    @GetMapping("/delinquent")
    @Operation(summary = "Get delinquent loan accounts")
    public ResponseEntity<Page<LoanAccountResponse>> getDelinquentLoanAccounts(Pageable pageable) {
        Page<LoanAccount> accounts = accountService.getDelinquentLoanAccounts(pageable);
        return ResponseEntity.ok(accounts.map(LoanAccountMapper::toResponse));
    }

    @GetMapping("/delinquency-bucket/{bucket}")
    @Operation(summary = "Get loan accounts by delinquency bucket")
    public ResponseEntity<Page<LoanAccountResponse>> getLoanAccountsByDelinquencyBucket(
            @PathVariable DelinquencyBucket bucket, Pageable pageable) {

        Page<LoanAccount> accounts = accountService.getLoanAccountsByDelinquencyBucket(bucket, pageable);
        return ResponseEntity.ok(accounts.map(LoanAccountMapper::toResponse));
    }

    @GetMapping("/maturing")
    @Operation(summary = "Get loan accounts maturing within a date range")
    public ResponseEntity<Page<LoanAccountResponse>> getLoanAccountsMaturingBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, Pageable pageable) {

        Page<LoanAccount> accounts = accountService.getLoanAccountsMaturingBetween(startDate, endDate, pageable);
        return ResponseEntity.ok(accounts.map(LoanAccountMapper::toResponse));
    }

    @GetMapping("/{id}/outstanding")
    @Operation(summary = "Calculate total outstanding amount for a loan account")
    public ResponseEntity<BigDecimal> calculateTotalOutstanding(@PathVariable UUID id) {
        BigDecimal outstanding = accountService.calculateTotalOutstanding(id);
        return ResponseEntity.ok(outstanding);
    }

    @GetMapping("/customer/{customerId}/exposure")
    @Operation(summary = "Calculate total exposure for a customer across all loans")
    public ResponseEntity<BigDecimal> calculateCustomerTotalExposure(@PathVariable UUID customerId) {
        BigDecimal exposure = accountService.calculateCustomerTotalExposure(customerId);
        return ResponseEntity.ok(exposure);
    }

    @GetMapping("/customer/{customerId}/count/active")
    @Operation(summary = "Count active loan accounts for a customer")
    public ResponseEntity<Long> countActiveLoansByCustomer(@PathVariable UUID customerId) {
        long count = accountService.countActiveLoansByCustomer(customerId);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/{id}/statement")
    @Operation(summary = "Generate loan statement for a specific period")
    public ResponseEntity<LoanStatementResponse> generateLoanStatement(@PathVariable UUID id,
            @Parameter(description = "Start date of the statement period") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fromDate,
            @Parameter(description = "End date of the statement period") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate toDate) {

        LoanStatementResponse statement = accountService.generateLoanStatement(id, fromDate, toDate);

        return ResponseEntity.ok(statement);
    }

    @GetMapping("/{id}/validate-closure")
    @Operation(summary = "Validate if a loan account can be closed")
    public ResponseEntity<ValidationResult> validateForClosure(@PathVariable UUID id) {
        ValidationResult result = accountService.validateForClosure(id);
        return ResponseEntity.ok(result);
    }

    @PostMapping
    @Operation(summary = "Create a new loan account from approved application")
    public ResponseEntity<LoanAccountResponse> createLoanAccount(@Valid @RequestBody LoanAccountCreateRequest request) {

        LoanAccount account = accountService.createLoanAccount(request.getApplicationId(), "TODO_CURRENT_USER");
        return ResponseEntity.status(HttpStatus.CREATED).body(LoanAccountMapper.toResponse(account));
    }

    @PatchMapping("/{id}/status")
    @Operation(summary = "Update loan account status")
    public ResponseEntity<LoanAccountResponse> updateLoanAccountStatus(@PathVariable UUID id,
            @Valid @RequestBody LoanAccountStatusUpdateRequest request) {

        LoanAccount account = accountService
                .updateLoanAccountStatus(id, request.getNewStatus(), request.getReason(), "TODO_CURRENT_USER");
        return ResponseEntity.ok(LoanAccountMapper.toResponse(account));
    }

    @PostMapping("/{id}/disburse")
    @PreAuthorize("hasAuthority('loan:disburse')")
    @Operation(summary = "Initiate loan disbursement")
    public ResponseEntity<LoanAccountResponse> disburseLoan(@PathVariable UUID id,
            @Valid @RequestBody LoanAccountDisburseRequest request) {

        LoanAccount account = accountService.disburseLoan(id, request.getDisbursementDate(), "TODO_CURRENT_USER");
        return ResponseEntity.ok(LoanAccountMapper.toResponse(account));
    }

    @PostMapping("/{id}/close")
    @Operation(summary = "Close a fully-paid loan account")
    public ResponseEntity<LoanAccountResponse> closeLoanAccount(@PathVariable UUID id,
            @Valid @RequestBody LoanAccountCloseRequest request) {

        LoanAccount account = accountService.closeLoanAccount(id, request.getClosureDate(), "TODO_CURRENT_USER");
        return ResponseEntity.ok(LoanAccountMapper.toResponse(account));
    }

    @PostMapping("/{id}/write-off")
    @PreAuthorize("hasAuthority('loan:write-off')")
    @Operation(summary = "Write off a delinquent loan account")
    public ResponseEntity<LoanAccountResponse> writeOffLoan(@PathVariable UUID id,
            @Valid @RequestBody LoanAccountWriteOffRequest request) {

        LoanAccount account = accountService
                .writeOffLoan(id, request.getWriteOffDate(), request.getReason(), "TODO_CURRENT_USER");
        return ResponseEntity.ok(LoanAccountMapper.toResponse(account));
    }

    @PostMapping("/{id}/top-up")
    @Operation(summary = "Create a top-up loan for existing account")
    public ResponseEntity<LoanAccountResponse> createTopUpLoan(@PathVariable UUID id,
            @Valid @RequestBody LoanAccountTopUpRequest request) {

        LoanAccount account = accountService.createTopUpLoan(id, request.getTopUpAmount(), "TODO_CURRENT_USER");
        return ResponseEntity.ok(LoanAccountMapper.toResponse(account));
    }

    @PostMapping("/batch/status-update")
    @Operation(summary = "Batch update status for multiple loan accounts")
    public ResponseEntity<Void> batchUpdateLoanAccountStatus(
            @Valid @RequestBody LoanAccountBatchStatusUpdateRequest request) {

        accountService.batchUpdateLoanAccountStatus(
                request.getLoanAccountIds(),
                request.getNewStatus(),
                request.getReason(),
                "TODO_CURRENT_USER");

        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/balances")
    @Operation(summary = "Update outstanding balances for a loan account")
    public ResponseEntity<LoanAccountResponse> updateOutstandingBalances(@PathVariable UUID id,
            @Valid @RequestBody LoanAccountBalanceUpdateRequest request) {

        accountService.updateOutstandingBalances(
                id,
                request.getPrincipalDelta(),
                request.getInterestDelta(),
                request.getFeesDelta(),
                request.getPenaltiesDelta());

        return accountService.getLoanAccountById(id).map(LoanAccountMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/mark-restructured")
    @PreAuthorize("hasAuthority('loan:restructure')")
    @Operation(summary = "Mark loan account as restructured")
    public ResponseEntity<LoanAccountResponse> markAsRestructured(@PathVariable UUID id,
            @Valid @RequestBody LoanAccountRestructureMarkRequest request) {

        LoanAccount account = accountService.markAsRestructured(id, request.getRestructuredDate());
        return ResponseEntity.ok(LoanAccountMapper.toResponse(account));
    }
}
