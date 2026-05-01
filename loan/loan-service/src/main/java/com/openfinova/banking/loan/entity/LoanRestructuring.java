package com.openfinova.banking.loan.entity;

import com.openfinova.banking.loan.api.entity.RestructuringStatus;
import com.openfinova.banking.loan.api.entity.RestructuringType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Loan Restructuring entity tracking loan modification history.
 * Immutable record of restructuring events for regulatory reporting.
 */
@Entity
@Table(name = "loan_restructurings", indexes = {
        @Index(name = "idx_loan_restructurings_account", columnList = "loan_account_id"),
        @Index(name = "idx_loan_restructurings_date", columnList = "restructuring_date"),
        @Index(name = "idx_loan_restructurings_type", columnList = "restructuring_type") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoanRestructuring {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @NotNull(message = "Restructuring date is required")
    @Column(name = "restructuring_date", nullable = false)
    private LocalDate restructuringDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "restructuring_type", nullable = false, length = 30)
    @NotNull(message = "Restructuring type is required")
    private RestructuringType restructuringType;

    @Enumerated(EnumType.STRING)
    @Column(name = "restructuring_status", nullable = false, length = 20)
    @NotNull(message = "Restructuring status is required")
    private RestructuringStatus restructuringStatus = RestructuringStatus.PENDING;

    @DecimalMin(value = "0.0", message = "Old principal balance must be positive")
    @Column(name = "old_principal_balance", precision = 19, scale = 4)
    private BigDecimal oldPrincipalBalance;

    @DecimalMin(value = "0.0", message = "New principal balance must be positive")
    @Column(name = "new_principal_balance", precision = 19, scale = 4)
    private BigDecimal newPrincipalBalance;

    @DecimalMin(value = "0.0", message = "Old interest rate must be positive")
    @DecimalMax(value = "100.0", message = "Old interest rate cannot exceed 100%")
    @Column(name = "old_interest_rate", precision = 5, scale = 2)
    private BigDecimal oldInterestRate;

    @DecimalMin(value = "0.0", message = "New interest rate must be positive")
    @DecimalMax(value = "100.0", message = "New interest rate cannot exceed 100%")
    @Column(name = "new_interest_rate", precision = 5, scale = 2)
    private BigDecimal newInterestRate;

    @Min(value = 1, message = "Old tenor must be at least 1 month")
    @Column(name = "old_tenor_months")
    private Integer oldTenorMonths;

    @Min(value = 1, message = "New tenor must be at least 1 month")
    @Column(name = "new_tenor_months")
    private Integer newTenorMonths;

    @Column(length = 1000)
    @Size(max = 1000, message = "Reason must not exceed 1000 characters")
    private String reason;

    @Column(name = "approved_by", length = 100)
    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    private String approvedBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public LoanRestructuring() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public LocalDate getRestructuringDate() {
        return restructuringDate;
    }

    public void setRestructuringDate(LocalDate restructuringDate) {
        this.restructuringDate = restructuringDate;
    }

    public RestructuringType getRestructuringType() {
        return restructuringType;
    }

    public void setRestructuringType(RestructuringType restructuringType) {
        this.restructuringType = restructuringType;
    }

    public RestructuringStatus getRestructuringStatus() {
        return restructuringStatus;
    }

    public void setRestructuringStatus(RestructuringStatus restructuringStatus) {
        this.restructuringStatus = restructuringStatus;
    }

    public BigDecimal getOldPrincipalBalance() {
        return oldPrincipalBalance;
    }

    public void setOldPrincipalBalance(BigDecimal oldPrincipalBalance) {
        this.oldPrincipalBalance = oldPrincipalBalance;
    }

    public BigDecimal getNewPrincipalBalance() {
        return newPrincipalBalance;
    }

    public void setNewPrincipalBalance(BigDecimal newPrincipalBalance) {
        this.newPrincipalBalance = newPrincipalBalance;
    }

    public BigDecimal getOldInterestRate() {
        return oldInterestRate;
    }

    public void setOldInterestRate(BigDecimal oldInterestRate) {
        this.oldInterestRate = oldInterestRate;
    }

    public BigDecimal getNewInterestRate() {
        return newInterestRate;
    }

    public void setNewInterestRate(BigDecimal newInterestRate) {
        this.newInterestRate = newInterestRate;
    }

    public Integer getOldTenorMonths() {
        return oldTenorMonths;
    }

    public void setOldTenorMonths(Integer oldTenorMonths) {
        this.oldTenorMonths = oldTenorMonths;
    }

    public Integer getNewTenorMonths() {
        return newTenorMonths;
    }

    public void setNewTenorMonths(Integer newTenorMonths) {
        this.newTenorMonths = newTenorMonths;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
