package com.openfinova.banking.loan.api.entity;

/**
 * Payment status of a loan installment.
 */
public enum ScheduleStatus {

    /** Installment not yet due or paid */
    PENDING,

    /** Installment partially paid but amount still outstanding */
    PARTIALLY_PAID,

    /** Installment fully paid */
    PAID,

    /** Installment past due date and unpaid */
    OVERDUE,

    /** Installment amount waived by bank */
    WAIVED
}
