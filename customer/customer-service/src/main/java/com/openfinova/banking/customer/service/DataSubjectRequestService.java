package com.openfinova.banking.customer.service;

import com.openfinova.banking.customer.api.entity.DataSubjectRequestStatus;
import com.openfinova.banking.customer.api.entity.DataSubjectRequestType;
import com.openfinova.banking.customer.entity.*;
import com.openfinova.banking.customer.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Service managing the full lifecycle of Data Subject Requests (DSARs) under GDPR Arts. 15–22.
 *
 * Responsibilities:
 *   Submit — create a new DSAR and assign a reference number.
 *   Review — advance the request to IDENTITY_VERIFICATION then IN_REVIEW.
 *   Fulfill / Reject / Defer — record the outcome with a legal basis.
 *   Withdraw — allow the customer to cancel their own request.
 *   SLA monitoring — surface overdue requests for compliance alerting.
 *
 * Erasure (Art. 17) requests cannot be fulfilled while an AML/CFT retention
 * obligation is in force. In that case, the request is DEFERRED until
 * {@link CustomerDataRetention#getRetentionExpiresAt()}.
 */
@Service
@Transactional
public class DataSubjectRequestService {

    private static final Logger log = LoggerFactory.getLogger(DataSubjectRequestService.class);

    private final DataSubjectRequestRepository dsarRepository;
    private final CustomerRepository customerRepository;
    private final CustomerDataRetentionRepository retentionRepository;
    private final CustomerAuditLogRepository auditLogRepository;

    public DataSubjectRequestService(DataSubjectRequestRepository dsarRepository, CustomerRepository customerRepository,
            CustomerDataRetentionRepository retentionRepository, CustomerAuditLogRepository auditLogRepository) {
        this.dsarRepository = dsarRepository;
        this.customerRepository = customerRepository;
        this.retentionRepository = retentionRepository;
        this.auditLogRepository = auditLogRepository;
    }

    /**
     * Submit a new data subject request on behalf of a customer.
     *
     * @param customerId    customer who is the data subject
     * @param requestType   type of GDPR right being exercised
     * @param channel       submission channel (EMAIL, PORTAL, BRANCH, etc.)
     * @param customerNotes optional message from the customer
     * @return the persisted request, including generated reference number
     */
    public DataSubjectRequest submitRequest(UUID customerId, DataSubjectRequestType requestType, String channel,
            String customerNotes) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        DataSubjectRequest request = new DataSubjectRequest(customer, requestType, channel, customerNotes);
        request.setReferenceNumber(generateReferenceNumber(requestType));

        // For ERASURE requests: check if a retention obligation exists
        if (requestType == DataSubjectRequestType.ERASURE) {
            retentionRepository.findByCustomerId(customerId).ifPresent(retention -> {
                if (!retention.isAnonymized() && !retention.isRetentionExpired()) {
                    // Cannot erase yet — defer to retention expiry
                    request.defer(
                            retention.getRetentionExpiresAt(),
                            """
                                    Legal retention obligation in force under %s until %s. Request will be re-evaluated automatically.\
                                    """
                                    .formatted(retention.getLegalBasis(), retention.getRetentionExpiresAt()),
                            "SYSTEM");
                    log.info(
                            "Erasure request {} deferred to {} (customer {}) due to retention obligation.",
                            request.getReferenceNumber(),
                            retention.getRetentionExpiresAt(),
                            customerId);
                }
            });
        }

        DataSubjectRequest saved = dsarRepository.save(request);

        auditLogRepository.save(
                new CustomerAuditLog(
                        customer,
                        CustomerAuditAction.DSAR_SUBMITTED,
                        "dataSubjectRequest",
                        null,
                        requestType.name() + " | ref: " + saved.getReferenceNumber(),
                        "CUSTOMER:" + customerId));

        log.info("DSAR {} submitted for customer {} (type: {}).", saved.getReferenceNumber(), customerId, requestType);
        return saved;
    }

    /**
     * Advance a request to IN_REVIEW status after identity has been verified.
     */
    public DataSubjectRequest confirmIdentityVerified(UUID requestId, String verifiedBy) {
        DataSubjectRequest request = findOrThrow(requestId);
        assertNotTerminal(request);
        request.setStatus(DataSubjectRequestStatus.IN_REVIEW);
        request.setHandledBy(verifiedBy);
        return dsarRepository.save(request);
    }

    /**
     * Fulfill a request (mark as responded to).
     */
    public DataSubjectRequest fulfill(UUID requestId, String handledBy) {
        DataSubjectRequest request = findOrThrow(requestId);
        assertNotTerminal(request);
        request.markFulfilled(handledBy);

        auditLogRepository.save(
                new CustomerAuditLog(
                        request.getCustomer(),
                        CustomerAuditAction.DSAR_FULFILLED,
                        "dataSubjectRequest",
                        DataSubjectRequestStatus.IN_REVIEW.name(),
                        DataSubjectRequestStatus.FULFILLED.name(),
                        handledBy));

        dsarRepository.save(request);
        log.info("DSAR {} fulfilled by {}.", requestId, handledBy);
        return request;
    }

    /**
     * Reject a request with a documented reason and legal basis.
     */
    public DataSubjectRequest reject(UUID requestId, String reason, String handledBy) {
        DataSubjectRequest request = findOrThrow(requestId);
        assertNotTerminal(request);
        request.reject(reason, handledBy);

        auditLogRepository.save(
                new CustomerAuditLog(
                        request.getCustomer(),
                        CustomerAuditAction.DSAR_REJECTED,
                        "dataSubjectRequest",
                        request.getStatus().name(),
                        DataSubjectRequestStatus.REJECTED.name(),
                        handledBy));

        dsarRepository.save(request);
        log.info("DSAR {} rejected by {}. Reason: {}", requestId, handledBy, reason);
        return request;
    }

    /**
     * Allow a customer to withdraw their own request.
     */
    public DataSubjectRequest withdraw(UUID requestId, UUID customerId) {
        DataSubjectRequest request = findOrThrow(requestId);
        if (!request.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException("Request does not belong to this customer.");
        }
        assertNotTerminal(request);
        request.setStatus(DataSubjectRequestStatus.WITHDRAWN);

        dsarRepository.save(request);
        log.info("DSAR {} withdrawn by customer {}.", requestId, customerId);
        return request;
    }

    /**
     * Extend the SLA deadline when the request is complex (max +60 days, GDPR Art. 12(3)).
     * Must be called within the original 30-day window.
     */
    public DataSubjectRequest extendDeadline(UUID requestId, int additionalDays, String handledBy) {
        DataSubjectRequest request = findOrThrow(requestId);
        assertNotTerminal(request);
        request.extendDeadline(additionalDays);
        request.setHandledBy(handledBy);
        return dsarRepository.save(request);
    }

    // ---- Queries ----

    @Transactional(readOnly = true)
    public List<DataSubjectRequest> getRequestsForCustomer(UUID customerId) {
        return dsarRepository.findByCustomerIdOrderByReceivedAtDesc(customerId);
    }

    @Transactional(readOnly = true)
    public List<DataSubjectRequest> getOverdueRequests() {
        return dsarRepository.findOverdueRequests(LocalDate.now());
    }

    @Transactional(readOnly = true)
    public List<DataSubjectRequest> getDeferredRequestsReadyForProcessing() {
        return dsarRepository.findReadyToProcessDeferredRequests(LocalDate.now());
    }

    // ---- Private helpers ----

    private DataSubjectRequest findOrThrow(UUID requestId) {
        return dsarRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("DSAR not found: " + requestId));
    }

    private void assertNotTerminal(DataSubjectRequest request) {
        DataSubjectRequestStatus s = request.getStatus();
        if (s == DataSubjectRequestStatus.FULFILLED || s == DataSubjectRequestStatus.REJECTED
                || s == DataSubjectRequestStatus.WITHDRAWN) {
            throw new IllegalStateException("Request " + request.getId() + " is already in terminal state: " + s);
        }
    }

    private String generateReferenceNumber(DataSubjectRequestType type) {
        String prefix = switch (type) {
            case ACCESS -> "DSR-ACC";
            case ERASURE -> "DSR-ERA";
            case PORTABILITY -> "DSR-PORT";
            case RECTIFICATION -> "DSR-RECT";
            case OBJECTION -> "DSR-OBJ";
            case RESTRICTION -> "DSR-RESTR";
        };
        return prefix + "-" + System.currentTimeMillis() % 1_000_000L;
    }
}
