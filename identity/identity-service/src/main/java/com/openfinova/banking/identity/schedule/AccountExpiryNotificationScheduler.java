package com.openfinova.banking.identity.schedule;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.identity.config.AccountLifecycleProperties;
import com.openfinova.banking.identity.entity.AccountProvisioningStatus;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.event.UserAccountExpiryWarningEvent;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.service.SecurityAuditService;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Publishes {@link UserAccountExpiryWarningEvent} for accounts approaching
 * {@code accountExpiresAt}.
 */
@Component
public class AccountExpiryNotificationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AccountExpiryNotificationScheduler.class);

    private final UserRepository userRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final AccountLifecycleProperties lifecycleProperties;
    private final DateTimeService dateTimeService;
    private final SecurityAuditService auditService;

    public AccountExpiryNotificationScheduler(UserRepository userRepository, ApplicationEventPublisher eventPublisher,
            AccountLifecycleProperties lifecycleProperties, DateTimeService dateTimeService,
            SecurityAuditService auditService) {
        this.userRepository = userRepository;
        this.eventPublisher = eventPublisher;
        this.lifecycleProperties = lifecycleProperties;
        this.dateTimeService = dateTimeService;
        this.auditService = auditService;
    }

    @Scheduled(cron = "${identity.lifecycle.expiry-notification-cron:0 0 7 * * *}")
    @Transactional
    public void sendExpiryWarnings() {
        if (!lifecycleProperties.isExpiryNotificationsEnabled()) {
            return;
        }
        int leadDays = lifecycleProperties.getExpiryWarningLeadDays();
        if (leadDays <= 0) {
            return;
        }

        var now = dateTimeService.now();
        var horizon = now.plusDays(leadDays);
        var candidates = userRepository
                .findAccountsInExpiryWarningWindow(now, horizon, AccountProvisioningStatus.ACTIVE);

        candidates.stream().filter(user -> user.getAccountExpiryWarningNotifiedAt() == null).forEach(user -> {
            eventPublisher.publishEvent(
                    new UserAccountExpiryWarningEvent(
                            this,
                            user.getId(),
                            user.getUsername(),
                            user.getAccountExpiresAt(),
                            now));
            user.setAccountExpiryWarningNotifiedAt(now);
            userRepository.save(user);
            auditService.recordParticipating(
                    SecurityAuditEventType.ACCOUNT_EXPIRY_WARNING_SENT,
                    user.getId(),
                    user.getUsername(),
                    "Account expires at " + user.getAccountExpiresAt());
            log.info(
                    "Published account expiry warning for user {} expiring {}",
                    user.getUsername(),
                    user.getAccountExpiresAt());
        });
    }
}
