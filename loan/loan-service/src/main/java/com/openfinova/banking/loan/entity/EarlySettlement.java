package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.SettlementCalculationMethod;
import com.openfinova.banking.loan.api.entity.SettlementStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Represents an early settlement (prepayment/early payoff) of a loan.
 *
 * WHAT IS EARLY SETTLEMENT?
 * Early Settlement is when a borrower pays off their entire loan BEFORE the scheduled maturity date.
 * This entity tracks the calculation and execution of the payoff process.
 *
 * KEY CONCEPTS:
 * - Payoff Quote: A time-limited calculation of the exact amount needed to close the loan
 * - Interest Discount: Reduction in outstanding interest to incentivize early payment
 * - Prepayment Penalty: Fee charged for paying off early (compensates lender for lost interest)
 * - Quote Validity: Quotes expire because interest accrues daily
 *
 * SETTLEMENT AMOUNT CALCULATION:
 * Settlement Amount = Outstanding Principal
 *                   + Outstanding Interest (may be discounted)
 *                   + Outstanding Fees
 *                   + Outstanding Penalties
 *                   + Prepayment Penalty (if applicable)
 *
 * EXAMPLE SCENARIO:
 * Original Loan: $10,000 for 12 months at 12% APR
 * After 6 months, customer wants to pay off early:
 *
 * Outstanding Principal:  $5,143.97
 * Outstanding Interest:   $   50.00
 * Outstanding Fees:       $   25.00
 * Outstanding Penalties:  $    0.00
 * Subtotal:               $5,218.97
 *
 * With DISCOUNTED method:
 * Less: Interest Discount (10%): $    5.00
 * Add: Prepayment Penalty (1%):  $   51.44
 *
 * SETTLEMENT AMOUNT:      $5,265.41
 *
 * CALCULATION METHODS:
 * - FULL_OUTSTANDING: Pay full amount without any discount (standard method)
 * - DISCOUNTED: Get 10% discount on outstanding interest (incentive for early payment)
 *
 * WORKFLOW:
 * 1. Quote Generation: Customer requests payoff quote → System calculates settlement amount
 * 2. Quote Review: Customer reviews quote with validity period (typically 30 days)
 * 3. Quote Acceptance: Customer accepts and makes payment within validity period
 * 4. Settlement: Payment processed → Loan closed → Collateral/guarantors released
 * 5. Quote Expiration: If not used in time, new quote needed (interest has accrued)
 *
 * STATUS FLOW:
 * QUOTE → PENDING_APPROVAL → APPROVED → COMPLETED
 *   ↓           ↓
 * EXPIRED   REJECTED
 *   ↓           ↓
 * CANCELLED ← CANCELLED
 *
 * BUSINESS RULES:
 * - Quotes have expiration dates because interest accrues daily
 * - Discount calculation method depends on loan product configuration
 * - Prepayment penalties may not be allowed on certain loan types (regulatory)
 * - Penalties typically 1% of outstanding principal
 * - Customer has right to pay off loan early in most jurisdictions
 *
 * REGULATORY COMPLIANCE:
 * - Must provide accurate payoff quotes within mandated timeframe
 * - Quote must be clearly itemized showing all components
 * - Some jurisdictions mandate specific calculation methods
 * - Prepayment penalties must be disclosed upfront
 * - Audit trail required for all settlement calculations
 *
 * INTEGRATION POINTS:
 * - LoanAccount: Read balances, update status to SETTLED
 * - LoanPayment: Create final payment record
 * - LoanSchedule: Mark future schedules as WAIVED
 * - Collateral: Release collateral when loan settled
 * - Guarantor: Release guarantors when loan settled
 *
 * @see LoanAccount
 * @see com.openfinova.banking.loan.api.entity.SettlementCalculationMethod
 * @see com.openfinova.banking.loan.api.entity.SettlementStatus
 * @see LoanPayment
 * @see Collateral
 * @see Guarantor
 */
