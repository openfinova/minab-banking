package com.openfinova.banking.loan.api.entity;

/**
 * Method used to disburse loan funds to borrower.
 */
public enum DisbursementMethod {

    /** Electronic transfer to borrower's bank account */
    BANK_TRANSFER,

    /** Cheque issued to borrower */
    CHEQUE,

    /** Cash disbursement at branch */
    CASH,

    /** Payment made directly to vendor (e.g., car dealer, property seller) */
    DIRECT_TO_VENDOR,

    /**
     * Disbursement via mobile money service (e.g., M-Pesa, MTN Mobile Money, Airtel Money, GCash, PayTM).
     * Mobile money allows users to store, send, and receive money using their mobile phone.
     * Common in regions with limited traditional banking infrastructure.
     * Examples:
     * - M-Pesa (Kenya, Tanzania, South Africa)
     * - MTN Mobile Money (Uganda, Ghana, Cameroon)
     * - Airtel Money (Multiple African countries)
     * - GCash (Philippines)
     * - PayTM (India)
     * - bKash (Bangladesh)
     * - EcoCash (Zimbabwe)
     */
    MOBILE_MONEY
}
