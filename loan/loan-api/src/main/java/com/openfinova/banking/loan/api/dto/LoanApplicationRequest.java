package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;
import java.util.UUID;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a loan application.
 */
public class LoanApplicationRequest {

    @NotNull(message = "Customer ID is required")
    private UUID customerId;

    @NotNull(message = "Product ID is required")
    private UUID productId;

    @NotNull(message = "Requested amount is required")
    @DecimalMin(value = "0.01", message = "Requested amount must be greater than 0")
    private BigDecimal requestedAmount;

    @NotNull(message = "Requested tenor is required")
    @Min(value = 1, message = "Requested tenor must be at least 1 month")
    private Integer requestedTenorMonths;

    @NotBlank(message = "Currency is required")
    private String currency;

    private String purpose;

    @DecimalMin(value = "0.0", message = "Monthly income must be positive")
    private BigDecimal monthlyIncome;

    @DecimalMin(value = "0.0", message = "Existing obligations must be positive")
    private BigDecimal existingObligations;

    private String remarks;

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

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
