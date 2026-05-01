package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * Request DTO for creating (opening) a new fiscal period.
 * Only the fields that callers are allowed to supply are exposed here.
 * Server-managed fields ({@code id}, {@code status}, {@code createdAt}, etc.)
 * are never accepted from the caller and are set by the service.
 */
@Schema(description = "Request to open a new fiscal period")
public class CreateFiscalPeriodRequest {

    @NotBlank(message = "Period name is required")
    @Size(max = 50, message = "Period name must not exceed 50 characters")
    @Schema(description = "Human-readable period name", example = "February 2026")
    private String name;

    @NotNull(message = "Start date is required")
    @Schema(description = "First day of the period (inclusive)", example = "2026-02-01")
    private LocalDate startDate;

    @NotNull(message = "End date is required")
    @Schema(description = "Last day of the period (inclusive)", example = "2026-02-28")
    private LocalDate endDate;

    @NotNull(message = "Fiscal year is required")
    @Min(value = 1900, message = "Fiscal year must be 1900 or later")
    @Max(value = 2200, message = "Fiscal year must be 2200 or earlier")
    @Schema(description = "Calendar/fiscal year this period belongs to", example = "2026")
    private Integer fiscalYear;

    @NotNull(message = "Period number is required")
    @Min(value = 1, message = "Period number must be at least 1")
    @Max(value = 13, message = "Period number must not exceed 13 (13 = year-end adjustments)")
    @Schema(description = "Sequential period number within the fiscal year. Monthly: 1-12, Quarterly: 1-4, Period 13 reserved for year-end adjustment entries.", example = "2")
    private Integer periodNumber;

    // Constructors
    public CreateFiscalPeriodRequest() {
    }

    public CreateFiscalPeriodRequest(String name, LocalDate startDate, LocalDate endDate, Integer fiscalYear,
            Integer periodNumber) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.fiscalYear = fiscalYear;
        this.periodNumber = periodNumber;
    }

    // Getters and Setters
    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
    }

    public Integer getFiscalYear() {
        return fiscalYear;
    }

    public void setFiscalYear(Integer fiscalYear) {
        this.fiscalYear = fiscalYear;
    }

    public Integer getPeriodNumber() {
        return periodNumber;
    }

    public void setPeriodNumber(Integer periodNumber) {
        this.periodNumber = periodNumber;
    }
}
