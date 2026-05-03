package com.openfinova.banking.loan.entity;

import java.math.BigDecimal;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.loan.api.entity.GuarantorStatus;
import com.openfinova.banking.loan.api.entity.GuarantorType;
import com.openfinova.banking.loan.api.entity.LoanStatus;
import com.openfinova.banking.loan.testsupport.LoanTestFixtures;

class LoanAccountTest {

    @Test
    void getTotalOutstanding_sumsComponents() {
        LoanAccount a = LoanTestFixtures.activeLoanAccount();
        assertThat(a.getTotalOutstanding()).isEqualByComparingTo(new BigDecimal("8012.5000"));
    }

    @Test
    void isDelinquent_whenDaysPastDuePositive() {
        LoanAccount a = LoanTestFixtures.activeLoanAccount();
        assertThat(a.isDelinquent()).isFalse();
        a.setDaysPastDue(3);
        assertThat(a.isDelinquent()).isTrue();
    }

    @Test
    void isClosed_forSettledOrClosedStatus() {
        LoanAccount a = LoanTestFixtures.activeLoanAccount();
        a.setStatus(LoanStatus.CLOSED);
        assertThat(a.isClosed()).isTrue();
        a.setStatus(LoanStatus.SETTLED);
        assertThat(a.isClosed()).isTrue();
    }

    @Test
    void canTransitionTo_respectsStateMachine() {
        LoanAccount a = LoanTestFixtures.activeLoanAccount();
        assertThat(a.canTransitionTo(LoanStatus.CLOSED)).isTrue();
        assertThat(a.canTransitionTo(LoanStatus.PENDING_APPROVAL)).isFalse();

        a.setStatus(LoanStatus.CLOSED);
        assertThat(a.canTransitionTo(LoanStatus.ACTIVE)).isFalse();
    }

    @Test
    void getActiveGuarantorCount_filtersByStatus() {
        LoanAccount a = LoanTestFixtures.activeLoanAccount();
        Guarantor g1 = new Guarantor();
        g1.setCustomerId(UUID.randomUUID());
        g1.setGuarantorType(GuarantorType.INDIVIDUAL);
        g1.setGuaranteedAmount(new BigDecimal("1000"));
        g1.setStatus(GuarantorStatus.ACTIVE);
        Guarantor g2 = new Guarantor();
        g2.setCustomerId(UUID.randomUUID());
        g2.setGuarantorType(GuarantorType.INDIVIDUAL);
        g2.setGuaranteedAmount(new BigDecimal("500"));
        g2.setStatus(GuarantorStatus.REMOVED);
        a.addGuarantor(g1);
        a.addGuarantor(g2);
        assertThat(a.getActiveGuarantorCount()).isEqualTo(1);
    }

    @Test
    void addGuarantor_setsBackReference() {
        LoanAccount a = LoanTestFixtures.activeLoanAccount();
        Guarantor g = new Guarantor();
        g.setCustomerId(UUID.randomUUID());
        g.setGuarantorType(GuarantorType.INDIVIDUAL);
        g.setGuaranteedAmount(BigDecimal.ONE);
        a.addGuarantor(g);
        assertThat(g.getLoanAccount()).isSameAs(a);
    }
}
