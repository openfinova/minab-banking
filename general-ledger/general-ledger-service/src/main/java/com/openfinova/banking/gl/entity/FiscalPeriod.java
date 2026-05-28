package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import com.openfinova.banking.gl.api.entity.FiscalPeriodStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a value fiscal/accounting period (e.g. Month OR Quarter).
 * Used to control posting permissions for back-dated or future-dated
 * transactions.
 */
@Entity
@Table(name = "fiscal_periods", indexes = {
        @Index(name = "idx_fiscal_period_dates", columnList = "start_date, end_date"),
        @Index(name = "idx_fiscal_period_status", columnList = "status"),
        @Index(name = "idx_fiscal_period_year", columnList = "fiscal_year"),
        @Index(name = "idx_fiscal_period_year_num", columnList = "fiscal_year, period_number") }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_fiscal_period_year_number", columnNames = { "fiscal_year",
                        "period_number" }) })
public class FiscalPeriod {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(unique = true, nullable = false, length = 50)
    @NotBlank(message = "Period name is required")
    @Size(max = 50, message = "Period name must not exceed 50 characters")
    private String name;

    @Column(name = "start_date", nullable = false)
    @NotNull(message = "Start date is required")
    private LocalDate startDate;

    @Column(name = "end_date", nullable = false)
    @NotNull(message = "End date is required")
    private LocalDate endDate;

    /**
     * The calendar year this period belongs to (e.g. 2024).
     * Enables direct queries like "all periods in fiscal year 2024" without
     * scanning dates, and is required for regulatory reporting extracts.
     */
    @Column(name = "fiscal_year", nullable = false)
    @NotNull(message = "Fiscal year is required")
    @Min(value = 1900, message = "Fiscal year must be 1900 or later")
    @Max(value = 2200, message = "Fiscal year must be 2200 or earlier")
    private Integer fiscalYear;

    /**
     * Sequential number of this period within its fiscal year.
     * <ul>
     *   <li>Monthly calendars: 1–12</li>
     *   <li>Quarterly calendars: 1–4</li>
     *   <li>Period 13 is reserved for year-end adjustment entries</li>
     * </ul>
     * Combined with {@code fiscalYear} this forms the natural, human-readable
     * key used in regulatory reports and audit trails.
     */
    @Column(name = "period_number", nullable = false)
    @NotNull(message = "Period number is required")
    @Min(value = 1, message = "Period number must be at least 1")
    @Max(value = 13, message = "Period number must not exceed 13")
    private Integer periodNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private FiscalPeriodStatus status = FiscalPeriodStatus.OPEN;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "closed_at")
    private LocalDateTime closedAt;

    @Column(name = "closed_by")
    private String closedBy;

    @Column(name = "reopened_at")
    private LocalDateTime reopenedAt;

    @Column(name = "reopened_by", length = 100)
    private String reopenedBy;

    // Constructors
    public FiscalPeriod() {
    }

    public FiscalPeriod(String name, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.startDate = startDate;
        this.endDate = endDate;
        validateDates();
    }

    public FiscalPeriod(String name, int fiscalYear, int periodNumber, LocalDate startDate, LocalDate endDate) {
        this.name = name;
        this.fiscalYear = fiscalYear;
        this.periodNumber = periodNumber;
        this.startDate = startDate;
        this.endDate = endDate;
        validateDates();
    }

    private void validateDates() {
        if (startDate != null && endDate != null && startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date cannot be after end date");
        }
    }

    public boolean isOpen() {
        return FiscalPeriodStatus.OPEN.equals(status);
    }

    public boolean isClosed() {
        return FiscalPeriodStatus.CLOSED.equals(status) || FiscalPeriodStatus.LOCKED.equals(status);
    }

    public void close(String userId, LocalDateTime closedAt) {
        this.status = FiscalPeriodStatus.CLOSED;
        this.closedAt = closedAt;
        this.closedBy = userId;
    }

    public void lock() {
        this.status = FiscalPeriodStatus.LOCKED;
    }

    /**
     * Reopens a closed fiscal period, recording the authorising user.
     * Mirrors {@link #close(String)} — the actor is persisted on the entity,
     * not just in the audit log.
     *
     * @param userId the user authorising the reopen (must not be blank)
     */
    public void reopen(String userId, LocalDateTime reopenedAt) {
        this.status = FiscalPeriodStatus.OPEN;
        this.closedAt = null;
        this.closedBy = null;
        this.reopenedAt = reopenedAt;
        this.reopenedBy = userId;
    }

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
        validateDates();
    }

    public LocalDate getEndDate() {
        return endDate;
    }

    public void setEndDate(LocalDate endDate) {
        this.endDate = endDate;
        validateDates();
    }

    public FiscalPeriodStatus getStatus() {
        return status;
    }

    public void setStatus(FiscalPeriodStatus status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosedBy() {
        return closedBy;
    }

    public void setClosedBy(String closedBy) {
        this.closedBy = closedBy;
    }

    public LocalDateTime getReopenedAt() {
        return reopenedAt;
    }

    public void setReopenedAt(LocalDateTime reopenedAt) {
        this.reopenedAt = reopenedAt;
    }

    public String getReopenedBy() {
        return reopenedBy;
    }

    public void setReopenedBy(String reopenedBy) {
        this.reopenedBy = reopenedBy;
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
