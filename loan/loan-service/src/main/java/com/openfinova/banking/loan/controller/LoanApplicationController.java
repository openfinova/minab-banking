package com.openfinova.banking.loan.controller;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.loan.api.dto.*;
import com.openfinova.banking.loan.api.entity.ApplicationStatus;
import com.openfinova.banking.loan.entity.LoanApplication;
import com.openfinova.banking.loan.mapper.LoanApplicationMapper;
import com.openfinova.banking.loan.service.LoanApplicationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.UUID;

/**
 * REST controller for managing loan applications.
 * Provides operations for the complete loan application lifecycle.
 */
@RestController
@RequestMapping("/api/v1/loan-applications")
@Tag(name = "Loan Applications", description = "Loan application lifecycle management APIs")
public class LoanApplicationController {

    private final LoanApplicationService applicationService;

    public LoanApplicationController(LoanApplicationService applicationService) {
        this.applicationService = applicationService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Create a new loan application")
    public ResponseEntity<LoanApplicationResponse> createApplication(@Valid @RequestBody LoanApplicationRequest request,
            Authentication auth) {

        LoanApplication application = LoanApplicationMapper.toEntity(request);
        LoanApplication created = applicationService
                .createApplication(application, BankingPrincipal.from(auth).username());

        return ResponseEntity.status(HttpStatus.CREATED).body(LoanApplicationMapper.toResponse(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get loan application by ID")
    public ResponseEntity<LoanApplicationResponse> getApplicationById(@PathVariable UUID id) {
        return applicationService.getApplicationById(id).map(LoanApplicationMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/number/{applicationNumber}")
    @Operation(summary = "Get loan application by number")
    public ResponseEntity<LoanApplicationResponse> getApplicationByNumber(@PathVariable String applicationNumber) {

        return applicationService.getApplicationByNumber(applicationNumber).map(LoanApplicationMapper::toResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/customer/{customerId}")
    @Operation(summary = "Get all loan applications for a customer")
    public ResponseEntity<Page<LoanApplicationResponse>> getApplicationsByCustomer(@PathVariable UUID customerId,
            Pageable pageable) {

        Page<LoanApplication> applications = applicationService.getApplicationsByCustomer(customerId, pageable);
        return ResponseEntity.ok(applications.map(LoanApplicationMapper::toResponse));
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get loan applications by status")
    public ResponseEntity<Page<LoanApplicationResponse>> getApplicationsByStatus(@PathVariable ApplicationStatus status,
            Pageable pageable) {

        Page<LoanApplication> applications = applicationService.getApplicationsByStatus(status, pageable);
        return ResponseEntity.ok(applications.map(LoanApplicationMapper::toResponse));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending loan applications")
    public ResponseEntity<Page<LoanApplicationResponse>> getPendingApplications(Pageable pageable) {
        Page<LoanApplication> applications = applicationService.getPendingApplications(pageable);
        return ResponseEntity.ok(applications.map(LoanApplicationMapper::toResponse));
    }

    @GetMapping("/approved")
    @Operation(summary = "Get approved loan applications")
    public ResponseEntity<Page<LoanApplicationResponse>> getApprovedApplications(Pageable pageable) {
        Page<LoanApplication> applications = applicationService.getApprovedApplications(pageable);
        return ResponseEntity.ok(applications.map(LoanApplicationMapper::toResponse));
    }

    @GetMapping("/rejected")
    @Operation(summary = "Get rejected loan applications")
    public ResponseEntity<Page<LoanApplicationResponse>> getRejectedApplications(Pageable pageable) {
        Page<LoanApplication> applications = applicationService.getRejectedApplications(pageable);
        return ResponseEntity.ok(applications.map(LoanApplicationMapper::toResponse));
    }

    @GetMapping("/approved-between")
    @Operation(summary = "Get applications approved within a date range")
    public ResponseEntity<Page<LoanApplicationResponse>> getApplicationsApprovedBetween(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, Pageable pageable) {

        Page<LoanApplication> applications = applicationService
                .getApplicationsApprovedBetween(startDate, endDate, pageable);
        return ResponseEntity.ok(applications.map(LoanApplicationMapper::toResponse));
    }

    @GetMapping("/requiring-guarantors")
    @Operation(summary = "Get applications requiring guarantors")
    public ResponseEntity<Page<LoanApplicationResponse>> getApplicationsRequiringGuarantors(Pageable pageable) {
        Page<LoanApplication> applications = applicationService.getApplicationsRequiringGuarantors(pageable);
        return ResponseEntity.ok(applications.map(LoanApplicationMapper::toResponse));
    }

    @PostMapping("/{id}/submit")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Submit a loan application")
    public ResponseEntity<LoanApplicationResponse> submitApplication(@PathVariable UUID id, Authentication auth) {
        LoanApplication application = applicationService.submitApplication(id, BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(LoanApplicationMapper.toResponse(application));
    }

    @PostMapping("/{id}/assign-underwriter")
    @PreAuthorize("hasAuthority('loan:approve')")
    @Operation(summary = "Assign an underwriter to a loan application")
    public ResponseEntity<LoanApplicationResponse> assignUnderwriter(@PathVariable UUID id,
            @Valid @RequestBody LoanApplicationAssignUnderwriterRequest request, Authentication auth) {

        LoanApplication application = applicationService
                .assignToUnderwriter(id, request.getUnderwriterId(), BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(LoanApplicationMapper.toResponse(application));
    }

    @PostMapping("/{id}/evaluate-credit-score")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Evaluate credit score for a loan application")
    public ResponseEntity<LoanApplicationResponse> evaluateCreditScore(@PathVariable UUID id) {
        applicationService.performCreditScoring(id);
        return applicationService.getApplicationById(id).map(LoanApplicationMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/assess-risk")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Assess risk for a loan application")
    public ResponseEntity<LoanApplicationResponse> assessRisk(@PathVariable UUID id) {
        applicationService.performRiskAssessment(id);
        return applicationService.getApplicationById(id).map(LoanApplicationMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAuthority('loan:approve')")
    @Operation(summary = "Approve a loan application")
    public ResponseEntity<LoanApplicationResponse> approveApplication(@PathVariable UUID id,
            @Valid @RequestBody LoanApplicationApprovalRequest request, Authentication auth) {

        LoanApplication application = applicationService.approveApplication(
                id,
                request.getApprovedAmount(),
                request.getApprovedTenorMonths(),
                request.getApprovedInterestRate(),
                request.getGuarantorsRequired(),
                BankingPrincipal.from(auth).username());

        return ResponseEntity.ok(LoanApplicationMapper.toResponse(application));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('loan:approve')")
    @Operation(summary = "Reject a loan application")
    public ResponseEntity<LoanApplicationResponse> rejectApplication(@PathVariable UUID id,
            @Valid @RequestBody LoanApplicationRejectionRequest request, Authentication auth) {

        LoanApplication application = applicationService
                .rejectApplication(id, request.getRejectionReason(), BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(LoanApplicationMapper.toResponse(application));
    }

    @PostMapping("/{id}/request-additional-info")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Request additional information from applicant")
    public ResponseEntity<LoanApplicationResponse> requestAdditionalInfo(@PathVariable UUID id,
            @Valid @RequestBody LoanApplicationAdditionalInfoRequest request, Authentication auth) {

        LoanApplication application = applicationService.requestAdditionalInformation(
                id,
                request.getInformationRequired(),
                BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(LoanApplicationMapper.toResponse(application));
    }

    @GetMapping("/{id}/guarantors-required")
    @Operation(summary = "Check if all required guarantors are provided for an application")
    public ResponseEntity<Boolean> areGuarantorsRequired(@PathVariable UUID id) {
        boolean required = applicationService.hasRequiredGuarantors(id);
        return ResponseEntity.ok(required);
    }
}
