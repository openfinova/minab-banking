package com.openfinova.banking.compliance.entity;

import java.math.BigDecimal;
import java.util.Arrays;
import java.util.UUID;

import com.openfinova.banking.compliance.api.entity.AmlSeverity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "aml_monitoring_rules", uniqueConstraints = @UniqueConstraint(name = "uk_aml_monitoring_rule_code", columnNames = {
        "code" }))
public class MonitoringRule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 64)
    private String code;

    @Column(name = "display_name", nullable = false, length = 160)
    private String displayName;

    @Column(nullable = false)
    private boolean enabled = true;

    /**
     * Inclusive lower bound; null means no lower bound check.
     */
    @Column(name = "threshold_min_inclusive", precision = 21, scale = 6)
    private BigDecimal thresholdMinInclusive;

    /**
     * Inclusive upper bound for band rules (e.g. structuring); null means no upper bound.
     */
    @Column(name = "threshold_max_inclusive", precision = 21, scale = 6)
    private BigDecimal thresholdMaxInclusive;

    /**
     * Comma-separated uppercase transaction type constants, or the literal ALL.
     */
    @Column(name = "match_transaction_types", length = 512)
    private String matchTransactionTypes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private AmlSeverity severity;

    /**
     * When true, escalate with an investigative account hold covering the alerted amount.
     */
    @Column(name = "investigation_hold_recommended", nullable = false)
    private boolean investigationHoldRecommended;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder;

    public boolean matches(BigDecimal amount, String transactionTypeName) {
        if (!enabled) {
            return false;
        }
        if (thresholdMinInclusive != null && amount.compareTo(thresholdMinInclusive) < 0) {
            return false;
        }
        if (thresholdMaxInclusive != null && amount.compareTo(thresholdMaxInclusive) > 0) {
            return false;
        }
        if (matchTransactionTypes == null || matchTransactionTypes.isBlank()) {
            return true;
        }
        String trimmed = matchTransactionTypes.trim();
        if ("ALL".equalsIgnoreCase(trimmed)) {
            return true;
        }
        String normalized = transactionTypeName == null ? "" : transactionTypeName.trim().toUpperCase();
        return Arrays.stream(trimmed.split(",")).map(String::trim).filter(s -> !s.isEmpty()).map(String::toUpperCase)
                .anyMatch(t -> t.equals(normalized));
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean enabled) {
        this.enabled = enabled;
    }

    public BigDecimal getThresholdMinInclusive() {
        return thresholdMinInclusive;
    }

    public void setThresholdMinInclusive(BigDecimal thresholdMinInclusive) {
        this.thresholdMinInclusive = thresholdMinInclusive;
    }

    public BigDecimal getThresholdMaxInclusive() {
        return thresholdMaxInclusive;
    }

    public void setThresholdMaxInclusive(BigDecimal thresholdMaxInclusive) {
        this.thresholdMaxInclusive = thresholdMaxInclusive;
    }

    public String getMatchTransactionTypes() {
        return matchTransactionTypes;
    }

    public void setMatchTransactionTypes(String matchTransactionTypes) {
        this.matchTransactionTypes = matchTransactionTypes;
    }

    public AmlSeverity getSeverity() {
        return severity;
    }

    public void setSeverity(AmlSeverity severity) {
        this.severity = severity;
    }

    public boolean isInvestigationHoldRecommended() {
        return investigationHoldRecommended;
    }

    public void setInvestigationHoldRecommended(boolean investigationHoldRecommended) {
        this.investigationHoldRecommended = investigationHoldRecommended;
    }

    public int getSortOrder() {
        return sortOrder;
    }

    public void setSortOrder(int sortOrder) {
        this.sortOrder = sortOrder;
    }
}
