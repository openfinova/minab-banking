package com.openfinova.banking.tp.entity;

import static com.openfinova.banking.tp.testsupport.TpEntityTestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.openfinova.banking.tp.api.entity.ReservationType;
import com.openfinova.banking.tp.api.entity.TransactionStatus;
import com.openfinova.banking.tp.testsupport.TpEntityTestFixtures;

class TransactionTest {

    @Test
    void transitionTo_rejectsInvalidStateChange() {
        Transaction tx = TpEntityTestFixtures.transaction();
        assertThatThrownBy(() -> tx.transitionTo(TransactionStatus.POSTED, "skip", NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void transitionTo_validPathAndRecordsEvent() {
        Transaction tx = TpEntityTestFixtures.transaction();
        tx.transitionTo(TransactionStatus.PENDING_RESERVATION, null, NOW);
        assertThat(tx.getStatus()).isEqualTo(TransactionStatus.PENDING_RESERVATION);
        assertThat(tx.getEvents()).hasSize(1);
    }

    @Test
    void getTotalAmount_includesFee_whenSet() {
        Transaction tx = TpEntityTestFixtures.transaction();
        tx.setFeeAmount(new BigDecimal("2.5000"));
        assertThat(tx.getTotalAmount()).isEqualByComparingTo(new BigDecimal("102.5000"));
    }

    @Test
    void addReservation_setsParentOnChild() {
        Transaction tx = TpEntityTestFixtures.transaction();
        BalanceReservation r = new BalanceReservation(
                tx,
                UUID.randomUUID(),
                new BigDecimal("50.0000"),
                "USD",
                ReservationType.DEBIT_HOLD,
                NOW.plusHours(1),
                "res-key-1",
                "ref-1");
        tx.addReservation(r);
        assertThat(tx.getReservations()).containsExactly(r);
        assertThat(r.getTransaction()).isSameAs(tx);
    }

    @Test
    void terminalAndOutcomeFlags_followStatus() {
        Transaction tx = TpEntityTestFixtures.transaction();
        assertThat(tx.isTerminal()).isFalse();
        tx.setStatus(TransactionStatus.POSTED);
        assertThat(tx.isTerminal()).isTrue();
        assertThat(tx.isSuccessful()).isTrue();
        assertThat(tx.isFailed()).isFalse();

        tx.setStatus(TransactionStatus.FAILED);
        assertThat(tx.isFailed()).isTrue();
        assertThat(tx.isSuccessful()).isFalse();
    }
}
