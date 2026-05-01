package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Entity representing detailed revaluation information for a single account.
 * Links to the parent GLRevaluationRun and stores the specific adjustments made.
 */
@Entity
@Table(name = "gl_revaluation_details", indexes = {
        @Index(name = "idx_gl_revaluation_details_run", columnList = "revaluation_run_id"),
        @Index(name = "idx_gl_revaluation_details_account", columnList = "account_id"),
        @Index(name = "idx_gl_revaluation_details_transaction", columnList = "journal_transaction_id") })
public class GLRevaluationDetail {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "revaluation_run_id", nullable = false)
    @NotNull(message = "Revaluation run is required")
    private GLRevaluationRun revaluationRun;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account is required")
    private GLAccount glAccount;

    @Column(name = "account_currency", length = 3, nullable = false)
    @NotBlank(message = "Account currency is required")
    private String accountCurrency;

    @Column(name = "account_balance", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Account balance is required")
    private BigDecimal accountBalance;

    @Column(name = "old_exchange_rate", precision = 19, scale = 8, nullable = false)
    @NotNull(message = "Old exchange rate is required")
    private BigDecimal oldExchangeRate;

    @Column(name = "new_exchange_rate", precision = 19, scale = 8, nullable = false)
    @NotNull(message = "New exchange rate is required")
    private BigDecimal newExchangeRate;

    @Column(name = "old_base_value", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Old base value is required")
    private BigDecimal oldBaseValue;

    @Column(name = "new_base_value", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "New base value is required")
    private BigDecimal newBaseValue;

    @Column(name = "unrealized_gain_loss", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Unrealized gain/loss is required")
    private BigDecimal unrealizedGainLoss;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_transaction_id")
    private GLTransaction journalTransaction; // Link to the posted revaluation entry

    // Constructors
    public GLRevaluationDetail() {
    }

    public GLRevaluationDetail(GLRevaluationRun revaluationRun, GLAccount glAccount, String accountCurrency,
            BigDecimal accountBalance, BigDecimal oldRate, BigDecimal newRate, BigDecimal oldBaseValue,
            BigDecimal newBaseValue, BigDecimal unrealizedGainLoss) {
        this.revaluationRun = revaluationRun;
        this.glAccount = glAccount;
        this.accountCurrency = accountCurrency;
        this.accountBalance = accountBalance;
        this.oldExchangeRate = oldRate;
        this.newExchangeRate = newRate;
        this.oldBaseValue = oldBaseValue;
        this.newBaseValue = newBaseValue;
        this.unrealizedGainLoss = unrealizedGainLoss;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLRevaluationRun getRevaluationRun() {
        return revaluationRun;
    }

    public void setRevaluationRun(GLRevaluationRun revaluationRun) {
        this.revaluationRun = revaluationRun;
    }

    public GLAccount getGlAccount() {
        return glAccount;
    }

    public void setGlAccount(GLAccount glAccount) {
        this.glAccount = glAccount;
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

    public GLTransaction getJournalTransaction() {
        return journalTransaction;
    }

    public void setJournalTransaction(GLTransaction journalTransaction) {
        this.journalTransaction = journalTransaction;
    }

    @Override
    public String toString() {
        return "GLRevaluationDetail{" + "id=" + id + ", accountId=" + (glAccount != null ? glAccount.getId() : null)
                + ", accountCurrency='" + accountCurrency + '\'' + ", accountBalance=" + accountBalance + ", oldRate="
                + oldExchangeRate + ", newRate=" + newExchangeRate + ", unrealizedGainLoss=" + unrealizedGainLoss + '}';
    }
}
