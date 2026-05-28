package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.ApplicationStatus;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
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
 * Loan Application entity representing a customer's request for a loan.
 * Tracks the application workflow from submission through approval/rejection.
 */
@Entity
@Table(name = "loan_applications", indexes = {
        @Index(name = "idx_loan_applications_number", columnList = "application_number"),
        @Index(name = "idx_loan_applications_customer", columnList = "customer_id"),
        @Index(name = "idx_loan_applications_product", columnList = "product_id"),
        @Index(name = "idx_loan_applications_status", columnList = "status"),
        @Index(name = "idx_loan_applications_created", columnList = "created_at") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoanApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Unique application number for tracking and reference.
     * Generated automatically upon creation.
     */
    @NotBlank(message = "Application number is required")
    @Column(name = "application_number", nullable = false, unique = true, length = 50)
    @Size(max = 50, message = "Application number must not exceed 50 characters")
    private String applicationNumber;

    /**
     * Reference to the customer applying for the loan.
     * Links to Customer module without direct entity relationship.
     */
    @NotNull(message = "Customer ID is required")
    @Column(name = "customer_id", nullable = false)
    private UUID customerId;

    /**
     * Reference to the loan product being applied for.
     */
    @NotNull(message = "Product ID is required")
    @Column(name = "product_id", nullable = false)
    private UUID productId;

    /**
     * Amount requested by the customer.
     */
    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "0.0", message = "Requested amount must be positive")
    @Column(name = "requested_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal requestedAmount;

    /**
     * Loan tenor requested in months.
     */
    @NotNull(message = "Requested tenor is required")
    @Min(value = 1, message = "Requested tenor must be at least 1 month")
    @Column(name = "requested_tenor_months", nullable = false)
    private Integer requestedTenorMonths;

    /**
     * Three-letter ISO currency code.
     */
    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull(message = "Application status is required")
    private ApplicationStatus status = ApplicationStatus.DRAFT;

    /**
     * Purpose or reason for the loan.
     */
    @Column(length = 100)
    @Size(max = 100, message = "Purpose must not exceed 100 characters")
    private String purpose;

    /**
     * Applicant's monthly income for affordability assessment.
     */
    @DecimalMin(value = "0.0", message = "Monthly income must be positive")
    @Column(name = "monthly_income", precision = 19, scale = 4)
    private BigDecimal monthlyIncome;

    /**
     * Applicant's existing financial obligations (other loans, credit cards, etc.).
     */
    @DecimalMin(value = "0.0", message = "Existing obligations must be positive")
    @Column(name = "existing_obligations", precision = 19, scale = 4)
    private BigDecimal existingObligations;

    /**
     * Credit score from credit bureau or internal scoring model.
     * Typically ranges from 0 to 1000.
     */
    @Min(value = 0, message = "Credit score cannot be negative")
    @Max(value = 1000, message = "Credit score cannot exceed 1000")
    @Column(name = "credit_score", precision = 5, scale = 2)
    private BigDecimal creditScore;

    /**
     * Risk rating assigned during underwriting (e.g., "A", "B", "C", "D").
     */
    @Column(name = "risk_rating", length = 20)
    @Size(max = 20, message = "Risk rating must not exceed 20 characters")
    private String riskRating;

    /**
     * Interest rate approved by underwriter (may differ from requested).
     */
    @DecimalMin(value = "0.0", message = "Approved interest rate must be positive")
    @Column(name = "approved_interest_rate", precision = 5, scale = 2)
    private BigDecimal approvedInterestRate;

    /**
     * Loan amount approved (may differ from requested amount).
     */
    @DecimalMin(value = "0.0", message = "Approved amount must be positive")
    @Column(name = "approved_amount", precision = 19, scale = 4)
    private BigDecimal approvedAmount;

    /**
     * Tenor approved in months (may differ from requested).
     */
    @Min(value = 1, message = "Approved tenor must be at least 1 month")
    @Column(name = "approved_tenor_months")
    private Integer approvedTenorMonths;

    @Column(name = "approval_date")
    private LocalDate approvalDate;

    /**
     * Username or ID of the person who approved the application.
     */
    @Column(name = "approved_by", length = 100)
    @Size(max = 100, message = "Approved by must not exceed 100 characters")
    private String approvedBy;

    @Column(name = "rejection_date")
    private LocalDate rejectionDate;

    @Column(name = "rejection_reason", length = 500)
    @Size(max = 500, message = "Rejection reason must not exceed 500 characters")
    private String rejectionReason;

    /**
     * Username or ID of the person who rejected the application.
     */
    @Column(name = "rejected_by", length = 100)
    @Size(max = 100, message = "Rejected by must not exceed 100 characters")
    private String rejectedBy;

    /**
     * Number of guarantors required for this loan application.
     * Based on product configuration and underwriting decision.
     */
    @Min(value = 0, message = "Number of guarantors cannot be negative")
    @Column(name = "guarantors_required")
    private Integer guarantorsRequired = 0;

    /**
     * Guarantors provided for this loan application.
     * These will be transferred to the loan account upon approval.
     */
    @OneToMany(mappedBy = "loanApplication", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Guarantor> guarantors = new ArrayList<>();

    /**
     * ID or username of the underwriter assigned to review this application.
     */
    @Column(name = "underwriter_id", length = 100)
    @Size(max = 100, message = "Underwriter ID must not exceed 100 characters")
    private String underwriterId;

    /**
     * Username or ID of the person who assigned the application to the underwriter.
     */
    @Column(name = "underwriter_assigned_by", length = 100)
    @Size(max = 100, message = "Underwriter assigned by must not exceed 100 characters")
    private String underwriterAssignedBy;

    /**
     * Timestamp when the application was assigned to an underwriter.
     */
    @Column(name = "underwriter_assigned_at")
    private Instant underwriterAssignedAt;

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
    public LoanApplication() {
    }

    public LoanApplication(UUID customerId, UUID productId, BigDecimal requestedAmount, Integer requestedTenorMonths,
            String currency) {
        this.customerId = customerId;
        this.productId = productId;
        this.requestedAmount = requestedAmount;
        this.requestedTenorMonths = requestedTenorMonths;
        this.currency = currency;
    }

    // Business Logic
    public boolean isApproved() {
        return ApplicationStatus.APPROVED.equals(status);
    }

    public boolean isRejected() {
        return ApplicationStatus.REJECTED.equals(status);
    }

    public boolean isPending() {
        return ApplicationStatus.SUBMITTED.equals(status) || ApplicationStatus.UNDER_REVIEW.equals(status)
                || ApplicationStatus.UNDERWRITING.equals(status);
    }

    public void approve(BigDecimal approvedAmount, Integer approvedTenorMonths, BigDecimal approvedRate,
            String approvedBy, LocalDate approvalDate) {
        this.status = ApplicationStatus.APPROVED;
        this.approvedAmount = approvedAmount;
        this.approvedTenorMonths = approvedTenorMonths;
        this.approvedInterestRate = approvedRate;
        this.approvalDate = approvalDate;
        this.approvedBy = approvedBy;
    }

    public void reject(String reason, String rejectedBy, LocalDate rejectionDate) {
        this.status = ApplicationStatus.REJECTED;
        this.rejectionReason = reason;
        this.rejectionDate = rejectionDate;
        this.rejectedBy = rejectedBy;
    }

    public boolean hasRequiredGuarantors() {
        return getGuarantorsProvided() >= (guarantorsRequired != null ? guarantorsRequired : 0);
    }

    /**
     * Gets the number of guarantors actually provided for this application.
     * Calculated from the guarantors list.
     *
     * @return count of guarantors
     */
    public int getGuarantorsProvided() {
        return guarantors != null ? guarantors.size() : 0;
    }

    /**
     * Gets the number of active guarantors for this application.
     *
     * @return count of active guarantors
     */
    public int getActiveGuarantorsCount() {
        return guarantors != null ? (int) guarantors.stream().filter(Guarantor::isActive).count() : 0;
    }

    /**
     * Adds a guarantor to this loan application.
     *
     * @param guarantor the guarantor to add
     */
    public void addGuarantor(Guarantor guarantor) {
        if (guarantors == null) {
            guarantors = new ArrayList<>();
        }
        guarantors.add(guarantor);
        guarantor.setLoanApplication(this);
    }

    /**
     * Removes a guarantor from this loan application.
     *
     * @param guarantor the guarantor to remove
     */
    public void removeGuarantor(Guarantor guarantor) {
        if (guarantors != null) {
            guarantors.remove(guarantor);
            guarantor.setLoanApplication(null);
        }
    }

    /**
     * Validates if a state transition is allowed based on the current status.
     *
     * @param targetStatus the status to transition to
     * @return true if the transition is allowed, false otherwise
     */
    public boolean canTransitionTo(ApplicationStatus targetStatus) {
        if (this.status == null || targetStatus == null) {
            return false;
        }

        // Same status is always allowed (idempotent)
        if (this.status == targetStatus) {
            return true;
        }

        return switch (this.status) {
            case DRAFT -> targetStatus == ApplicationStatus.SUBMITTED || targetStatus == ApplicationStatus.WITHDRAWN;

            case SUBMITTED ->
                targetStatus == ApplicationStatus.UNDER_REVIEW || targetStatus == ApplicationStatus.PENDING_DOCUMENTS
                        || targetStatus == ApplicationStatus.REJECTED || targetStatus == ApplicationStatus.WITHDRAWN;

            case UNDER_REVIEW ->
                targetStatus == ApplicationStatus.UNDERWRITING || targetStatus == ApplicationStatus.PENDING_DOCUMENTS
                        || targetStatus == ApplicationStatus.REJECTED || targetStatus == ApplicationStatus.WITHDRAWN;

            case PENDING_DOCUMENTS ->
                targetStatus == ApplicationStatus.UNDER_REVIEW || targetStatus == ApplicationStatus.REJECTED
                        || targetStatus == ApplicationStatus.WITHDRAWN || targetStatus == ApplicationStatus.EXPIRED;

            case UNDERWRITING -> targetStatus == ApplicationStatus.APPROVED
                    || targetStatus == ApplicationStatus.REJECTED || targetStatus == ApplicationStatus.PENDING_DOCUMENTS
                    || targetStatus == ApplicationStatus.WITHDRAWN;

            case APPROVED -> false; // Terminal state - no transitions allowed

            case REJECTED -> false; // Terminal state - no transitions allowed

            case WITHDRAWN -> false; // Terminal state - no transitions allowed

            case EXPIRED -> false; // Terminal state - no transitions allowed
        };
    }

    /**
     * Gets a human-readable error message for invalid state transitions.
     *
     * @param targetStatus the status attempting to transition to
     * @return error message describing why the transition is not allowed
     */
    public String getTransitionErrorMessage(ApplicationStatus targetStatus) {
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

    public String getApplicationNumber() {
        return applicationNumber;
    }

    public void setApplicationNumber(String applicationNumber) {
        this.applicationNumber = applicationNumber;
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

    public BigDecimal getRequestedAmount() {
        return requestedAmount;
    }

    public void setRequestedAmount(BigDecimal requestedAmount) {
        this.requestedAmount = requestedAmount;
    }

    public Integer getRequestedTenorMonths() {
        return requestedTenorMonths;
    }

    public void setRequestedTenorMonths(Integer requestedTenorMonths) {
        this.requestedTenorMonths = requestedTenorMonths;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public ApplicationStatus getStatus() {
        return status;
    }

    public void setStatus(ApplicationStatus status) {
        this.status = status;
    }

    public String getPurpose() {
        return purpose;
    }

    public void setPurpose(String purpose) {
        this.purpose = purpose;
    }

    public BigDecimal getMonthlyIncome() {
        return monthlyIncome;
    }

    public void setMonthlyIncome(BigDecimal monthlyIncome) {
        this.monthlyIncome = monthlyIncome;
    }

    public BigDecimal getExistingObligations() {
        return existingObligations;
    }

    public void setExistingObligations(BigDecimal existingObligations) {
        this.existingObligations = existingObligations;
    }

    public BigDecimal getCreditScore() {
        return creditScore;
    }

    public void setCreditScore(BigDecimal creditScore) {
        this.creditScore = creditScore;
    }

    public String getRiskRating() {
        return riskRating;
    }

    public void setRiskRating(String riskRating) {
        this.riskRating = riskRating;
    }

    public BigDecimal getApprovedInterestRate() {
        return approvedInterestRate;
    }

    public void setApprovedInterestRate(BigDecimal approvedInterestRate) {
        this.approvedInterestRate = approvedInterestRate;
    }

    public BigDecimal getApprovedAmount() {
        return approvedAmount;
    }

    public void setApprovedAmount(BigDecimal approvedAmount) {
        this.approvedAmount = approvedAmount;
    }

    public Integer getApprovedTenorMonths() {
        return approvedTenorMonths;
    }

    public void setApprovedTenorMonths(Integer approvedTenorMonths) {
        this.approvedTenorMonths = approvedTenorMonths;
    }

    public LocalDate getApprovalDate() {
        return approvalDate;
    }

    public void setApprovalDate(LocalDate approvalDate) {
        this.approvalDate = approvalDate;
    }

    public String getApprovedBy() {
        return approvedBy;
    }

    public void setApprovedBy(String approvedBy) {
        this.approvedBy = approvedBy;
    }

    public LocalDate getRejectionDate() {
        return rejectionDate;
    }

    public void setRejectionDate(LocalDate rejectionDate) {
        this.rejectionDate = rejectionDate;
    }

    public String getRejectionReason() {
        return rejectionReason;
    }

    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }

    public String getRejectedBy() {
        return rejectedBy;
    }

    public void setRejectedBy(String rejectedBy) {
        this.rejectedBy = rejectedBy;
    }

    public Integer getGuarantorsRequired() {
        return guarantorsRequired;
    }

    public void setGuarantorsRequired(Integer guarantorsRequired) {
        this.guarantorsRequired = guarantorsRequired;
    }

    public List<Guarantor> getGuarantors() {
        return guarantors;
    }

    public void setGuarantors(List<Guarantor> guarantors) {
        this.guarantors = guarantors;
    }

    public String getUnderwriterId() {
        return underwriterId;
    }

    public void setUnderwriterId(String underwriterId) {
        this.underwriterId = underwriterId;
    }

    public String getUnderwriterAssignedBy() {
        return underwriterAssignedBy;
    }

    public void setUnderwriterAssignedBy(String underwriterAssignedBy) {
        this.underwriterAssignedBy = underwriterAssignedBy;
    }

    public Instant getUnderwriterAssignedAt() {
        return underwriterAssignedAt;
    }

    public void setUnderwriterAssignedAt(Instant underwriterAssignedAt) {
        this.underwriterAssignedAt = underwriterAssignedAt;
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
