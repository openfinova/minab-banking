package com.openfinova.banking.loan.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.ApplicationEventPublisher;

import com.openfinova.banking.loan.dto.PaymentAllocation;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.loan.repository.LoanPaymentRepository;
import com.openfinova.banking.setup.api.DateTimeService;

class LoanPaymentServiceAllocationTest {

    private LoanPaymentService service;
    private LoanAccountRepository accountRepository;

    @BeforeEach
    void setUp() {
        accountRepository = mock(LoanAccountRepository.class);
        service = new LoanPaymentService(
                mock(LoanPaymentRepository.class),
                accountRepository,
                mock(LoanScheduleService.class),
                mock(ApplicationEventPublisher.class),
                mock(DateTimeService.class));
    }

    @Test
    void calculatePaymentAllocation_waterfallPaysPenaltiesFirst() {
        UUID id = UUID.randomUUID();
        LoanAccount account = new LoanAccount();
        account.setOutstandingPenalties(new BigDecimal("10.00"));
        account.setOutstandingFees(new BigDecimal("5.00"));
        account.setOutstandingInterest(new BigDecimal("20.00"));
        account.setOutstandingPrincipal(new BigDecimal("1000.00"));

        when(accountRepository.findById(id)).thenReturn(Optional.of(account));

        PaymentAllocation alloc = service.calculatePaymentAllocation(id, new BigDecimal("25.00"));

        assertEquals(0, new BigDecimal("10.00").compareTo(alloc.getPenaltiesAmount()));
        assertEquals(0, new BigDecimal("5.00").compareTo(alloc.getFeesAmount()));
        assertEquals(0, new BigDecimal("10.00").compareTo(alloc.getInterestAmount()));
        assertEquals(0, BigDecimal.ZERO.compareTo(alloc.getPrincipalAmount()));
    }
}
