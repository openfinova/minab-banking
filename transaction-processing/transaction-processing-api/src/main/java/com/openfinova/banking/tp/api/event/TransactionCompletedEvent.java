package com.openfinova.banking.tp.api.event;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Published after a transaction is posted. Consumed by compliance AML monitoring and other modules.
 */
public class TransactionCompletedEvent {

    private final UUID transactionId;
    private final UUID sourceAccountId;
    private final BigDecimal amount;
    private final String currency;
    private final String transactionType;

    public TransactionCompletedEvent(UUID transactionId, UUID sourceAccountId, BigDecimal amount, String currency,
            String transactionType) {
        this.transactionId = transactionId;
        this.sourceAccountId = sourceAccountId;
        this.amount = amount;
        this.currency = currency;
        this.transactionType = transactionType;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public UUID getSourceAccountId() {
        return sourceAccountId;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public String getCurrency() {
        return currency;
    }

    public String getTransactionType() {
        return transactionType;
    }
}
