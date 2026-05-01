package com.openfinova.banking.customer.account.api.entity;

import java.util.Set;

/**
 * Enumeration of relationship types between users and customer accounts.
 * Defines the role and default permissions for account relationships.
 */
public enum RelationshipType {
    /**
     * The primary owner of the account.
     * Has full access and liability.
     */
    PRIMARY_HOLDER,

    /**
     * A joint owner of the account.
     * Has full access and joint liability.
     */
    SECONDARY_HOLDER,

    /**
     * A user authorized to transact on the account but with no ownership or
     * liability.
     * e.g., Employee of a business, Family member with card access.
     */
    AUTHORIZED_USER,

    /**
     * A designated beneficiary who receives the funds upon death of the owners.
     * Has Read-Only access (View permissions) by default.
     */
    BENEFICIARY,

    /**
     * A legal guardian managing the account for a minor or incapacitated person.
     * Has View and Manage permissions.
     */
    GUARDIAN;

    /**
     * Determines if this relationship type is a primary role.
     *
     * @return true if this is a primary holder role
     */
    public boolean isPrimaryRole() {
        return this == PRIMARY_HOLDER;
    }

    /**
     * Gets the default permissions for this relationship type.
     *
     * @return set of default account permissions
     */
    public Set<com.openfinova.banking.customer.account.api.entity.AccountPermission> getDefaultPermissions() {
        return switch (this) {
            case PRIMARY_HOLDER ->
                Set.of(com.openfinova.banking.customer.account.api.entity.AccountPermission.values());
            case SECONDARY_HOLDER -> Set.of(
                    com.openfinova.banking.customer.account.api.entity.AccountPermission.VIEW,
                    com.openfinova.banking.customer.account.api.entity.AccountPermission.TRANSACT,
                    com.openfinova.banking.customer.account.api.entity.AccountPermission.MANAGE);
            case AUTHORIZED_USER -> Set.of(
                    com.openfinova.banking.customer.account.api.entity.AccountPermission.VIEW,
                    com.openfinova.banking.customer.account.api.entity.AccountPermission.TRANSACT);
            case BENEFICIARY -> Set.of(com.openfinova.banking.customer.account.api.entity.AccountPermission.VIEW);
            case GUARDIAN -> Set.of(
                    com.openfinova.banking.customer.account.api.entity.AccountPermission.VIEW,
                    com.openfinova.banking.customer.account.api.entity.AccountPermission.MANAGE);
        };
    }
}