@Entity
@Table(name = "early_settlements", indexes = {
        @Index(name = "idx_early_settlements_reference", columnList = "quote_reference"),
        @Index(name = "idx_early_settlements_account", columnList = "loan_account_id"),
        @Index(name = "idx_early_settlements_quote_date", columnList = "quote_date"),
        @Index(name = "idx_early_settlements_valid_until", columnList = "valid_until"),
        @Index(name = "idx_early_settlements_status", columnList = "status") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class EarlySettlement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique quote reference for tracking.
     */
    @NotBlank(message = "Quote reference is required")
    @Column(name = "quote_reference", nullable = false, unique = true, length = 50)
    @Size(max = 50, message = "Quote reference must not exceed 50 characters")
    private String quoteReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @NotNull(message = "Quote date is required")
    @Column(name = "quote_date", nullable = false)
    private LocalDate quoteDate;

    /**
     * Date until which this quote is valid.
     */
    @NotNull(message = "Valid until date is required")
    @Column(name = "valid_until", nullable = false)
    private LocalDate validUntil;

    @NotNull(message = "Outstanding principal is required")
    @DecimalMin(value = "0.0", message = "Outstanding principal must be positive")
    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingPrincipal;

    @NotNull(message = "Outstanding interest is required")
    @DecimalMin(value = "0.0", message = "Outstanding interest must be positive")
    @Column(name = "outstanding_interest", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingInterest;

    @NotNull(message = "Outstanding fees is required")
    @DecimalMin(value = "0.0", message = "Outstanding fees must be positive")
    @Column(name = "outstanding_fees", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingFees;

    /**
     * Rebate amount for unearned interest (if applicable).
     */
    @NotNull(message = "Rebate amount is required")
    @DecimalMin(value = "0.0", message = "Rebate amount must be positive")
    @Column(name = "rebate_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal rebateAmount;

    /**
     * Penalty for early settlement (if applicable).
     */
    @NotNull(message = "Penalty amount is required")
    @DecimalMin(value = "0.0", message = "Penalty amount must be positive")
    @Column(name = "penalty_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal penaltyAmount;

    /**
     * Final settlement amount to be paid.
     */
    @NotNull(message = "Settlement amount is required")
    @DecimalMin(value = "0.0", message = "Settlement amount must be positive")
    @Column(name = "settlement_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal settlementAmount;

    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    /**
     * Method used to calculate rebate (Rule of 78, Actuarial, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_method", nullable = false, length = 30)
    @NotNull(message = "Calculation method is required")
    private SettlementCalculationMethod calculationMethod;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private SettlementStatus status = SettlementStatus.QUOTE;

    @Column(name = "settled_date")
    private LocalDate settledDate;

    @Column(name = "payment_reference", length = 100)
    @Size(max = 100, message = "Payment reference must not exceed 100 characters")
    private String paymentReference;

    /**
     * Date when the settlement was approved.
     */
    @Column(name = "approved_date")
    private LocalDate approvedDate;

    /**
     * User who approved the settlement.
     */
    @Column(name = "approved_by", length = 100)
    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    private String approvedBy;

    /**
     * Date when the settlement was rejected.
     */
    @Column(name = "rejected_date")
    private LocalDate rejectedDate;

    /**
     * User who rejected the settlement.
     */
    @Column(name = "rejected_by", length = 100)
    @Size(max = 100, message = "Rejected by must not exceed 100 characters")
    private String rejectedBy;

    /**
     * Reason for rejecting the settlement.
     */
    @Column(name = "rejection_reason", length = 500)
    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    private String rejectionReason;

    /**
     * Date when the settlement was cancelled.
     */
    @Column(name = "cancelled_date")
    private LocalDate cancelledDate;

    /**
     * User who cancelled the settlement.
     */
    @Column(name = "cancelled_by", length = 100)
    @Size(max = 100, message = "Cancelled by must not exceed 100 characters")
    private String cancelledBy;

    /**
     * Reason for cancelling the settlement.
     */
    @Column(name = "cancellation_reason", length = 500)
    @Size(max = 500, message = "Cancellation reason must not exceed 500 characters")
    private String cancellationReason;

    /**
     * General remarks or notes about the settlement.
     */
    @Column(length = 1000)
    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public EarlySettlement() {
    }

    // Business Logic
    public boolean isValid(LocalDate currentDate) {
        return validUntil != null && !validUntil.isBefore(currentDate) && SettlementStatus.QUOTE.equals(status);
    }

    public boolean isSettled() {
        return SettlementStatus.COMPLETED.equals(status);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getQuoteReference() {
        return quoteReference;
    }

    public void setQuoteReference(String quoteReference) {
        this.quoteReference = quoteReference;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public LocalDate getQuoteDate() {
        return quoteDate;
    }

    public void setQuoteDate(LocalDate quoteDate) {
        this.quoteDate = quoteDate;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public BigDecimal getOutstandingPrincipal() {
        return outstandingPrincipal;
    }

    public void setOutstandingPrincipal(BigDecimal outstandingPrincipal) {
        this.outstandingPrincipal = outstandingPrincipal;
    }

    public BigDecimal getOutstandingInterest() {
        return outstandingInterest;
    }

    public void setOutstandingInterest(BigDecimal outstandingInterest) {
        this.outstandingInterest = outstandingInterest;
    }

    public BigDecimal getOutstandingFees() {
        return outstandingFees;
    }

    public void setOutstandingFees(BigDecimal outstandingFees) {
        this.outstandingFees = outstandingFees;
    }

    public BigDecimal getRebateAmount() {
        return rebateAmount;
    }

    public void setRebateAmount(BigDecimal rebateAmount) {
        this.rebateAmount = rebateAmount;
    }

    public BigDecimal getPenaltyAmount() {
        return penaltyAmount;
    }

    public void setPenaltyAmount(BigDecimal penaltyAmount) {
        this.penaltyAmount = penaltyAmount;
    }

    public BigDecimal getSettlementAmount() {
        return settlementAmount;
    }

    public void setSettlementAmount(BigDecimal settlementAmount) {
        this.settlementAmount = settlementAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public SettlementCalculationMethod getCalculationMethod() {
        return calculationMethod;
    }

    public void setCalculationMethod(SettlementCalculationMethod calculationMethod) {
        this.calculationMethod = calculationMethod;
    }

    public SettlementStatus getStatus() {
        return status;
    }

    public void setStatus(SettlementStatus status) {
        this.status = status;
    }

    public LocalDate getSettledDate() {
        return settledDate;
    }

    public void setSettledDate(LocalDate settledDate) {
        this.settledDate = settledDate;
    }

    public String getPaymentReference() {
        return paymentReference;
    }

    public void setPaymentReference(String paymentReference) {
        this.paymentReference = paymentReference;
    }

    public LocalDate getApprovedDate() {
        return approvedDate;
    }

    public void setApprovedDate(LocalDate approvedDate) {
        this.approvedDate = approvedDate;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getRejectedDate() {
        return rejectedDate;
    }

    public void setRejectedDate(LocalDate rejectedDate) {
        this.rejectedDate = rejectedDate;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public LocalDate getCancelledDate() {
        return cancelledDate;
    }

    public void setCancelledDate(LocalDate cancelledDate) {
        this.cancelledDate = cancelledDate;
    }

    public String getCancelledBy() {
        return cancelledBy;
    }

    public void setCancelledBy(String cancelledBy) {
        this.cancelledBy = cancelledBy;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
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
