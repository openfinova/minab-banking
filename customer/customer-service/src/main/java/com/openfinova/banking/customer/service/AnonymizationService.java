package com.openfinova.banking.customer.service;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.event.CustomerLifecycleEvent;
import com.openfinova.banking.customer.entity.*;
import com.openfinova.banking.customer.repository.*;
import com.openfinova.banking.setup.api.DateTimeService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

/**
 * Service responsible for anonymizing customer personal data after the
 * mandatory AML/CFT retention period has expired.
 *
 * Anonymization strategy
 * PII string fields (name, tax ID, date of birth, etc.) are set to null
 * or replaced with a non-reversible token (taxId).
 * All {@link CustomerAddress} and {@link ContactDetail} rows are
 * physically deleted — they contain direct identifiers.
 * All {@link IdentificationDocument} rows are physically deleted.
 * The {@link Customer} record is retained (ANONYMIZED status) to
 * preserve the audit trail, account history, and transaction references.
 * A {@link CustomerAuditLog} entry is written for the anonymization event.
 * The {@link CustomerDataRetention} record is updated to reflect completion.
 *
 * What is NOT deleted
 * Transaction records (general ledger) — required by AML rules.
 * KYC audit logs, DSARs, consent history — required for accountability.
 * Risk profiles — required for ongoing monitoring analysis.
 */
@Service
@Transactional
public class AnonymizationService {

    private static final Logger log = LoggerFactory.getLogger(AnonymizationService.class);

    private static final String ANONYMIZED_TAX_ID_PREFIX = "ANON-";

    private final CustomerRepository customerRepository;
    private final CustomerAddressRepository addressRepository;
    private final ContactDetailRepository contactDetailRepository;
    private final IdentificationDocumentRepository documentRepository;
    private final CustomerAuditLogRepository auditLogRepository;
    private final CustomerDataRetentionRepository retentionRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final DateTimeService dateTimeService;
    private final byte[] hmacSecret;

    public AnonymizationService(CustomerRepository customerRepository, CustomerAddressRepository addressRepository,
            ContactDetailRepository contactDetailRepository, IdentificationDocumentRepository documentRepository,
            CustomerAuditLogRepository auditLogRepository, CustomerDataRetentionRepository retentionRepository,
            ApplicationEventPublisher eventPublisher, DateTimeService dateTimeService,
            @Value("${customer.anonymization.hmac-secret:dev-default-change-in-production}") String hmacSecret) {
        this.customerRepository = customerRepository;
        this.addressRepository = addressRepository;
        this.contactDetailRepository = contactDetailRepository;
        this.documentRepository = documentRepository;
        this.auditLogRepository = auditLogRepository;
        this.retentionRepository = retentionRepository;
        this.eventPublisher = eventPublisher;
        this.dateTimeService = dateTimeService;
        this.hmacSecret = hmacSecret.getBytes(StandardCharsets.UTF_8);
    }

