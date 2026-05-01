package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import com.openfinova.banking.gl.api.entity.BalanceType;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DailyBalance entity for performance optimization of balance calculations.
 * Stores daily snapshots of account balances to avoid recalculating from all
 * historical transactions.
 */
@Entity
@Table(name = "gl_daily_balances", uniqueConstraints = @UniqueConstraint(name = "uk_gl_daily_balances_account_date", columnNames = {
        "account_id", "balance_date" }), indexes = {
                @Index(name = "idx_gl_daily_balances_account_date", columnList = "account_id, balance_date"),
                @Index(name = "idx_gl_daily_balances_date", columnList = "balance_date"),
                @Index(name = "idx_gl_daily_balances_account", columnList = "account_id") })
public class GLDailyBalance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account is required")
    private GLAccount glAccount;

    @Column(name = "balance_date", nullable = false)
    @NotNull(message = "Balance date is required")
    private LocalDate balanceDate;

    @Column(name = "opening_balance", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Opening balance is required")
    private BigDecimal openingBalance = BigDecimal.ZERO;

    @Column(name = "closing_balance", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Closing balance is required")
    private BigDecimal closingBalance = BigDecimal.ZERO;

    @Column(name = "total_debits", precision = 19, scale = 4, nullable = false)
    @DecimalMin(value = "0.0", message = "Total debits must be non-negative")
    @NotNull(message = "Total debits is required")
    private BigDecimal totalDebits = BigDecimal.ZERO;

    @Column(name = "total_credits", precision = 19, scale = 4, nullable = false)
    @DecimalMin(value = "0.0", message = "Total credits must be non-negative")
    @NotNull(message = "Total credits is required")
    private BigDecimal totalCredits = BigDecimal.ZERO;

    @Column(name = "transaction_count", nullable = false)
    @Min(value = 0, message = "Transaction count must be non-negative")
    @NotNull(message = "Transaction count is required")
    private Integer transactionCount = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public GLDailyBalance() {
    }

    public GLDailyBalance(GLAccount account, LocalDate balanceDate) {
        this.glAccount = account;
        this.balanceDate = balanceDate;
    }

    public GLDailyBalance(GLAccount account, LocalDate balanceDate, BigDecimal openingBalance,
            BigDecimal closingBalance, BigDecimal totalDebits, BigDecimal totalCredits, Integer transactionCount) {
        this.glAccount = account;
        this.balanceDate = balanceDate;
        this.openingBalance = openingBalance != null ? openingBalance : BigDecimal.ZERO;
        this.closingBalance = closingBalance != null ? closingBalance : BigDecimal.ZERO;
        this.totalDebits = totalDebits != null ? totalDebits : BigDecimal.ZERO;
        this.totalCredits = totalCredits != null ? totalCredits : BigDecimal.ZERO;
        this.transactionCount = transactionCount != null ? transactionCount : 0;
    }

    /**
     * Calculates the net change for the day (closing - opening balance)
     *
     * @return the net change amount
     */
    public BigDecimal getNetChange() {
        return closingBalance.subtract(openingBalance);
    }

    /**
     * Calculates the net activity for the day based on account type normal balance
     * For debit-normal accounts: debits - credits
     * For credit-normal accounts: credits - debits
     *
     * @return the net activity amount
     */
    public BigDecimal getNetActivity() {
        if (glAccount != null && glAccount.getNormalBalance() == BalanceType.DEBIT) {
            return totalDebits.subtract(totalCredits);
        } else {
            return totalCredits.subtract(totalDebits);
        }
    }

    /**
     * Validates that the closing balance equals opening balance plus net activity
     *
     * @return true if the balance is consistent, false otherwise
     */
    public boolean isBalanceConsistent() {
        BigDecimal expectedClosing = openingBalance.add(getNetActivity());
        return closingBalance.compareTo(expectedClosing) == 0;
    }

    /**
     * Checks if this daily balance has any activity (transactions)
     *
     * @return true if there were transactions on this date, false otherwise
     */
    public boolean hasActivity() {
        return transactionCount > 0 || totalDebits.compareTo(BigDecimal.ZERO) > 0
                || totalCredits.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Updates the daily balance with new transaction activity
     *
     * @param debitAmount  the debit amount to add
     * @param creditAmount the credit amount to add
     */
    public void addActivity(BigDecimal debitAmount, BigDecimal creditAmount) {
        if (debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.totalDebits = this.totalDebits.add(debitAmount);
            if (glAccount != null && glAccount.getNormalBalance() == BalanceType.DEBIT) {
                this.closingBalance = this.closingBalance.add(debitAmount);
            } else {
                this.closingBalance = this.closingBalance.subtract(debitAmount);
            }
        }

        if (creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0) {
            this.totalCredits = this.totalCredits.add(creditAmount);
            if (glAccount != null && glAccount.getNormalBalance() == BalanceType.CREDIT) {
                this.closingBalance = this.closingBalance.add(creditAmount);
            } else {
                this.closingBalance = this.closingBalance.subtract(creditAmount);
            }
        }

        this.transactionCount++;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLAccount getGlAccount() {
        return glAccount;
    }

    public void setGlAccount(GLAccount glAccount) {
        this.glAccount = glAccount;
    }

    public LocalDate getBalanceDate() {
        return balanceDate;
    }

    public void setBalanceDate(LocalDate balanceDate) {
        this.balanceDate = balanceDate;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance != null ? openingBalance : BigDecimal.ZERO;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance != null ? closingBalance : BigDecimal.ZERO;
    }

    public BigDecimal getTotalDebits() {
        return totalDebits;
    }

    public BigDecimal getDebitAmount() {
        return totalDebits;
    }

    public void setTotalDebits(BigDecimal totalDebits) {
        this.totalDebits = totalDebits != null ? totalDebits : BigDecimal.ZERO;
    }

    public BigDecimal getTotalCredits() {
        return totalCredits;
    }

    public BigDecimal getCreditAmount() {
        return totalCredits;
    }

    public void setTotalCredits(BigDecimal totalCredits) {
        this.totalCredits = totalCredits != null ? totalCredits : BigDecimal.ZERO;
    }

    public Integer getTransactionCount() {
        return transactionCount;
    }

    public void setTransactionCount(Integer transactionCount) {
        this.transactionCount = transactionCount != null ? transactionCount : 0;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GLDailyBalance that))
            return false;
        return glAccount != null && glAccount.equals(that.glAccount) && balanceDate != null
                && balanceDate.equals(that.balanceDate);
    }

    @Override
    public int hashCode() {
        int result = glAccount != null ? glAccount.hashCode() : 0;
        result = 31 * result + (balanceDate != null ? balanceDate.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "GLDailyBalance{" + "id=" + id + ", account=" + (glAccount != null ? glAccount.getCode() : null)
                + ", balanceDate=" + balanceDate + ", openingBalance=" + openingBalance + ", closingBalance="
                + closingBalance + ", totalDebits=" + totalDebits + ", totalCredits=" + totalCredits
                + ", transactionCount=" + transactionCount + '}';
    }
}
