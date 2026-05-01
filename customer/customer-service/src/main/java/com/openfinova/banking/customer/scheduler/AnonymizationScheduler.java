package com.openfinova.banking.customer.scheduler;

import com.openfinova.banking.customer.entity.CustomerDataRetention;
import com.openfinova.banking.customer.service.AnonymizationService;
import com.openfinova.banking.customer.service.DataSubjectRequestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.UUID;

/**
 * Nightly batch job that automates privacy compliance tasks.
 *
 * Scheduled tasks:
 * - Anonymization sweep (02:00 UTC) — Finds all customers whose AML/CFT
 * retention period has expired and anonymizes their PII.
 * - Deferred erasure resolution (02:30 UTC) — Finds DEFERRED erasure DSARs
 * whose deferral date has been reached and triggers anonymization + fulfillment.
 * - SLA overdue alert (08:00 UTC on weekdays) — Logs overdue DSARs so the
 * compliance team is alerted for manual review.
 */
@Component
public class AnonymizationScheduler {

    private static final Logger log = LoggerFactory.getLogger(AnonymizationScheduler.class);
    private static final String SYSTEM_ACTOR = "SYSTEM_SCHEDULER";

    private final AnonymizationService anonymizationService;
    private final DataSubjectRequestService dataSubjectRequestService;

    public AnonymizationScheduler(AnonymizationService anonymizationService,
            DataSubjectRequestService dataSubjectRequestService) {
        this.anonymizationService = anonymizationService;
        this.dataSubjectRequestService = dataSubjectRequestService;
    }

    /**
     * Daily sweep at 02:00 UTC — anonymize customers whose retention has expired.
     */
    @Scheduled(cron = "0 0 2 * * ?", zone = "UTC")
    public void runAnonymizationSweep() {
        String jobRef = "ANON-JOB-" + System.currentTimeMillis();
        log.info("[{}] Starting nightly anonymization sweep.", jobRef);

        List<CustomerDataRetention> dueRecords = anonymizationService.findDueForAnonymization();
        log.info("[{}] Found {} customer(s) due for anonymization.", jobRef, dueRecords.size());

        int success = 0;
        int failed = 0;
        for (CustomerDataRetention retention : dueRecords) {
            UUID customerId = retention.getCustomer().getId();
            try {
                anonymizationService.anonymizeCustomer(customerId, SYSTEM_ACTOR, jobRef);
                success++;
            } catch (Exception e) {
                failed++;
                log.error("[{}] Failed to anonymize customer {}: {}", jobRef, customerId, e.getMessage(), e);
            }
        }

        log.info("[{}] Anonymization sweep complete. Success: {}, Failed: {}.", jobRef, success, failed);
    }

    /**
     * Daily at 02:30 UTC — find DEFERRED erasure requests whose retention expiry
     * has now passed and auto-process them.
     */
    @Scheduled(cron = "0 30 2 * * ?", zone = "UTC")
    public void processDeferredErasureRequests() {
        log.info("Processing deferred erasure requests that are now due.");

        dataSubjectRequestService.getDeferredRequestsReadyForProcessing().forEach(request -> {
            UUID customerId = request.getCustomer().getId();
            UUID requestId = request.getId();
            try {
                // Trigger anonymization (idempotent — skips if already anonymized)
                anonymizationService.anonymizeCustomer(customerId, SYSTEM_ACTOR, null);
                // Fulfill the DSAR
                dataSubjectRequestService.fulfill(requestId, SYSTEM_ACTOR);
                log.info("Deferred erasure request {} fulfilled for customer {}.", requestId, customerId);
            } catch (Exception e) {
                log.error(
                        "Failed to process deferred erasure request {} for customer {}: {}",
                        requestId,
                        customerId,
                        e.getMessage(),
                        e);
            }
        });
    }

    /**
     * Weekdays at 08:00 UTC — alert on overdue DSARs (SLA breach warning for compliance team).
     */
    @Scheduled(cron = "0 0 8 * * MON-FRI", zone = "UTC")
    public void alertOnOverdueDsars() {
        var overdue = dataSubjectRequestService.getOverdueRequests();
        if (overdue.isEmpty()) {
            log.info("SLA check: No overdue DSARs.");
            return;
        }
        // Log at WARN level so monitoring/alerting systems can pick it up
        log.warn("SLA BREACH: {} overdue DSAR(s) require immediate attention:", overdue.size());
        overdue.forEach(
                r -> log.warn(
                        "  -> DSAR {} | type: {} | due: {} | customer: {}",
                        r.getReferenceNumber(),
                        r.getRequestType(),
                        r.getDueBy(),
                        r.getCustomer().getId()));
    }
}
