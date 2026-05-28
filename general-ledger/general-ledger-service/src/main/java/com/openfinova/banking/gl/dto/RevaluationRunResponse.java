package com.openfinova.banking.gl.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public class RevaluationRunResponse {

    private UUID id;
    private LocalDate revaluationDate;
    private LocalDateTime executedAt;
    private String executedBy;
    private Integer accountsProcessed;
    private Integer accountsRevalued;
    private Integer accountsFailed;
    private BigDecimal totalAdjustment;
    private String baseCurrency;
    private String triggerType;
    private UUID correlationId;
    private String notes;

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

    public Integer getAccountsFailed() {
        return accountsFailed;
    }

    public void setAccountsFailed(Integer accountsFailed) {
        this.accountsFailed = accountsFailed;
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
}
