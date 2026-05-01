package com.openfinova.banking.loan.controller;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.loan.api.dto.*;
import com.openfinova.banking.loan.api.entity.ScheduleStatus;
import com.openfinova.banking.loan.entity.LoanSchedule;
import com.openfinova.banking.loan.mapper.LoanScheduleMapper;
import com.openfinova.banking.loan.service.LoanScheduleService;
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
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * REST controller for managing loan payment schedules.
 * Provides operations for schedule generation, updates, and inquiries.
 */
@RestController
@RequestMapping("/api/v1/loan-accounts/{loanAccountId}/schedules")
@Tag(name = "Loan Schedules", description = "Loan payment schedule management APIs")
public class LoanScheduleController {

    private final LoanScheduleService scheduleService;

    public LoanScheduleController(LoanScheduleService scheduleService) {
        this.scheduleService = scheduleService;
    }

    @PostMapping("/generate")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Generate amortization schedule for a loan account")
    public ResponseEntity<Void> generateSchedule(@PathVariable UUID loanAccountId, Authentication auth) {

        scheduleService.generateSchedule(loanAccountId, BankingPrincipal.from(auth).username());
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/regenerate")
    @PreAuthorize("hasAuthority('loan:write')")
    @Operation(summary = "Regenerate schedule from effective date")
    public ResponseEntity<Void> regenerateSchedule(@PathVariable UUID loanAccountId,
            @Valid @RequestBody LoanScheduleRegenerateRequest request, Authentication auth) {

        scheduleService
                .regenerateSchedule(loanAccountId, request.getEffectiveDate(), BankingPrincipal.from(auth).username());
        return ResponseEntity.ok().build();
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('loan:read')")
    @Operation(summary = "Get schedule installment by ID")
    public ResponseEntity<LoanScheduleResponse> getScheduleById(@PathVariable UUID loanAccountId,
            @PathVariable UUID id) {

        return scheduleService.getScheduleForLoanAccount(loanAccountId, id).map(LoanScheduleMapper::toResponse)
                .map(ResponseEntity::ok).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping
    @Operation(summary = "Get all schedule installments for a loan account")
    public ResponseEntity<Page<LoanScheduleResponse>> getSchedulesByLoanAccount(@PathVariable UUID loanAccountId,
            Pageable pageable) {

        Page<LoanSchedule> schedules = scheduleService.getSchedulesByLoanAccount(loanAccountId, pageable);
        return ResponseEntity.ok(schedules.map(LoanScheduleMapper::toResponse));
    }

    @GetMapping("/pending")
    @Operation(summary = "Get pending installments for a loan account")
    public ResponseEntity<List<LoanScheduleResponse>> getPendingInstallments(@PathVariable UUID loanAccountId) {

        List<LoanSchedule> schedules = scheduleService.getPendingSchedules(loanAccountId);
        List<LoanScheduleResponse> responses = schedules.stream().map(LoanScheduleMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/overdue")
    @Operation(summary = "Get overdue installments for a loan account")
    public ResponseEntity<List<LoanScheduleResponse>> getOverdueInstallments(@PathVariable UUID loanAccountId) {

        List<LoanSchedule> schedules = scheduleService.getOverdueSchedules(loanAccountId);
        List<LoanScheduleResponse> responses = schedules.stream().map(LoanScheduleMapper::toResponse)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responses);
    }

    @GetMapping("/status/{status}")
    @Operation(summary = "Get installments by status")
    public ResponseEntity<List<LoanScheduleResponse>> getSchedulesByStatus(@PathVariable UUID loanAccountId,
            @PathVariable ScheduleStatus status) {

        List<LoanSchedule> schedules = scheduleService.getSchedulesByStatus(loanAccountId, status);
        List<LoanScheduleResponse> responses = schedules.stream().map(LoanScheduleMapper::toResponse)
                .collect(Collectors.toList());
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/due-between")
    @Operation(summary = "Get installments due between dates")
    public ResponseEntity<Page<LoanScheduleResponse>> getInstallmentsDueBetween(@PathVariable UUID loanAccountId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate, Pageable pageable) {

        Page<LoanSchedule> schedules = scheduleService
                .getSchedulesDueBetweenForLoanAccount(loanAccountId, startDate, endDate, pageable);
        return ResponseEntity.ok(schedules.map(LoanScheduleMapper::toResponse));
    }

    @PutMapping("/{id}/update-payment")
    @PreAuthorize("hasAuthority('loan:collect')")
    @Operation(summary = "Update payment amounts for an installment")
    public ResponseEntity<LoanScheduleResponse> updateSchedulePayment(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanSchedulePaymentUpdateRequest request, Authentication auth) {

        LoanSchedule schedule = scheduleService.updateSchedulePaymentForLoanAccount(
                loanAccountId,
                id,
                request.getPrincipalPaid(),
                request.getInterestPaid(),
                request.getFeesPaid(),
                request.getPenaltiesPaid(),
                BankingPrincipal.from(auth).username());

        return ResponseEntity.ok(LoanScheduleMapper.toResponse(schedule));
    }

    @PostMapping("/{id}/mark-paid")
    @PreAuthorize("hasAuthority('loan:collect')")
    @Operation(summary = "Mark installment as paid")
    public ResponseEntity<LoanScheduleResponse> markInstallmentAsPaid(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanScheduleMarkPaidRequest request, Authentication auth) {

        LoanSchedule schedule = scheduleService.markScheduleAsPaidForLoanAccount(
                loanAccountId,
                id,
                request.getPaidDate(),
                BankingPrincipal.from(auth).username());
        return ResponseEntity.ok(LoanScheduleMapper.toResponse(schedule));
    }

    @PostMapping("/{id}/update-overdue")
    @Operation(summary = "Update overdue status for an installment")
    public ResponseEntity<LoanScheduleResponse> updateOverdueStatus(@PathVariable UUID loanAccountId,
            @PathVariable UUID id, @Valid @RequestBody LoanScheduleOverdueUpdateRequest request) {

        LoanSchedule schedule = scheduleService
                .updateOverdueStatusForLoanAccount(loanAccountId, id, request.getIsOverdue(), request.getDaysPastDue());
        return ResponseEntity.ok(LoanScheduleMapper.toResponse(schedule));
    }

    @GetMapping("/pending/count")
    @Operation(summary = "Count pending installments for a loan account")
    public ResponseEntity<Long> countPendingInstallments(@PathVariable UUID loanAccountId) {
        long count = scheduleService.countSchedulesByStatus(loanAccountId, ScheduleStatus.PENDING);
        return ResponseEntity.ok(count);
    }

    @GetMapping("/overdue/count")
    @Operation(summary = "Count overdue installments for a loan account")
    public ResponseEntity<Long> countOverdueInstallments(@PathVariable UUID loanAccountId) {
        long count = scheduleService.countOverdueSchedules(loanAccountId);
        return ResponseEntity.ok(count);
    }
}
