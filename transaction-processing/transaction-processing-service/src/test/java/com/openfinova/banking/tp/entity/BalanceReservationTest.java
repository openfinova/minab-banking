package com.openfinova.banking.tp.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.openfinova.banking.tp.api.entity.ReservationStatus;
import com.openfinova.banking.tp.api.entity.ReservationType;
import com.openfinova.banking.tp.testsupport.TpEntityTestFixtures;

class BalanceReservationTest {

    @Test
    void release_marksReleasedFromActive() {
        Transaction tx = TpEntityTestFixtures.transaction();
        BalanceReservation r = new BalanceReservation(
                tx,
                UUID.randomUUID(),
                new BigDecimal("10.0000"),
                "USD",
                ReservationType.DEBIT_HOLD,
                LocalDateTime.now().plusHours(2),
                "k1",
                "r1");
        r.release("user cancelled");
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
        assertThat(r.getReleaseReason()).isEqualTo("user cancelled");
        assertThatThrownBy(() -> r.release("again")).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markExpired_isIdempotentWhenNotActive() {
        Transaction tx = TpEntityTestFixtures.transaction();
        BalanceReservation r = new BalanceReservation(
                tx,
                UUID.randomUUID(),
                new BigDecimal("10.0000"),
                "USD",
                ReservationType.DEBIT_HOLD,
                LocalDateTime.now().plusHours(2),
                "k2",
                "r2");
        r.release("x");
        r.markExpired();
        assertThat(r.getStatus()).isEqualTo(ReservationStatus.RELEASED);
    }

    @Test
    void hasExpired_comparesToNow() {
        Transaction tx = TpEntityTestFixtures.transaction();
        BalanceReservation past = new BalanceReservation(
                tx,
                UUID.randomUUID(),
                new BigDecimal("10.0000"),
                "USD",
                ReservationType.DEBIT_HOLD,
                LocalDateTime.now().minusMinutes(1),
                "k3",
                "r3");
        assertThat(past.hasExpired()).isTrue();

        BalanceReservation future = new BalanceReservation(
                tx,
                UUID.randomUUID(),
                new BigDecimal("10.0000"),
                "USD",
                ReservationType.DEBIT_HOLD,
                LocalDateTime.now().plusHours(1),
                "k4",
                "r4");
        assertThat(future.hasExpired()).isFalse();
    }

    @Test
    void getEffectiveAmount_zeroWhenCreditHold() {
        Transaction tx = TpEntityTestFixtures.transaction();
        BalanceReservation r = new BalanceReservation(
                tx,
                UUID.randomUUID(),
                new BigDecimal("10.0000"),
                "USD",
                ReservationType.CREDIT_HOLD,
                LocalDateTime.now().plusHours(1),
                "k5",
                "r5");
        assertThat(r.getEffectiveAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }
}
