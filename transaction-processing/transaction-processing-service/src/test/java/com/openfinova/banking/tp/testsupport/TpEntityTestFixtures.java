package com.openfinova.banking.tp.testsupport;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.entity.TransactionRequest;

public final class TpEntityTestFixtures {

    public static final LocalDate TODAY = LocalDate.of(2026, 5, 28);
    public static final LocalDateTime NOW = LocalDateTime.of(2026, 5, 28, 12, 0);

    private TpEntityTestFixtures() {
    }

    public static TransactionRequest basicRequest() {
        return new TransactionRequest("idem-unit-1", TransactionType.TRANSFER, new BigDecimal("100.0000"));
    }

    public static Transaction transaction() {
        return new Transaction(basicRequest(), TODAY, NOW);
    }
}
