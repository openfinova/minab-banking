package com.openfinova.banking.loan.entity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.loan.api.entity.ApplicationStatus;
import com.openfinova.banking.loan.api.entity.GuarantorType;

class LoanApplicationTest {

    @Test
    void approve_and_reject_updateState() {
        LoanApplication app = new LoanApplication(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("5000"),
                24,
                "USD");
        app.setApplicationNumber("APP-UT-1");
        app.setStatus(ApplicationStatus.UNDERWRITING);

        app.approve(new BigDecimal("4800"), 24, new BigDecimal("6.5"), "underwriter-1");

        assertThat(app.getStatus()).isEqualTo(ApplicationStatus.APPROVED);
        assertThat(app.isApproved()).isTrue();
        assertThat(app.getApprovedAmount()).isEqualByComparingTo(new BigDecimal("4800"));

        LoanApplication rejected = new LoanApplication(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("1000"),
                12,
                "USD");
        rejected.setApplicationNumber("APP-UT-2");
        rejected.setStatus(ApplicationStatus.UNDERWRITING);
        rejected.reject("DTI too high", "uw-2");
        assertThat(rejected.isRejected()).isTrue();
        assertThat(rejected.getRejectionReason()).isEqualTo("DTI too high");
    }

    @Test
    void canTransitionTo_followsApplicationWorkflow() {
        LoanApplication app = new LoanApplication();
        app.setStatus(ApplicationStatus.DRAFT);
        assertThat(app.canTransitionTo(ApplicationStatus.SUBMITTED)).isTrue();
        assertThat(app.canTransitionTo(ApplicationStatus.APPROVED)).isFalse();

        app.setStatus(ApplicationStatus.APPROVED);
        assertThat(app.canTransitionTo(ApplicationStatus.SUBMITTED)).isFalse();
    }

    @Test
    void hasRequiredGuarantors_comparesCountToThreshold() {
        LoanApplication app = new LoanApplication(
                UUID.randomUUID(),
                UUID.randomUUID(),
                new BigDecimal("10000"),
                36,
                "USD");
        app.setApplicationNumber("APP-UT-3");
        app.setGuarantorsRequired(2);
        assertThat(app.hasRequiredGuarantors()).isFalse();

        Guarantor g1 = new Guarantor();
        g1.setCustomerId(UUID.randomUUID());
        g1.setGuarantorType(GuarantorType.INDIVIDUAL);
        g1.setGuaranteedAmount(BigDecimal.ONE);
        Guarantor g2 = new Guarantor();
        g2.setCustomerId(UUID.randomUUID());
        g2.setGuarantorType(GuarantorType.INDIVIDUAL);
        g2.setGuaranteedAmount(BigDecimal.ONE);
        app.addGuarantor(g1);
        app.addGuarantor(g2);
        assertThat(app.hasRequiredGuarantors()).isTrue();
    }

    @Test
    void isPending_coversInFlightStatuses() {
        LoanApplication app = new LoanApplication();
        app.setStatus(ApplicationStatus.SUBMITTED);
        assertThat(app.isPending()).isTrue();
        app.setStatus(ApplicationStatus.UNDER_REVIEW);
        assertThat(app.isPending()).isTrue();
        app.setStatus(ApplicationStatus.UNDERWRITING);
        assertThat(app.isPending()).isTrue();
        app.setStatus(ApplicationStatus.APPROVED);
        assertThat(app.isPending()).isFalse();
    }
}
