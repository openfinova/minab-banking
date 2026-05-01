package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.FiscalPeriodStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for Fiscal Period information exposed to external modules.
 * Contains period dates and status for posting validation.
 */
@Schema(description = "Fiscal period with date range and status")
public class FiscalPeriodDTO {
    @Schema(description = "Unique period identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Period name", example = "2026 Q1")
    private String name;

    @Schema(description = "Period start date", example = "2026-01-01")
    private LocalDate startDate;

    @Schema(description = "Period end date", example = "2026-03-31")
    private LocalDate endDate;

    @Schema(description = "Period status", example = "OPEN")
    private FiscalPeriodStatus status;

    @Schema(description = "Fiscal year this period belongs to", example = "2026")
    private Integer fiscalYear;

    @Schema(description = "Sequential period number within the fiscal year (1-12 monthly, 1-4 quarterly, 13 for adjustments)", example = "1")
    private Integer periodNumber;

    public FiscalPeriodDTO() {
    }

    public FiscalPeriodDTO(UUID id, String name, LocalDate startDate, LocalDate endDate, FiscalPeriodStatus status) {
        this.id = id;
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    public FiscalPeriodDTO(UUID id, String name, int fiscalYear, int periodNumber, LocalDate startDate,
            LocalDate endDate, FiscalPeriodStatus status) {
        this.id = id;
        this.name = name;
        this.fiscalYear = fiscalYear;
        this.periodNumber = periodNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        this.status = status;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

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

    public FiscalPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(FiscalPeriodStatus status) {
        this.status = status;
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
