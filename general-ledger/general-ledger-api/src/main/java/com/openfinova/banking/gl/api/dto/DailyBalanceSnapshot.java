package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Data Transfer Object representing a daily balance snapshot.
 */
@Schema(description = "Daily balance snapshot for an account")
public class DailyBalanceSnapshot {
    @Schema(description = "Account identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID accountId;

    @Schema(description = "Balance date", example = "2026-02-14")
    private LocalDate date;

    @Schema(description = "Opening balance for the day", example = "45000.00")
    private BigDecimal openingBalance;

    @Schema(description = "Closing balance for the day", example = "50000.00")
    private BigDecimal closingBalance;

    @Schema(description = "Total debit transactions for the day", example = "10000.00")
    private BigDecimal totalDebits;

    @Schema(description = "Total credit transactions for the day", example = "5000.00")
    private BigDecimal totalCredits;

    @Schema(description = "Number of transactions for the day", example = "15")
    private int transactionCount;

    public DailyBalanceSnapshot() {
    }

    public DailyBalanceSnapshot(UUID accountId, LocalDate date, BigDecimal openingBalance, BigDecimal closingBalance,
            BigDecimal totalDebits, BigDecimal totalCredits, int transactionCount) {
        this.accountId = accountId;
        this.date = date;
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

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public void setBalanceDate(LocalDate balanceDate) {
        this.date = balanceDate;
    }

    public LocalDate getBalanceDate() {
        return date;
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
