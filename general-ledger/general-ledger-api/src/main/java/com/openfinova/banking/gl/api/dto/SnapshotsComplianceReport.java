package com.openfinova.banking.gl.api.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Compliance report for regulatory requirements.
 */
public class SnapshotsComplianceReport {
    private LocalDate startDate;
    private LocalDate endDate;
    private boolean compliant;
    private List<String> complianceIssues;
    private int totalTransactions;
    private int gaplessSequenceViolations;
    private int dataIntegrityIssues;
    private long generatedAt;

    public SnapshotsComplianceReport() {
    }

    // Getters and setters
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

    public boolean isCompliant() {
        return compliant;
    }

    public void setCompliant(boolean compliant) {
        this.compliant = compliant;
    }

    public List<String> getComplianceIssues() {
        return complianceIssues;
    }

    public void setComplianceIssues(List<String> complianceIssues) {
        this.complianceIssues = complianceIssues;
    }

    public int getTotalTransactions() {
        return totalTransactions;
    }

    public void setTotalTransactions(int totalTransactions) {
        this.totalTransactions = totalTransactions;
    }

    public int getGaplessSequenceViolations() {
        return gaplessSequenceViolations;
    }

    public void setGaplessSequenceViolations(int gaplessSequenceViolations) {
        this.gaplessSequenceViolations = gaplessSequenceViolations;
    }

    public int getDataIntegrityIssues() {
        return dataIntegrityIssues;
    }

    public void setDataIntegrityIssues(int dataIntegrityIssues) {
        this.dataIntegrityIssues = dataIntegrityIssues;
    }

    public long getGeneratedAt() {
        return generatedAt;
    }

    public void setGeneratedAt(long generatedAt) {
        this.generatedAt = generatedAt;
    }
}
