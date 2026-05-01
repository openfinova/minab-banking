package com.openfinova.banking.gl.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Response DTO for a balance sheet (statement of financial position).
 *
 * <p>
 * Snapshot as of {@code asOfDate}.
 * The accounting equation holds when
 * {@code totalAssets == totalLiabilities + totalEquity}
 * (indicated by the {@code balanced} flag).
 */
public class BalanceSheetResponse {

    private LocalDate asOfDate;

    private List<FinancialStatementLine> assetLines;
    private List<FinancialStatementLine> liabilityLines;
    private List<FinancialStatementLine> equityLines;

    private BigDecimal totalAssets;
    private BigDecimal totalLiabilities;
    private BigDecimal totalEquity;
    /**
     * True when totalAssets == totalLiabilities + totalEquity within rounding
     * tolerance.
     */
    private boolean balanced;

    public BalanceSheetResponse() {
    }

    public BalanceSheetResponse(LocalDate asOfDate, List<FinancialStatementLine> assetLines,
            List<FinancialStatementLine> liabilityLines, List<FinancialStatementLine> equityLines,
            BigDecimal totalAssets, BigDecimal totalLiabilities, BigDecimal totalEquity, boolean balanced) {
        this.asOfDate = asOfDate;
        this.assetLines = assetLines;
        this.liabilityLines = liabilityLines;
        this.equityLines = equityLines;
        this.totalAssets = totalAssets;
        this.totalLiabilities = totalLiabilities;
        this.totalEquity = totalEquity;
        this.balanced = balanced;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public List<FinancialStatementLine> getAssetLines() {
        return assetLines;
    }

    public void setAssetLines(List<FinancialStatementLine> assetLines) {
        this.assetLines = assetLines;
    }

    public List<FinancialStatementLine> getLiabilityLines() {
        return liabilityLines;
    }

    public void setLiabilityLines(List<FinancialStatementLine> liabilityLines) {
        this.liabilityLines = liabilityLines;
    }

    public List<FinancialStatementLine> getEquityLines() {
        return equityLines;
    }

    public void setEquityLines(List<FinancialStatementLine> equityLines) {
        this.equityLines = equityLines;
    }

    public BigDecimal getTotalAssets() {
        return totalAssets;
    }

    public void setTotalAssets(BigDecimal totalAssets) {
        this.totalAssets = totalAssets;
    }

    public BigDecimal getTotalLiabilities() {
        return totalLiabilities;
    }

    public void setTotalLiabilities(BigDecimal totalLiabilities) {
        this.totalLiabilities = totalLiabilities;
    }

    public BigDecimal getTotalEquity() {
        return totalEquity;
    }

    public void setTotalEquity(BigDecimal totalEquity) {
        this.totalEquity = totalEquity;
    }

    public boolean isBalanced() {
        return balanced;
    }

    public void setBalanced(boolean balanced) {
        this.balanced = balanced;
    }
}
