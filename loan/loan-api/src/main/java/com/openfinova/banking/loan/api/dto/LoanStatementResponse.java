package com.openfinova.banking.loan.api.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Response DTO for loan statement information.
 */
public class LoanStatementResponse {

    private UUID loanAccountId;
    private String loanAccountNumber;
    private LocalDate fromDate;
    private LocalDate toDate;
    private BigDecimal openingBalance;
    private BigDecimal closingBalance;
    private BigDecimal principalPaid;
    private BigDecimal interestPaid;
    private BigDecimal feesPaid;
    private BigDecimal penaltiesPaid;
    private List<LoanStatementTransactionResponse> transactions;

    // Getters and Setters
    public UUID getLoanAccountId() {
        return loanAccountId;
    }

    public void setLoanAccountId(UUID loanAccountId) {
        this.loanAccountId = loanAccountId;
    }

    public String getLoanAccountNumber() {
        return loanAccountNumber;
    }

    public void setLoanAccountNumber(String loanAccountNumber) {
        this.loanAccountNumber = loanAccountNumber;
    }

    public LocalDate getFromDate() {
        return fromDate;
    }

    public void setFromDate(LocalDate fromDate) {
        this.fromDate = fromDate;
    }

    public LocalDate getToDate() {
        return toDate;
    }

    public void setToDate(LocalDate toDate) {
        this.toDate = toDate;
    }

    public BigDecimal getOpeningBalance() {
        return openingBalance;
    }

    public void setOpeningBalance(BigDecimal openingBalance) {
        this.openingBalance = openingBalance;
    }

    public BigDecimal getClosingBalance() {
        return closingBalance;
    }

    public void setClosingBalance(BigDecimal closingBalance) {
        this.closingBalance = closingBalance;
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

    public List<LoanStatementTransactionResponse> getTransactions() {
        return transactions;
    }

    public void setTransactions(List<LoanStatementTransactionResponse> transactions) {
        this.transactions = transactions;
    }
}
