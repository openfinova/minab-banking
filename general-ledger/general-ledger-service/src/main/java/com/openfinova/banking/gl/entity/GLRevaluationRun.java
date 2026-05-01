package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a currency revaluation run.
 * Tracks when revaluation was executed, by whom, and summary statistics.
 * This provides an audit trail for all revaluation operations.
 */
@Entity
@Table(name = "gl_revaluation_runs", indexes = {
        @Index(name = "idx_gl_revaluation_runs_date", columnList = "revaluation_date"),
        @Index(name = "idx_gl_revaluation_runs_executed", columnList = "executed_at") })
public class GLRevaluationRun {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "revaluation_date", nullable = false)
    @NotNull(message = "Revaluation date is required")
    private LocalDate revaluationDate;

    @CreationTimestamp
    @Column(name = "executed_at", nullable = false, updatable = false)
    private LocalDateTime executedAt;

    @Column(name = "executed_by", length = 100, nullable = false)
    @NotBlank(message = "Executed by is required")
    private String executedBy;

    @Column(name = "accounts_processed", nullable = false)
    @Min(value = 0, message = "Accounts processed must be non-negative")
    @NotNull(message = "Accounts processed is required")
    private Integer accountsProcessed = 0;

    @Column(name = "accounts_revalued", nullable = false)
    @Min(value = 0, message = "Accounts revalued must be non-negative")
    @NotNull(message = "Accounts revalued is required")
    private Integer accountsRevalued = 0;

    @Column(name = "accounts_failed", nullable = false)
    @Min(value = 0, message = "Accounts failed must be non-negative")
    @NotNull(message = "Accounts failed is required")
    private Integer accountsFailed = 0;

    @Column(name = "total_adjustment", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Total adjustment is required")
    private BigDecimal totalAdjustment = BigDecimal.ZERO;

    @Column(name = "base_currency", length = 3, nullable = false)
    @NotBlank(message = "Base currency is required")
    private String baseCurrency;

    @Column(name = "trigger_type", length = 20, nullable = false)
    @NotBlank(message = "Trigger type is required")
    private String triggerType; // MANUAL, PERIOD_CLOSE, SCHEDULED

    @Column(name = "correlation_id")
    private UUID correlationId; // For linking to period close or other process

    @Column(name = "notes", length = 500)
    private String notes;

    // Constructors
    public GLRevaluationRun() {
    }

    public GLRevaluationRun(LocalDate revaluationDate, String executedBy, String baseCurrency, String triggerType) {
        this.revaluationDate = revaluationDate;
        this.executedBy = executedBy;
        this.baseCurrency = baseCurrency;
        this.triggerType = triggerType;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getRevaluationDate() {
        return revaluationDate;
    }

    public void setRevaluationDate(LocalDate revaluationDate) {
        this.revaluationDate = revaluationDate;
    }

    public LocalDateTime getExecutedAt() {
        return executedAt;
    }

    public void setExecutedAt(LocalDateTime executedAt) {
        this.executedAt = executedAt;
    }

    public String getExecutedBy() {
        return executedBy;
    }

    public void setExecutedBy(String executedBy) {
        this.executedBy = executedBy;
    }

    public Integer getAccountsProcessed() {
        return accountsProcessed;
    }

    public void setAccountsProcessed(Integer accountsProcessed) {
        this.accountsProcessed = accountsProcessed;
    }

    public Integer getAccountsRevalued() {
        return accountsRevalued;
    }

    public void setAccountsRevalued(Integer accountsRevalued) {
        this.accountsRevalued = accountsRevalued;
    }

    public BigDecimal getTotalAdjustment() {
        return totalAdjustment;
    }

    public void setTotalAdjustment(BigDecimal totalAdjustment) {
        this.totalAdjustment = totalAdjustment;
    }

    public String getBaseCurrency() {
        return baseCurrency;
    }

    public void setBaseCurrency(String baseCurrency) {
        this.baseCurrency = baseCurrency;
    }

    public String getTriggerType() {
        return triggerType;
    }

    public void setTriggerType(String triggerType) {
        this.triggerType = triggerType;
    }

    public UUID getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(UUID correlationId) {
        this.correlationId = correlationId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void incrementAccountsProcessed() {
        this.accountsProcessed++;
    }

    public void incrementAccountsRevalued() {
        this.accountsRevalued++;
    }

    public Integer getAccountsFailed() {
        return accountsFailed;
    }

    public void setAccountsFailed(Integer accountsFailed) {
        this.accountsFailed = accountsFailed;
    }

    public void incrementAccountsFailed() {
        this.accountsFailed++;
    }

    public void addToTotalAdjustment(BigDecimal adjustment) {
        this.totalAdjustment = this.totalAdjustment.add(adjustment);
    }

    @Override
    public String toString() {
        return "GLRevaluationRun{" + "id=" + id + ", revaluationDate=" + revaluationDate + ", executedAt=" + executedAt
                + ", executedBy='" + executedBy + '\'' + ", accountsProcessed=" + accountsProcessed
                + ", accountsRevalued=" + accountsRevalued + ", accountsFailed=" + accountsFailed + ", totalAdjustment="
                + totalAdjustment + ", baseCurrency='" + baseCurrency + '\'' + ", triggerType='" + triggerType + '\''
                + '}';
    }
}
