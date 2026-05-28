package com.openfinova.banking.loan.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.loan.api.dto.CollateralLiquidationRequest;
import com.openfinova.banking.loan.api.dto.CollateralRequest;
import com.openfinova.banking.loan.api.dto.CollateralResponse;
import com.openfinova.banking.loan.api.dto.CollateralStatusUpdateRequest;
import com.openfinova.banking.loan.api.dto.CollateralValuationRequest;
import com.openfinova.banking.loan.api.entity.CollateralStatus;
import com.openfinova.banking.loan.entity.Collateral;
import com.openfinova.banking.loan.mapper.CollateralMapper;
import com.openfinova.banking.loan.service.CollateralService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for managing loan collateral.
 * Provides operations for collateral registration, valuation, and release.
 */
@RestController
@RequestMapping("/api/v1/loan-accounts/{loanAccountId}/collateral")
@Tag(name = "Loan Collateral", description = "Loan collateral management APIs")
public class CollateralController {

    private final CollateralService collateralService;

    public CollateralController(CollateralService collateralService) {
        this.collateralService = collateralService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Register collateral for a loan account")
    public ResponseEntity<CollateralResponse> registerCollateral(@PathVariable UUID loanAccountId,
            @Valid @RequestBody CollateralRequest request, Authentication auth) {

        Collateral collateral = new Collateral();
        collateral.setCollateralType(request.getCollateralType());
        collateral.setDescription(request.getDescription());
        collateral.setValuationAmount(request.getValuationAmount());
        collateral.setCurrency(request.getCurrency());
        collateral.setValuationDate(request.getValuationDate());
        collateral.setLocation(request.getLocation());
        collateral.setInsuranceExpiryDate(request.getInsuranceExpiryDate());
        collateral.setInsurancePolicyNumber(request.getInsurancePolicyNumber());

        Collateral created = collateralService
                .registerCollateral(loanAccountId, collateral, BankingPrincipal.from(auth).username());

        return ResponseEntity.status(HttpStatus.CREATED).body(CollateralMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get collateral by ID")
    public ResponseEntity<CollateralResponse> getCollateralById(@PathVariable UUID loanAccountId,
            @PathVariable UUID id) {

        return collateralService.getCollateralForLoanAccount(loanAccountId, id).map(CollateralMapper::toResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get all collateral for a loan account")
    public ResponseEntity<List<CollateralResponse>> getCollateralByLoanAccount(@PathVariable UUID loanAccountId) {

        List<Collateral> collaterals = collateralService.getCollateralByLoanAccount(loanAccountId);
        List<CollateralResponse> responses = collaterals.stream().map(CollateralMapper::toResponse).toList();
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get collateral by status")
    public ResponseEntity<Page<CollateralResponse>> getCollateralByStatus(@PathVariable UUID loanAccountId,
            @PathVariable CollateralStatus status, Pageable pageable) {

        Page<Collateral> collaterals = collateralService.getCollateralByStatus(status, pageable);
        return ResponseEntity.ok(collaterals.map(CollateralMapper::toResponse));
    }

    @PutMapping("/{id}/valuation")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Update collateral valuation")
    public ResponseEntity<CollateralResponse> updateValuation(@PathVariable UUID loanAccountId, @PathVariable UUID id,
            @Valid @RequestBody CollateralValuationRequest request, Authentication auth) {

        Collateral collateral = collateralService.updateValuation(
                loanAccountId,
                id,
                request.getValuationAmount(),
                request.getValuationDate(),
                BankingPrincipal.from(auth).username());

        return ResponseEntity.ok(CollateralMapper.toResponse(collateral));
    }

    @PostMapping("/{id}/status")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Update collateral status")
    public ResponseEntity<CollateralResponse> updateCollateralStatus(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody CollateralStatusUpdateRequest request, Authentication auth) {

        Collateral collateral = collateralService.updateCollateralStatus(
                loanAccountId,
                id,
                request.getNewStatus(),
                BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(CollateralMapper.toResponse(collateral));
    }

    @PostMapping("/{id}/release")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Release collateral from a loan")
    public ResponseEntity<CollateralResponse> releaseCollateral(@PathVariable UUID loanAccountId, @PathVariable UUID id,
            Authentication auth) {

        Collateral collateral = collateralService
                .releaseCollateral(loanAccountId, id, BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(CollateralMapper.toResponse(collateral));
    }

    @PostMapping("/{id}/liquidate")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Mark collateral as liquidated")
    public ResponseEntity<CollateralResponse> liquidateCollateral(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody CollateralLiquidationRequest request, Authentication auth) {

        Collateral collateral = collateralService.liquidateCollateral(
                loanAccountId,
                id,
                request.getLiquidationAmount(),
                BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(CollateralMapper.toResponse(collateral));
    }

    @GetMapping("/active/count")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Count active collateral items for a loan account")
    public ResponseEntity<Long> countActiveCollateral(@PathVariable UUID loanAccountId) {
        long count = collateralService.countCollateralByLoanAccount(loanAccountId);
        return ResponseEntity.ok(count);
    }
}
