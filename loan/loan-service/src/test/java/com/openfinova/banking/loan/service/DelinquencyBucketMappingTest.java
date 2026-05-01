package com.openfinova.banking.loan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

import com.openfinova.banking.loan.api.entity.DelinquencyBucket;

class DelinquencyBucketMappingTest {

    @Test
    void fromDaysPastDue_mapsToExpectedBuckets() {
        assertEquals(DelinquencyBucket.CURRENT, DelinquencyBucket.fromDaysPastDue(0));
        assertEquals(DelinquencyBucket.CURRENT, DelinquencyBucket.fromDaysPastDue(-1));
        assertEquals(DelinquencyBucket.DPD_1_30, DelinquencyBucket.fromDaysPastDue(1));
        assertEquals(DelinquencyBucket.DPD_1_30, DelinquencyBucket.fromDaysPastDue(30));
        assertEquals(DelinquencyBucket.DPD_31_60, DelinquencyBucket.fromDaysPastDue(31));
        assertEquals(DelinquencyBucket.DPD_61_90, DelinquencyBucket.fromDaysPastDue(61));
        assertEquals(DelinquencyBucket.DPD_91_180, DelinquencyBucket.fromDaysPastDue(91));
        assertEquals(DelinquencyBucket.DPD_180_PLUS, DelinquencyBucket.fromDaysPastDue(181));
    }
}
