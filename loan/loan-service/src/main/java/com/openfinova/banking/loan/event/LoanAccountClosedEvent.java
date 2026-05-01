package com.openfinova.banking.loan.event;

import java.time.LocalDate;
import java.util.UUID;

public record LoanAccountClosedEvent(UUID loanAccountId, LocalDate closedOn, String closedBy) {
}
