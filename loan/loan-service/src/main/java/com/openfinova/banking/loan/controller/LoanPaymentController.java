package com.openfinova.banking.loan.controller;

import java.math.BigDecimal;
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
import com.openfinova.banking.loan.api.dto.LoanPaymentAllocationRequest;
import com.openfinova.banking.loan.api.dto.LoanPaymentRequest;
import com.openfinova.banking.loan.api.dto.LoanPaymentResponse;
import com.openfinova.banking.loan.api.dto.PaymentAllocationResponse;
import com.openfinova.banking.loan.api.entity.PaymentMethod;
import com.openfinova.banking.loan.api.entity.PaymentType;
import com.openfinova.banking.loan.dto.PaymentAllocation;
import com.openfinova.banking.loan.entity.LoanPayment;
import com.openfinova.banking.loan.mapper.LoanPaymentMapper;
import com.openfinova.banking.loan.service.LoanPaymentService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST controller for managing loan payments.
 * Provides user-facing operations for recording, viewing, and managing loan payments.
 */
@RestController
@RequestMapping("/api/v1/loan-payments")
@Tag(name = "Loan Payments", description = "Loan payment management APIs")
public class LoanPaymentController {

    private final LoanPaymentService paymentService;

    public LoanPaymentController(LoanPaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping
    @PreAuthorize("hasAnyAuthority('loan:collect', 'loan:write')")
    @Operation(summary = "Record a loan payment with automatic allocation")
    public ResponseEntity<LoanPaymentResponse> recordPayment(@Valid @RequestBody LoanPaymentRequest request,
            Authentication auth) {

        LoanPayment payment = paymentService.recordPayment(
                request.getLoanAccountId(),
                request.getPaymentAmount(),
                request.getPaymentDate(),
                request.getPaymentMethod(),
                request.getTransactionReference(),
                BankingPrincipal.from(auth).username());

        return ResponseEntity.status(HttpStatus.CREATED).body(LoanPaymentMapper.toResponse(payment));
    }

    @PostMapping("/with-allocation")
    @PreAuthorize("hasAnyAuthority('loan:collect', 'loan:write')")
    @Operation(summary = "Record a loan payment with manual allocation")
    public ResponseEntity<LoanPaymentResponse> recordPaymentWithAllocation(
            @Valid @RequestBody LoanPaymentAllocationRequest request, Authentication auth) {

        LoanPayment payment = paymentService.recordPaymentWithAllocation(
                request.getLoanAccountId(),
                request.getPaymentAmount(),
                request.getPrincipalPaid(),
                request.getInterestPaid(),
                request.getFeesPaid(),
                request.getPenaltiesPaid(),
                request.getPaymentDate(),
                request.getPaymentType(),
                request.getPaymentMethod(),
                request.getTransactionReference(),
                BankingPrincipal.from(auth).username());

        return ResponseEntity.status(HttpStatus.CREATED).body(LoanPaymentMapper.toResponse(payment));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get payment by ID")
    public ResponseEntity<LoanPaymentResponse> getPaymentById(@PathVariable UUID id) {
        return paymentService.getPaymentById(id).map(LoanPaymentMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reference/{paymentReference}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get payment by reference number")
    public ResponseEntity<LoanPaymentResponse> getPaymentByReference(@PathVariable String paymentReference) {

        return paymentService.getPaymentByReference(paymentReference).map(LoanPaymentMapper::toResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/loan-account/{loanAccountId}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get all payments for a loan account")
    public ResponseEntity<Page<LoanPaymentResponse>> getPaymentsByLoanAccount(@PathVariable UUID loanAccountId,
            Pageable pageable) {

        Page<LoanPayment> payments = paymentService.getPaymentsByLoanAccount(loanAccountId, pageable);
        return ResponseEntity.ok(payments.map(LoanPaymentMapper::toResponse));
    }

    @GetMapping("/loan-account/{loanAccountId}/date-range")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get payments for a loan account within a date range")
    public ResponseEntity<Page<LoanPaymentResponse>> getPaymentsByDateRange(@PathVariable UUID loanAccountId,
            @Parameter(description = "Start date of the range") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date of the range") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            Pageable pageable) {

        Page<LoanPayment> payments = paymentService
                .getPaymentsByLoanAccountAndDateRange(loanAccountId, startDate, endDate, pageable);
        return ResponseEntity.ok(payments.map(LoanPaymentMapper::toResponse));
    }

    @GetMapping("/type/{paymentType}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get payments by payment type")
    public ResponseEntity<Page<LoanPaymentResponse>> getPaymentsByType(@PathVariable PaymentType paymentType,
            Pageable pageable) {

        Page<LoanPayment> payments = paymentService.getPaymentsByType(paymentType, pageable);
        return ResponseEntity.ok(payments.map(LoanPaymentMapper::toResponse));
    }

    @GetMapping("/method/{paymentMethod}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get payments by payment method")
    public ResponseEntity<Page<LoanPaymentResponse>> getPaymentsByMethod(@PathVariable PaymentMethod paymentMethod,
            Pageable pageable) {

        Page<LoanPayment> payments = paymentService.getPaymentsByMethod(paymentMethod, pageable);
        return ResponseEntity.ok(payments.map(LoanPaymentMapper::toResponse));
    }

    @GetMapping("/reversed")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get all reversed payments")
    public ResponseEntity<Page<LoanPaymentResponse>> getReversedPayments(Pageable pageable) {
        Page<LoanPayment> payments = paymentService.getReversedPayments(pageable);
        return ResponseEntity.ok(payments.map(LoanPaymentMapper::toResponse));
    }

    @PostMapping("/{id}/reverse")
    @PreAuthorize("hasAuthority('loan:collect')")
    @Operation(summary = "Reverse a payment")
    public ResponseEntity<LoanPaymentResponse> reversePayment(@PathVariable UUID id,
            @RequestParam String reversalReason, Authentication auth) {

        LoanPayment payment = paymentService.reversePayment(id, reversalReason, BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(LoanPaymentMapper.toResponse(payment));
    }

    @GetMapping("/loan-account/{loanAccountId}/allocation")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Calculate payment allocation using waterfall method")
    public ResponseEntity<PaymentAllocationResponse> calculatePaymentAllocation(@PathVariable UUID loanAccountId,
            @RequestParam BigDecimal paymentAmount) {

        PaymentAllocation allocation = paymentService.calculatePaymentAllocation(loanAccountId, paymentAmount);

        return ResponseEntity.ok(LoanPaymentMapper.toAllocationResponse(allocation));
    }

    @GetMapping("/loan-account/{loanAccountId}/total-payments")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Calculate total payments for a loan account")
    public ResponseEntity<BigDecimal> calculateTotalPayments(@PathVariable UUID loanAccountId) {
        BigDecimal total = paymentService.calculateTotalPayments(loanAccountId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/loan-account/{loanAccountId}/total-principal-paid")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Calculate total principal paid for a loan account")
    public ResponseEntity<BigDecimal> calculateTotalPrincipalPaid(@PathVariable UUID loanAccountId) {
        BigDecimal total = paymentService.calculateTotalPrincipalPaid(loanAccountId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/loan-account/{loanAccountId}/total-interest-paid")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Calculate total interest paid for a loan account")
    public ResponseEntity<BigDecimal> calculateTotalInterestPaid(@PathVariable UUID loanAccountId) {
        BigDecimal total = paymentService.calculateTotalInterestPaid(loanAccountId);
        return ResponseEntity.ok(total);
    }

    @GetMapping("/loan-account/{loanAccountId}/last-payment")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get the last payment for a loan account")
    public ResponseEntity<LoanPaymentResponse> getLastPayment(@PathVariable UUID loanAccountId) {
        return paymentService.getLastPayment(loanAccountId).map(LoanPaymentMapper::toResponse).map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/loan-account/{loanAccountId}/has-payments")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Check if a loan account has any payments")
    public ResponseEntity<Boolean> hasPayments(@PathVariable UUID loanAccountId) {
        boolean hasPayments = paymentService.hasPayments(loanAccountId);
        return ResponseEntity.ok(hasPayments);
    }

    @GetMapping("/loan-account/{loanAccountId}/count")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Count total payments for a loan account")
    public ResponseEntity<Long> countPayments(@PathVariable UUID loanAccountId) {
        long count = paymentService.countPayments(loanAccountId);
        return ResponseEntity.ok(count);
    }
}
