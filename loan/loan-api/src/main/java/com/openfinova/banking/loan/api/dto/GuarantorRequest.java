package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.openfinova.banking.loan.api.entity.GuarantorType;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for adding a guarantor.
 */
public class GuarantorRequest {

    @NotNull(message = "Loan account ID is required")
    private UUID loanAccountId;

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Guarantor type is required")
    private GuarantorType guarantorType;

    @NotNull(message = "Guaranteed amount is required")
    @DecimalMin(value = "0.01", message = "Guaranteed amount must be greater than 0")
    private BigDecimal guaranteedAmount;

    @DecimalMin(value = "0.0", message = "Guarantee percentage must be positive")
    @DecimalMax(value = "100.0", message = "Guarantee percentage cannot exceed 100%")
    private BigDecimal guaranteePercentage;

    private String relationshipToCustomer;
    private String remarks;
    private String addedBy;

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public GuarantorType getGuarantorType() {
        return guarantorType;
    }

    public void setGuarantorType(GuarantorType guarantorType) {
        this.guarantorType = guarantorType;
    }

    public BigDecimal getGuaranteedAmount() {
        return guaranteedAmount;
    }

    public void setGuaranteedAmount(BigDecimal guaranteedAmount) {
        this.guaranteedAmount = guaranteedAmount;
    }

    public BigDecimal getGuaranteePercentage() {
        return guaranteePercentage;
    }

    public void setGuaranteePercentage(BigDecimal guaranteePercentage) {
        this.guaranteePercentage = guaranteePercentage;
    }

    public String getRelationshipToCustomer() {
        return relationshipToCustomer;
    }

    public void setRelationshipToCustomer(String relationshipToCustomer) {
        this.relationshipToCustomer = relationshipToCustomer;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }
}
