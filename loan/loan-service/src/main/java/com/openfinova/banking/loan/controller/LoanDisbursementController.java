package com.openfinova.banking.loan.controller;

import java.time.LocalDate;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.loan.api.dto.LoanDisbursementCancellationRequest;
import com.openfinova.banking.loan.api.dto.LoanDisbursementCompletionRequest;
import com.openfinova.banking.loan.api.dto.LoanDisbursementFailureRequest;
import com.openfinova.banking.loan.api.dto.LoanDisbursementProcessRequest;
import com.openfinova.banking.loan.api.dto.LoanDisbursementRequest;
import com.openfinova.banking.loan.api.dto.LoanDisbursementResponse;
import com.openfinova.banking.loan.api.entity.DisbursementStatus;
import com.openfinova.banking.loan.entity.LoanDisbursement;
import com.openfinova.banking.loan.mapper.LoanDisbursementMapper;
import com.openfinova.banking.loan.service.LoanDisbursementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for managing loan disbursements.
 * Provides operations for fund disbursement processing and tracking.
 */
@RestController
@RequestMapping("/api/v1/loan-accounts/{loanAccountId}/disbursements")
@Tag(name = "Loan Disbursements", description = "Loan disbursement management APIs")
public class LoanDisbursementController {

    private final LoanDisbursementService disbursementService;

    public LoanDisbursementController(LoanDisbursementService disbursementService) {
        this.disbursementService = disbursementService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('loan:disburse')")
    @Operation(summary = "Create a new loan disbursement")
    public ResponseEntity<LoanDisbursementResponse> createDisbursement(@PathVariable UUID loanAccountId,
            @Valid @RequestBody LoanDisbursementRequest request, Authentication auth) {

        LoanDisbursement created = disbursementService.createDisbursement(
                loanAccountId,
                request.getDisbursementAmount(),
                request.getDisbursementDate(),
                request.getDisbursementMethod(),
                request.getDestinationAccountNumber(),
                request.getCreatedBy() != null && !request.getCreatedBy().isBlank() ? request.getCreatedBy()
                        : BankingPrincipal.from(auth).username());

        return ResponseEntity.status(HttpStatus.CREATED).body(LoanDisbursementMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get disbursement by ID")
    public ResponseEntity<LoanDisbursementResponse> getDisbursementById(@PathVariable UUID loanAccountId,
            @PathVariable UUID id) {

        return disbursementService.getDisbursementForLoanAccount(loanAccountId, id)
                .map(LoanDisbursementMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{reference}")
    @Operation(summary = "Get disbursement by reference number")
    public ResponseEntity<LoanDisbursementResponse> getDisbursementByReference(@PathVariable UUID loanAccountId,
            @PathVariable String reference) {

        return disbursementService.getDisbursementByReference(reference).map(LoanDisbursementMapper::toResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all disbursements for a loan account")
    public ResponseEntity<java.util.List<LoanDisbursementResponse>> getDisbursementsByLoanAccount(
            @PathVariable UUID loanAccountId) {

        java.util.List<LoanDisbursement> disbursements = disbursementService
                .getDisbursementsByLoanAccount(loanAccountId);
        return ResponseEntity.ok(disbursements.stream().map(LoanDisbursementMapper::toResponse).toList());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get disbursements by status")
    public ResponseEntity<Page<LoanDisbursementResponse>> getDisbursementsByStatus(@PathVariable UUID loanAccountId,
            @PathVariable DisbursementStatus status, Pageable pageable) {

        Page<LoanDisbursement> disbursements = disbursementService
                .getDisbursementsByLoanAccountAndStatus(loanAccountId, status, pageable);
        return ResponseEntity.ok(disbursements.map(LoanDisbursementMapper::toResponse));
    }

    @GetMapping("/by-date-range")
    @Operation(summary = "Get disbursements between dates")
    public ResponseEntity<Page<LoanDisbursementResponse>> getDisbursementsByDateRange(@PathVariable UUID loanAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, Pageable pageable) {

        Page<LoanDisbursement> disbursements = disbursementService
                .getDisbursementsByLoanAccountAndDateRange(loanAccountId, startDate, endDate, pageable);
        return ResponseEntity.ok(disbursements.map(LoanDisbursementMapper::toResponse));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('loan:disburse:approve')")
    @Operation(summary = "Process a pending disbursement")
    public ResponseEntity<LoanDisbursementResponse> processDisbursement(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanDisbursementProcessRequest request) {

        LoanDisbursement disbursement = disbursementService
                .processDisbursementForLoanAccount(loanAccountId, id, request.getProcessedBy());
        return ResponseEntity.ok(LoanDisbursementMapper.toResponse(disbursement));
    }

    @PostMapping("/{id}/complete")
    @PreAuthorize("hasAuthority('loan:disburse:approve')")
    @Operation(summary = "Mark disbursement as completed")
    public ResponseEntity<LoanDisbursementResponse> completeDisbursement(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanDisbursementCompletionRequest request) {

        LoanDisbursement disbursement = disbursementService
                .completeDisbursementForLoanAccount(loanAccountId, id, request.getCompletedBy());
        return ResponseEntity.ok(LoanDisbursementMapper.toResponse(disbursement));
    }

    @PostMapping("/{id}/fail")
    @PreAuthorize("hasAuthority('loan:disburse')")
    @Operation(summary = "Mark disbursement as failed")
    public ResponseEntity<LoanDisbursementResponse> failDisbursement(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanDisbursementFailureRequest request) {

        LoanDisbursement disbursement = disbursementService
                .failDisbursementForLoanAccount(loanAccountId, id, request.getFailureReason(), request.getFailedBy());
        return ResponseEntity.ok(LoanDisbursementMapper.toResponse(disbursement));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('loan:disburse')")
    @Operation(summary = "Cancel a pending disbursement")
    public ResponseEntity<LoanDisbursementResponse> cancelDisbursement(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanDisbursementCancellationRequest request) {

        LoanDisbursement disbursement = disbursementService.cancelDisbursementForLoanAccount(
                loanAccountId,
                id,
                request.getCancellationReason(),
                request.getCancelledBy());
        return ResponseEntity.ok(LoanDisbursementMapper.toResponse(disbursement));
    }

    @GetMapping("/pending/count")
    @Operation(summary = "Count pending disbursements for this loan account")
    public ResponseEntity<Long> countPendingDisbursements(@PathVariable UUID loanAccountId) {
        return ResponseEntity.ok(disbursementService.countPendingDisbursementsForLoanAccount(loanAccountId));
    }
}
