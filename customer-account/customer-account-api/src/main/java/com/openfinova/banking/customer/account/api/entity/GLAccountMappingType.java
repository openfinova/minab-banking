package com.openfinova.banking.customer.account.api.entity;

/**
 * Enumeration of GL account mapping types that define how customer accounts
 * relate to underlying General Ledger accounts.
 */
public enum GLAccountMappingType {
    PRIMARY_BALANCE("Primary balance account for customer funds", "PB"),
    INTEREST_ACCRUAL("Interest accrual account for earned interest", "IA"),
    FEE_COLLECTION("Fee collection account for account fees", "FC"),
    OVERDRAFT_FACILITY("Overdraft facility account for overdraft protection", "OF"),
    ESCROW_HOLD("Escrow hold account for temporary fund holds", "EH");

    private final String description;
    private final String code;

    GLAccountMappingType(String description, String code) {
        this.description = description;
        this.code = code;
    }

    public String getDescription() {
        return description;
    }

    public String getCode() {
        return code;
    }
}