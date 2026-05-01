package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.AmortizationType;
import com.openfinova.banking.loan.api.entity.DelinquencyBucket;
import com.openfinova.banking.loan.api.entity.InterestCalculationMethod;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.api.entity.RepaymentFrequency;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Loan Account entity representing an active loan.
 * Created after loan application approval and tracks the complete loan lifecycle.
 */
@Entity
@Table(name = "loan_accounts", indexes = {
        @Index(name = "idx_loan_accounts_number", columnList = "loan_account_number"),
        @Index(name = "idx_loan_accounts_customer", columnList = "customer_id"),
        @Index(name = "idx_loan_accounts_application", columnList = "application_id"),
        @Index(name = "idx_loan_accounts_product", columnList = "product_id"),
        @Index(name = "idx_loan_accounts_status", columnList = "status"),
        @Index(name = "idx_loan_accounts_disbursement", columnList = "disbursement_date"),
        @Index(name = "idx_loan_accounts_maturity", columnList = "maturity_date"),
        @Index(name = "idx_loan_accounts_delinquency", columnList = "days_past_due") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
@BatchSize(size = 16)
public class LoanAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version")
    private Long version;

    /**
     * Unique loan account number for identification and transactions.
     * Generated automatically upon account creation.
     */
    @NotBlank(message = "Loan account number is required")
    @Column(name = "loan_account_number", nullable = false, unique = true, length = 50)
    @Size(max = 50, message = "Loan account number must not exceed 50 characters")
    private String loanAccountNumber;

    /**
     * Reference to the approved loan application.
     */
    @NotNull(message = "Application ID is required")
    @Column(name = "application_id", nullable = false)
    private UUID applicationId;

    /**
     * Reference to the customer who owns this loan.
     */
    @NotNull(message = "Customer ID is required")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /**
     * Reference to the loan product.
     */
    @NotNull(message = "Product ID is required")
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /**
     * Original principal amount disbursed.
     */
    @NotNull(message = "Principal amount is required")
    @DecimalMin(value = "0.0", message = "Principal amount must be positive")
    @Column(name = "principal_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal principalAmount;

    /**
     * Current outstanding principal balance.
     * Reduced as principal payments are made.
     */
    @NotNull(message = "Outstanding principal is required")
    @DecimalMin(value = "0.0", message = "Outstanding principal must be positive")
    @Column(name = "outstanding_principal", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingPrincipal;

    /**
     * Accrued interest not yet paid.
     */
    @NotNull(message = "Outstanding interest is required")
    @DecimalMin(value = "0.0", message = "Outstanding interest must be positive")
    @Column(name = "outstanding_interest", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingInterest = BigDecimal.ZERO;

    /**
     * Outstanding fees (processing, late payment, etc.).
     */
    @NotNull(message = "Outstanding fees is required")
    @DecimalMin(value = "0.0", message = "Outstanding fees must be positive")
    @Column(name = "outstanding_fees", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingFees = BigDecimal.ZERO;

    /**
     * Outstanding penalties for late payments or other violations.
     */
    @NotNull(message = "Outstanding penalties is required")
    @DecimalMin(value = "0.0", message = "Outstanding penalties must be positive")
    @Column(name = "outstanding_penalties", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingPenalties = BigDecimal.ZERO;

    /**
     * Loan tenor in months.
     */
    @NotNull(message = "Tenor is required")
    @Min(value = 1, message = "Tenor must be at least 1 month")
    @Column(name = "tenor_months", nullable = false)
    private Integer tenorMonths;

    /**
     * Annual interest rate as a percentage.
     */
    @NotNull(message = "Interest rate is required")
    @DecimalMin(value = "0.0", message = "Interest rate must be positive")
    @DecimalMax(value = "100.0", message = "Interest rate cannot exceed 100%")
    @Column(name = "interest_rate", nullable = false, precision = 5, scale = 2)
    private BigDecimal interestRate;

    @Enumerated(EnumType.STRING)
    @Column(name = "interest_calculation_method", nullable = false, length = 30)
    @NotNull(message = "Interest calculation method is required")
    private InterestCalculationMethod interestCalculationMethod;

    @Enumerated(EnumType.STRING)
    @Column(name = "repayment_frequency", nullable = false, length = 30)
    @NotNull(message = "Repayment frequency is required")
    private RepaymentFrequency repaymentFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "amortization_type", nullable = false, length = 30)
    @NotNull(message = "Amortization type is required")
    private AmortizationType amortizationType;

    /**
     * Three-letter ISO currency code.
     */
    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull(message = "Loan status is required")
    private LoanStatus status = LoanStatus.PENDING_APPROVAL;

    /**
     * Date when the loan was disbursed to the customer.
     */
    @NotNull(message = "Disbursement date is required")
    @Column(name = "disbursement_date", nullable = false)
    private LocalDate disbursementDate;

    /**
     * Date when the loan is expected to be fully repaid.
     */
    @NotNull(message = "Maturity date is required")
    @Column(name = "maturity_date", nullable = false)
    private LocalDate maturityDate;

    /**
     * Date of the first scheduled payment.
     */
    @Column(name = "first_payment_date")
    private LocalDate firstPaymentDate;

    /**
     * Date of the most recent payment received.
     */
    @Column(name = "last_payment_date")
    private LocalDate lastPaymentDate;

    /**
     * Business date the loan was closed (fully repaid or administratively closed).
     */
    @Column(name = "closed_date")
    private LocalDate closedDate;

    /**
     * Total amount paid towards this loan (principal + interest + fees + penalties).
     */
    @DecimalMin(value = "0.0", message = "Total paid must be positive")
    @Column(name = "total_paid", precision = 19, scale = 4)
    private BigDecimal totalPaid = BigDecimal.ZERO;

    /**
     * Number of days the loan payment is overdue.
     * 0 means current, >0 means delinquent.
     */
    @Min(value = 0, message = "Days past due cannot be negative")
    @Column(name = "days_past_due", nullable = false)
    private Integer daysPastDue = 0;

    /**
     * Delinquency classification bucket.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "delinquency_bucket", length = 30)
    private DelinquencyBucket delinquencyBucket;

    /**
     * Whether this loan has been restructured.
     */
    @Column(name = "is_restructured", nullable = false)
    private Boolean isRestructured = false;

    @Column(name = "restructured_date")
    private LocalDate restructuredDate;

    /**
     * Whether this loan is a top-up of an existing loan.
     */
    @Column(name = "is_top_up", nullable = false)
    private Boolean isTopUp = false;

    /**
     * Reference to the original loan if this is a top-up.
     */
    @Column(name = "original_loan_id")
    private UUID originalLoanId;

    /**
     * Guarantors for this loan account.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Guarantor> guarantors = new ArrayList<>();

    /**
     * Payment schedules for this loan.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<LoanSchedule> schedules = new ArrayList<>();

    /**
     * Payments made towards this loan.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<LoanPayment> payments = new ArrayList<>();

    /**
     * All transactions on this loan account.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanTransaction> transactions = new ArrayList<>();

    /**
     * Disbursements for this loan.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<LoanDisbursement> disbursements = new ArrayList<>();

    /**
     * Collateral pledged for this loan.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<Collateral> collaterals = new ArrayList<>();

    /**
     * Documents associated with this loan.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<LoanDocument> documents = new ArrayList<>();

    /**
     * Fees charged on this loan.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<LoanFee> fees = new ArrayList<>();

    /**
     * Interest accrual records.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<InterestAccrual> interestAccruals = new ArrayList<>();

    /**
     * Provision records for this loan.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<LoanProvision> provisions = new ArrayList<>();

    /**
     * Restructuring history.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<LoanRestructuring> restructurings = new ArrayList<>();

    /**
     * Early settlement quotes.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<EarlySettlement> earlySettlements = new ArrayList<>();

    /**
     * Collection activities for this loan.
     */
    @OneToMany(mappedBy = "loanAccount", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
    private List<CollectionActivity> collectionActivities = new ArrayList<>();

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
    public LoanAccount() {
    }

    public LoanAccount(UUID applicationId, UUID customerId, UUID productId, BigDecimal principalAmount,
            String currency) {
        this.applicationId = applicationId;
        this.customerId = customerId;
        this.productId = productId;
        this.principalAmount = principalAmount;
        this.outstandingPrincipal = principalAmount;
        this.currency = currency;
    }

    // Business Logic
    public boolean isActive() {
        return LoanStatus.ACTIVE.equals(status);
    }

    public boolean isDelinquent() {
        return daysPastDue != null && daysPastDue > 0;
    }

    public boolean isClosed() {
        return LoanStatus.CLOSED.equals(status) || LoanStatus.SETTLED.equals(status);
    }

    public BigDecimal getTotalOutstanding() {
        return outstandingPrincipal.add(outstandingInterest).add(outstandingFees).add(outstandingPenalties);
    }

    /**
     * Checks if this loan has any guarantors.
     *
     * @return true if loan has guarantors, false otherwise
     */
    public boolean hasGuarantors() {
        return guarantors != null && !guarantors.isEmpty();
    }

    /**
     * Gets the count of all guarantors (active and inactive).
     *
     * @return number of guarantors
     */
    public int getGuarantorCount() {
        return guarantors != null ? guarantors.size() : 0;
    }

    /**
     * Gets the count of active guarantors only.
     *
     * @return number of active guarantors
     */
    public int getActiveGuarantorCount() {
        return guarantors != null ? (int) guarantors.stream().filter(Guarantor::isActive).count() : 0;
    }

    /**
     * Adds a guarantor to this loan account.
     *
     * @param guarantor the guarantor to add
     */
    public void addGuarantor(Guarantor guarantor) {
        guarantors.add(guarantor);
        guarantor.setLoanAccount(this);
    }

    /**
     * Removes a guarantor from this loan account.
     *
     * @param guarantor the guarantor to remove
     */
    public void removeGuarantor(Guarantor guarantor) {
        guarantors.remove(guarantor);
        guarantor.setLoanAccount(null);
    }

    /**
     * Gets all active guarantors for this loan.
     *
     * @return list of active guarantors
     */
    public List<Guarantor> getActiveGuarantors() {
        return guarantors.stream().filter(Guarantor::isActive).toList();
    }

    /**
     * Validates if a state transition is allowed based on the current status.
     *
     * @param targetStatus the status to transition to
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canTransitionTo(LoanStatus targetStatus) {
        if (this.status == null || targetStatus == null) {
            return false;
        }

        // Same status is always allowed (idempotent)
        if (this.status == targetStatus) {
            return true;
        }

        return switch (this.status) {
            case PENDING_APPROVAL -> targetStatus == LoanStatus.APPROVED;

            case APPROVED -> targetStatus == LoanStatus.ACTIVE || targetStatus == LoanStatus.SUSPENDED;

            case ACTIVE -> targetStatus == LoanStatus.SUSPENDED || targetStatus == LoanStatus.CLOSED
                    || targetStatus == LoanStatus.WRITTEN_OFF || targetStatus == LoanStatus.RESTRUCTURED
                    || targetStatus == LoanStatus.SETTLED;

            case SUSPENDED -> targetStatus == LoanStatus.ACTIVE || targetStatus == LoanStatus.WRITTEN_OFF
                    || targetStatus == LoanStatus.RESTRUCTURED;

            case RESTRUCTURED -> targetStatus == LoanStatus.ACTIVE || targetStatus == LoanStatus.SUSPENDED
                    || targetStatus == LoanStatus.CLOSED || targetStatus == LoanStatus.WRITTEN_OFF
                    || targetStatus == LoanStatus.SETTLED;

            case CLOSED -> false; // Terminal state - no transitions allowed

            case WRITTEN_OFF -> false; // Terminal state - no transitions allowed

            case SETTLED -> false; // Terminal state - no transitions allowed
        };
    }

    /**
     * Gets a human-readable error message for invalid state transitions.
     *
     * @param targetStatus the status attempting to transition to
     * @return error message describing why the transition is not allowed
     */
    public String getTransitionErrorMessage(LoanStatus targetStatus) {
        if (this.status == null) {
            return "Current status is null";
        }
        if (targetStatus == null) {
            return "Target status is null";
        }
        if (this.status == targetStatus) {
            return "Already in " + targetStatus + " status";
        }

        return String
                .format("Cannot transition from %s to %s. Current status: %s", this.status, targetStatus, this.status);
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getLoanAccountNumber() {
        return loanAccountNumber;
    }

    public void setLoanAccountNumber(String loanAccountNumber) {
        this.loanAccountNumber = loanAccountNumber;
    }

    public UUID getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(UUID applicationId) {
        this.applicationId = applicationId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public UUID getProductId() {
        return productId;
    }

    public void setProductId(UUID productId) {
        this.productId = productId;
    }

    public BigDecimal getPrincipalAmount() {
        return principalAmount;
    }

    public void setPrincipalAmount(BigDecimal principalAmount) {
        this.principalAmount = principalAmount;
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

    public BigDecimal getOutstandingPenalties() {
        return outstandingPenalties;
    }

    public void setOutstandingPenalties(BigDecimal outstandingPenalties) {
        this.outstandingPenalties = outstandingPenalties;
    }

    public Integer getTenorMonths() {
        return tenorMonths;
    }

    public void setTenorMonths(Integer tenorMonths) {
        this.tenorMonths = tenorMonths;
    }

    public BigDecimal getInterestRate() {
        return interestRate;
    }

    public void setInterestRate(BigDecimal interestRate) {
        this.interestRate = interestRate;
    }

    public InterestCalculationMethod getInterestCalculationMethod() {
        return interestCalculationMethod;
    }

    public void setInterestCalculationMethod(InterestCalculationMethod interestCalculationMethod) {
        this.interestCalculationMethod = interestCalculationMethod;
    }

    public RepaymentFrequency getRepaymentFrequency() {
        return repaymentFrequency;
    }

    public void setRepaymentFrequency(RepaymentFrequency repaymentFrequency) {
        this.repaymentFrequency = repaymentFrequency;
    }

    public AmortizationType getAmortizationType() {
        return amortizationType;
    }

    public void setAmortizationType(AmortizationType amortizationType) {
        this.amortizationType = amortizationType;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LoanStatus getStatus() {
        return status;
    }

    public void setStatus(LoanStatus status) {
        this.status = status;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    public LocalDate getMaturityDate() {
        return maturityDate;
    }

    public void setMaturityDate(LocalDate maturityDate) {
        this.maturityDate = maturityDate;
    }

    public LocalDate getFirstPaymentDate() {
        return firstPaymentDate;
    }

    public void setFirstPaymentDate(LocalDate firstPaymentDate) {
        this.firstPaymentDate = firstPaymentDate;
    }

    public LocalDate getLastPaymentDate() {
        return lastPaymentDate;
    }

    public void setLastPaymentDate(LocalDate lastPaymentDate) {
        this.lastPaymentDate = lastPaymentDate;
    }

    public LocalDate getClosedDate() {
        return closedDate;
    }

    public void setClosedDate(LocalDate closedDate) {
        this.closedDate = closedDate;
    }

    public BigDecimal getTotalPaid() {
        return totalPaid;
    }

    public void setTotalPaid(BigDecimal totalPaid) {
        this.totalPaid = totalPaid;
    }

    public Integer getDaysPastDue() {
        return daysPastDue;
    }

    public void setDaysPastDue(Integer daysPastDue) {
        this.daysPastDue = daysPastDue;
    }

    public DelinquencyBucket getDelinquencyBucket() {
        return delinquencyBucket;
    }

    public void setDelinquencyBucket(DelinquencyBucket delinquencyBucket) {
        this.delinquencyBucket = delinquencyBucket;
    }

    public Boolean getIsRestructured() {
        return isRestructured;
    }

    public void setIsRestructured(Boolean isRestructured) {
        this.isRestructured = isRestructured;
    }

    public LocalDate getRestructuredDate() {
        return restructuredDate;
    }

    public void setRestructuredDate(LocalDate restructuredDate) {
        this.restructuredDate = restructuredDate;
    }

    public Boolean getIsTopUp() {
        return isTopUp;
    }

    public void setIsTopUp(Boolean isTopUp) {
        this.isTopUp = isTopUp;
    }

    public UUID getOriginalLoanId() {
        return originalLoanId;
    }

    public void setOriginalLoanId(UUID originalLoanId) {
        this.originalLoanId = originalLoanId;
    }

    public List<Guarantor> getGuarantors() {
        return guarantors;
    }

    public void setGuarantors(List<Guarantor> guarantors) {
        this.guarantors = guarantors;
    }

    public List<LoanSchedule> getSchedules() {
        return schedules;
    }

    public void setSchedules(List<LoanSchedule> schedules) {
        this.schedules = schedules;
    }

    public List<LoanPayment> getPayments() {
        return payments;
    }

    public void setPayments(List<LoanPayment> payments) {
        this.payments = payments;
    }

    public List<LoanTransaction> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<LoanTransaction> transactions) {
        this.transactions = transactions;
    }

    public List<LoanDisbursement> getDisbursements() {
        return disbursements;
    }

    public void setDisbursements(List<LoanDisbursement> disbursements) {
        this.disbursements = disbursements;
    }

    public List<Collateral> getCollaterals() {
        return collaterals;
    }

    public void setCollaterals(List<Collateral> collaterals) {
        this.collaterals = collaterals;
    }

    public List<LoanDocument> getDocuments() {
        return documents;
    }

    public void setDocuments(List<LoanDocument> documents) {
        this.documents = documents;
    }

    public List<LoanFee> getFees() {
        return fees;
    }

    public void setFees(List<LoanFee> fees) {
        this.fees = fees;
    }

    public List<InterestAccrual> getInterestAccruals() {
        return interestAccruals;
    }

    public void setInterestAccruals(List<InterestAccrual> interestAccruals) {
        this.interestAccruals = interestAccruals;
    }

    public List<LoanProvision> getProvisions() {
        return provisions;
    }

    public void setProvisions(List<LoanProvision> provisions) {
        this.provisions = provisions;
    }

    public List<LoanRestructuring> getRestructurings() {
        return restructurings;
    }

    public void setRestructurings(List<LoanRestructuring> restructurings) {
        this.restructurings = restructurings;
    }

    public List<EarlySettlement> getEarlySettlements() {
        return earlySettlements;
    }

    public void setEarlySettlements(List<EarlySettlement> earlySettlements) {
        this.earlySettlements = earlySettlements;
    }

    public List<CollectionActivity> getCollectionActivities() {
        return collectionActivities;
    }

    public void setCollectionActivities(List<CollectionActivity> collectionActivities) {
        this.collectionActivities = collectionActivities;
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
