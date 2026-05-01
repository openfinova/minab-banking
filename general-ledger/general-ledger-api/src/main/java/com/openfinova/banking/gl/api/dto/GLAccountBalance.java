package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Data Transfer Object representing a summary of account balance and activity.
 */
@Schema(description = "Account balance summary with transaction activity")
public class GLAccountBalance {
    @Schema(description = "Account identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID accountId;

    @Schema(description = "Account code", example = "1000")
    private String accountCode;

    @Schema(description = "Account name", example = "Cash in Bank - USD")
    private String accountName;

    @Schema(description = "Current account balance", example = "50000.00")
    private BigDecimal currentBalance;

    @Schema(description = "Opening balance for the period", example = "45000.00")
    private BigDecimal openingBalance;

    @Schema(description = "Closing balance for the period", example = "50000.00")
    private BigDecimal closingBalance;

    @Schema(description = "Total debit amount in the period", example = "10000.00")
    private BigDecimal totalDebits;

    @Schema(description = "Total credit amount in the period", example = "5000.00")
    private BigDecimal totalCredits;

    @Schema(description = "Number of transactions in the period", example = "25")
    private int transactionCount;

    public GLAccountBalance() {
    }

    public GLAccountBalance(UUID accountId, String accountCode, String accountName, BigDecimal currentBalance,
            BigDecimal openingBalance, BigDecimal closingBalance, BigDecimal totalDebits, BigDecimal totalCredits,
            int transactionCount) {
        this.accountId = accountId;
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.currentBalance = currentBalance;
        this.openingBalance = openingBalance;
        this.closingBalance = closingBalance;
        this.totalDebits = totalDebits;
        this.totalCredits = totalCredits;
        this.transactionCount = transactionCount;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
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

    public int getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(int transactionCount) {
        this.transactionCount = transactionCount;
    }

    public BigDecimal getBalance() {
        return currentBalance;
    }

    public void setBalance(BigDecimal balance) {
        this.currentBalance = balance;
    }

    public BigDecimal getDebitAmount() {
        return totalDebits;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.totalDebits = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return totalCredits;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.totalCredits = creditAmount;
    }

}
