package com.openfinova.banking.loan.dto;

import java.math.BigDecimal;

/**
 * Affordability metrics for a loan application.
 */
public class AffordabilityAssessment {
    private BigDecimal monthlyIncome;
    private BigDecimal existingObligations;
    private BigDecimal proposedInstallment;
    private BigDecimal debtToIncomeRatio;
    private Boolean affordable;
    private String recommendation;

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getExistingObligations() {
        return existingObligations;
    }

    public void setExistingObligations(BigDecimal existingObligations) {
        this.existingObligations = existingObligations;
    }

    public BigDecimal getProposedInstallment() {
        return proposedInstallment;
    }

    public void setProposedInstallment(BigDecimal proposedInstallment) {
        this.proposedInstallment = proposedInstallment;
    }

    public BigDecimal getDebtToIncomeRatio() {
        return debtToIncomeRatio;
    }

    public void setDebtToIncomeRatio(BigDecimal debtToIncomeRatio) {
        this.debtToIncomeRatio = debtToIncomeRatio;
    }

    public Boolean getAffordable() {
        return affordable;
    }

    public void setAffordable(Boolean affordable) {
        this.affordable = affordable;
    }

    public String getRecommendation() {
        return recommendation;
    }

    public void setRecommendation(String recommendation) {
        this.recommendation = recommendation;
    }
}
