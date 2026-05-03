package com.openfinova.banking.gl.entity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.gl.api.entity.FiscalPeriodStatus;

class FiscalPeriodTest {

    @Test
    void dateRange_rejectsStartAfterEnd() {
        LocalDate start = LocalDate.of(2026, 5, 10);
        LocalDate end = LocalDate.of(2026, 5, 1);

        assertThatThrownBy(() -> new FiscalPeriod("May", start, end)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Start date cannot be after end date");
    }

    @Test
    void mutatingDates_revalidatesRange() {
        FiscalPeriod p = new FiscalPeriod("May", LocalDate.of(2026, 5, 1), LocalDate.of(2026, 5, 31));

        assertThatThrownBy(() -> p.setEndDate(LocalDate.of(2026, 4, 1))).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void statusQueries_reflectOpenClosedAndLocked() {
        FiscalPeriod open = periodWithStatus(FiscalPeriodStatus.OPEN);
        FiscalPeriod adjusting = periodWithStatus(FiscalPeriodStatus.ADJUSTING);
        FiscalPeriod closed = periodWithStatus(FiscalPeriodStatus.CLOSED);
        FiscalPeriod locked = periodWithStatus(FiscalPeriodStatus.LOCKED);

        assertThat(open.isOpen()).isTrue();
        assertThat(open.isClosed()).isFalse();

        assertThat(adjusting.isOpen()).isFalse();
        assertThat(adjusting.isClosed()).isFalse();

        assertThat(closed.isClosed()).isTrue();
        assertThat(locked.isClosed()).isTrue();
    }

    @Test
    void close_lock_reopen_mutateStatePredictably() {
        FiscalPeriod p = new FiscalPeriod("Q1", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 3, 31));
        p.setStatus(FiscalPeriodStatus.OPEN);

        p.close("user-a");
        assertThat(p.getStatus()).isEqualTo(FiscalPeriodStatus.CLOSED);
        assertThat(p.getClosedBy()).isEqualTo("user-a");
        assertThat(p.getClosedAt()).isNotNull();

        p.lock();
        assertThat(p.getStatus()).isEqualTo(FiscalPeriodStatus.LOCKED);

        p.reopen("user-b");
        assertThat(p.getStatus()).isEqualTo(FiscalPeriodStatus.OPEN);
        assertThat(p.getClosedAt()).isNull();
        assertThat(p.getClosedBy()).isNull();
        assertThat(p.getReopenedBy()).isEqualTo("user-b");
        assertThat(p.getReopenedAt()).isNotNull();
    }

    private static FiscalPeriod periodWithStatus(FiscalPeriodStatus status) {
        FiscalPeriod p = new FiscalPeriod("P", LocalDate.of(2026, 1, 1), LocalDate.of(2026, 1, 31));
        p.setStatus(status);
        return p;
    }
}
