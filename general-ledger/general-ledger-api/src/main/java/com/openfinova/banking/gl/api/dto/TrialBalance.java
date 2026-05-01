package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Data Transfer Object representing a Trial Balance report.
 * A Trial Balance is a fundamental accounting report that lists all general
 * ledger accounts
 * and their respective debit or credit balances at a specific point in time. It
 * serves as
 * a crucial tool for ensuring the accounting equation (Assets = Liabilities +
 * Equity) remains
 * balanced and helps identify potential errors in the double-entry bookkeeping
 * system.
 *
 * Key characteristics of a Trial Balance:
 * - Shows account balances as of a specific date (asOfDate)
 * - Contains all GL accounts with non-zero balances
 * - Separates accounts into debit and credit columns based on their normal
 * balance
 * - Total debits must equal total credits for the books to be in balance
 * - Used as a preliminary step before preparing financial statements
 *
 * This DTO encapsulates:
 * - asOfDate: The specific date for which the trial balance is prepared
 * - accountBalances: List of all GL account balances included in the report
 * - totalDebits: Sum of all debit balances
 * - totalCredits: Sum of all credit balances
 * - isBalanced: Boolean flag indicating whether total debits equal total
 * credits
 *
 * The trial balance is essential for:
 * - Detecting mathematical errors in journal entries
 * - Preparing adjusting entries
 * - Creating financial statements (Income Statement, Balance Sheet)
 * - Monthly/quarterly closing procedures
 * - Audit trail and compliance requirements
 */
@Schema(description = "Trial Balance report showing all account balances to verify accounting equation")
public class TrialBalance {
    @Schema(description = "Report date", example = "2026-02-14")
    private LocalDate asOfDate;

    @Schema(description = "List of account balances included in the trial balance")
    private List<GLAccountBalance> accountBalances;

    @Schema(description = "Sum of all debit balances", example = "500000.00")
    private BigDecimal totalDebits;

    @Schema(description = "Sum of all credit balances", example = "500000.00")
    private BigDecimal totalCredits;

    @Schema(description = "Whether total debits equal total credits", example = "true")
    private boolean isBalanced;

    public TrialBalance() {
    }

    public TrialBalance(LocalDate asOfDate, List<GLAccountBalance> accountBalances, BigDecimal totalDebits,
            BigDecimal totalCredits, boolean isBalanced) {
        this.asOfDate = asOfDate;
        this.accountBalances = accountBalances;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.isBalanced = isBalanced;
    }

    public LocalDate getAsOfDate() {
        return asOfDate;
    }

    public void setAsOfDate(LocalDate asOfDate) {
        this.asOfDate = asOfDate;
    }

    public List<GLAccountBalance> getAccountBalances() {
        return accountBalances;
    }

    public void setAccountBalances(List<GLAccountBalance> accountBalances) {
        this.accountBalances = accountBalances;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits;
    }

    public boolean isBalanced() {
        return isBalanced;
    }

    public void setBalanced(boolean isBalanced) {
        this.isBalanced = isBalanced;
    }

    public boolean getIsBalanced() {
        return isBalanced;
    }

    public void setIsBalanced(boolean isBalanced) {
        this.isBalanced = isBalanced;
    }

}
