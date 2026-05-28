package com.openfinova.banking.gl.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.gl.api.entity.GLTransactionSource;
import com.openfinova.banking.gl.api.entity.GLTransactionStatus;
import com.openfinova.banking.gl.testsupport.GlEntityFixtures;

class GLTransactionTest {
    private static final LocalDateTime SUBMITTED_AT = LocalDateTime.of(2026, 1, 1, 10, 0);
    private static final Instant POSTED_AT = Instant.parse("2026-01-01T10:00:00Z");
    private static final LocalDate VALUE_DATE = LocalDate.of(2026, 1, 1);

    @Test
    void sourceAndStatusPredicates() {
        GLTransaction manual = GlEntityFixtures.draftTransaction("R1");
        manual.setSource(GLTransactionSource.MANUAL_ENTRY);
        assertThat(manual.isDraft()).isTrue();
        assertThat(manual.isManualEntry()).isTrue();
        assertThat(manual.requiresApproval()).isTrue();
        assertThat(manual.isSystemGenerated()).isFalse();

        GLTransaction batch = GlEntityFixtures.draftTransaction("R2");
        batch.setSource(GLTransactionSource.BATCH_IMPORT);
        assertThat(batch.isSystemGenerated()).isTrue();
        assertThat(batch.requiresApproval()).isFalse();
    }

    @Test
    void lifecycle_submitApproveRejectCancel() {
        GLTransaction tx = GlEntityFixtures.draftTransaction("R3");

        tx.submitForApproval("alice", SUBMITTED_AT);
        assertThat(tx.getStatus()).isEqualTo(GLTransactionStatus.PENDING_APPROVAL);
        assertThat(tx.getSubmittedBy()).isEqualTo("alice");

        tx.reject();
        assertThat(tx.getStatus()).isEqualTo(GLTransactionStatus.REJECTED);
        assertThatThrownBy(() -> tx.reject()).isInstanceOf(IllegalStateException.class);

        GLTransaction draft2 = GlEntityFixtures.draftTransaction("R4");
        assertThatThrownBy(() -> draft2.reject()).isInstanceOf(IllegalStateException.class);

        draft2.cancel();
        assertThat(draft2.getStatus()).isEqualTo(GLTransactionStatus.CANCELLED);

        GLTransaction draft3 = GlEntityFixtures.draftTransaction("R5");
        draft3.submitForApproval("bob", SUBMITTED_AT);
        draft3.approveAndPost("checker", POSTED_AT);
        assertThat(draft3.getStatus()).isEqualTo(GLTransactionStatus.POSTED);
        assertThat(draft3.getPostedBy()).isEqualTo("checker");
        assertThat(draft3.getPostingDate()).isNotNull();
    }

    @Test
    void approveAndPost_allowsDraft_orPendingApproval() {
        GLTransaction fromDraft = GlEntityFixtures.draftTransaction("R6");
        fromDraft.approveAndPost("SYS", POSTED_AT);
        assertThat(fromDraft.isPosted()).isTrue();

        GLTransaction pending = GlEntityFixtures.draftTransaction("R7");
        pending.submitForApproval("u", SUBMITTED_AT);
        pending.approveAndPost("a", POSTED_AT);
        assertThat(pending.isPosted()).isTrue();
    }

    @Test
    void submitForApproval_requiresDraft() {
        GLTransaction tx = GlEntityFixtures.draftTransaction("R8");
        tx.submitForApproval("u", SUBMITTED_AT);
        assertThatThrownBy(() -> tx.submitForApproval("again", SUBMITTED_AT)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void markReversed_onlyWhenPostedAndNotAlreadyReversed() {
        GLTransaction original = GlEntityFixtures.draftTransaction("R9");
        original.approveAndPost("p", POSTED_AT);
        GLTransaction reversal = GlEntityFixtures.draftTransaction("R10");

        original.markReversed(reversal);
        assertThat(original.getStatus()).isEqualTo(GLTransactionStatus.REVERSED);
        assertThat(original.getReversedBy()).isSameAs(reversal);

        assertThatThrownBy(() -> original.markReversed(reversal)).isInstanceOf(IllegalStateException.class);

        GLTransaction draft = GlEntityFixtures.draftTransaction("R11");
        assertThatThrownBy(() -> draft.markReversed(reversal)).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void journalEntries_lineNumbersAssignedAndRenumberedOnRemove() {
        GLTransaction tx = GlEntityFixtures.draftTransaction("R12");
        GLAccount a1 = GlEntityFixtures.usdAssetAccount();
        GLAccount a2 = GlEntityFixtures.usdLiabilityAccount();
        GLJournalEntry e1 = GLJournalEntry.debit(a1, BigDecimal.TEN, "1", VALUE_DATE);
        GLJournalEntry e2 = GLJournalEntry.credit(a2, BigDecimal.TEN, "2", VALUE_DATE);
        GLJournalEntry e3 = GLJournalEntry.debit(a1, BigDecimal.ONE, "3", VALUE_DATE);

        tx.addGLJournalEntry(e1);
        tx.addGLJournalEntry(e2);
        tx.addGLJournalEntry(e3);
        assertThat(e1.getLineNumber()).isEqualTo(1);
        assertThat(e2.getLineNumber()).isEqualTo(2);
        assertThat(e3.getLineNumber()).isEqualTo(3);

        tx.removeGLJournalEntry(e2);
        assertThat(tx.getJournalEntries()).hasSize(2);
        assertThat(e1.getLineNumber()).isEqualTo(1);
        assertThat(e3.getLineNumber()).isEqualTo(2);
        assertThat(e2.getTransaction()).isNull();
    }
}
