package com.openfinova.banking.customer.api.event;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import org.springframework.context.ApplicationEvent;

import java.util.UUID;

/**
 * Published by the customer module whenever a customer's lifecycle status changes
 * (BLOCKED, CLOSED, DECEASED, ANONYMIZED, ACTIVE, etc.).
 *
 * <p>Consumers (e.g. the identity module) listen for this event to cascade access
 * control changes — for example, disabling the linked identity account when a
 * customer is blocked or closed.
 *
 * <p>The event carries both the previous and new status so listeners can decide
 * which transitions require action without re-loading the customer record.
 */
public class CustomerLifecycleEvent extends ApplicationEvent {

    private final UUID customerId;
    private final CustomerStatus previousStatus;
    private final CustomerStatus newStatus;

    public CustomerLifecycleEvent(Object source, UUID customerId, CustomerStatus previousStatus,
            CustomerStatus newStatus) {
        super(source);
        this.customerId = customerId;
        this.previousStatus = previousStatus;
        this.newStatus = newStatus;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public CustomerStatus getPreviousStatus() {
        return previousStatus;
    }

    public CustomerStatus getNewStatus() {
        return newStatus;
    }
}
