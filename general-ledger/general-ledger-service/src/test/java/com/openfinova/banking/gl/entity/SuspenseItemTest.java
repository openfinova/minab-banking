package com.openfinova.banking.gl.entity;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.gl.api.entity.AgingBracket;
import com.openfinova.banking.gl.api.entity.SuspenseReasonCode;
import com.openfinova.banking.gl.api.entity.SuspenseStatus;
import com.openfinova.banking.gl.testsupport.GlEntityFixtures;

class SuspenseItemTest {

    @Test
    void ageAndAgingBracket_followPostingDate() {
        SuspenseItem item = new SuspenseItem();
        item.setPostingDate(LocalDate.now().minusDays(10));
        item.setGlTransaction(GlEntityFixtures.draftTransaction("S1"));

        assertThat(item.getAgeDays()).isEqualTo(10);
        assertThat(item.getAgingBracket()).isEqualTo(AgingBracket.RECENT_8_30_DAYS);
    }

    @Test
    void requiresAMLReview_delegatesToReasonCode() {
        SuspenseItem aml = itemWithReason(SuspenseReasonCode.UNIDENTIFIED_DEPOSIT);
        SuspenseItem routine = itemWithReason(SuspenseReasonCode.SYSTEM_ERROR);

        assertThat(aml.requiresAMLReview()).isTrue();
        assertThat(routine.requiresAMLReview()).isFalse();
    }

    @Test
    void workflow_startInvestigate_escalate_clear() {
        GLTransaction glTx = GlEntityFixtures.draftTransaction("S2");
        SuspenseItem item = new SuspenseItem(glTx, new BigDecimal("100.00"), "USD", SuspenseReasonCode.OTHER, "x");
        GLTransaction clearing = GlEntityFixtures.draftTransaction("S3");

        item.startInvestigation("team-a");
        assertThat(item.getStatus()).isEqualTo(SuspenseStatus.UNDER_INVESTIGATION);
        assertThat(item.getAssignedTo()).isEqualTo("team-a");

        item.escalate();
        assertThat(item.getStatus()).isEqualTo(SuspenseStatus.ESCALATED);

        item.markCleared("alice", clearing);
        assertThat(item.getStatus()).isEqualTo(SuspenseStatus.CLEARED);
        assertThat(item.getClearedBy()).isEqualTo("alice");
        assertThat(item.getClearingTransaction()).isSameAs(clearing);

        SuspenseItem item2 = new SuspenseItem(glTx, BigDecimal.ONE, "USD", SuspenseReasonCode.SYSTEM_ERROR, "y");
        item2.markAutoCleared(clearing);
        assertThat(item2.getStatus()).isEqualTo(SuspenseStatus.AUTO_CLEARED);
        assertThat(item2.getClearedBy()).isEqualTo("SYSTEM");
    }

    private static SuspenseItem itemWithReason(SuspenseReasonCode reason) {
        SuspenseItem item = new SuspenseItem();
        item.setReasonCode(reason);
        item.setPostingDate(LocalDate.now());
        item.setGlTransaction(GlEntityFixtures.draftTransaction("S0"));
        return item;
    }
}
