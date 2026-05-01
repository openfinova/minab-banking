package com.openfinova.banking.loan.api.entity;

/**
 * Type of collateral pledged to secure a loan.
 */
public enum CollateralType {

    /** Land, buildings, or other immovable property */
    REAL_ESTATE,

    /** Cars, trucks, motorcycles, or other vehicles */
    VEHICLE,

    /** Gold jewelry, coins, or bars */
    GOLD,

    /** Stocks, bonds, or other financial instruments */
    SECURITIES,

    /** Fixed deposit or certificate of deposit */
    FIXED_DEPOSIT,

    /** Machinery, tools, or business equipment */
    EQUIPMENT,

    /** Business inventory or stock */
    INVENTORY,

    /** Outstanding invoices or receivables */
    ACCOUNTS_RECEIVABLE,

    /** Other types of collateral not listed above */
    OTHER
}
