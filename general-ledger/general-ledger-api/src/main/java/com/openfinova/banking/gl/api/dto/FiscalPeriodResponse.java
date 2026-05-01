package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.FiscalPeriodStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

@Schema(description = "Fiscal Period response")
public class FiscalPeriodResponse {

    @Schema(description = "Period unique identifier")
    private UUID id;

    @Schema(description = "Period name")
    private String name;

    @Schema(description = "Period start date")
    private LocalDate startDate;

    @Schema(description = "Period end date")
    private LocalDate endDate;

    @Schema(description = "Period status")
    private FiscalPeriodStatus status;

    @Schema(description = "User who closed the period")
    private String closedBy;

    @Schema(description = "Period close timestamp")
    private Instant closedAt;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Fiscal year this period belongs to", example = "2026")
    private Integer fiscalYear;

    @Schema(description = "Sequential period number within the fiscal year (1-12 monthly, 1-4 quarterly, 13 for adjustments)", example = "1")
    private Integer periodNumber;

    // Constructors
    public FiscalPeriodResponse() {
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

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public Instant getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(Instant closedAt) {
        this.closedAt = closedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
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
