package com.openfinova.banking.customer.entity;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.KYCDecision;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.testsupport.CustomerTestFixtures;

class KYCWorkflowTest {

    @Test
    void submitApproveRejectExpireAndCompletionFlag() {
        Customer customer = CustomerTestFixtures.individual("K1");
        KYCWorkflow wf = new KYCWorkflow(customer, "system");
        assertThat(wf.getStatus()).isEqualTo(KYCStatus.PENDING);

        wf.submitForReview("alice");
        assertThat(wf.getStatus()).isEqualTo(KYCStatus.IN_REVIEW);
        assertThat(wf.getInitiatedBy()).isEqualTo("alice");

        wf.approve("bob", "ok");
        assertThat(wf.getStatus()).isEqualTo(KYCStatus.VERIFIED);
        assertThat(wf.getReviewedBy()).isEqualTo("bob");
        assertThat(wf.getComments()).isEqualTo("ok");
        assertThat(wf.getCompletedAt()).isNotNull();
        assertThat(wf.isCompleted()).isTrue();

        KYCWorkflow wf2 = new KYCWorkflow(customer, "x");
        wf2.submitForReview("u");
        wf2.reject("bob", "bad id");
        assertThat(wf2.getStatus()).isEqualTo(KYCStatus.REJECTED);
        assertThat(wf2.getRejectionReason()).isEqualTo("bad id");
        assertThat(wf2.isCompleted()).isTrue();

        KYCWorkflow wf3 = new KYCWorkflow(customer, "y");
        wf3.expire();
        assertThat(wf3.getStatus()).isEqualTo(KYCStatus.EXPIRED);
        assertThat(wf3.isCompleted()).isFalse();
    }

    @Test
    void addReviewStep_wiresBidirectionalAssociation() {
        Customer customer = CustomerTestFixtures.individual("K2");
        KYCWorkflow wf = new KYCWorkflow(customer, "s");
        KYCReviewStep step = new KYCReviewStep(null, "doc check", KYCDecision.APPROVED, "r", null);

        wf.addReviewStep(step);

        assertThat(wf.getReviewSteps()).containsExactly(step);
        assertThat(step.getKycWorkflow()).isSameAs(wf);
    }
}
