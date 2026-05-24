package com.openfinova.banking.compliance.api.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.openfinova.banking.compliance.api.entity.AmlAlertStatus;
import com.openfinova.banking.compliance.api.entity.AmlSeverity;

/**
 * API projection for AML alert rows (dashboard / integrations).
 */
public class AmlAlertResponse {

    private UUID id;
    private UUID transactionId;
    private UUID accountId;
    private UUID customerId;
    private String ruleCode;
    private AmlSeverity severity;
    private AmlAlertStatus status;
    private BigDecimal amount;
    private String currency;
    private String detail;
    private boolean investigationHoldPlaced;
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public String getRuleCode() {
        return ruleCode;
    }

    public void setRuleCode(String ruleCode) {
        this.ruleCode = ruleCode;
    }

    public AmlSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AmlSeverity severity) {
        this.severity = severity;
    }

    public AmlAlertStatus getStatus() {
        return status;
    }

    public void setStatus(AmlAlertStatus status) {
        this.status = status;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDetail() {
        return detail;
    }

    public void setDetail(String detail) {
        this.detail = detail;
    }

    public boolean isInvestigationHoldPlaced() {
        return investigationHoldPlaced;
    }

    public void setInvestigationHoldPlaced(boolean investigationHoldPlaced) {
        this.investigationHoldPlaced = investigationHoldPlaced;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
