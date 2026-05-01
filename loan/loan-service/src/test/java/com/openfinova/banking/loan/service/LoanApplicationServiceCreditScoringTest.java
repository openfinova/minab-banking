package com.openfinova.banking.loan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.openfinova.banking.loan.entity.LoanApplication;
import com.openfinova.banking.loan.repository.LoanApplicationRepository;

class LoanApplicationServiceCreditScoringTest {

    @Test
    void performCreditScoring_zeroMonthlyIncome_doesNotDivideByZero() {
        LoanApplicationRepository repo = mock(LoanApplicationRepository.class);
        LoanApplicationService service = new LoanApplicationService(repo);

        UUID id = UUID.randomUUID();
        LoanApplication app = new LoanApplication();
        app.setMonthlyIncome(BigDecimal.ZERO);
        app.setExistingObligations(new BigDecimal("100.00"));

        when(repo.findById(id)).thenReturn(Optional.of(app));
        when(repo.save(any(LoanApplication.class))).thenAnswer(inv -> inv.getArgument(0));

        BigDecimal score = service.performCreditScoring(id);

        assertEquals(0, BigDecimal.valueOf(600).compareTo(score));
        verify(repo).save(app);
    }
}
