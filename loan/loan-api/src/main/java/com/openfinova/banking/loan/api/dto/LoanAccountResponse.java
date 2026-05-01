package com.openfinova.banking.loan.api.dto;

import com.openfinova.banking.loan.api.entity.AmortizationType;
import com.openfinova.banking.loan.api.entity.InterestCalculationMethod;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.api.entity.RepaymentFrequency;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Response DTO for loan account information.
 */
public class LoanAccountResponse {

    private UUID id;
    private String loanAccountNumber;
    private UUID applicationId;
    private UUID customerId;
    private UUID productId;
    private BigDecimal principalAmount;
    private BigDecimal outstandingPrincipal;
    private BigDecimal outstandingInterest;
    private BigDecimal outstandingFees;
    private BigDecimal outstandingPenalties;
    private BigDecimal totalOutstanding;
    private Integer tenorMonths;
    private BigDecimal interestRate;
    private InterestCalculationMethod interestCalculationMethod;
    private RepaymentFrequency repaymentFrequency;
    private AmortizationType amortizationType;
    private String currency;
    private LoanStatus status;
    private LocalDate disbursementDate;
    private LocalDate maturityDate;
    private LocalDate firstPaymentDate;
    private LocalDate lastPaymentDate;
    private LocalDate closedDate;
    private BigDecimal totalPaid;
    private Integer daysPastDue;
    private String delinquencyBucket;
    private Boolean isRestructured;
    private LocalDate restructuredDate;
    private Boolean isTopUp;
    private UUID originalLoanId;
    private String remarks;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public BigDecimal getTotalOutstanding() {
        return totalOutstanding;
    }

    public void setTotalOutstanding(BigDecimal totalOutstanding) {
        this.totalOutstanding = totalOutstanding;
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

    public String getDelinquencyBucket() {
        return delinquencyBucket;
    }

    public void setDelinquencyBucket(String delinquencyBucket) {
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
