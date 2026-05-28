package com.openfinova.banking.loan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.openfinova.banking.loan.dto.ScheduleCalculation;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.loan.repository.LoanScheduleRepository;
import com.openfinova.banking.setup.api.DateTimeService;

class LoanScheduleServiceAmortizationTest {

    private LoanScheduleService service;

    @BeforeEach
    void setUp() {
        service = new LoanScheduleService(
                mock(LoanScheduleRepository.class),
                mock(LoanAccountRepository.class),
                mock(DateTimeService.class));
    }

    @Test
    void monthlySchedule_hasExpectedInstallmentCountAndPrincipalSum() {
        LocalDate first = LocalDate.of(2026, 1, 1);
        List<ScheduleCalculation> rows = service.calculateAmortizationSchedule(
                new BigDecimal("12000.00"),
                new BigDecimal("12.0"),
                12,
                first,
                "MONTHLY");

        assertEquals(12, rows.size());
        BigDecimal principalSum = rows.stream().map(ScheduleCalculation::getPrincipalDue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        assertEquals(0, new BigDecimal("12000.00").compareTo(principalSum.setScale(2, java.math.RoundingMode.HALF_UP)));
    }

    @Test
    void quarterlySchedule_stepsDueDatesByThreeMonths() {
        LocalDate first = LocalDate.of(2026, 1, 15);
        List<ScheduleCalculation> rows = service
                .calculateAmortizationSchedule(new BigDecimal("9000.00"), BigDecimal.ZERO, 12, first, "QUARTERLY");

        assertEquals(4, rows.size());
        assertEquals(first, rows.get(0).getDueDate());
        assertEquals(LocalDate.of(2026, 4, 15), rows.get(1).getDueDate());
        assertTrue(rows.get(0).getInterestDue().compareTo(BigDecimal.ZERO) >= 0);
    }
}
