package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.ApplicationStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for loan applications.
 */
public class LoanApplicationResponse {

    private UUID id;
    private String applicationNumber;
    private UUID customerId;
    private UUID productId;
    private BigDecimal requestedAmount;
    private Integer requestedTenorMonths;
    private String currency;
    private ApplicationStatus status;
    private String purpose;
    private BigDecimal monthlyIncome;
    private BigDecimal existingObligations;
    private BigDecimal creditScore;
    private String riskRating;
    private BigDecimal approvedInterestRate;
    private BigDecimal approvedAmount;
    private Integer approvedTenorMonths;
    private LocalDate approvalDate;
    private String approvedBy;
    private LocalDate rejectionDate;
    private String rejectionReason;
    private String rejectedBy;
    private Integer guarantorsRequired;
    private String underwriterId;
    private String underwriterAssignedBy;
    private Instant underwriterAssignedAt;
    private String remarks;
    private Instant createdAt;
    private Instant updatedAt;

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
