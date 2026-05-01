package com.openfinova.banking.loan.api.entity;

/**
 * Standard delinquency bucket for loan accounts (aligned with scheduled batch and manual updates).
 */
public enum DelinquencyBucket {

    CURRENT,
    DPD_1_30,
    DPD_31_60,
    DPD_61_90,
    DPD_91_180,
    DPD_180_PLUS;

    /**
     * Maps days past due to a bucket (0 or negative days treated as current).
     */
    public static DelinquencyBucket fromDaysPastDue(int daysPastDue) {
        if (daysPastDue <= 0) {
            return CURRENT;
        }
        if (daysPastDue <= 30) {
            return DPD_1_30;
        }
        if (daysPastDue <= 60) {
            return DPD_31_60;
        }
        if (daysPastDue <= 90) {
            return DPD_61_90;
        }
        if (daysPastDue <= 180) {
            return DPD_91_180;
        }
        return DPD_180_PLUS;
    }
}
