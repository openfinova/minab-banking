package com.openfinova.banking.loan.entity;

import com.openfinova.banking.loan.api.entity.ScheduleStatus;
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
 * Loan Schedule entity representing individual installment payments.
 * Tracks due dates, amounts, and payment status for each installment.
 */
@Entity
@Table(name = "loan_schedules", indexes = { @Index(name = "idx_loan_schedules_account", columnList = "loan_account_id"),
        @Index(name = "idx_loan_schedules_due_date", columnList = "due_date"),
        @Index(name = "idx_loan_schedules_status", columnList = "status"),
        @Index(name = "idx_loan_schedules_overdue", columnList = "is_overdue"),
        @Index(name = "idx_loan_schedules_account_installment", columnList = "loan_account_id, installment_number") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoanSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Reference to the loan account this schedule belongs to.
     */
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    /**
     * Sequential installment number (1, 2, 3, ...).
     */
    @NotNull(message = "Installment number is required")
    @Min(value = 1, message = "Installment number must be at least 1")
    @Column(name = "installment_number", nullable = false)
    private Integer installmentNumber;

    /**
     * Date when this installment is due.
     */
    @NotNull(message = "Due date is required")
    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    /**
     * Principal amount due in this installment.
     */
    @NotNull(message = "Principal due is required")
    @DecimalMin(value = "0.0", message = "Principal due must be positive")
    @Column(name = "principal_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalDue;

    /**
     * Interest amount due in this installment.
     */
    @NotNull(message = "Interest due is required")
    @DecimalMin(value = "0.0", message = "Interest due must be positive")
    @Column(name = "interest_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestDue;

    /**
     * Total amount due (principal + interest).
     */
    @NotNull(message = "Total due is required")
    @DecimalMin(value = "0.0", message = "Total due must be positive")
    @Column(name = "total_due", nullable = false, precision = 19, scale = 4)
    private BigDecimal totalDue;

    /**
     * Principal amount paid towards this installment.
     */
    @NotNull(message = "Principal paid is required")
    @DecimalMin(value = "0.0", message = "Principal paid must be positive")
    @Column(name = "principal_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalPaid = BigDecimal.ZERO;

    /**
     * Interest amount paid towards this installment.
     */
    @NotNull(message = "Interest paid is required")
    @DecimalMin(value = "0.0", message = "Interest paid must be positive")
    @Column(name = "interest_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal interestPaid = BigDecimal.ZERO;

    @NotNull(message = "Fees paid is required")
    @DecimalMin(value = "0.0", message = "Fees paid must be positive")
    @Column(name = "fees_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal feesPaid = BigDecimal.ZERO;

    @NotNull(message = "Penalties paid is required")
    @DecimalMin(value = "0.0", message = "Penalties paid must be positive")
    @Column(name = "penalties_paid", nullable = false, precision = 19, scale = 4)
    private BigDecimal penaltiesPaid = BigDecimal.ZERO;

    /**
     * Remaining balance after payments.
     */
    @NotNull(message = "Outstanding balance is required")
    @DecimalMin(value = "0.0", message = "Outstanding balance must be positive")
    @Column(name = "outstanding_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingBalance;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private ScheduleStatus status = ScheduleStatus.PENDING;

    /**
     * Date when this installment was fully paid.
     */
    @Column(name = "paid_date")
    private LocalDate paidDate;

    /**
     * Whether this installment is overdue.
     */
    @Column(name = "is_overdue", nullable = false)
    private Boolean isOverdue = false;

    /**
     * Number of days this installment is overdue.
     */
    @Min(value = 0, message = "Days past due cannot be negative")
    @Column(name = "days_past_due", nullable = false)
    private Integer daysPastDue = 0;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public LoanSchedule() {
    }

    public LoanSchedule(LoanAccount loanAccount, Integer installmentNumber, LocalDate dueDate, BigDecimal principalDue,
            BigDecimal interestDue) {
        this.loanAccount = loanAccount;
        this.installmentNumber = installmentNumber;
        this.dueDate = dueDate;
        this.principalDue = principalDue;
        this.interestDue = interestDue;
        this.totalDue = principalDue.add(interestDue);
        this.outstandingBalance = this.totalDue;
    }

    // Business Logic
    public boolean isPaid() {
        return ScheduleStatus.PAID.equals(status);
    }

    public boolean isPending() {
        return ScheduleStatus.PENDING.equals(status);
    }

    public BigDecimal getRemainingAmount() {
        return totalDue.subtract(principalPaid).subtract(interestPaid);
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

    public Integer getInstallmentNumber() {
        return installmentNumber;
    }

    public void setInstallmentNumber(Integer installmentNumber) {
        this.installmentNumber = installmentNumber;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    public void setDueDate(LocalDate dueDate) {
        this.dueDate = dueDate;
    }

    public BigDecimal getPrincipalDue() {
        return principalDue;
    }

    public void setPrincipalDue(BigDecimal principalDue) {
        this.principalDue = principalDue;
    }

    public BigDecimal getInterestDue() {
        return interestDue;
    }

    public void setInterestDue(BigDecimal interestDue) {
        this.interestDue = interestDue;
    }

    public BigDecimal getTotalDue() {
        return totalDue;
    }

    public void setTotalDue(BigDecimal totalDue) {
        this.totalDue = totalDue;
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

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public ScheduleStatus getStatus() {
        return status;
    }

    public void setStatus(ScheduleStatus status) {
        this.status = status;
    }

    public LocalDate getPaidDate() {
        return paidDate;
    }

    public void setPaidDate(LocalDate paidDate) {
        this.paidDate = paidDate;
    }

    public Boolean getIsOverdue() {
        return isOverdue;
    }

    public void setIsOverdue(Boolean isOverdue) {
        this.isOverdue = isOverdue;
    }

    public Integer getDaysPastDue() {
        return daysPastDue;
    }

    public void setDaysPastDue(Integer daysPastDue) {
        this.daysPastDue = daysPastDue;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
