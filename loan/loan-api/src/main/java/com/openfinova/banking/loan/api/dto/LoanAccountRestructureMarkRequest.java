package com.openfinova.banking.loan.api.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for marking a loan as restructured.
 */
public class LoanAccountRestructureMarkRequest {

    @NotNull(message = "Restructured date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate restructuredDate;

    public LocalDate getRestructuredDate() {
        return restructuredDate;
    }

    public void setRestructuredDate(LocalDate restructuredDate) {
        this.restructuredDate = restructuredDate;
    }
}
