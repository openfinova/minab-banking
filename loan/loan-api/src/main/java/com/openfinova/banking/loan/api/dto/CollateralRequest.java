package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.CollateralType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for registering collateral.
 */
public class CollateralRequest {

    @NotNull(message = "Loan account ID is required")
    private UUID loanAccountId;

    @NotNull(message = "Collateral type is required")
    private CollateralType collateralType;

    @NotBlank(message = "Description is required")
    private String description;

    @NotNull(message = "Valuation amount is required")
    @DecimalMin(value = "0.01", message = "Valuation amount must be greater than 0")
    private BigDecimal valuationAmount;

    @NotBlank(message = "Currency is required")
    private String currency;

    @NotNull(message = "Valuation date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate valuationDate;

    private String valuedBy;

    private String location;

    private String registrationNumber;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate insuranceExpiryDate;

    private String insurancePolicyNumber;

    private String remarks;

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public CollateralType getCollateralType() {
        return collateralType;
    }

    public void setCollateralType(CollateralType collateralType) {
        this.collateralType = collateralType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getValuationAmount() {
        return valuationAmount;
    }

    public void setValuationAmount(BigDecimal valuationAmount) {
        this.valuationAmount = valuationAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getValuationDate() {
        return valuationDate;
    }

    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }

    public String getValuedBy() {
        return valuedBy;
    }

    public void setValuedBy(String valuedBy) {
        this.valuedBy = valuedBy;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public LocalDate getInsuranceExpiryDate() {
        return insuranceExpiryDate;
    }

    public void setInsuranceExpiryDate(LocalDate insuranceExpiryDate) {
        this.insuranceExpiryDate = insuranceExpiryDate;
    }

    public String getInsurancePolicyNumber() {
        return insurancePolicyNumber;
    }

    public void setInsurancePolicyNumber(String insurancePolicyNumber) {
        this.insurancePolicyNumber = insurancePolicyNumber;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
