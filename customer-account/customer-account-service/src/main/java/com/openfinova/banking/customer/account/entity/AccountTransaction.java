package com.openfinova.banking.customer.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing a user-facing transaction on a customer account.
 * This serves as the statement view or "memo post" record.
 */
@Entity
@Table(name = "account_transactions", indexes = {
        @Index(name = "idx_acc_trx_account", columnList = "customer_account_id"),
        @Index(name = "idx_acc_trx_date", columnList = "transaction_date"),
        @Index(name = "idx_acc_trx_ref", columnList = "reference_id") })
public class AccountTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false)
    private Account customerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 30)
    @NotNull(message = "Transaction type is required")
    private com.openfinova.banking.customer.account.api.entity.AccountTransactionType transactionType;

    /**
     * The value of the transaction.
     * Always positive. Direction (debit/credit) is determined by transactionType.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.0001", message = "Amount must be positive")
    private BigDecimal amount;

    @Column(name = "currency", nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    private String currency;

    /**
     * External reference ID for reconciliation (e.g., payment gateway ID).
     */
    @Column(name = "reference_id", length = 100)
    private String referenceId;

    @Column(name = "description", length = 255)
    private String description;

    @Column(name = "status", nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private String status = "PENDING"; // Simplified status for now

    /**
     * Link to the corresponding GL Transaction when posted to ledger.
     */
    @Column(name = "gl_transaction_id")
    private UUID glTransactionId;

    /**
     * The business date/time when this transaction occurred.
     * This is the date that appears on customer statements and is used for
     * business logic, reporting, and compliance.
     *
     * This is NOT the same as createdAt (audit timestamp).
     * Examples where they differ:
     * - Backdated transactions for corrections
     * - Batch processing of transactions from previous days
     * - Transactions recorded after business hours
     * - Timezone differences between transaction location and system
     */
    @Column(name = "transaction_date", nullable = false)
    @NotNull(message = "Transaction date is required")
    private LocalDateTime transactionDate;

    /**
     * Audit timestamp - when this record was created in the database.
     * Automatically set by the database/ORM on insert.
     * This is for audit trail purposes only, not business logic.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    /**
     * Audit timestamp - when this record was last updated in the database.
     * Automatically updated by the database/ORM on any modification.
     * This is for audit trail purposes only, not business logic.
     */
    @UpdateTimestamp
    @Column(name = "updated_at")
    private Instant updatedAt;

    public AccountTransaction() {
    }

    public AccountTransaction(Account account,
            com.openfinova.banking.customer.account.api.entity.AccountTransactionType type, BigDecimal amount,
            String currency, LocalDateTime transactionDate) {
        this.customerAccount = account;
        this.transactionType = type;
        this.amount = amount;
        this.currency = currency;
        this.transactionDate = transactionDate;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Account getCustomerAccount() {
        return customerAccount;
    }

    public void setCustomerAccount(Account customerAccount) {
        this.customerAccount = customerAccount;
    }

    public com.openfinova.banking.customer.account.api.entity.AccountTransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(
            com.openfinova.banking.customer.account.api.entity.AccountTransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public UUID getGlTransactionId() {
        return glTransactionId;
    }

    public void setGlTransactionId(UUID glTransactionId) {
        this.glTransactionId = glTransactionId;
    }

    public LocalDateTime getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDateTime transactionDate) {
        this.transactionDate = transactionDate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }
}
