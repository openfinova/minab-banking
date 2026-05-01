package com.openfinova.banking.customer.account.api.entity;

/**
 * Enumeration of account permissions that can be granted to account holders.
 * Defines the specific rights and capabilities for account access.
 */
public enum AccountPermission {
    VIEW("View account details and balances"),
    TRANSACT("Perform transactions"),
    MANAGE("Manage account settings and limits"),
    ADMIN("Full administrative access");

    private final String description;

    AccountPermission(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}