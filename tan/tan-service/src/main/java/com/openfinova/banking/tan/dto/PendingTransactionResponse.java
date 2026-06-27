package com.openfinova.banking.tan.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

public record PendingTransactionResponse(
        UUID txnId, BigDecimal amount, String currency, String payeeIban, String payeeName, String description,
        Instant expiresAt
) {
}
