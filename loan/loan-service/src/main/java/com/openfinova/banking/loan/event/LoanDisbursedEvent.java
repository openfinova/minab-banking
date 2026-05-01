package com.openfinova.banking.loan.event;

import java.time.LocalDate;
import java.util.UUID;

public record LoanDisbursedEvent(UUID loanAccountId, LocalDate disbursementDate, String disbursedBy) {
}
