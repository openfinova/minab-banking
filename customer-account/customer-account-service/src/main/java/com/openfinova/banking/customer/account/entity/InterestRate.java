package com.openfinova.banking.customer.account.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity representing the interest rate configuration for a customer account.
 * Allows for different rates (credit/debit) and effective periods.
 */
@Entity
@Table(name = "interest_rates", indexes = {
        @Index(name = "idx_interest_rates_account", columnList = "customer_account_id"),
        @Index(name = "idx_interest_rates_type", columnList = "rate_type"),
        @Index(name = "idx_interest_rates_effective", columnList = "effective_from, effective_until") })
public class InterestRate {

    public enum RateType {
        CREDIT("Interest paid to customer on positive balance"),
        DEBIT("Interest charged to customer on negative balance (overdraft)");

        private final String description;

        RateType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_account_id", nullable = false)
    private Account customerAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, length = 20)
    @NotNull(message = "Rate type is required")
    private RateType rateType;

    /**
     * Annual Percentage Rate (APR).
     * Represented as a percentage (e.g., 5.00 for 5%).
     */
    @Column(name = "annual_percentage_rate", nullable = false, precision = 10, scale = 4)
    @NotNull(message = "APR is required")
    @DecimalMin(value = "0.0000", message = "Rate must be non-negative")
    private BigDecimal annualPercentageRate;

    /**
     * Date from which this rate becomes active.
     */
    @Column(name = "effective_from", nullable = false)
    @NotNull(message = "Effective from date is required")
    private LocalDateTime effectiveFrom;

    /**
     * Date until which this rate is active.
     * If null, the rate is effective indefinitely.
     */
    @Column(name = "effective_until")
    private LocalDateTime effectiveUntil;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "created_by", length = 100)
    private String createdBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    public InterestRate() {
    }

    public InterestRate(Account account, RateType type, BigDecimal rate) {
        this.customerAccount = account;
        this.rateType = type;
        this.annualPercentageRate = rate;
        this.effectiveFrom = LocalDateTime.now();
    }

    public boolean isEffective() {
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(effectiveFrom) && (effectiveUntil == null || !now.isAfter(effectiveUntil));
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Account getCustomerAccount() {
        return customerAccount;
    }

    public void setCustomerAccount(Account customerAccount) {
        this.customerAccount = customerAccount;
    }

    public RateType getRateType() {
        return rateType;
    }

    public void setRateType(RateType rateType) {
        this.rateType = rateType;
    }

    public BigDecimal getAnnualPercentageRate() {
        return annualPercentageRate;
    }

    public void setAnnualPercentageRate(BigDecimal annualPercentageRate) {
        this.annualPercentageRate = annualPercentageRate;
    }

    public LocalDateTime getEffectiveFrom() {
        return effectiveFrom;
    }

    public void setEffectiveFrom(LocalDateTime effectiveFrom) {
        this.effectiveFrom = effectiveFrom;
    }

    public LocalDateTime getEffectiveUntil() {
        return effectiveUntil;
    }

    public void setEffectiveUntil(LocalDateTime effectiveUntil) {
        this.effectiveUntil = effectiveUntil;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
}
