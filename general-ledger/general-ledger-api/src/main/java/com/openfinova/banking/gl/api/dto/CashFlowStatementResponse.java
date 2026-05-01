package com.openfinova.banking.gl.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for a simplified statement of cash flows (indirect method).
 *
 * <p>
 * Covers a reporting period defined by {@code startDate} and {@code endDate}.
 *
 * <ul>
 * <li><b>Operating activities</b> – Net income plus changes in working capital
 * (current asset and current liability account movements).</li>
 * <li><b>Investing activities</b> – Net changes in long-term ASSET accounts
 * during the period (negative = outflow; positive = inflow from
 * disposals).</li>
 * <li><b>Financing activities</b> – Net changes in LIABILITY and EQUITY
 * accounts
 * (positive = new borrowings/equity raised; negative =
 * repayments/distributions).</li>
 * </ul>
 *
 * {@code netCashChange} = totalOperating + totalInvesting + totalFinancing.
 */
public class CashFlowStatementResponse {

    private LocalDate startDate;
    private LocalDate endDate;

    /**
     * Net income (loss) for the period, as the starting point of operating
     * activities.
     */
    private BigDecimal netIncome;

    private List<FinancialStatementLine> operatingActivities;
    private List<FinancialStatementLine> investingActivities;
    private List<FinancialStatementLine> financingActivities;

    private BigDecimal totalOperating;
    private BigDecimal totalInvesting;
    private BigDecimal totalFinancing;
    /** Net change in cash = totalOperating + totalInvesting + totalFinancing. */
    private BigDecimal netCashChange;

    public CashFlowStatementResponse() {
    }

    public CashFlowStatementResponse(LocalDate startDate, LocalDate endDate, BigDecimal netIncome,
            List<FinancialStatementLine> operatingActivities, List<FinancialStatementLine> investingActivities,
            List<FinancialStatementLine> financingActivities, BigDecimal totalOperating, BigDecimal totalInvesting,
            BigDecimal totalFinancing, BigDecimal netCashChange) {
        this.startDate = startDate;
        this.endDate = endDate;
        this.netIncome = netIncome;
        this.operatingActivities = operatingActivities;
        this.investingActivities = investingActivities;
        this.financingActivities = financingActivities;
        this.totalOperating = totalOperating;
        this.totalInvesting = totalInvesting;
        this.totalFinancing = totalFinancing;
        this.netCashChange = netCashChange;
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

    public BigDecimal getNetIncome() {
        return netIncome;
    }

    public void setNetIncome(BigDecimal netIncome) {
        this.netIncome = netIncome;
    }

    public List<FinancialStatementLine> getOperatingActivities() {
        return operatingActivities;
    }

    public void setOperatingActivities(List<FinancialStatementLine> operatingActivities) {
        this.operatingActivities = operatingActivities;
    }

    public List<FinancialStatementLine> getInvestingActivities() {
        return investingActivities;
    }

    public void setInvestingActivities(List<FinancialStatementLine> investingActivities) {
        this.investingActivities = investingActivities;
    }

    public List<FinancialStatementLine> getFinancingActivities() {
        return financingActivities;
    }

    public void setFinancingActivities(List<FinancialStatementLine> financingActivities) {
        this.financingActivities = financingActivities;
    }

    public BigDecimal getTotalOperating() {
        return totalOperating;
    }

    public void setTotalOperating(BigDecimal totalOperating) {
        this.totalOperating = totalOperating;
    }

    public BigDecimal getTotalInvesting() {
        return totalInvesting;
    }

    public void setTotalInvesting(BigDecimal totalInvesting) {
        this.totalInvesting = totalInvesting;
    }

    public BigDecimal getTotalFinancing() {
        return totalFinancing;
    }

    public void setTotalFinancing(BigDecimal totalFinancing) {
        this.totalFinancing = totalFinancing;
    }

    public BigDecimal getNetCashChange() {
        return netCashChange;
    }

    public void setNetCashChange(BigDecimal netCashChange) {
        this.netCashChange = netCashChange;
    }
}
