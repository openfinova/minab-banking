package com.openfinova.banking.loan.entity;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.LoanFeeType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Loan Fee entity tracking fees charged on a loan.
 * Includes processing fees, late payment fees, and other charges.
 */
@Entity
@Table(name = "loan_fees", indexes = { @Index(name = "idx_loan_fees_account", columnList = "loan_account_id"),
        @Index(name = "idx_loan_fees_type", columnList = "fee_type"),
        @Index(name = "idx_loan_fees_charge_date", columnList = "charge_date"),
        @Index(name = "idx_loan_fees_waived", columnList = "is_waived") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoanFee {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "fee_type", nullable = false, length = 30)
    @NotNull(message = "Fee type is required")
    private LoanFeeType feeType;

    /**
     * Original fee amount charged.
     */
    @NotNull(message = "Fee amount is required")
    @DecimalMin(value = "0.0", message = "Fee amount must be positive")
    @Column(name = "fee_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal feeAmount;

    /**
     * Remaining unpaid fee amount.
     */
    @NotNull(message = "Outstanding amount is required")
    @DecimalMin(value = "0.0", message = "Outstanding amount must be positive")
    @Column(name = "outstanding_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal outstandingAmount;

    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    @NotNull(message = "Charge date is required")
    @Column(name = "charge_date", nullable = false)
    private LocalDate chargeDate;

    @Column(name = "is_waived", nullable = false)
    private Boolean isWaived = false;

    @Column(name = "waived_date")
    private LocalDate waivedDate;

    @Column(name = "waiver_reason", length = 500)
    @Size(max = 500, message = "Waiver reason must not exceed 500 characters")
    private String waiverReason;

    @Column(length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public LoanFee() {
    }

    // Business Logic
    public boolean isWaived() {
        return Boolean.TRUE.equals(isWaived);
    }

    public boolean isPaid() {
        return outstandingAmount.compareTo(BigDecimal.ZERO) == 0;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public LoanFeeType getFeeType() {
        return feeType;
    }

    public void setFeeType(LoanFeeType feeType) {
        this.feeType = feeType;
    }

    public BigDecimal getFeeAmount() {
        return feeAmount;
    }

    public void setFeeAmount(BigDecimal feeAmount) {
        this.feeAmount = feeAmount;
    }

    public BigDecimal getOutstandingAmount() {
        return outstandingAmount;
    }

    public void setOutstandingAmount(BigDecimal outstandingAmount) {
        this.outstandingAmount = outstandingAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getChargeDate() {
        return chargeDate;
    }

    public void setChargeDate(LocalDate chargeDate) {
        this.chargeDate = chargeDate;
    }

    public Boolean getIsWaived() {
        return isWaived;
    }

    public void setIsWaived(Boolean isWaived) {
        this.isWaived = isWaived;
    }

    public LocalDate getWaivedDate() {
        return waivedDate;
    }

    public void setWaivedDate(LocalDate waivedDate) {
        this.waivedDate = waivedDate;
    }

    public String getWaiverReason() {
        return waiverReason;
    }

    public void setWaiverReason(String waiverReason) {
        this.waiverReason = waiverReason;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
