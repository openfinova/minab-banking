package com.openfinova.banking.customer.account.api.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for account statement with transactions and summary.
 */
public class AccountStatement {

    private UUID accountId;
    private String accountNumber;
    private LocalDate fromDate;
    private LocalDate toDate;
    private List<Object> transactions;
    private StatementSummary summary;

    public AccountStatement() {
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public List<Object> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<Object> transactions) {
        this.transactions = transactions;
    }

    public StatementSummary getSummary() {
        return summary;
    }

    public void setSummary(StatementSummary summary) {
        this.summary = summary;
    }
}
