package com.openfinova.banking.compliance.event;

import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.openfinova.banking.compliance.api.ComplianceService;
import com.openfinova.banking.tp.api.event.TransactionCompletedEvent;

/**
 * Listens after TP commits so monitoring sees posted state without blocking synchronous posting.
 */
@Component
public class TransactionCompletedComplianceListener {

    private final ComplianceService complianceService;

    public TransactionCompletedComplianceListener(ComplianceService complianceService) {
        this.complianceService = complianceService;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onPosted(TransactionCompletedEvent event) {
        complianceService.evaluatePostedTransaction(
                event.getTransactionId(),
                event.getSourceAccountId(),
                event.getAmount(),
                event.getCurrency(),
                event.getTransactionType());
    }
}
