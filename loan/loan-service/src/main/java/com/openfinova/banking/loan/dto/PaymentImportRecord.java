package com.openfinova.banking.loan.dto;

import com.openfinova.banking.loan.api.entity.PaymentMethod;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * One row from an external payment import file.
 */
public class PaymentImportRecord {
    private String loanAccountNumber;
    private BigDecimal paymentAmount;
    private LocalDate paymentDate;
    private PaymentMethod paymentMethod;
    private String transactionReference;

    public String getLoanAccountNumber() {
        return loanAccountNumber;
    }

    public void setLoanAccountNumber(String loanAccountNumber) {
        this.loanAccountNumber = loanAccountNumber;
    }

    public BigDecimal getPaymentAmount() {
        return paymentAmount;
    }

    public void setPaymentAmount(BigDecimal paymentAmount) {
        this.paymentAmount = paymentAmount;
    }

    public LocalDate getPaymentDate() {
        return paymentDate;
    }

    public void setPaymentDate(LocalDate paymentDate) {
        this.paymentDate = paymentDate;
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
}
