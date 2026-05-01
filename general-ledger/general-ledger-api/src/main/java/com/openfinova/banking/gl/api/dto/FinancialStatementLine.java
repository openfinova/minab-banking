package com.openfinova.banking.gl.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * A single line item in a financial statement (income statement, balance sheet,
 * or cash flow).
 * Represents the contribution of one GL account to a statement section.
 */
public class FinancialStatementLine {

    private UUID accountId;
    private String accountCode;
    private String accountName;
    /** The account's contribution to this section (always a positive magnitude). */
    private BigDecimal amount;

    public FinancialStatementLine() {
    }

    public FinancialStatementLine(UUID accountId, String accountCode, String accountName, BigDecimal amount) {
        this.accountId = accountId;
        this.accountCode = accountCode;
        this.accountName = accountName;
        this.amount = amount;
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }
}
