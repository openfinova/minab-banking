package com.openfinova.banking.tp.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Entity representing a velocity limit breach incident.
 */
@Entity
@Table(name = "velocity_limit_breaches")
public class VelocityLimitBreach {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private TransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "limit_period", nullable = false)
    private VelocityLimitPeriod limitPeriod;

    @Column(name = "attempted_amount", precision = 19, scale = 4)
    private BigDecimal attemptedAmount;

    @Column(name = "attempted_count")
    private Integer attemptedCount;

    @Column(name = "limit_amount", precision = 19, scale = 4)
    private BigDecimal limitAmount;

    @Column(name = "limit_count")
    private Integer limitCount;

    @Column(name = "reason", length = 500)
    private String reason;

    @Column(name = "breach_type", length = 20)
    private String breachType; // "AMOUNT" or "COUNT"

    @CreationTimestamp
    @Column(name = "breach_timestamp", nullable = false)
    private Instant breachTimestamp;

    // Default constructor
    public VelocityLimitBreach() {
    }

    // Constructor
    public VelocityLimitBreach(UUID accountId, TransactionType transactionType, VelocityLimitPeriod limitPeriod,
            BigDecimal attemptedAmount, String reason) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.limitPeriod = limitPeriod;
        this.attemptedAmount = attemptedAmount;
        this.reason = reason;
        this.breachType = "AMOUNT";
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public TransactionType getTransactionType() {
        return transactionType;
    }

    public void setTransactionType(TransactionType transactionType) {
        this.transactionType = transactionType;
    }

    public VelocityLimitPeriod getVelocityLimitPeriod() {
        return limitPeriod;
    }

    public void setVelocityLimitPeriod(VelocityLimitPeriod limitPeriod) {
        this.limitPeriod = limitPeriod;
    }

    public BigDecimal getAttemptedAmount() {
        return attemptedAmount;
    }

    public void setAttemptedAmount(BigDecimal attemptedAmount) {
        this.attemptedAmount = attemptedAmount;
    }

    public Integer getAttemptedCount() {
        return attemptedCount;
    }

    public void setAttemptedCount(Integer attemptedCount) {
        this.attemptedCount = attemptedCount;
    }

    public BigDecimal getLimitAmount() {
        return limitAmount;
    }

    public void setLimitAmount(BigDecimal limitAmount) {
        this.limitAmount = limitAmount;
    }

    public Integer getLimitCount() {
        return limitCount;
    }

    public void setLimitCount(Integer limitCount) {
        this.limitCount = limitCount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public String getBreachType() {
        return breachType;
    }

    public void setBreachType(String breachType) {
        this.breachType = breachType;
    }

    public Instant getBreachTimestamp() {
        return breachTimestamp;
    }

    public void setBreachTimestamp(Instant breachTimestamp) {
        this.breachTimestamp = breachTimestamp;
    }

    @Override
    public String toString() {
        return "VelocityLimitBreachEntity{" + "id=" + id + ", accountId=" + accountId + ", transactionType="
                + transactionType + ", limitPeriod=" + limitPeriod + ", attemptedAmount=" + attemptedAmount
                + ", reason='" + reason + '\'' + ", breachTimestamp=" + breachTimestamp + ", breachType='" + breachType
                + '\'' + '}';
    }
}