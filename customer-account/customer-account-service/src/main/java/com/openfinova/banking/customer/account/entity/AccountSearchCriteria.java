package com.openfinova.banking.customer.account.entity;

import com.openfinova.banking.customer.account.api.entity.AccountStatus;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Search criteria DTO for filtering customer accounts.
 */
public class AccountSearchCriteria {

    private UUID primaryUserProfileId;
    private String accountNumberPrefix;
    private String iban;
    private com.openfinova.banking.customer.account.api.entity.AccountProductType productType;
    private AccountStatus status;
    private String currency;
    private String displayNameContains;
    private LocalDateTime createdAfter;
    private LocalDateTime createdBefore;
    private LocalDateTime closedAfter;
    private LocalDateTime closedBefore;

    // Constructors
    public AccountSearchCriteria() {
    }

    // Getters and Setters
    public UUID getPrimaryUserProfileId() {
        return primaryUserProfileId;
    }

    public void setPrimaryUserProfileId(UUID primaryUserProfileId) {
        this.primaryUserProfileId = primaryUserProfileId;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public com.openfinova.banking.customer.account.api.entity.AccountProductType getProductType() {
        return productType;
    }

    public void setProductType(com.openfinova.banking.customer.account.api.entity.AccountProductType productType) {
        this.productType = productType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getAccountNumberPrefix() {
        return accountNumberPrefix;
    }

    public void setAccountNumberPrefix(String accountNumberPrefix) {
        this.accountNumberPrefix = accountNumberPrefix;
    }

    public String getDisplayNameContains() {
        return displayNameContains;
    }

    public void setDisplayNameContains(String displayNameContains) {
        this.displayNameContains = displayNameContains;
    }

    public LocalDateTime getCreatedAfter() {
        return createdAfter;
    }

    public void setCreatedAfter(LocalDateTime createdAfter) {
        this.createdAfter = createdAfter;
    }

    public LocalDateTime getCreatedBefore() {
        return createdBefore;
    }

    public void setCreatedBefore(LocalDateTime createdBefore) {
        this.createdBefore = createdBefore;
    }

    public LocalDateTime getClosedAfter() {
        return closedAfter;
    }

    public void setClosedAfter(LocalDateTime closedAfter) {
        this.closedAfter = closedAfter;
    }

    public LocalDateTime getClosedBefore() {
        return closedBefore;
    }

    public void setClosedBefore(LocalDateTime closedBefore) {
        this.closedBefore = closedBefore;
    }
}