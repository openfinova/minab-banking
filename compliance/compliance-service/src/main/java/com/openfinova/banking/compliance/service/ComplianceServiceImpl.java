package com.openfinova.banking.compliance.service;

import java.math.BigDecimal;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.openfinova.banking.compliance.api.ComplianceService;
import com.openfinova.banking.compliance.api.entity.RiskRating;
import com.openfinova.banking.tp.api.event.TransactionCompletedEvent;

@Service
@Transactional
public class ComplianceServiceImpl implements ComplianceService {

    private final ComplianceEvaluationService complianceEvaluationService;
    private final ScreeningService screeningService;
    private final PostedTransactionAmlMonitoringService postedTransactionAmlMonitoringService;

    public ComplianceServiceImpl(ComplianceEvaluationService complianceEvaluationService,
            ScreeningService screeningService,
            PostedTransactionAmlMonitoringService postedTransactionAmlMonitoringService) {
        this.complianceEvaluationService = complianceEvaluationService;
        this.screeningService = screeningService;
        this.postedTransactionAmlMonitoringService = postedTransactionAmlMonitoringService;
    }

    @Override
    public RiskRating evaluateCustomerRisk(UUID customerId) {
        return complianceEvaluationService.evaluateCustomerRisk(customerId);
    }

    @Override
    public boolean performSanctionsScreening(String partyName, String country) {
        return screeningService.checkSanctions(partyName, country);
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void evaluatePostedTransaction(UUID transactionId, UUID sourceAccountId, BigDecimal amount, String currency,
            String transactionTypeName) {
        postedTransactionAmlMonitoringService
                .evaluatePostedTransaction(transactionId, sourceAccountId, amount, currency, transactionTypeName);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    public void onPosted(TransactionCompletedEvent event) {
        evaluatePostedTransaction(
                event.getTransactionId(),
                event.getSourceAccountId(),
                event.getAmount(),
                event.getCurrency(),
                event.getTransactionType());
    }
}
