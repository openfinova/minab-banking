package com.openfinova.banking.loan.api.entity;

/**
 * Type of entity providing loan guarantee.
 */
public enum GuarantorType {

    /** Individual person guaranteeing the loan */
    INDIVIDUAL,

    /** Company or corporation providing guarantee */
    CORPORATE,

    /** Government entity providing guarantee */
    GOVERNMENT,

    /** Bank guarantee or letter of credit */
    BANK_GUARANTEE
}
