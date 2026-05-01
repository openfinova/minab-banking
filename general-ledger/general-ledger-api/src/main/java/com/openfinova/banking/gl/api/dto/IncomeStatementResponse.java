package com.openfinova.banking.gl.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for an income statement (profit and loss statement).
 *
 * <p>
 * Covers a reporting period defined by {@code startDate} and {@code endDate}.
 * Revenue lines represent income earned; expense lines represent costs
 * incurred.
 * {@code netIncome} = totalRevenue − totalExpenses (negative = net loss).
 */
public class IncomeStatementResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    private List<FinancialStatementLine> revenueLines;
    private List<FinancialStatementLine> expenseLines;

    private BigDecimal totalRevenue;
    private BigDecimal totalExpenses;
    /**
     * Net income = totalRevenue - totalExpenses. Negative value indicates a net
     * loss.
     */
    private BigDecimal netIncome;

    public IncomeStatementResponse() {
    }

    public IncomeStatementResponse(LocalDate startDate, LocalDate endDate, List<FinancialStatementLine> revenueLines,
            List<FinancialStatementLine> expenseLines, BigDecimal totalRevenue, BigDecimal totalExpenses,
            BigDecimal netIncome) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.revenueLines = revenueLines;
        this.expenseLines = expenseLines;
        this.totalRevenue = totalRevenue;
        this.totalExpenses = totalExpenses;
        this.netIncome = netIncome;
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

    public List<FinancialStatementLine> getRevenueLines() {
        return revenueLines;
    }

    public void setRevenueLines(List<FinancialStatementLine> revenueLines) {
        this.revenueLines = revenueLines;
    }

    public List<FinancialStatementLine> getExpenseLines() {
        return expenseLines;
    }

    public void setExpenseLines(List<FinancialStatementLine> expenseLines) {
        this.expenseLines = expenseLines;
    }

    public BigDecimal getTotalRevenue() {
        return totalRevenue;
    }

    public void setTotalRevenue(BigDecimal totalRevenue) {
        this.totalRevenue = totalRevenue;
    }

    public BigDecimal getTotalExpenses() {
        return totalExpenses;
    }

    public void setTotalExpenses(BigDecimal totalExpenses) {
        this.totalExpenses = totalExpenses;
    }

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public void setNetIncome(BigDecimal netIncome) {
        this.netIncome = netIncome;
    }
}
