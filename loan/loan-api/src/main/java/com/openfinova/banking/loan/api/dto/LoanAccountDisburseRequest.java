package com.openfinova.banking.loan.api.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for disbursing a loan account.
 */
public class LoanAccountDisburseRequest {

    @NotNull(message = "Disbursement date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate disbursementDate;

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }
}
