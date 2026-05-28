package com.openfinova.banking.tp.entity;

import static com.openfinova.banking.tp.testsupport.TpEntityTestFixtures.NOW;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.junit.jupiter.api.Test;

import com.openfinova.banking.tp.api.entity.CompensationStatus;
import com.openfinova.banking.tp.api.entity.CompensationStepType;
import com.openfinova.banking.tp.testsupport.TpEntityTestFixtures;

class CompensationWorkflowTest {

    @Test
    void transitionTo_rejectsIllegalMove() {
        CompensationWorkflow wf = new CompensationWorkflow(TpEntityTestFixtures.transaction(), "gl timeout");
        assertThatThrownBy(() -> wf.transitionTo(CompensationStatus.COMPLETED, null, NOW))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void transitionTo_happyPath_setsCompletedAt() {
        CompensationWorkflow wf = new CompensationWorkflow(TpEntityTestFixtures.transaction(), "rollback");
        wf.transitionTo(CompensationStatus.IN_PROGRESS, null, NOW);
        wf.transitionTo(CompensationStatus.COMPLETED, null, NOW);
        assertThat(wf.getWorkflowStatus()).isEqualTo(CompensationStatus.COMPLETED);
        assertThat(wf.getCompletedAt()).isNotNull();
    }

    @Test
    void addCompensationStep_appendsSteps() {
        CompensationWorkflow wf = new CompensationWorkflow(TpEntityTestFixtures.transaction(), "err");
        CompensationStep step = new CompensationStep(
                "s1",
                CompensationStepType.RELEASE_RESERVATIONS,
                "release",
                Map.of(),
                1);
        wf.addCompensationStep(step);
        assertThat(wf.getCompensationSteps()).containsExactly(step);
    }

    @Test
    void canRetry_afterFailureWhenUnderCap() {
        CompensationWorkflow wf = new CompensationWorkflow(TpEntityTestFixtures.transaction(), "err");
        wf.setMaxRetries(3);
        wf.transitionTo(CompensationStatus.IN_PROGRESS, null, NOW);
        wf.transitionTo(CompensationStatus.FAILED, null, NOW);
        assertThat(wf.canRetry()).isTrue();
    }
}
