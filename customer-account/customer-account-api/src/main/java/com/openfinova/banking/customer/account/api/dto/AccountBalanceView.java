package com.openfinova.banking.customer.account.api.dto;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Data Transfer Object representing a comprehensive view of an account balance.
 * Provides transparency into ledger, available, pending, and reserved amounts.
 */
public class AccountBalanceView {
    private UUID accountId;
    private String accountNumber;

    /**
     * The actual balance on the ledger (settled transactions).
     */
    private BigDecimal currentBalance;

    /**
     * The balance available for use (Current - Reserved - Pending Debits).
     */
    private BigDecimal availableBalance;

    /**
     * Total amount of incoming transactions not yet settled.
     */
    private BigDecimal pendingCredits;

    /**
     * Total amount of outgoing transactions not yet settled.
     */
    private BigDecimal pendingDebits;

    /**
     * Total amount of active fund reservations (holds).
     */
    private BigDecimal reservedAmount;

    private String currency;
    private LocalDateTime lastUpdated;

    /**
     * Number of underlying GL accounts mapped to this account.
     */
    private Integer glAccountCount;

    /**
     * Optional detailed breakdown of GL components.
     */
    private List<GLComponentBalance> components;

    public static class GLComponentBalance {
        private UUID glAccountId;
        private String mappingType;
        private BigDecimal balance;
        private Integer weight;

        public GLComponentBalance() {
        }

        public UUID getGlAccountId() {
            return glAccountId;
        }

        public void setGlAccountId(UUID glAccountId) {
            this.glAccountId = glAccountId;
        }

        public String getMappingType() {
            return mappingType;
        }

        public void setMappingType(String mappingType) {
            this.mappingType = mappingType;
        }

        public BigDecimal getBalance() {
            return balance;
        }

        public void setBalance(BigDecimal balance) {
            this.balance = balance;
        }

        public Integer getWeight() {
            return weight;
        }

        public void setWeight(Integer weight) {
            this.weight = weight;
        }
    }

    public AccountBalanceView() {
    }

    // Getters and Setters
    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public BigDecimal getCurrentBalance() {
        return currentBalance;
    }

    public void setCurrentBalance(BigDecimal currentBalance) {
        this.currentBalance = currentBalance;
    }

    public void setLedgerBalance(BigDecimal ledgerBalance) {
        this.currentBalance = ledgerBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public BigDecimal getPendingCredits() {
        return pendingCredits;
    }

    public void setPendingCredits(BigDecimal pendingCredits) {
        this.pendingCredits = pendingCredits;
    }

    public BigDecimal getPendingDebits() {
        return pendingDebits;
    }

    public void setPendingDebits(BigDecimal pendingDebits) {
        this.pendingDebits = pendingDebits;
    }

    public BigDecimal getReservedAmount() {
        return reservedAmount;
    }

    public void setReservedAmount(BigDecimal reservedAmount) {
        this.reservedAmount = reservedAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDateTime getLastUpdated() {
        return lastUpdated;
    }

    public void setLastUpdated(LocalDateTime lastUpdated) {
        this.lastUpdated = lastUpdated;
    }

    public Integer getGlAccountCount() {
        return glAccountCount;
    }

    public void setGlAccountCount(Integer glAccountCount) {
        this.glAccountCount = glAccountCount;
    }

    public List<GLComponentBalance> getComponents() {
        return components;
    }

    public void setComponents(List<GLComponentBalance> components) {
        this.components = components;
    }
}
