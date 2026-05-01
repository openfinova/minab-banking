package com.openfinova.banking.tp.api.entity;

/**
 * Enumeration of transaction types supported by the transaction processing system.
 */
public enum TransactionType {
    /**
     * Peer-to-peer transfer between two accounts within the bank
     */
    P2P("Peer-to-Peer Transfer", true, true),

    /**
     * Inbound funds from external gateways (Card-to-Card, ACH, Wire)
     */
    CASH_IN("Cash In / Deposit", false, true),

    /**
     * Outbound transfers including ATM withdrawals and external transfers
     */
    CASH_OUT("Cash Out / Withdrawal", true, false),

    /**
     * Specialized outbound transaction for utility and service payments
     */
    BILL_PAYMENT("Bill Payment", true, false),

    /**
     * Point-of-sale or online purchase transactions with merchant-specific fee
     * logic
     */
    MERCHANT_PURCHASE("Merchant Purchase", true, false),

    /**
     * Alias for P2P for backward compatibility
     */
    TRANSFER("Transfer", true, true),

    /**
     * Alias for CASH_IN for backward compatibility
     */
    DEPOSIT("Deposit", false, true),

    /**
     * Refund transaction - returns funds to the original source
     * Can be full or partial refund of a previous transaction
     */
    REFUND("Refund", false, true);

    private final String displayName;
    private final boolean requiresSourceAccount;
    private final boolean requiresDestinationAccount;

    TransactionType(String displayName, boolean requiresSourceAccount, boolean requiresDestinationAccount) {
        this.displayName = displayName;
        this.requiresSourceAccount = requiresSourceAccount;
        this.requiresDestinationAccount = requiresDestinationAccount;
    }

    public String getDisplayName() {
        return displayName;
    }

    public boolean requiresSourceAccount() {
        return requiresSourceAccount;
    }

    public boolean requiresDestinationAccount() {
        return requiresDestinationAccount;
    }

    /**
     * Checks if this transaction type involves external gateways
     *
     * @return true if external gateway integration is required
     */
    public boolean requiresExternalGateway() {
        return this == CASH_IN || this == CASH_OUT || this == BILL_PAYMENT || this == MERCHANT_PURCHASE;
    }

    /**
     * Checks if this transaction type supports authorization holds
     *
     * @return true if authorization holds are supported
     */
    public boolean supportsAuthorizationHolds() {
        return this == MERCHANT_PURCHASE;
    }

    /**
     * Gets the canonical transaction type (resolves aliases)
     *
     * @return the canonical transaction type
     */
    public TransactionType getCanonical() {
        return switch (this) {
            case TRANSFER -> P2P;
            case DEPOSIT -> CASH_IN;
            default -> this;
        };
    }
}