package com.openfinova.banking.gl.entity;

import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.gl.api.entity.EscalationLevel;
import com.openfinova.banking.gl.testsupport.GlEntityFixtures;

class SuspenseEscalationTest {

    @Test
    void resolve_recordsResolution() {
        SuspenseItem item = new SuspenseItem();
        item.setGlTransaction(GlEntityFixtures.draftTransaction("E0"));
        SuspenseEscalation esc = new SuspenseEscalation();
        esc.setSuspenseItem(item);
        esc.setEscalationLevel(EscalationLevel.LEVEL_1_SUPERVISOR);
        esc.setEscalatedDate(LocalDate.of(2026, 1, 1));
        esc.setAssignedTo("supervisor");
        esc.setDueDate(LocalDate.now().plusDays(7));

        esc.resolve("resolver", "fixed");
        assertThat(esc.getIsResolved()).isTrue();
        assertThat(esc.getResolvedBy()).isEqualTo("resolver");
        assertThat(esc.getResolutionNotes()).isEqualTo("fixed");
        assertThat(esc.getResolvedDate()).isNotNull();
    }

    @Test
    void slaBreach_whenPastDueAndUnresolved() {
        SuspenseEscalation esc = new SuspenseEscalation();
        esc.setDueDate(LocalDate.now().minusDays(1));
        esc.setIsResolved(false);

        assertThat(esc.checkSLABreach()).isTrue();
        assertThat(esc.getSlaBreached()).isTrue();
        assertThat(esc.isOverdue()).isTrue();

        esc.setIsResolved(true);
        assertThat(esc.checkSLABreach()).isFalse();
    }

    @Test
    void requiresBoardAttention_onlyForCriticalLevel() {
        SuspenseEscalation low = new SuspenseEscalation();
        low.setEscalationLevel(EscalationLevel.LEVEL_2_MANAGER);
        assertThat(low.requiresBoardAttention()).isFalse();

        SuspenseEscalation board = new SuspenseEscalation();
        board.setEscalationLevel(EscalationLevel.CRITICAL_BOARD_LEVEL);
        assertThat(board.requiresBoardAttention()).isTrue();
    }
}
