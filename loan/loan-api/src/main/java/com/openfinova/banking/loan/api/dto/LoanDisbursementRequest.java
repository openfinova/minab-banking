package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import com.openfinova.banking.loan.api.entity.DisbursementMethod;
import org.springframework.format.annotation.DateTimeFormat;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Request DTO for creating a loan disbursement.
 */
public class LoanDisbursementRequest {

    @NotNull(message = "Loan account ID is required")
    private UUID loanAccountId;

    @NotNull(message = "Disbursement amount is required")
    @DecimalMin(value = "0.01", message = "Disbursement amount must be greater than 0")
    private BigDecimal disbursementAmount;

    @NotNull(message = "Disbursement date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate disbursementDate;

    @NotNull(message = "Disbursement method is required")
    private DisbursementMethod disbursementMethod;

    @NotBlank(message = "Destination account number is required")
    private String destinationAccountNumber;

    private String beneficiaryName;

    private String remarks;

    private String createdBy;

    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public BigDecimal getDisbursementAmount() {
        return disbursementAmount;
    }

    public void setDisbursementAmount(BigDecimal disbursementAmount) {
        this.disbursementAmount = disbursementAmount;
    }

    public LocalDate getDisbursementDate() {
        return disbursementDate;
    }

    public void setDisbursementDate(LocalDate disbursementDate) {
        this.disbursementDate = disbursementDate;
    }

    public DisbursementMethod getDisbursementMethod() {
        return disbursementMethod;
    }

    public void setDisbursementMethod(DisbursementMethod disbursementMethod) {
        this.disbursementMethod = disbursementMethod;
    }

    public String getDestinationAccountNumber() {
        return destinationAccountNumber;
    }

    public void setDestinationAccountNumber(String destinationAccountNumber) {
        this.destinationAccountNumber = destinationAccountNumber;
    }

    public String getBeneficiaryName() {
        return beneficiaryName;
    }

    public void setBeneficiaryName(String beneficiaryName) {
        this.beneficiaryName = beneficiaryName;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
