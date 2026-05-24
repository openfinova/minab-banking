package com.openfinova.banking.compliance.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import com.openfinova.banking.compliance.api.entity.AmlAlertStatus;
import com.openfinova.banking.compliance.api.entity.AmlSeverity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "aml_alerts", uniqueConstraints = @UniqueConstraint(name = "uk_aml_alert_tx_rule", columnNames = {
        "transaction_id", "rule_code" }), indexes = {
                @Index(name = "idx_aml_alert_created_at", columnList = "created_at"),
                @Index(name = "idx_aml_alert_account", columnList = "source_account_id") })
public class AmlAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "transaction_id", nullable = false)
    private UUID transactionId;

    @Column(name = "source_account_id", nullable = false)
    private UUID sourceAccountId;

    @Column(name = "customer_party_id")
    private UUID customerPartyId;

    @Column(name = "rule_code", nullable = false, length = 64)
    private String ruleCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AmlSeverity severity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AmlAlertStatus status = AmlAlertStatus.OPEN;

    @Column(name = "monitored_amount", nullable = false, precision = 21, scale = 6)
    private BigDecimal monitoredAmount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(name = "detail_summary", nullable = false, length = 2000)
    private String detailSummary;

    @Column(name = "transaction_type_name", nullable = false, length = 64)
    private String transactionTypeName;

    @Column(name = "investigation_hold_placed", nullable = false)
    private boolean investigationHoldPlaced;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt = Instant.now();

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

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public void setSourceAccountId(UUID sourceAccountId) {
        this.sourceAccountId = sourceAccountId;
    }

    public UUID getCustomerPartyId() {
        return customerPartyId;
    }

    public void setCustomerPartyId(UUID customerPartyId) {
        this.customerPartyId = customerPartyId;
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

    public BigDecimal getMonitoredAmount() {
        return monitoredAmount;
    }

    public void setMonitoredAmount(BigDecimal monitoredAmount) {
        this.monitoredAmount = monitoredAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDetailSummary() {
        return detailSummary;
    }

    public void setDetailSummary(String detailSummary) {
        this.detailSummary = detailSummary;
    }

    public String getTransactionTypeName() {
        return transactionTypeName;
    }

    public void setTransactionTypeName(String transactionTypeName) {
        this.transactionTypeName = transactionTypeName;
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
