package com.openfinova.banking.loan.event;

import java.math.BigDecimal;
import java.util.UUID;

public record LoanRepaymentRecordedEvent(UUID loanAccountId, UUID paymentId, BigDecimal amount, String recordedBy) {
}
