package com.openfinova.banking.customer.account.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;

/**
 * Filter criteria for self-service transaction search across a user's accounts.
 */
public class AccountTransactionSearchCriteria {

    private LocalDateTime fromDate;
    private LocalDateTime toDate;
    private UUID accountId;
    private AccountTransactionType transactionType;
    private String status;
    private String search;

    public LocalDateTime getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDateTime fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDateTime getToDate() {
        return toDate;
    }

    public void setToDate(LocalDateTime toDate) {
        this.toDate = toDate;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public AccountTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(AccountTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getSearch() {
        return search;
    }

    public void setSearch(String search) {
        this.search = search;
    }
}
