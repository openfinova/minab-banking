package com.openfinova.banking.gl.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.openfinova.banking.common.lib.validation.ValidCurrency;

/**
 * JournalEntry entity representing individual debit or credit entries within a
 * transaction.
 * Each entry must have either a debit amount OR a credit amount, but not both.
 */
@Entity
@Table(name = "gl_journal_entries", indexes = {
        @Index(name = "idx_gl_journal_entries_transaction", columnList = "transaction_id"),
        @Index(name = "idx_gl_journal_entries_account", columnList = "account_id"),
        @Index(name = "idx_gl_journal_entries_account_date", columnList = "account_id, transaction_id") }, check = @CheckConstraint(constraint = "debit_amount >= 0 AND credit_amount >= 0 AND "
                + "((debit_amount > 0 AND credit_amount = 0) OR (debit_amount = 0 AND credit_amount > 0))"))
public class GLJournalEntry {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    @NotNull(message = "Transaction is required")
    private GLTransaction transaction;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "account_id", nullable = false)
    @NotNull(message = "Account is required")
    private GLAccount account;

    /**
     * Debit amount. Must be 0 if Credit > 0.
     */
    @Column(name = "debit_amount", precision = 19, scale = 4, nullable = false)
    @DecimalMin(value = "0.0", message = "Debit amount must be non-negative")
    @NotNull(message = "Debit amount is required")
    private BigDecimal debitAmount = BigDecimal.ZERO;

    /**
     * Credit amount. Must be 0 if Debit > 0.
     */
    @Column(name = "credit_amount", precision = 19, scale = 4, nullable = false)
    @DecimalMin(value = "0.0", message = "Credit amount must be non-negative")
    @NotNull(message = "Credit amount is required")
    private BigDecimal creditAmount = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    @Size(max = 1000, message = "Description must not exceed 1000 characters")
    private String description;

    /**
     * Line number of the journal entry within the transaction.
     * This is used to maintain the order of entries and uniquely identify lines
     * within a single transaction.
     * Must be unique within the scope of the associated transaction.
     */
    @Column(name = "line_number", nullable = false)
    @NotNull(message = "Line number is required")
    private Integer lineNumber;

    /**
     * The date when the transaction is effective for accounting purposes.
     * This may differ from the transaction's entry date.
     */
    @Column(name = "value_date", nullable = false)
    @NotNull(message = "Value date is required")
    private LocalDate valueDate;

    /**
     * Three-letter ISO currency code for the entry.
     */
    @Column(name = "currency", length = 3, nullable = false)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    /**
     * Exchange rate for the entry.
     * This is the rate at which the currency was converted to the base currency.
     */
    @Column(name = "exchange_rate", precision = 19, scale = 6, nullable = false)
    @NotNull(message = "Exchange rate is required")
    private BigDecimal exchangeRate = BigDecimal.ONE;

    /**
     * Base currency amount for the entry.
     * This is the amount in the base currency (e.g., USD) for the entry.
     */
    @Column(name = "base_debit_amount", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Base debit amount is required")
    private BigDecimal baseDebitAmount = BigDecimal.ZERO;

    /**
     * Base currency amount for the entry.
     * This is the amount in the base currency (e.g., USD) for the entry.
     */
    @Column(name = "base_credit_amount", precision = 19, scale = 4, nullable = false)
    @NotNull(message = "Base credit amount is required")
    private BigDecimal baseCreditAmount = BigDecimal.ZERO;

    // Constructors
    public GLJournalEntry() {
    }

    public GLJournalEntry(GLAccount account, BigDecimal debitAmount, BigDecimal creditAmount, String description,
            LocalDate valueDate) {
        this.account = account;
        this.debitAmount = debitAmount != null ? debitAmount : BigDecimal.ZERO;
        this.creditAmount = creditAmount != null ? creditAmount : BigDecimal.ZERO;
        this.description = description;
        this.valueDate = valueDate;
        validateAmounts();
    }

    /**
     * Creates a debit journal entry
     *
     * @param account     the account to debit
     * @param amount      the debit amount
     * @param description optional description
     * @return new debit journal entry
     */
    public static GLJournalEntry debit(GLAccount account, BigDecimal amount, String description, LocalDate valueDate) {
        return new GLJournalEntry(account, amount, BigDecimal.ZERO, description, valueDate);
    }

    /**
     * Creates a credit journal entry
     *
     * @param account     the account to credit
     * @param amount      the credit amount
     * @param description optional description
     * @return new credit journal entry
     */
    public static GLJournalEntry credit(GLAccount account, BigDecimal amount, String description, LocalDate valueDate) {
        return new GLJournalEntry(account, BigDecimal.ZERO, amount, description, valueDate);
    }

    // Business logic methods

    /**
     * Validates that exactly one of debit or credit amount is set
     */
    private void validateAmounts() {
        if (debitAmount == null)
            debitAmount = BigDecimal.ZERO;
        if (creditAmount == null)
            creditAmount = BigDecimal.ZERO;

        boolean hasDebit = debitAmount.compareTo(BigDecimal.ZERO) > 0;
        boolean hasCredit = creditAmount.compareTo(BigDecimal.ZERO) > 0;

        if (hasDebit && hasCredit) {
            throw new IllegalArgumentException("Journal entry cannot have both debit and credit amounts");
        }
        if (!hasDebit && !hasCredit) {
            throw new IllegalArgumentException("Journal entry must have either debit or credit amount");
        }
    }

    /**
     * Checks if this is a debit entry
     *
     * @return true if debit amount > 0, false otherwise
     */
    public boolean isDebit() {
        return debitAmount != null && debitAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Checks if this is a credit entry
     *
     * @return true if credit amount > 0, false otherwise
     */
    public boolean isCredit() {
        return creditAmount != null && creditAmount.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Gets the entry amount (either debit or credit)
     *
     * @return the non-zero amount
     */
    public BigDecimal getAmount() {
        return isDebit() ? debitAmount : creditAmount;
    }

    /**
     * Gets the entry type as a string
     *
     * @return "DEBIT" or "CREDIT"
     */
    public String getEntryType() {
        return isDebit() ? "DEBIT" : "CREDIT";
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public GLTransaction getTransaction() {
        return transaction;
    }

    public void setTransaction(GLTransaction transaction) {
        this.transaction = transaction;
    }

    public GLAccount getAccount() {
        return account;
    }

    public void setAccount(GLAccount account) {
        this.account = account;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount != null ? debitAmount : BigDecimal.ZERO;
        validateAmounts();
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount != null ? creditAmount : BigDecimal.ZERO;
        validateAmounts();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getExchangeRate() {
        return exchangeRate;
    }

    public void setExchangeRate(BigDecimal exchangeRate) {
        this.exchangeRate = exchangeRate != null ? exchangeRate : BigDecimal.ONE;
    }

    public BigDecimal getBaseDebitAmount() {
        return baseDebitAmount;
    }

    public void setBaseDebitAmount(BigDecimal baseDebitAmount) {
        this.baseDebitAmount = baseDebitAmount != null ? baseDebitAmount : BigDecimal.ZERO;
    }

    public BigDecimal getBaseCreditAmount() {
        return baseCreditAmount;
    }

    public void setBaseCreditAmount(BigDecimal baseCreditAmount) {
        this.baseCreditAmount = baseCreditAmount != null ? baseCreditAmount : BigDecimal.ZERO;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof GLJournalEntry that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "GLJournalEntry{" + "id=" + id + ", account=" + (account != null ? account.getCode() : null)
                + ", debitAmount=" + debitAmount + ", creditAmount=" + creditAmount + ", currency='" + currency + '\''
                + ", baseDebitAmount=" + baseDebitAmount + ", baseCreditAmount=" + baseCreditAmount + ", lineNumber="
                + lineNumber + '}';
    }
}
