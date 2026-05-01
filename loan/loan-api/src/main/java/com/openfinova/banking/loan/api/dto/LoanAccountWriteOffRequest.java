package com.openfinova.banking.loan.api.dto;

import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for writing off a loan account.
 */
public class LoanAccountWriteOffRequest {

    @NotNull(message = "Write-off date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate writeOffDate;

    @NotBlank(message = "Reason is required")
    private String reason;

    public LocalDate getWriteOffDate() {
        return writeOffDate;
    }

    public void setWriteOffDate(LocalDate writeOffDate) {
        this.writeOffDate = writeOffDate;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