    /**
     * Anonymize a single customer's personal data.
     *
     * @param customerId   the customer to anonymize
     * @param anonymizedBy user ID or "SYSTEM_SCHEDULER" that triggered the job
     * @param jobReference optional batch job reference; null for manual runs
     * @throws IllegalStateException if the customer is not found,
     *                               already anonymized, or retention has not yet expired
     */
    public void anonymizeCustomer(UUID customerId, String anonymizedBy, String jobReference) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalStateException("Customer not found: " + customerId));

        if (customer.getStatus() == CustomerStatus.ANONYMIZED) {
            log.info("Customer {} is already anonymized — skipping.", customerId);
            return;
        }

        CustomerDataRetention retention = retentionRepository.findByCustomerId(customerId)
                .orElseThrow(() -> new IllegalStateException("No retention record found for customer: " + customerId));

        if (!retention.isRetentionExpired(dateTimeService.today())) {
            throw new IllegalStateException(
                    "Retention period has not yet expired for customer " + customerId + ". Expires: "
                            + retention.getRetentionExpiresAt());
        }

        log.info(
                "Starting anonymization for customer {} (triggered by: {}, job: {})",
                customerId,
                anonymizedBy,
                jobReference);

        // 1. Null out all direct PII fields on Customer
        CustomerStatus previousStatus = customer.getStatus();
        wipePiiFields(customer);

        // 2. Update status to ANONYMIZED
        customer.setStatus(CustomerStatus.ANONYMIZED);

        // 3. Hard-delete addresses & contact details (direct identifiers)
        addressRepository.deleteByCustomerId(customerId);
        contactDetailRepository.deleteByCustomerId(customerId);

        // 4. Hard-delete identification documents (contain document numbers / images)
        List<IdentificationDocument> docs = documentRepository.findByCustomerId(customerId);
        documentRepository.deleteAll(docs);

        // 5. Persist the anonymized customer
        customerRepository.save(customer);

        // 6. Write immutable audit log entry
        CustomerAuditLog auditEntry = new CustomerAuditLog(
                customer,
                CustomerAuditAction.ANONYMIZATION,
                "status",
                CustomerStatus.ACTIVE.name(),
                CustomerStatus.ANONYMIZED.name(),
                anonymizedBy);
        auditEntry.setChannel("BATCH_JOB");
        auditEntry.setChangeReason(
                "GDPR Art. 17 / GDPR Art. 5(1)(e) — retention period expired on " + retention.getRetentionExpiresAt());
        auditLogRepository.save(auditEntry);

        // 7. Update the retention record
        retention.recordAnonymization(anonymizedBy, jobReference, dateTimeService.now());
        retentionRepository.save(retention);

        // 8. Notify identity module to revoke access for the linked identity user
        eventPublisher
                .publishEvent(new CustomerLifecycleEvent(this, customerId, previousStatus, CustomerStatus.ANONYMIZED));

        log.info("Anonymization complete for customer {}.", customerId);
    }

    /**
     * Find all customers whose retention has expired and who have not yet been anonymized.
     * Intended for use by the nightly scheduler.
     */
    @Transactional(readOnly = true)
    public List<CustomerDataRetention> findDueForAnonymization() {
        return retentionRepository.findByAnonymizedFalseAndRetentionExpiresAtBefore(dateTimeService.today());
    }

    /**
     * Nullify all direct personal information fields on a Customer entity.
     *
     * @param customer the customer to anonymize
     */
    private void wipePiiFields(Customer customer) {
        // Replace taxId with HMAC-SHA256 token (non-reversible, preserves uniqueness constraint).
        // MD5 (UUID.nameUUIDFromBytes) is insecure for tax IDs due to small input space.
        if (customer.getTaxId() != null) {
            String token = ANONYMIZED_TAX_ID_PREFIX + computeHmacToken(customer.getTaxId());
            customer.setTaxId(token);
        }
        customer.setFirstName(null);
        customer.setLastName(null);
        customer.setBusinessName(null);
        customer.setDateOfBirth(null);
        customer.setNationality(null);
        customer.setResidenceCountry(null);
        customer.setGender(null);
        customer.setMaritalStatus(null);
        customer.setPlaceOfBirth(null);
        customer.setMotherMaidenName(null);
        customer.setOccupation(null);
        customer.setAnnualIncome(null);
        // Retain: customerNumber, type, status, kycStatus, segment, risk data, flags
        // (needed for analytical/reporting purposes without identifying the individual)
    }

    /**
     * Compute a non-reversible HMAC-SHA256 token for the given tax ID.
     * Uses first 32 hex chars (16 bytes) to fit within tax_id column length (50).
     */
    private String computeHmacToken(String taxId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(hmacSecret, "HmacSHA256"));
            byte[] hash = mac.doFinal(taxId.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash).substring(0, 32);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            throw new IllegalStateException("HMAC-SHA256 initialization failed", e);
        }
    }
}
