package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.PaymentMethod;
import com.openfinova.banking.loan.api.entity.PaymentType;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Request DTO for recording a loan payment with manual allocation.
 */
public class LoanPaymentAllocationRequest {

    @NotNull(message = "Loan account ID is required")
    private UUID loanAccountId;

    @NotNull(message = "Payment amount is required")
    @DecimalMin(value = "0.01", message = "Payment amount must be greater than 0")
    private BigDecimal paymentAmount;

    @NotNull(message = "Principal paid is required")
    @DecimalMin(value = "0.0", message = "Principal paid cannot be negative")
    private BigDecimal principalPaid;

    @NotNull(message = "Interest paid is required")
    @DecimalMin(value = "0.0", message = "Interest paid cannot be negative")
    private BigDecimal interestPaid;

    @NotNull(message = "Fees paid is required")
    @DecimalMin(value = "0.0", message = "Fees paid cannot be negative")
    private BigDecimal feesPaid;

    @NotNull(message = "Penalties paid is required")
    @DecimalMin(value = "0.0", message = "Penalties paid cannot be negative")
    private BigDecimal penaltiesPaid;

    @NotNull(message = "Payment date is required")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDate;

    @NotNull(message = "Payment type is required")
    private PaymentType paymentType;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;

    private String transactionReference;

    private String remarks;

    // Getters and Setters
    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public BigDecimal getPrincipalPaid() {
        return principalPaid;
    }

    public void setPrincipalPaid(BigDecimal principalPaid) {
        this.principalPaid = principalPaid;
    }

    public BigDecimal getInterestPaid() {
        return interestPaid;
    }

    public void setInterestPaid(BigDecimal interestPaid) {
        this.interestPaid = interestPaid;
    }

    public BigDecimal getFeesPaid() {
        return feesPaid;
    }

    public void setFeesPaid(BigDecimal feesPaid) {
        this.feesPaid = feesPaid;
    }

    public BigDecimal getPenaltiesPaid() {
        return penaltiesPaid;
    }

    public void setPenaltiesPaid(BigDecimal penaltiesPaid) {
        this.penaltiesPaid = penaltiesPaid;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
    }

    public PaymentType getPaymentType() {
        return paymentType;
    }

    public void setPaymentType(PaymentType paymentType) {
        this.paymentType = paymentType;
    }

    public PaymentMethod getPaymentMethod() {
        return paymentMethod;
    }

    public void setPaymentMethod(PaymentMethod paymentMethod) {
        this.paymentMethod = paymentMethod;
    }

    public String getTransactionReference() {
        return transactionReference;
    }

    public void setTransactionReference(String transactionReference) {
        this.transactionReference = transactionReference;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }
}
