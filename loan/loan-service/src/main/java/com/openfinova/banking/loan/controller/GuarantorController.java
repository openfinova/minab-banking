package com.openfinova.banking.loan.controller;

import com.openfinova.banking.loan.api.dto.*;
import com.openfinova.banking.loan.api.entity.GuarantorStatus;
import com.openfinova.banking.loan.entity.Guarantor;
import com.openfinova.banking.loan.mapper.GuarantorMapper;
import com.openfinova.banking.loan.service.GuarantorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

/**
 * REST controller for managing loan guarantors.
 * Provides operations for guarantor addition, verification, and removal.
 */
@RestController
@RequestMapping("/api/v1/loan-accounts/{loanAccountId}/guarantors")
@Tag(name = "Loan Guarantors", description = "Loan guarantor management APIs")
public class GuarantorController {

    private final GuarantorService guarantorService;

    public GuarantorController(GuarantorService guarantorService) {
        this.guarantorService = guarantorService;
    }

    @PostMapping
    @Operation(summary = "Add a guarantor to a loan account")
    public ResponseEntity<GuarantorResponse> addGuarantor(@PathVariable UUID loanAccountId,
            @Valid @RequestBody GuarantorRequest request) {

        Guarantor guarantor = new Guarantor();
        guarantor.setCustomerId(request.getCustomerId());
        guarantor.setGuarantorType(request.getGuarantorType());
        guarantor.setGuaranteedAmount(request.getGuaranteedAmount());
        guarantor.setRemarks(request.getRemarks());

        Guarantor created = guarantorService.addGuarantor(
                loanAccountId,
                guarantor,
                request.getAddedBy() != null && !request.getAddedBy().isBlank() ? request.getAddedBy()
                        : "TODO_CURRENT_USER");

        return ResponseEntity.status(HttpStatus.CREATED).body(GuarantorMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get guarantor by ID")
    public ResponseEntity<GuarantorResponse> getGuarantorById(@PathVariable UUID loanAccountId, @PathVariable UUID id) {

        return guarantorService.getGuarantorForLoanAccount(loanAccountId, id).map(GuarantorMapper::toResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all guarantors for a loan account")
    public ResponseEntity<java.util.List<GuarantorResponse>> getGuarantorsByLoanAccount(
            @PathVariable UUID loanAccountId) {

        java.util.List<Guarantor> guarantors = guarantorService.getGuarantorsByLoanAccount(loanAccountId);
        return ResponseEntity.ok(guarantors.stream().map(GuarantorMapper::toResponse).toList());
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get guarantors by status")
    public ResponseEntity<Page<GuarantorResponse>> getGuarantorsByStatus(@PathVariable UUID loanAccountId,
            @PathVariable GuarantorStatus status, Pageable pageable) {

        Page<Guarantor> guarantors = guarantorService
                .getGuarantorsByLoanAccountAndStatus(loanAccountId, status, pageable);
        return ResponseEntity.ok(guarantors.map(GuarantorMapper::toResponse));
    }

    @PostMapping("/{id}/verify")
    @Operation(summary = "Verify a guarantor")
    public ResponseEntity<GuarantorResponse> verifyGuarantor(@PathVariable UUID loanAccountId, @PathVariable UUID id,
            @Valid @RequestBody GuarantorVerificationRequest request) {

        Guarantor guarantor = guarantorService.verifyGuarantor(loanAccountId, id, "TODO_CURRENT_USER");
        return ResponseEntity.ok(GuarantorMapper.toResponse(guarantor));
    }

    @PostMapping("/{id}/status")
    @Operation(summary = "Update guarantor status")
    public ResponseEntity<GuarantorResponse> updateGuarantorStatus(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody GuarantorStatusUpdateRequest request) {

        Guarantor guarantor = guarantorService
                .updateGuarantorStatus(loanAccountId, id, request.getNewStatus(), request.getUpdatedBy());
        return ResponseEntity.ok(GuarantorMapper.toResponse(guarantor));
    }

    @PostMapping("/{id}/release")
    @Operation(summary = "Release a guarantor from loan")
    public ResponseEntity<GuarantorResponse> releaseGuarantor(@PathVariable UUID loanAccountId, @PathVariable UUID id,
            @Valid @RequestBody GuarantorReleaseRequest request) {

        Guarantor guarantor = guarantorService.releaseGuarantor(loanAccountId, id, request.getReleasedBy());
        return ResponseEntity.ok(GuarantorMapper.toResponse(guarantor));
    }

    @PostMapping("/{id}/remove")
    @Operation(summary = "Remove a guarantor from loan")
    public ResponseEntity<Void> removeGuarantor(@PathVariable UUID loanAccountId, @PathVariable UUID id,
            @Valid @RequestBody GuarantorRemovalRequest request) {

        guarantorService.removeGuarantor(loanAccountId, id, request.getRemovalReason(), request.getRemovedBy());
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/active/count")
    @Operation(summary = "Count active guarantors for a loan account")
    public ResponseEntity<Long> countActiveGuarantors(@PathVariable UUID loanAccountId) {
        long count = guarantorService.getActiveGuarantorsByLoanAccount(loanAccountId).size();
        return ResponseEntity.ok(count);
    }
}
