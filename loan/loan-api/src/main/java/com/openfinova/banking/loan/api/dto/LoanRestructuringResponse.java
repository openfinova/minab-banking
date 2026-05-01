package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.RestructuringType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for loan restructuring records.
 */
public class LoanRestructuringResponse {

    private UUID id;
    private UUID loanAccountId;
    private LocalDate restructuringDate;
    private RestructuringType restructuringType;
    private BigDecimal oldPrincipalBalance;
    private BigDecimal newPrincipalBalance;
    private BigDecimal oldInterestRate;
    private BigDecimal newInterestRate;
    private Integer oldTenorMonths;
    private Integer newTenorMonths;
    private String reason;
    private String approvedBy;
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public LocalDate getRestructuringDate() {
        return restructuringDate;
    }

    public void setRestructuringDate(LocalDate restructuringDate) {
        this.restructuringDate = restructuringDate;
    }

    public RestructuringType getRestructuringType() {
        return restructuringType;
    }

    public void setRestructuringType(RestructuringType restructuringType) {
        this.restructuringType = restructuringType;
    }

    public BigDecimal getOldPrincipalBalance() {
        return oldPrincipalBalance;
    }

    public void setOldPrincipalBalance(BigDecimal oldPrincipalBalance) {
        this.oldPrincipalBalance = oldPrincipalBalance;
    }

    public BigDecimal getNewPrincipalBalance() {
        return newPrincipalBalance;
    }

    public void setNewPrincipalBalance(BigDecimal newPrincipalBalance) {
        this.newPrincipalBalance = newPrincipalBalance;
    }

    public BigDecimal getOldInterestRate() {
        return oldInterestRate;
    }

    public void setOldInterestRate(BigDecimal oldInterestRate) {
        this.oldInterestRate = oldInterestRate;
    }

    public BigDecimal getNewInterestRate() {
        return newInterestRate;
    }

    public void setNewInterestRate(BigDecimal newInterestRate) {
        this.newInterestRate = newInterestRate;
    }

    public Integer getOldTenorMonths() {
        return oldTenorMonths;
    }

    public void setOldTenorMonths(Integer oldTenorMonths) {
        this.oldTenorMonths = oldTenorMonths;
    }

    public Integer getNewTenorMonths() {
        return newTenorMonths;
    }

    public void setNewTenorMonths(Integer newTenorMonths) {
        this.newTenorMonths = newTenorMonths;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
