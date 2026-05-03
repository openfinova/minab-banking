package com.openfinova.banking.tp.entity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.tp.api.entity.TransactionStatus;
import com.openfinova.banking.tp.testsupport.TpEntityTestFixtures;

class TransactionEventTest {

    @Test
    void isErrorEvent_whenCodeOrMessagePresent() {
        Transaction tx = TpEntityTestFixtures.transaction();
        TransactionEvent ev = new TransactionEvent(tx, "PROCESSING", TransactionStatus.INITIATED);
        assertThat(ev.isErrorEvent()).isFalse();
        ev.setErrorCode("E500");
        assertThat(ev.isErrorEvent()).isTrue();
    }

    @Test
    void isStateTransition_detectsEventType() {
        Transaction tx = TpEntityTestFixtures.transaction();
        TransactionEvent ev = new TransactionEvent(tx, "STATE_TRANSITION", TransactionStatus.AUTHORIZED);
        assertThat(ev.isStateTransition()).isTrue();
    }
}
