package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.DisbursementMethod;
import com.openfinova.banking.loan.api.entity.DisbursementStatus;
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
 * Loan Disbursement entity tracking fund transfers to borrowers.
 * Records disbursement details, beneficiary information, and status.
 */
@Entity
@Table(name = "loan_disbursements", indexes = {
        @Index(name = "idx_loan_disbursements_reference", columnList = "disbursement_reference"),
        @Index(name = "idx_loan_disbursements_account", columnList = "loan_account_id"),
        @Index(name = "idx_loan_disbursements_date", columnList = "disbursement_date"),
        @Index(name = "idx_loan_disbursements_status", columnList = "status") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoanDisbursement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Disbursement reference is required")
    @Column(name = "disbursement_reference", nullable = false, unique = true, length = 50)
    @Size(max = 50, message = "Disbursement reference must not exceed 50 characters")
    private String disbursementReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @NotNull(message = "Disbursement date is required")
    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;

    @NotNull(message = "Disbursement amount is required")
    @DecimalMin(value = "0.0", message = "Disbursement amount must be positive")
    @Column(name = "disbursement_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal disbursementAmount;

    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(name = "disbursement_method", nullable = false, length = 30)
    @NotNull(message = "Disbursement method is required")
    private DisbursementMethod disbursementMethod;

    /**
     * Account number where funds are disbursed.
     */
    @Column(name = "beneficiary_account_number", length = 100)
    @Size(max = 100, message = "Beneficiary account number must not exceed 100 characters")
    private String beneficiaryAccountNumber;

    @Column(name = "beneficiary_name", length = 200)
    @Size(max = 200, message = "Beneficiary name must not exceed 200 characters")
    private String beneficiaryName;

    @Column(name = "transaction_reference", length = 100)
    @Size(max = 100, message = "Transaction reference must not exceed 100 characters")
    private String transactionReference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private DisbursementStatus status = DisbursementStatus.PENDING;

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
    public LoanDisbursement() {
    }

    // Business Logic
    public boolean isCompleted() {
        return DisbursementStatus.COMPLETED.equals(status);
    }

    public boolean isPending() {
        return DisbursementStatus.PENDING.equals(status);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getDisbursementReference() {
        return disbursementReference;
    }

    public void setDisbursementReference(String disbursementReference) {
        this.disbursementReference = disbursementReference;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    public BigDecimal getDisbursementAmount() {
        return disbursementAmount;
    }

    public void setDisbursementAmount(BigDecimal disbursementAmount) {
        this.disbursementAmount = disbursementAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public DisbursementMethod getDisbursementMethod() {
        return disbursementMethod;
    }

    public void setDisbursementMethod(DisbursementMethod disbursementMethod) {
        this.disbursementMethod = disbursementMethod;
    }

    public String getBeneficiaryAccountNumber() {
        return beneficiaryAccountNumber;
    }

    public void setBeneficiaryAccountNumber(String beneficiaryAccountNumber) {
        this.beneficiaryAccountNumber = beneficiaryAccountNumber;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public DisbursementStatus getStatus() {
        return status;
    }

    public void setStatus(DisbursementStatus status) {
        this.status = status;
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
