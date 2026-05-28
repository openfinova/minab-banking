package com.openfinova.banking.loan.controller;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.loan.api.dto.LoanRestructuringApprovalRequest;
import com.openfinova.banking.loan.api.dto.LoanRestructuringProcessRequest;
import com.openfinova.banking.loan.api.dto.LoanRestructuringRejectionRequest;
import com.openfinova.banking.loan.api.dto.LoanRestructuringRequest;
import com.openfinova.banking.loan.api.dto.LoanRestructuringResponse;
import com.openfinova.banking.loan.entity.LoanRestructuring;
import com.openfinova.banking.loan.mapper.LoanRestructuringMapper;
import com.openfinova.banking.loan.service.LoanRestructuringService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for managing loan restructuring.
 * Provides operations for restructuring requests, approvals, and processing.
 */
@RestController
@RequestMapping("/api/v1/loan-accounts/{loanAccountId}/restructurings")
@Tag(name = "Loan Restructuring", description = "Loan restructuring management APIs")
public class LoanRestructuringController {

    private final LoanRestructuringService restructuringService;

    public LoanRestructuringController(LoanRestructuringService restructuringService) {
        this.restructuringService = restructuringService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('loan:restructure')")
    @Operation(summary = "Create a loan restructuring request")
    public ResponseEntity<LoanRestructuringResponse> createRestructuringRequest(@PathVariable UUID loanAccountId,
            @Valid @RequestBody LoanRestructuringRequest request, Authentication auth) {

        LoanRestructuring created = restructuringService.createRestructuringRequest(
                loanAccountId,
                request.getRestructuringType(),
                request.getNewTenorMonths(),
                request.getNewInterestRate(),
                request.getReason(),
                request.getRequestedBy() != null && !request.getRequestedBy().isBlank() ? request.getRequestedBy()
                        : BankingPrincipal.from(auth).username());

        return ResponseEntity.status(HttpStatus.CREATED).body(LoanRestructuringMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get restructuring by ID")
    public ResponseEntity<LoanRestructuringResponse> getRestructuringById(@PathVariable UUID loanAccountId,
            @PathVariable UUID id) {

        return restructuringService.getRestructuringForLoanAccount(loanAccountId, id)
                .map(LoanRestructuringMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all restructuring requests for a loan account")
    public ResponseEntity<java.util.List<LoanRestructuringResponse>> getRestructuringsByLoanAccount(
            @PathVariable UUID loanAccountId) {

        java.util.List<LoanRestructuring> restructurings = restructuringService
                .getRestructuringsByLoanAccount(loanAccountId);
        return ResponseEntity.ok(restructurings.stream().map(LoanRestructuringMapper::toResponse).toList());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('loan:restructure:approve')")
    @Operation(summary = "Approve loan restructuring request")
    public ResponseEntity<LoanRestructuringResponse> approveRestructuring(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanRestructuringApprovalRequest request) {

        LoanRestructuring restructuring = restructuringService
                .approveRestructuring(loanAccountId, id, request.getApprovedBy());
        return ResponseEntity.ok(LoanRestructuringMapper.toResponse(restructuring));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('loan:restructure:approve')")
    @Operation(summary = "Reject loan restructuring request")
    public ResponseEntity<LoanRestructuringResponse> rejectRestructuring(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanRestructuringRejectionRequest request) {

        LoanRestructuring restructuring = restructuringService
                .rejectRestructuring(loanAccountId, id, request.getRejectionReason(), request.getRejectedBy());
        return ResponseEntity.ok(LoanRestructuringMapper.toResponse(restructuring));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('loan:restructure')")
    @Operation(summary = "Process approved loan restructuring")
    public ResponseEntity<LoanRestructuringResponse> processRestructuring(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanRestructuringProcessRequest request) {

        LoanRestructuring restructuring = restructuringService
                .processRestructuring(loanAccountId, id, request.getProcessedBy());
        return ResponseEntity.ok(LoanRestructuringMapper.toResponse(restructuring));
    }

    @GetMapping("/pending/count")
    @Operation(summary = "Count restructuring requests for a loan account")
    public ResponseEntity<Long> countPendingRestructuring(@PathVariable UUID loanAccountId) {
        long count = restructuringService.countRestructuringsByLoanAccount(loanAccountId);
        return ResponseEntity.ok(count);
    }
}
