package com.openfinova.banking.loan.controller;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.loan.api.dto.*;
import com.openfinova.banking.loan.api.entity.SettlementStatus;
import com.openfinova.banking.loan.entity.EarlySettlement;
import com.openfinova.banking.loan.mapper.EarlySettlementMapper;
import com.openfinova.banking.loan.service.EarlySettlementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST controller for managing early loan settlements.
 * Provides operations for settlement quotes, approvals, and processing.
 */
@RestController
@RequestMapping("/api/v1/loan-accounts/{loanAccountId}/settlements")
@Tag(name = "Loan Settlements", description = "Early settlement management APIs")
public class EarlySettlementController {

    private final EarlySettlementService settlementService;

    public EarlySettlementController(EarlySettlementService settlementService) {
        this.settlementService = settlementService;
    }

    @PostMapping("/quote")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Generate early settlement quote")
    public ResponseEntity<EarlySettlementResponse> generateSettlementQuote(@PathVariable UUID loanAccountId,
            @Valid @RequestBody EarlySettlementQuoteRequest request, Authentication auth) {

        EarlySettlement settlement = settlementService.generateSettlementQuote(
                loanAccountId,
                request.getSettlementDate(),
                request.getCalculationMethod(),
                BankingPrincipal.from(auth).username());

        return ResponseEntity.status(HttpStatus.CREATED).body(EarlySettlementMapper.toResponse(settlement));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get settlement by ID")
    public ResponseEntity<EarlySettlementResponse> getSettlementById(@PathVariable UUID loanAccountId,
            @PathVariable UUID id) {

        return settlementService.getEarlySettlementForLoanAccount(loanAccountId, id)
                .map(EarlySettlementMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get all settlements for a loan account")
    public ResponseEntity<List<EarlySettlementResponse>> getSettlementsByLoanAccount(@PathVariable UUID loanAccountId) {

        List<EarlySettlement> settlements = settlementService.getEarlySettlementsByLoanAccount(loanAccountId);
        return ResponseEntity.ok(settlements.stream().map(EarlySettlementMapper::toResponse).toList());
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get settlements by status")
    public ResponseEntity<Page<EarlySettlementResponse>> getSettlementsByStatus(@PathVariable UUID loanAccountId,
            @PathVariable SettlementStatus status, Pageable pageable) {

        Page<EarlySettlement> settlements = settlementService
                .getEarlySettlementsByLoanAccountAndStatus(loanAccountId, status, pageable);
        return ResponseEntity.ok(settlements.map(EarlySettlementMapper::toResponse));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('loan:approve')")
    @Operation(summary = "Approve early settlement request")
    public ResponseEntity<EarlySettlementResponse> approveSettlement(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody EarlySettlementApprovalRequest request) {

        EarlySettlement settlement = settlementService.approveSettlement(loanAccountId, id, request.getApprovedBy());
        return ResponseEntity.ok(EarlySettlementMapper.toResponse(settlement));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('loan:approve')")
    @Operation(summary = "Reject early settlement request")
    public ResponseEntity<EarlySettlementResponse> rejectSettlement(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody EarlySettlementRejectionRequest request) {

        EarlySettlement settlement = settlementService
                .rejectSettlement(loanAccountId, id, request.getRejectionReason(), request.getRejectedBy());
        return ResponseEntity.ok(EarlySettlementMapper.toResponse(settlement));
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Cancel early settlement request")
    public ResponseEntity<EarlySettlementResponse> cancelSettlement(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody EarlySettlementCancellationRequest request) {

        EarlySettlement settlement = settlementService
                .cancelSettlement(loanAccountId, id, request.getCancellationReason(), request.getCancelledBy());
        return ResponseEntity.ok(EarlySettlementMapper.toResponse(settlement));
    }

    @PostMapping("/{id}/process")
    @PreAuthorize("hasAuthority('loan:collect')")
    @Operation(summary = "Process early settlement payment")
    public ResponseEntity<EarlySettlementResponse> processSettlement(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody EarlySettlementProcessRequest request) {

        EarlySettlement settlement = settlementService
                .processSettlement(loanAccountId, id, request.getPaymentDate(), request.getProcessedBy());
        return ResponseEntity.ok(EarlySettlementMapper.toResponse(settlement));
    }

    @GetMapping("/pending/count")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Count pending settlement requests for this loan account")
    public ResponseEntity<Long> countPendingSettlements(@PathVariable UUID loanAccountId) {
        return ResponseEntity.ok(settlementService.countPendingSettlementsForLoanAccount(loanAccountId));
    }
}
