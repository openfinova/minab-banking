package com.openfinova.banking.gl.dto;

import java.math.BigDecimal;
import java.util.UUID;

public class RevaluationDetailResponse {

    private UUID id;
    private UUID revaluationRunId;
    private UUID accountId;
    private String accountCurrency;
    private BigDecimal accountBalance;
    private BigDecimal oldExchangeRate;
    private BigDecimal newExchangeRate;
    private BigDecimal oldBaseValue;
    private BigDecimal newBaseValue;
    private BigDecimal unrealizedGainLoss;
    private UUID journalTransactionId;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getRevaluationRunId() {
        return revaluationRunId;
    }

    public void setRevaluationRunId(UUID revaluationRunId) {
        this.revaluationRunId = revaluationRunId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountCurrency() {
        return accountCurrency;
    }

    public void setAccountCurrency(String accountCurrency) {
        this.accountCurrency = accountCurrency;
    }

    public BigDecimal getAccountBalance() {
        return accountBalance;
    }

    public void setAccountBalance(BigDecimal accountBalance) {
        this.accountBalance = accountBalance;
    }

    public BigDecimal getOldExchangeRate() {
        return oldExchangeRate;
    }

    public void setOldExchangeRate(BigDecimal oldExchangeRate) {
        this.oldExchangeRate = oldExchangeRate;
    }

    public BigDecimal getNewExchangeRate() {
        return newExchangeRate;
    }

    public void setNewExchangeRate(BigDecimal newExchangeRate) {
        this.newExchangeRate = newExchangeRate;
    }

    public BigDecimal getOldBaseValue() {
        return oldBaseValue;
    }

    public void setOldBaseValue(BigDecimal oldBaseValue) {
        this.oldBaseValue = oldBaseValue;
    }

    public BigDecimal getNewBaseValue() {
        return newBaseValue;
    }

    public void setNewBaseValue(BigDecimal newBaseValue) {
        this.newBaseValue = newBaseValue;
    }

    public BigDecimal getUnrealizedGainLoss() {
        return unrealizedGainLoss;
    }

    public void setUnrealizedGainLoss(BigDecimal unrealizedGainLoss) {
        this.unrealizedGainLoss = unrealizedGainLoss;
    }

    public UUID getJournalTransactionId() {
        return journalTransactionId;
    }

    public void setJournalTransactionId(UUID journalTransactionId) {
        this.journalTransactionId = journalTransactionId;
    }
}
