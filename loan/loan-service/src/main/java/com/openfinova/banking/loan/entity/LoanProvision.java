package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.ProvisionStage;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents loan loss provisions calculated for credit risk management and regulatory compliance.
 *
 * WHAT IS LOAN PROVISION?
 * Loan Provision (also called Loan Loss Provision or Allowance for Credit Losses) is money that
 * banks set aside to cover potential losses from loans that may not be fully repaid. It's a
 * financial buffer against expected credit losses, required by accounting standards and regulators.
 *
 * SIMPLE ANALOGY:
 * If you lend money to 100 people, you know from experience that maybe 5 won't pay you back.
 * So you set aside 5% of the total as a "provision" - money you expect to lose. When someone
 * defaults, you're financially prepared and your balance sheet remains accurate.
 *
 * WHY IT EXISTS:
 * - Risk Management: Banks know not all loans will be repaid (credit risk)
 * - Financial Accuracy: Shows realistic value of loan portfolio, not just face value
 * - Regulatory Requirement: IFRS 9, CECL, Basel III mandate provisions
 * - Investor Protection: Prevents banks from overstating assets and profits
 * - Early Warning: Identifies deteriorating loans before they default
 *
 * IFRS 9 THREE-STAGE MODEL:
 *
 * Stage 1 - Performing (Low Risk):
 * - Loan is current, no payment issues
 * - No significant increase in credit risk since origination
 * - Provision: 0.5% - 2% of outstanding balance (12-month expected loss)
 * - Example: $10,000 loan × 1% = $100 provision
 *
 * Stage 2 - Underperforming (Increased Risk):
 * - Payment delays (typically 30-89 days past due)
 * - Credit quality has deteriorated significantly
 * - Provision: 5% - 25% of outstanding balance (lifetime expected loss)
 * - Example: $10,000 loan × 15% = $1,500 provision
 *
 * Stage 3 - Non-Performing (High Risk/Default):
 * - Loan is in default (typically 90+ days past due)
 * - Significant doubt about full repayment
 * - Provision: 30% - 100% of outstanding balance (specific provision)
 * - Example: $10,000 loan × 75% = $7,500 provision
 *
 * PROVISION CALCULATION:
 * Provision Amount = Outstanding Balance × Provision Rate
 *
 * Example Calculations:
 *
 * Performing Loan (Stage 1):
 * - Outstanding Balance: $50,000
 * - Provision Rate: 1.5%
 * - Provision Amount: $50,000 × 1.5% = $750
 *
 * Underperforming Loan (Stage 2):
 * - Outstanding Balance: $50,000
 * - Days Past Due: 45 days
 * - Provision Rate: 20%
 * - Provision Amount: $50,000 × 20% = $10,000
 *
 * Non-Performing Loan (Stage 3):
 * - Outstanding Balance: $50,000
 * - Days Past Due: 120 days
 * - Provision Rate: 80%
 * - Provision Amount: $50,000 × 80% = $40,000
 *
 * STAGE TRANSITION TRIGGERS:
 *
 * Stage 1 → Stage 2 (Deterioration):
 * - Payment delay of 30+ days
 * - Significant decrease in credit score
 * - Borrower financial difficulties
 * - Industry/economic downturn affecting borrower
 *
 * Stage 2 → Stage 3 (Default):
 * - Payment delay of 90+ days
 * - Borrower bankruptcy or insolvency
 * - Loan restructuring due to financial difficulty
 * - Legal action initiated
 *
 * Stage 3 → Stage 2 (Improvement):
 * - Borrower resumes regular payments
 * - Restructuring successful
 * - Collateral value sufficient
 *
 * Stage 2 → Stage 1 (Recovery):
 * - Consistent payment history restored
 * - Credit risk returned to origination level
 * - No payments overdue
 *
 * WORKFLOW:
 * 1. Risk Assessment: System evaluates each loan's credit risk daily/monthly
 * 2. Stage Classification: Assigns loan to Stage 1, 2, or 3 based on criteria
 * 3. Rate Determination: Applies appropriate provision rate for the stage
 * 4. Calculation: Computes provision amount (Balance × Rate)
 * 5. Record Creation: Creates immutable LoanProvision record
 * 6. GL Posting: Posts provision expense to general ledger
 * 7. Reporting: Aggregates provisions for financial statements
 *
 * ACCOUNTING IMPACT:
 *
 * When Provision Increases (Expense):
 * Debit: Provision Expense (Income Statement - reduces profit)
 * Credit: Allowance for Loan Losses (Balance Sheet - reduces assets)
 *
 * When Loan Defaults (Write-off):
 * Debit: Allowance for Loan Losses (use the provision)
 * Credit: Loan Receivable (remove the bad loan)
 *
 * When Provision Decreases (Recovery):
 * Debit: Allowance for Loan Losses
 * Credit: Provision Expense (increases profit)
 *
 * EXAMPLE SCENARIO:
 *
 * Month 1 - Loan Originated:
 * - Outstanding Balance: $100,000
 * - Stage: 1 (Performing)
 * - Provision Rate: 1%
 * - Provision Amount: $1,000
 * - Accounting: Expense $1,000
 *
 * Month 6 - Payment Missed:
 * - Outstanding Balance: $95,000
 * - Stage: 2 (Underperforming)
 * - Provision Rate: 15%
 * - Provision Amount: $14,250
 * - Accounting: Additional Expense $13,250 ($14,250 - $1,000)
 *
 * Month 9 - Loan Defaults:
 * - Outstanding Balance: $92,000
 * - Stage: 3 (Non-Performing)
 * - Provision Rate: 70%
 * - Provision Amount: $64,400
 * - Accounting: Additional Expense $50,150 ($64,400 - $14,250)
 *
 * Month 12 - Loan Written Off:
 * - Write-off Amount: $92,000
 * - Use Provision: $64,400
 * - Additional Loss: $27,600 (not covered by provision)
 *
 * IMMUTABLE RECORD:
 * This entity is immutable (no updatedAt field) because:
 * - Historical accuracy: Provisions are point-in-time calculations
 * - Audit trail: Regulators require complete provision history
 * - Trend analysis: Track how provisions changed over time
 * - If correction needed: Create new provision record, don't modify original
 *
 * BUSINESS RULES:
 * - Provisions calculated monthly or quarterly (regulatory requirement)
 * - Stage classification must follow consistent criteria
 * - Provision rates must be documented and approved
 * - Total provisions cannot exceed outstanding balance
 * - Posted provisions appear in financial statements
 * - Provision changes must be explained in financial reports
 *
 * REGULATORY REQUIREMENTS:
 * - IFRS 9: International Financial Reporting Standard (most countries)
 * - CECL: Current Expected Credit Loss (US banks)
 * - Basel III: Capital adequacy requirements
 * - Regular stress testing of provision models
 * - External audit of provision calculations
 *
 * INTEGRATION POINTS:
 * - LoanAccount: Read balance, days past due, payment history
 * - GLJournalEntry: Post provision expense to general ledger
 * - Financial Reports: Calculate total provisions by stage
 * - Risk Reports: Track portfolio credit quality
 * - Regulatory Reports: Submit provision data to regulators
 *
 * COMMON QUERIES:
 * - Total provisions by stage (Stage 1, 2, 3)
 * - Provision coverage ratio (Provisions / Non-Performing Loans)
 * - Provision trend over time
 * - Loans requiring provision increase
 * - Provision expense for the period
 *
 * KEY METRICS:
 * - Provision Coverage Ratio = Total Provisions / Non-Performing Loans
 *   (Higher is better - shows adequate reserves)
 * - NPL Ratio = Non-Performing Loans / Total Loans
 *   (Lower is better - shows portfolio quality)
 * - Provision Expense Ratio = Provision Expense / Total Loans
 *   (Lower is better - shows stable portfolio)
 *
 * @see LoanAccount
 * @see com.openfinova.banking.loan.api.entity.ProvisionStage
 * @see com.openfinova.banking.loan.api.entity.LoanStatus
 */
