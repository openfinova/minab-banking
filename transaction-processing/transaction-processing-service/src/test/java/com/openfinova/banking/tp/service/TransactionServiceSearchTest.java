package com.openfinova.banking.tp.service;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.openfinova.banking.customer.account.api.CustomerAccountService;
import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.exchangerate.api.ExchangeRateService;
import com.openfinova.banking.gl.api.GeneralLedgerService;
import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tp.api.dto.TransactionResponse;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.mapper.TransactionMapper;
import com.openfinova.banking.tp.repository.TransactionRepository;
import com.openfinova.banking.tp.repository.TransactionRequestRepository;

@ExtendWith(MockitoExtension.class)
class TransactionServiceSearchTest {

    @Mock
    private TransactionRepository transactionRepository;
    @Mock
    private TransactionRequestRepository transactionRequestRepository;
    @Mock
    private FeeManagementService feeManagementService;
    @Mock
    private VelocityLimitService velocityLimitService;
    @Mock
    private BalanceReservationService balanceReservationService;
    @Mock
    private GeneralLedgerService generalLedgerService;
    @Mock
    private ExchangeRateService exchangeRateService;
    @Mock
    private CustomerAccountService customerAccountService;
    @Mock
    private CustomerInfoService customerInfoService;
    @Mock
    private CompensationWorkflowService compensationWorkflowService;
    @Mock
    private DateTimeService dateTimeService;
    @Mock
    private TransactionMapper transactionMapper;

    @InjectMocks
    private TransactionService transactionService;

    @Test
    void searchTransactions_returnsEmptyWhenNoMatches() {
        when(transactionRepository.findAll(ArgumentMatchers.<Specification<Transaction>>any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        Page<TransactionResponse> result = transactionService
                .searchTransactions(null, null, null, null, null, null, null, null, null, PageRequest.of(0, 20));

        assertThat(result.getContent()).isEmpty();
        assertThat(result.getTotalElements()).isZero();
    }

}
