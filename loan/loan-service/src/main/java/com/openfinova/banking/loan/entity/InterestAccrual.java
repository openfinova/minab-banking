package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents daily or periodic interest accrual calculations for a loan.
 *
 * WHAT IS INTEREST ACCRUAL?
 * Interest Accrual is the process of calculating and recording interest charges that accumulate
 * on a loan over time. This entity creates an immutable record of interest calculated for each
 * period (typically daily), which is later posted to the loan account and general ledger.
 *
 * KEY CONCEPTS:
 * - Daily Accrual: Interest is calculated daily based on the outstanding principal balance
 * - Accrual vs Payment: Interest accrues continuously but is typically paid monthly
 * - Compound Interest: Unpaid accrued interest may be added to principal (capitalization)
 * - GL Posting: Accrued interest must be posted to accounting system for financial reporting
 *
 * INTEREST CALCULATION:
 * Daily Interest = (Principal Balance × Annual Interest Rate) / Days in Year
 *
 * Example with 365-day year:
 * Daily Interest = ($10,000 × 12%) / 365
 *                = $1,200 / 365
 *                = $3.29 per day
 *
 * EXAMPLE SCENARIO:
 * Loan Details:
 * - Principal Balance: $10,000
 * - Annual Interest Rate: 12% (0.12)
 * - Calculation Method: Daily (365-day year)
 *
 * Day 1 Accrual:
 * - Principal Balance: $10,000.00
 * - Interest Rate: 12.00%
 * - Accrual Amount: $3.29
 * - Posted: false (will be posted at month-end)
 *
 * After 30 days:
 * - Total Accrued: $3.29 × 30 = $98.70
 * - Customer Payment Due: $98.70 (interest) + portion of principal
 *
 * CALCULATION METHODS:
 * Different loan products use different day-count conventions:
 * - 365-day year: Daily Interest = (Principal × Rate) / 365 (most common)
 * - 360-day year: Daily Interest = (Principal × Rate) / 360 (commercial loans)
 * - Actual/Actual: Uses actual days in month and year (mortgages)
 * - 30/360: Assumes 30 days per month (some bonds)
 *
 * WORKFLOW:
 * 1. Daily Calculation: System runs daily job to calculate interest for all active loans
 * 2. Record Creation: Creates InterestAccrual record with calculation details
 * 3. Accumulation: Interest accruals accumulate until payment or posting date
 * 4. GL Posting: At period-end, accruals are posted to general ledger
 * 5. Payment Application: When customer pays, accrued interest is reduced
 *
 * POSTING PROCESS:
 * Unposted (isPosted = false):
 * - Interest has been calculated but not yet recorded in GL
 * - Accumulates in loan account's "accrued interest" balance
 * - Visible to customer but not yet in financial statements
 *
 * Posted (isPosted = true):
 * - Interest has been recorded in general ledger
 * - Appears in financial statements as interest income
 * - Cannot be modified (immutable record)
 * - postedAt timestamp records when posting occurred
 *
 * WHY TRACK EACH ACCRUAL?
 * - Audit Trail: Complete history of interest calculations
 * - Regulatory Compliance: Prove interest was calculated correctly
 * - Dispute Resolution: Show customer exactly how interest was calculated
 * - Reconciliation: Match accruals to payments and GL postings
 * - Reporting: Generate interest income reports by period
 * - Rate Changes: Track when interest rate changed on variable-rate loans
 *
 * IMMUTABLE RECORD:
 * This entity is immutable (no updatedAt field) because:
 * - Historical accuracy: Once calculated, should never change
 * - Audit integrity: Modifications would compromise audit trail
 * - Regulatory requirement: Interest calculations must be preserved as-is
 * - If correction needed: Create reversing entry, don't modify original
 *
 * BUSINESS RULES:
 * - One accrual record per loan per day (typically)
 * - Accrual amount must always be positive (or zero)
 * - Principal balance is snapshot at time of calculation
 * - Interest rate is the effective rate on accrual date
 * - Posted accruals cannot be deleted or modified
 * - Accruals for closed loans should not be created
 *
 * INTEGRATION POINTS:
 * - LoanAccount: Read principal balance and interest rate
 * - LoanPayment: Reduce accrued interest when payment received
 * - GLJournalEntry: Post accrued interest to general ledger
 * - LoanSchedule: Compare accrued vs scheduled interest
 * - Financial Reports: Calculate interest income for period
 *
 * COMMON QUERIES:
 * - Total unposted interest for a loan
 * - Interest accrued in a date range
 * - Daily interest trend analysis
 * - Loans with posting discrepancies
 * - Interest income by period
 *
 * @see LoanAccount
 * @see com.openfinova.banking.loan.api.entity.InterestCalculationMethod
 * @see LoanPayment
 * @see LoanSchedule
 */
@Entity
@Table(name = "interest_accruals", indexes = {
        @Index(name = "idx_interest_accruals_account", columnList = "loan_account_id"),
        @Index(name = "idx_interest_accruals_date", columnList = "accrual_date"),
        @Index(name = "idx_interest_accruals_posted", columnList = "is_posted") })
public class InterestAccrual {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @NotNull(message = "Accrual date is required")
    @Column(name = "accrual_date", nullable = false)
    private LocalDate accrualDate;

    /**
     * Principal balance on which interest is calculated.
     */
    @NotNull(message = "Principal balance is required")
    @DecimalMin(value = "0.0", message = "Principal balance must be positive")
    @Column(name = "principal_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalBalance;

    /**
     * Interest rate used for calculation (annual percentage).
     */
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate must be positive")
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal interestRate;

    /**
     * Interest amount accrued for this period.
     */
    @NotNull(message = "Accrual amount is required")
    @DecimalMin(value = "0.0", message = "Accrual amount must be positive")
    @Column(name = "accrual_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal accrualAmount;

    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    /**
     * Whether this accrual has been posted to GL.
     */
    @Column(name = "is_posted", nullable = false)
    private Boolean isPosted = false;

    @Column(name = "posted_at")
    private Instant postedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public InterestAccrual() {
    }

    // Business Logic
    public boolean isPosted() {
        return Boolean.TRUE.equals(isPosted);
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

    public LocalDate getAccrualDate() {
        return accrualDate;
    }

    public void setAccrualDate(LocalDate accrualDate) {
        this.accrualDate = accrualDate;
    }

    public BigDecimal getPrincipalBalance() {
        return principalBalance;
    }

    public void setPrincipalBalance(BigDecimal principalBalance) {
        this.principalBalance = principalBalance;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public BigDecimal getAccrualAmount() {
        return accrualAmount;
    }

    public void setAccrualAmount(BigDecimal accrualAmount) {
        this.accrualAmount = accrualAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Boolean getIsPosted() {
        return isPosted;
    }

    public void setIsPosted(Boolean isPosted) {
        this.isPosted = isPosted;
    }

    public Instant getPostedAt() {
        return postedAt;
    }

    public void setPostedAt(Instant postedAt) {
        this.postedAt = postedAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
