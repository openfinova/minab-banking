package com.openfinova.banking.tp.testsupport;

import java.math.BigDecimal;

import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.entity.TransactionRequest;

public final class TpEntityTestFixtures {

    private TpEntityTestFixtures() {
    }

    public static TransactionRequest basicRequest() {
        return new TransactionRequest("idem-unit-1", TransactionType.TRANSFER, new BigDecimal("100.0000"), "tester");
    }

    public static Transaction transaction() {
        return new Transaction(basicRequest());
    }
}
