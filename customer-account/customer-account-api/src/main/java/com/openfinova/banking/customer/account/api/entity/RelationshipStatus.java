package com.openfinova.banking.customer.account.api.entity;

/**
 * Enumeration of relationship statuses for account relationships.
 * Defines the current state of a user's relationship with an account.
 */
public enum RelationshipStatus {
    ACTIVE("Relationship is active and effective"),
    INACTIVE("Relationship is inactive but can be reactivated"),
    SUSPENDED("Relationship is temporarily suspended");

    private final String description;

    RelationshipStatus(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}