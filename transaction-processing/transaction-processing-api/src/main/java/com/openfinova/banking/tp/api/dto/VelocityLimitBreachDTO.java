package com.openfinova.banking.tp.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.api.entity.VelocityLimitPeriod;

/**
 * DTO representing a velocity limit breach incident.
 */
public class VelocityLimitBreachDTO {
    private UUID id;
    private UUID accountId;
    private TransactionType transactionType;
    private VelocityLimitPeriod limitPeriod;
    private BigDecimal attemptedAmount;
    private Integer attemptedCount;
    private BigDecimal limitAmount;
    private Integer limitCount;
    private String reason;
    private LocalDateTime breachTimestamp;
    private String breachType; // "AMOUNT" or "COUNT"

    public VelocityLimitBreachDTO() {
    }

    public VelocityLimitBreachDTO(UUID accountId, TransactionType transactionType, VelocityLimitPeriod limitPeriod,
            BigDecimal attemptedAmount, String reason, LocalDateTime breachTimestamp) {
        this.accountId = accountId;
        this.transactionType = transactionType;
        this.limitPeriod = limitPeriod;
        this.attemptedAmount = attemptedAmount;
        this.reason = reason;
        this.breachTimestamp = breachTimestamp;
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

    public LocalDateTime getBreachTimestamp() {
        return breachTimestamp;
    }

    public void setBreachTimestamp(LocalDateTime breachTimestamp) {
        this.breachTimestamp = breachTimestamp;
    }

    public String getBreachType() {
        return breachType;
    }

    public void setBreachType(String breachType) {
        this.breachType = breachType;
    }

    @Override
    public String toString() {
        return "VelocityLimitBreachDTO{" + "id=" + id + ", accountId=" + accountId + ", transactionType="
                + transactionType + ", limitPeriod=" + limitPeriod + ", attemptedAmount=" + attemptedAmount
                + ", reason='" + reason + '\'' + ", breachTimestamp=" + breachTimestamp + ", breachType='" + breachType
                + '\'' + '}';
    }
}