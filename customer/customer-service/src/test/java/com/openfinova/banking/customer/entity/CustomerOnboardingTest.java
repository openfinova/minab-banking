package com.openfinova.banking.customer.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.CustomerType;
import com.openfinova.banking.customer.api.entity.OnboardingStatus;

class CustomerOnboardingTest {

    private static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 28, 12, 0);

    private Customer customer;
    private CustomerOnboarding onboarding;

    @BeforeEach
    void setUp() {
        customer = new Customer();
        customer.setId(UUID.randomUUID());
        customer.setCustomerNumber("CUST-001");
        customer.setType(CustomerType.INDIVIDUAL);
        customer.setStatus(CustomerStatus.PROSPECT);
        onboarding = new CustomerOnboarding(customer, "WEB_PORTAL", "system");
    }

    @Test
    void advanceTo_withOutcomeReason_capturesReasonForAbandoned() {
        onboarding.advanceTo(OnboardingStatus.KYC_IN_PROGRESS, NOW);
        onboarding.advanceTo(OnboardingStatus.ABANDONED, "Customer did not complete KYC", NOW);

        assertThat(onboarding.getStatus()).isEqualTo(OnboardingStatus.ABANDONED);
        assertThat(onboarding.getOutcomeReason()).isEqualTo("Customer did not complete KYC");
    }

    @Test
    void advanceTo_withOutcomeReason_capturesReasonForRejected() {
        onboarding.advanceTo(OnboardingStatus.KYC_IN_PROGRESS, NOW);
        onboarding.advanceTo(OnboardingStatus.KYC_COMPLETED, NOW);
        onboarding.advanceTo(OnboardingStatus.ACCOUNT_SETUP, NOW);
        onboarding.advanceTo(OnboardingStatus.REJECTED, "Failed compliance check", NOW);

        assertThat(onboarding.getStatus()).isEqualTo(OnboardingStatus.REJECTED);
        assertThat(onboarding.getOutcomeReason()).isEqualTo("Failed compliance check");
    }

    @Test
    void advanceTo_withoutOutcomeReason_delegatesCorrectly() {
        onboarding.advanceTo(OnboardingStatus.KYC_IN_PROGRESS, NOW);

        assertThat(onboarding.getStatus()).isEqualTo(OnboardingStatus.KYC_IN_PROGRESS);
    }

    @Test
    void advanceTo_invalidTransition_throws() {
        assertThatThrownBy(() -> onboarding.advanceTo(OnboardingStatus.KYC_COMPLETED, NOW))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Can only advance to KYC_COMPLETED from KYC_IN_PROGRESS");
    }

    @Test
    void isCompleted_andTerminalFlags() {
        onboarding.advanceTo(OnboardingStatus.KYC_IN_PROGRESS, NOW);

        assertThat(onboarding.isCompleted()).isFalse();
        assertThat(onboarding.isTerminal()).isFalse();

        onboarding.advanceTo(OnboardingStatus.ABANDONED, "timeout", NOW);
        assertThat(onboarding.isTerminal()).isTrue();
    }

    @Test
    void advanceTo_canCompleteFromAccountSetupShortPath() {
        onboarding.advanceTo(OnboardingStatus.KYC_IN_PROGRESS, NOW);
        onboarding.advanceTo(OnboardingStatus.KYC_COMPLETED, NOW);
        onboarding.advanceTo(OnboardingStatus.ACCOUNT_SETUP, NOW);
        onboarding.advanceTo(OnboardingStatus.COMPLETED, NOW);

        assertThat(onboarding.isCompleted()).isTrue();
        assertThat(onboarding.getCompletedAt()).isNotNull();
    }
}
