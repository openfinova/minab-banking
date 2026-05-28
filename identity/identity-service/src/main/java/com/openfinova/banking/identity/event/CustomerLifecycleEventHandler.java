package com.openfinova.banking.identity.event;

import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.event.CustomerLifecycleEvent;
import com.openfinova.banking.identity.audit.AuditEventDetail;
import com.openfinova.banking.identity.audit.SecurityAuditExtensions;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.service.SecurityAuditService;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Reacts to customer-party lifecycle status changes that should revoke access for the
 * linked identity user.
 *
 * <p>When a customer transitions to a status that indicates the party is no longer able to
 * operate (BLOCKED, CLOSED, DECEASED, ANONYMIZED), this handler automatically disables the
 * linked identity user account so that active sessions cannot be refreshed and new logins are
 * rejected immediately.
 *
 * <p>The handler runs in a dedicated transaction after the customer status change commits
 * ({@code AFTER_COMMIT}), ensuring the customer update is not rolled back if the identity
 * update fails. A failure here will be logged; the customer-service operation is
 * independently complete.
 */
@Component
public class CustomerLifecycleEventHandler {

    private static final Logger log = LoggerFactory.getLogger(CustomerLifecycleEventHandler.class);

    /** Statuses that indicate a customer party can no longer conduct banking business. */
    private static final Set<CustomerStatus> ACCESS_REVOKING_STATUSES = Set
            .of(CustomerStatus.BLOCKED, CustomerStatus.CLOSED, CustomerStatus.DECEASED, CustomerStatus.ANONYMIZED);

    private final UserRepository userRepository;
    private final SecurityAuditService auditService;
    private final DateTimeService dateTimeService;

    public CustomerLifecycleEventHandler(UserRepository userRepository, SecurityAuditService auditService,
            DateTimeService dateTimeService) {
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Disables the identity user linked to the customer when the customer's status
     * changes to an access-revoking state.
     *
     * @param event the customer lifecycle status-change event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void onCustomerStatusChanged(CustomerLifecycleEvent event) {
        if (!ACCESS_REVOKING_STATUSES.contains(event.getNewStatus())) {
            return;
        }

        userRepository.findByCustomerPartyId(event.getCustomerId()).ifPresent(user -> {
            if (!user.isEnabled()) {
                log.debug(
                        "Identity user {} is already disabled; no action needed for customer {} → {}",
                        user.getUsername(),
                        event.getCustomerId(),
                        event.getNewStatus());
                return;
            }

            log.info(
                    "Auto-disabling identity user {} (id={}) because customer {} transitioned to {}",
                    user.getUsername(),
                    user.getId(),
                    event.getCustomerId(),
                    event.getNewStatus());

            user.setEnabled(false);
            user.setDisabledAt(dateTimeService.now());
            userRepository.save(user);

            String details = "Automatically disabled: linked customer party " + event.getCustomerId()
                    + " transitioned to " + event.getNewStatus();
            auditService.recordParticipating(
                    SecurityAuditEventType.ACCOUNT_DISABLED,
                    user.getId(),
                    user.getUsername(),
                    details,
                    null,
                    SecurityAuditExtensions.NONE,
                    AuditEventDetail.customerStatusRevocation(event.getNewStatus().name(), event.getCustomerId()));
        });
    }
}
