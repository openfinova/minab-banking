package com.openfinova.banking.compliance.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.compliance.entity.MonitoringRule;
import com.openfinova.banking.compliance.entity.AmlAlert;
import com.openfinova.banking.compliance.repository.AmlAlertRepository;
import com.openfinova.banking.compliance.repository.MonitoringRuleRepository;
import com.openfinova.banking.customer.account.api.CustomerAccountService;
import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.notification.api.NotificationService;
import com.openfinova.banking.notification.api.dto.SendNotificationCommand;
import com.openfinova.banking.notification.api.entity.NotificationChannel;
import com.openfinova.banking.notification.api.entity.NotificationSeverity;

/**
 * Post-settlement transaction monitoring: deterministic rules, persisted alerts, inbox notifications,
 * and optional investigative account holds (see {@link MonitoringRule#isInvestigationHoldRecommended()}).
 */
@Service
public class PostedTransactionAmlMonitoringService {

    private static final Logger logger = LoggerFactory.getLogger(PostedTransactionAmlMonitoringService.class);

    private static final String COMPLIANCE_RECIPIENT = "COMPLIANCE_INBOX";

    /** Must match banking-app {@code BankingCacheNames.COMPLIANCE_RULES}. */
    public static final String CACHE_NAME = "complianceRules";

    private final AmlAlertRepository amlAlertRepository;
    private final CustomerAccountService customerAccountService;
    private final CustomerInfoService customerInfoService;
    private final NotificationService notificationService;
    private final MonitoringRuleRepository monitoringRuleRepository;

    public PostedTransactionAmlMonitoringService(AmlAlertRepository amlAlertRepository,
            CustomerAccountService customerAccountService, CustomerInfoService customerInfoService,
            NotificationService notificationService, MonitoringRuleRepository monitoringRuleRepository) {
        this.amlAlertRepository = amlAlertRepository;
        this.customerAccountService = customerAccountService;
        this.customerInfoService = customerInfoService;
        this.notificationService = notificationService;
        this.monitoringRuleRepository = monitoringRuleRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void evaluatePostedTransaction(UUID transactionId, UUID sourceAccountId, BigDecimal amount, String currency,
            String transactionTypeName) {
        if (transactionId == null || sourceAccountId == null || amount == null || currency == null
                || currency.isBlank()) {
            logger.debug("Skipping AML monitoring — missing transaction/account/amount/currency");
            return;
        }
        Optional<UUID> customerPartyIdOpt = customerAccountService.getPrimaryUserProfileIdForAccount(sourceAccountId)
                .flatMap(customerInfoService::getCustomerIdByLinkedIdentityUserId);

        List<MonitoringRule> rules = findEnabledOrdered();
        for (MonitoringRule rule : rules) {
            if (!rule.matches(amount, transactionTypeName)) {
                continue;
            }
            createAlertAndEscalate(
                    rule,
                    transactionId,
                    sourceAccountId,
                    customerPartyIdOpt.orElse(null),
                    amount,
                    currency,
                    transactionTypeName == null ? "" : transactionTypeName);
        }
    }

    @Cacheable(value = CACHE_NAME, key = "'enabled_ordered'")
    public List<MonitoringRule> findEnabledOrdered() {
        return monitoringRuleRepository.findAllByEnabledTrueOrderBySortOrderAsc();
    }

    private void createAlertAndEscalate(MonitoringRule rule, UUID transactionId, UUID sourceAccountId,
            UUID customerPartyId, BigDecimal amount, String currency, String transactionTypeName) {
        AmlAlert alert = new AmlAlert();
        alert.setTransactionId(transactionId);
        alert.setSourceAccountId(sourceAccountId);
        alert.setCustomerPartyId(customerPartyId);
        alert.setRuleCode(rule.getCode());
        alert.setSeverity(rule.getSeverity());
        alert.setMonitoredAmount(amount);
        alert.setCurrency(currency);
        alert.setTransactionTypeName(transactionTypeName);
        alert.setDetailSummary(
                String.format(
                        "%s — amount %s %s on account %s (tx %s, type %s)",
                        rule.getDisplayName(),
                        amount.toPlainString(),
                        currency,
                        sourceAccountId,
                        transactionId,
                        transactionTypeName));

        try {
            amlAlertRepository.saveAndFlush(alert);
        } catch (DataIntegrityViolationException ex) {
            logger.debug("Skipping duplicate AML alert for transaction {} and rule {}", transactionId, rule.getCode());
            return;
        }

        notifyCompliance(rule, alert);

        if (rule.isInvestigationHoldRecommended()) {
            placeInvestigationHold(alert, amount, currency);
        }
    }

    private void notifyCompliance(MonitoringRule rule, AmlAlert alert) {
        try {
            NotificationSeverity sev = switch (alert.getSeverity()) {
                case INFO -> NotificationSeverity.INFO;
                case WARNING, HIGH -> NotificationSeverity.WARNING;
                case CRITICAL -> NotificationSeverity.CRITICAL;
            };

            SendNotificationCommand cmd = new SendNotificationCommand(
                    COMPLIANCE_RECIPIENT,
                    "AML alert: " + rule.getCode(),
                    alert.getDetailSummary() + " [alert=" + alert.getId() + "]",
                    NotificationChannel.INBOX_ONLY,
                    sev);
            notificationService.sendNotification(cmd);
        } catch (RuntimeException ex) {
            logger.warn("Failed to enqueue compliance notification for alert {}: {}", alert.getId(), ex.getMessage());
        }
    }

    private void placeInvestigationHold(AmlAlert alert, BigDecimal amount, String currency) {
        try {
            customerAccountService.placeComplianceInvestigationHold(
                    alert.getSourceAccountId(),
                    amount,
                    currency,
                    "AML investigation hold [" + alert.getId() + "]",
                    alert.getId().toString());
            alert.setInvestigationHoldPlaced(true);
            amlAlertRepository.save(alert);
        } catch (RuntimeException ex) {
            logger.warn(
                    "Could not place AML investigation hold for alert {} on account {}: {}",
                    alert.getId(),
                    alert.getSourceAccountId(),
                    ex.getMessage());
        }
    }
}
