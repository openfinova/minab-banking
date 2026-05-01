package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Request body for the system initialization endpoints.
 *
 * <p>
 * Controls which currency the standard chart of accounts is denominated in,
 * which fiscal year to bootstrap (12 monthly periods), and which operator
 * identity to stamp on every created record for the audit trail.
 */
@Schema(description = "Request to bootstrap the GL system from a standing start")
public class SystemInitRequest {

    @NotBlank(message = "Currency is required")
    @Size(min = 3, max = 3, message = "Currency must be an ISO 4217 3-letter code")
    @Schema(description = "ISO 4217 base currency code for the standard chart of accounts", example = "USD", requiredMode = Schema.RequiredMode.REQUIRED)
    private String currency;

    @NotNull(message = "Fiscal year is required")
    @Min(value = 1900, message = "Fiscal year must be 1900 or later")
    @Max(value = 2200, message = "Fiscal year must be 2200 or earlier")
    @Schema(description = "Calendar/fiscal year for which 12 monthly periods are created", example = "2026", requiredMode = Schema.RequiredMode.REQUIRED)
    private Integer fiscalYear;

    @NotBlank(message = "createdBy is required")
    @Schema(description = "Username of the operator triggering initialization (audit trail)", example = "admin", requiredMode = Schema.RequiredMode.REQUIRED)
    private String createdBy;

    // Constructors
    public SystemInitRequest() {
    }

    public SystemInitRequest(String currency, Integer fiscalYear, String createdBy) {
        this.currency = currency;
        this.fiscalYear = fiscalYear;
        this.createdBy = createdBy;
    }

    // Getters and Setters
    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