@Entity
@Table(name = "loan_provisions", indexes = {
        @Index(name = "idx_loan_provisions_account", columnList = "loan_account_id"),
        @Index(name = "idx_loan_provisions_date", columnList = "provision_date"),
        @Index(name = "idx_loan_provisions_stage", columnList = "provision_stage"),
        @Index(name = "idx_loan_provisions_posted", columnList = "is_posted") })
public class LoanProvision {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @NotNull(message = "Provision date is required")
    @Column(name = "provision_date", nullable = false)
    private LocalDate provisionDate;

    /**
     * IFRS 9 stage classification (Stage 1, 2, or 3).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "provision_stage", nullable = false, length = 30)
    @NotNull(message = "Provision stage is required")
    private ProvisionStage provisionStage;

    /**
     * Outstanding loan balance used for provision calculation.
     */
    @NotNull(message = "Outstanding balance is required")
    @DecimalMin(value = "0.0", message = "Outstanding balance must be positive")
    @Column(name = "outstanding_balance", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingBalance;

    /**
     * Provision rate as a percentage.
     */
    @NotNull(message = "Provision rate is required")
    @DecimalMin(value = "0.0", message = "Provision rate must be positive")
    @DecimalMax(value = "100.0", message = "Provision rate cannot exceed 100%")
    @Column(name = "provision_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal provisionRate;

    /**
     * Calculated provision amount.
     */
    @NotNull(message = "Provision amount is required")
    @DecimalMin(value = "0.0", message = "Provision amount must be positive")
    @Column(name = "provision_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal provisionAmount;

    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    @Column(name = "is_posted", nullable = false)
    private Boolean isPosted = false;

    @Column(name = "posted_at")
    private Instant postedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    // Constructors
    public LoanProvision() {
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

    public LocalDate getProvisionDate() {
        return provisionDate;
    }

    public void setProvisionDate(LocalDate provisionDate) {
        this.provisionDate = provisionDate;
    }

    public ProvisionStage getProvisionStage() {
        return provisionStage;
    }

    public void setProvisionStage(ProvisionStage provisionStage) {
        this.provisionStage = provisionStage;
    }

    public BigDecimal getOutstandingBalance() {
        return outstandingBalance;
    }

    public void setOutstandingBalance(BigDecimal outstandingBalance) {
        this.outstandingBalance = outstandingBalance;
    }

    public BigDecimal getProvisionRate() {
        return provisionRate;
    }

    public void setProvisionRate(BigDecimal provisionRate) {
        this.provisionRate = provisionRate;
    }

    public BigDecimal getProvisionAmount() {
        return provisionAmount;
    }

    public void setProvisionAmount(BigDecimal provisionAmount) {
        this.provisionAmount = provisionAmount;
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
