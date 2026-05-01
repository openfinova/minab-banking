package com.openfinova.banking.loan.api.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for closing a loan account.
 */
public class LoanAccountCloseRequest {

    @NotNull(message = "Closure date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate closureDate;

    public LocalDate getClosureDate() {
        return closureDate;
    }

    public void setClosureDate(LocalDate closureDate) {
        this.closureDate = closureDate;
    }
}
