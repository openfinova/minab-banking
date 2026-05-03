package com.openfinova.banking.identity.entity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

class ApprovalWorkflowInstanceTest {

    @Test
    void addStep_wiresWorkflowAndAppendsToList() {
        ApprovalWorkflowInstance wf = new ApprovalWorkflowInstance("GL_POSTING", "tx-42");
        ApprovalWorkflowStep step = new ApprovalWorkflowStep(1, "SENIOR_ACCOUNTANT");

        wf.addStep(step);

        assertThat(wf.getSteps()).containsExactly(step);
        assertThat(step.getWorkflow()).isSameAs(wf);
    }
}
