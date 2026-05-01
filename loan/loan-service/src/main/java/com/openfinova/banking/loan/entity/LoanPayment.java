package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.PaymentMethod;
import com.openfinova.banking.loan.api.entity.PaymentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Loan Payment entity representing a payment made towards a loan.
 * Tracks payment allocation across principal, interest, fees, and penalties.
 */
@Entity
@Table(name = "loan_payments", indexes = {
        @Index(name = "idx_loan_payments_reference", columnList = "payment_reference"),
        @Index(name = "idx_loan_payments_account", columnList = "loan_account_id"),
        @Index(name = "idx_loan_payments_date", columnList = "payment_date"),
        @Index(name = "idx_loan_payments_type", columnList = "payment_type"),
        @Index(name = "idx_loan_payments_reversed", columnList = "is_reversed") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoanPayment {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique payment reference for tracking and reconciliation.
     */
    @NotBlank(message = "Payment reference is required")
    @Column(name = "payment_reference", nullable = false, unique = true, length = 50)
    @Size(max = 50, message = "Payment reference must not exceed 50 characters")
    private String paymentReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @NotNull(message = "Payment date is required")
    @Column(name = "payment_date", nullable = false)
    private LocalDate paymentDate;

    /**
     * Total payment amount received.
     */
    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.0", message = "Payment amount must be positive")
    @Column(name = "payment_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal paymentAmount;

    /**
     * Amount allocated to principal reduction.
     */
    @NotNull(message = "Principal paid is required")
    @DecimalMin(value = "0.0", message = "Principal paid must be positive")
    @Column(name = "principal_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalPaid;

    /**
     * Amount allocated to interest payment.
     */
    @NotNull(message = "Interest paid is required")
    @DecimalMin(value = "0.0", message = "Interest paid must be positive")
    @Column(name = "interest_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestPaid;

    @NotNull(message = "Fees paid is required")
    @DecimalMin(value = "0.0", message = "Fees paid must be positive")
    @Column(name = "fees_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal feesPaid = BigDecimal.ZERO;

    @NotNull(message = "Penalties paid is required")
    @DecimalMin(value = "0.0", message = "Penalties paid must be positive")
    @Column(name = "penalties_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal penaltiesPaid = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_type", nullable = false, length = 30)
    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_method", nullable = false, length = 30)
    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    /**
     * External transaction reference from payment gateway or bank.
     */
    @Column(name = "transaction_reference", length = 100)
    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionReference;

    @Column(name = "is_reversed", nullable = false)
    private Boolean isReversed = false;

    @Column(name = "reversed_at")
    private Instant reversedAt;

    @Column(name = "reversal_reason", length = 500)
    @Size(max = 500, message = "Reversal reason must not exceed 500 characters")
    private String reversalReason;

    @Column(length = 500)
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public LoanPayment() {
    }

    // Business Logic
    public boolean isReversed() {
        return Boolean.TRUE.equals(isReversed);
    }

    public void reverse(String reason) {
        this.isReversed = true;
        this.reversedAt = Instant.now();
        this.reversalReason = reason;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    public void setPrincipalPaid(BigDecimal principalPaid) {
        this.principalPaid = principalPaid;
    }

    public BigDecimal getInterestPaid() {
        return interestPaid;
    }

    public void setInterestPaid(BigDecimal interestPaid) {
        this.interestPaid = interestPaid;
    }

    public BigDecimal getFeesPaid() {
        return feesPaid;
    }

    public void setFeesPaid(BigDecimal feesPaid) {
        this.feesPaid = feesPaid;
    }

    public BigDecimal getPenaltiesPaid() {
        return penaltiesPaid;
    }

    public void setPenaltiesPaid(BigDecimal penaltiesPaid) {
        this.penaltiesPaid = penaltiesPaid;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public Boolean getIsReversed() {
        return isReversed;
    }

    public void setIsReversed(Boolean isReversed) {
        this.isReversed = isReversed;
    }

    public Instant getReversedAt() {
        return reversedAt;
    }

    public void setReversedAt(Instant reversedAt) {
        this.reversedAt = reversedAt;
    }

    public String getReversalReason() {
        return reversalReason;
    }

    public void setReversalReason(String reversalReason) {
        this.reversalReason = reversalReason;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
