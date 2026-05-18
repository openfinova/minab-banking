package com.openfinova.banking.customer.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.api.entity.CustomerRelationshipType;
import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.DocumentStatus;
import com.openfinova.banking.customer.api.entity.KYCDecision;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.api.event.CustomerLifecycleEvent;
import com.openfinova.banking.customer.dto.CustomerProfileUpdate;
import com.openfinova.banking.customer.dto.KYCDocumentSubmission;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.entity.CustomerRelationship;
import com.openfinova.banking.customer.entity.IdentificationDocument;
import com.openfinova.banking.customer.entity.KYCReviewStep;
import com.openfinova.banking.customer.entity.KYCWorkflow;
import com.openfinova.banking.customer.repository.CustomerRelationshipRepository;
import com.openfinova.banking.customer.repository.CustomerRepository;
import com.openfinova.banking.customer.repository.IdentificationDocumentRepository;
import com.openfinova.banking.customer.repository.KYCWorkflowRepository;

/**
 * Service class for managing customer profiles, relationships, and lifecycle operations
 * in the core banking system. This service serves as the central orchestration point
 * for customer data, including creation, status transitions, identity linking, and
 * KYC (Know Your Customer) workflows. All operations are transactional and enforce
 * strict compliance rules, such as preventing hard deletion of customer records.
 */
@Service
@Transactional
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final CustomerRelationshipRepository relationshipRepository;
    private final KYCWorkflowRepository kycWorkflowRepository;
    private final IdentificationDocumentRepository identificationDocumentRepository;
    private final ApplicationEventPublisher eventPublisher;

    /**
     * Constructs a new CustomerService with all required dependencies.
     *
     * @param customerRepository the repository for accessing customer profile data
     * @param relationshipRepository the repository for managing customer relationships
     * @param kycWorkflowRepository the repository for tracking KYC processes
     * @param identificationDocumentRepository the repository for customer identity documents
     * @param eventPublisher the publisher for broadcasting customer lifecycle events
     */
    public CustomerService(CustomerRepository customerRepository, CustomerRelationshipRepository relationshipRepository,
            KYCWorkflowRepository kycWorkflowRepository,
            IdentificationDocumentRepository identificationDocumentRepository,
            ApplicationEventPublisher eventPublisher) {
        this.customerRepository = customerRepository;
        this.relationshipRepository = relationshipRepository;
        this.kycWorkflowRepository = kycWorkflowRepository;
        this.identificationDocumentRepository = identificationDocumentRepository;
        this.eventPublisher = eventPublisher;
    }

    /**
     * Creates a new customer profile in the system.
     * Validates that the provided tax ID is unique before persisting the customer.
     *
     * @param customer the customer entity to create
     * @return the saved customer entity
     * @throws IllegalArgumentException if a customer with the same tax ID already exists
     */
    public Customer createCustomer(Customer customer) {
        if (customer.getTaxId() != null && customerRepository.existsByTaxId(customer.getTaxId())) {
            throw new IllegalArgumentException("Customer with tax ID " + customer.getTaxId() + " already exists");
        }
        return customerRepository.save(customer);
    }

    /**
     * Retrieves a customer profile by its unique identifier.
     *
     * @param id the unique identifier of the customer
     * @return an Optional containing the customer if found, or an empty Optional otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerById(UUID id) {
        return customerRepository.findById(id);
    }

    /**
     * Retrieves a customer profile by its unique tax identifier.
     *
     * @param taxId the tax identifier of the customer
     * @return an Optional containing the customer if found, or an empty Optional otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Customer> getCustomerByTaxId(String taxId) {
        return customerRepository.findByTaxId(taxId);
    }

    /**
     * Updates the core profile details of an existing customer.
     * Only non-null fields in the provided details object are updated.
     * Validates tax ID uniqueness if it is being changed.
     *
     * @param id the unique identifier of the customer to update
     * @param customerDetails an object containing the updated profile data
     * @return the updated customer entity
     * @throws IllegalArgumentException if the customer is not found or the new tax ID already exists
     */
    public Customer updateCustomer(UUID id, Customer customerDetails) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));

        // Update basic fields
        if (customerDetails.getFirstName() != null) {
            customer.setFirstName(customerDetails.getFirstName());
        }
        if (customerDetails.getLastName() != null) {
            customer.setLastName(customerDetails.getLastName());
        }
        if (customerDetails.getBusinessName() != null) {
            customer.setBusinessName(customerDetails.getBusinessName());
        }
        if (customerDetails.getDateOfBirth() != null) {
            customer.setDateOfBirth(customerDetails.getDateOfBirth());
        }
        if (customerDetails.getTaxId() != null && !customerDetails.getTaxId().equals(customer.getTaxId())) {
            if (customerRepository.existsByTaxId(customerDetails.getTaxId())) {
                throw new IllegalArgumentException("Tax ID already exists: " + customerDetails.getTaxId());
            }
            customer.setTaxId(customerDetails.getTaxId());
        }
        if (customerDetails.getNationality() != null) {
            customer.setNationality(customerDetails.getNationality());
        }
        if (customerDetails.getResidenceCountry() != null) {
            customer.setResidenceCountry(customerDetails.getResidenceCountry());
        }
        if (customerDetails.getSegment() != null) {
            customer.setSegment(customerDetails.getSegment());
        }
        if (customerDetails.getGender() != null) {
            customer.setGender(customerDetails.getGender());
        }
        if (customerDetails.getMaritalStatus() != null) {
            customer.setMaritalStatus(customerDetails.getMaritalStatus());
        }
        if (customerDetails.getPlaceOfBirth() != null) {
            customer.setPlaceOfBirth(customerDetails.getPlaceOfBirth());
        }
        if (customerDetails.getMotherMaidenName() != null) {
            customer.setMotherMaidenName(customerDetails.getMotherMaidenName());
        }
        if (customerDetails.getOccupation() != null) {
            customer.setOccupation(customerDetails.getOccupation());
        }
        if (customerDetails.getAnnualIncome() != null) {
            customer.setAnnualIncome(customerDetails.getAnnualIncome());
        }
        if (customerDetails.getIncorporationDate() != null) {
            customer.setIncorporationDate(customerDetails.getIncorporationDate());
        }
        if (customerDetails.getIncorporationCountry() != null) {
            customer.setIncorporationCountry(customerDetails.getIncorporationCountry());
        }
        if (customerDetails.getBusinessRegistrationNumber() != null) {
            customer.setBusinessRegistrationNumber(customerDetails.getBusinessRegistrationNumber());
        }
        if (customerDetails.getLegalEntityType() != null) {
            customer.setLegalEntityType(customerDetails.getLegalEntityType());
        }

        return customerRepository.save(customer);
    }

    /**
     * Updates PEP / sanctions screening flags. {@code null} means leave unchanged.
     */
    public Customer updateComplianceFlags(UUID id, Boolean pepFlag, Boolean sanctionFlag) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        if (pepFlag != null) {
            customer.setPepFlag(pepFlag);
        }
        if (sanctionFlag != null) {
            customer.setSanctionFlag(sanctionFlag);
        }
        return customerRepository.save(customer);
    }

    /**
     * Transitions a customer's status following business rules and triggers lifecycle events.
     * Prevents invalid transitions and enforces specific methods for blocking or anonymization.
     *
     * @param id the unique identifier of the customer
     * @param status the new target status
     * @throws IllegalArgumentException if the customer is not found, or the transition is invalid or forbidden
     * @throws IllegalStateException if attempting to transition to ANONYMIZED manually
     */
    public void updateCustomerStatus(UUID id, CustomerStatus status) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        CustomerStatus previousStatus = customer.getStatus();
        if (previousStatus == status) {
            return;
        }
        switch (status) {
            case ACTIVE -> customer.activate();
            case BLOCKED -> throw new IllegalArgumentException("Use block(customerId, reason) to block a customer");
            case CLOSED -> customer.close();
            case DECEASED -> customer.markDeceased();
            case PROSPECT, INACTIVE -> customer.setStatus(status);
            case ANONYMIZED -> throw new IllegalStateException(
                    "ANONYMIZED status can only be set by the anonymization pipeline");
            default -> throw new IllegalArgumentException(
                    "Invalid status transition from " + previousStatus + " to " + status);
        }
        customerRepository.save(customer);
        eventPublisher.publishEvent(new CustomerLifecycleEvent(this, id, previousStatus, status));
    }

    /**
     * Blocks a customer and records the reason for the block.
     * Publishes a lifecycle event to notify other modules of the block.
     *
     * @param id the unique identifier of the customer to block
     * @param reason the justification for blocking the customer
     * @throws IllegalArgumentException if the customer is not found
     */
    public void blockCustomer(UUID id, String reason) {
        Customer customer = customerRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + id));
        CustomerStatus previousStatus = customer.getStatus();
        customer.block(reason);
        customerRepository.save(customer);
        eventPublisher.publishEvent(new CustomerLifecycleEvent(this, id, previousStatus, CustomerStatus.BLOCKED));
    }

    /**
     * Links a core banking customer profile to an authentication identity user.
     *
     * @param customerId the unique identifier of the banking customer
     * @param identityUserId the unique identifier of the identity provider user
     * @param username the username associated with the identity
     * @throws IllegalArgumentException if the customer is not found
     */
    public void linkIdentityUser(UUID customerId, UUID identityUserId, String username) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));
        customer.setLinkedIdentityUserId(identityUserId);
        customer.setLinkedIdentityUsername(username);
        customerRepository.save(customer);
    }

    /**
     * Removes the linkage between a banking customer profile and an identity user.
     *
     * @param customerId the unique identifier of the banking customer
     */
    public void unlinkIdentityUser(UUID customerId) {
        customerRepository.findById(customerId).ifPresent(c -> {
            c.setLinkedIdentityUserId(null);
            c.setLinkedIdentityUsername(null);
            customerRepository.save(c);
        });
    }

    /**
     * Retrieves the identity provider user identifier linked to a customer.
     *
     * @param customerId the unique identifier of the banking customer
     * @return an Optional containing the linked identity user ID if found, or an empty Optional
     */
    @Transactional(readOnly = true)
    public Optional<UUID> getLinkedIdentityUserId(UUID customerId) {
        return customerRepository.findById(customerId).map(Customer::getLinkedIdentityUserId);
    }

    @Transactional(readOnly = true)
    public Optional<UUID> findCustomerIdByLinkedIdentityUserId(UUID linkedIdentityUserId) {
        if (linkedIdentityUserId == null) {
            return Optional.empty();
        }
        return customerRepository.findByLinkedIdentityUserId(linkedIdentityUserId).map(Customer::getId);
    }

    /**
     * KYC status must only change through the KYC workflow (initiate, submit, review).
     * Direct override is not allowed for compliance.
     *
     * @param id the unique identifier of the customer
     * @param kycStatus the target KYC status
     * @throws IllegalStateException always thrown to prevent manual overrides
     */
    public void updateKYCStatus(UUID id, KYCStatus kycStatus) {
        throw new IllegalStateException(
                "Direct KYC status update is not allowed. Use the KYC workflow (initiate, submit documents, review).");
    }

    /**
     * Retrieves a complete list of customers, optionally filtered by their current status.
     *
     * @param status the customer status to filter by, or null to retrieve all customers
     * @return a list of matching customers
     */
    @Transactional(readOnly = true)
    public List<Customer> listCustomers(CustomerStatus status) {
        if (status != null) {
            return customerRepository.findByStatus(status);
        }
        return customerRepository.findAll();
    }

    /**
     * Retrieves a paginated list of customers, optionally filtered by status and
     * an operator search string (customer number, name, business name, email/phone via contacts, customer UUID,
     * or linked identity user UUID when it equals {@code idMatch}).
     *
     * @param status the customer status to filter by, or null to retrieve all customers
     * @param search free-text search; blank means no search filter
     * @param pageable pagination and sorting configuration
     * @return a paginated result of matching customers
     */
    @Transactional(readOnly = true)
    public Page<Customer> listCustomers(CustomerStatus status, String search, Pageable pageable) {
        String q = search == null ? "" : search.trim();
        if (!q.isEmpty()) {
            UUID idMatch = null;
            try {
                idMatch = UUID.fromString(q);
            } catch (IllegalArgumentException ignored) {
                // not a UUID
            }
            return customerRepository.searchCustomers(q, status, idMatch, pageable);
        }
        if (status != null) {
            return customerRepository.findByStatus(status, pageable);
        }
        return customerRepository.findAll(pageable);
    }

    /**
     * Rejects customer deletion requests to comply with data retention and audit regulations.
     * Use the anonymization pipeline (DataSubjectRequestService + AnonymizationService)
     * after the retention period expires to anonymize PII while preserving the audit trail.
     *
     * @param id the unique identifier of the customer to delete
     * @throws IllegalArgumentException if the customer is not found
     * @throws IllegalStateException always thrown to prevent hard deletions
     */
    public void deleteCustomer(UUID id) {
        if (!customerRepository.existsById(id)) {
            throw new IllegalArgumentException("Customer not found: " + id);
        }
        throw new IllegalStateException(
                "Customer deletion is not allowed. Use the anonymization pipeline after the retention period expires.");
    }

    /**
     * Starts a new KYC workflow for a customer and updates their status to pending.
     *
     * @param customerId the unique identifier of the customer
     * @param initiatedBy the identifier of the user or system initiating the workflow
     * @return the newly created KYC workflow entity
     * @throws IllegalArgumentException if the customer is not found
     */
    public KYCWorkflow initiateKYCProcess(UUID customerId, String initiatedBy) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        KYCWorkflow workflow = new KYCWorkflow(customer, initiatedBy);
        workflow = kycWorkflowRepository.save(workflow);

        // Update customer KYC status
        customer.setKycStatus(KYCStatus.PENDING);
        customerRepository.save(customer);

        return workflow;
    }

    /**
     * Submits identification documents for an active KYC workflow and moves it to the review stage.
     *
     * @param customerId the unique identifier of the customer
     * @param documents a list of document details submitted for verification
     * @param submittedBy the identifier of the user or system submitting the documents
     * @return the updated KYC workflow entity
     * @throws IllegalArgumentException if the customer or an active KYC workflow is not found
     */
    public KYCWorkflow submitKYCDocuments(UUID customerId, List<KYCDocumentSubmission> documents, String submittedBy) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        KYCWorkflow workflow = kycWorkflowRepository.findLatestByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("No KYC workflow found for customer: " + customerId));

        // Create identification documents from submissions
        for (KYCDocumentSubmission docSubmission : documents) {
            IdentificationDocument doc = new IdentificationDocument();
            doc.setCustomer(customer);
            doc.setType(docSubmission.getDocumentType());
            doc.setDocumentNumber(docSubmission.getDocumentNumber());
            doc.setIssuingCountry(
                    docSubmission.getIssuingCountry() == null ? null
                            : docSubmission.getIssuingCountry().trim().toUpperCase());
            doc.setIssuingAuthority(docSubmission.getIssuingAuthority());
            doc.setIssueDate(docSubmission.getIssueDate());
            doc.setExpiryDate(docSubmission.getExpiryDate());
            doc.setVerified(false);
            doc.setDocumentStatus(DocumentStatus.UNDER_REVIEW);
            identificationDocumentRepository.save(doc);
        }

        // Update workflow status
        workflow.submitForReview(submittedBy);
        workflow = kycWorkflowRepository.save(workflow);

        // Update customer KYC status
        customer.setKycStatus(KYCStatus.IN_REVIEW);
        customerRepository.save(customer);

        return workflow;
    }

    /**
     * Records a review decision on submitted KYC documents and updates the customer's KYC status accordingly.
     *
     * @param customerId the unique identifier of the customer
     * @param decision the approval or rejection decision
     * @param comments optional notes provided by the reviewer
     * @param reviewedBy the identifier of the reviewer
     * @return the updated KYC workflow entity
     * @throws IllegalArgumentException if the customer or an active KYC workflow is not found
     */
    public KYCWorkflow reviewKYCDocuments(UUID customerId, KYCDecision decision, String comments, String reviewedBy) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        KYCWorkflow workflow = kycWorkflowRepository.findLatestByCustomerId(customerId)
                .orElseThrow(() -> new IllegalArgumentException("No KYC workflow found for customer: " + customerId));

        // Create review step
        KYCReviewStep reviewStep = new KYCReviewStep(workflow, "Document Review", decision, reviewedBy, comments);
        workflow.addReviewStep(reviewStep);

        // Update workflow based on decision
        if (decision == KYCDecision.APPROVED) {
            workflow.approve(reviewedBy, comments);
            customer.setKycStatus(KYCStatus.VERIFIED);
        } else if (decision == KYCDecision.REJECTED) {
            workflow.reject(reviewedBy, comments);
            customer.setKycStatus(KYCStatus.REJECTED);
        } else {
            // REQUIRES_ADDITIONAL_INFO - keep in review
            workflow.setComments(comments);
        }

        syncIdentificationDocumentsWithKycDecision(customerId, decision, reviewedBy);

        workflow = kycWorkflowRepository.save(workflow);
        customerRepository.save(customer);

        return workflow;
    }

    /**
     * Keeps {@link IdentificationDocument} rows aligned with the KYC review outcome.
     * Pending pipeline docs ({@link DocumentStatus#SUBMITTED}, {@link DocumentStatus#UNDER_REVIEW}) are
     * verified when KYC is approved, rejected when KYC is rejected, or marked under review when more
     * information is required.
     */
    private void syncIdentificationDocumentsWithKycDecision(UUID customerId, KYCDecision decision, String reviewedBy) {
        List<IdentificationDocument> docs = identificationDocumentRepository
                .findByCustomerIdAndDeletedAtIsNull(customerId);
        LocalDateTime now = LocalDateTime.now();
        for (IdentificationDocument doc : docs) {
            DocumentStatus st = doc.getDocumentStatus();
            if (st != DocumentStatus.SUBMITTED && st != DocumentStatus.UNDER_REVIEW) {
                continue;
            }
            if (decision == KYCDecision.APPROVED) {
                doc.setDocumentStatus(DocumentStatus.VERIFIED);
                doc.setVerified(true);
                doc.setVerifiedAt(now);
                doc.setVerifiedBy(reviewedBy);
            } else if (decision == KYCDecision.REJECTED) {
                doc.setDocumentStatus(DocumentStatus.REJECTED);
                doc.setVerified(false);
                doc.setVerifiedAt(null);
                doc.setVerifiedBy(null);
            } else {
                doc.setDocumentStatus(DocumentStatus.UNDER_REVIEW);
            }
            identificationDocumentRepository.save(doc);
        }
    }

    /**
     * Retrieves the most recent KYC workflow for a specific customer.
     *
     * @param customerId the unique identifier of the customer
     * @return an Optional containing the latest KYC workflow if found, or an empty Optional
     */
    @Transactional(readOnly = true)
    public Optional<KYCWorkflow> getKYCWorkflow(UUID customerId) {
        return kycWorkflowRepository.findLatestByCustomerId(customerId);
    }

    /**
     * Retrieves the complete history of all KYC workflows for a customer.
     *
     * @param customerId the unique identifier of the customer
     * @return a list of all historical and active KYC workflows
     */
    @Transactional(readOnly = true)
    public List<KYCWorkflow> getKYCWorkflowHistory(UUID customerId) {
        return kycWorkflowRepository.findAllByCustomerId(customerId);
    }

    /**
     * Retrieves all customers currently in the KYC review stage.
     *
     * @return a list of customers waiting for KYC document verification
     */
    @Transactional(readOnly = true)
    public List<Customer> getCustomersRequiringKYCReview() {
        return customerRepository.findByKycStatus(KYCStatus.IN_REVIEW);
    }

    /**
     * Scans for verified KYC workflows older than a specified number of months and marks them as expired.
     * Updates the associated customers' KYC status to expired.
     *
     * @param expirationMonths the threshold in months after which a KYC verification expires
     * @return the total number of KYC workflows successfully expired
     */
    public int expireOutdatedKYC(int expirationMonths) {
        LocalDateTime expirationDate = LocalDateTime.now().minusMonths(expirationMonths);
        List<KYCWorkflow> expiredWorkflows = kycWorkflowRepository
                .findByStatusAndCompletedAtBefore(KYCStatus.VERIFIED, expirationDate);

        int count = 0;
        for (KYCWorkflow workflow : expiredWorkflows) {
            workflow.expire();
            kycWorkflowRepository.save(workflow);

            Customer customer = workflow.getCustomer();
            customer.setKycStatus(KYCStatus.EXPIRED);
            customerRepository.save(customer);
            count++;
        }

        return count;
    }

    /**
     * Initiates a new KYC workflow requiring the customer to undergo verification again.
     *
     * @param customerId the unique identifier of the customer
     * @param reason the justification for requesting re-verification
     * @param requestedBy the identifier of the user or system requesting the review
     * @return the newly created KYC workflow entity
     * @throws IllegalArgumentException if the customer is not found
     */
    public KYCWorkflow requestKYCReVerification(UUID customerId, String reason, String requestedBy) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        KYCWorkflow workflow = new KYCWorkflow(customer, requestedBy);
        workflow.setReVerificationReason(reason);
        workflow = kycWorkflowRepository.save(workflow);

        customer.setKycStatus(KYCStatus.PENDING);
        customerRepository.save(customer);

        return workflow;
    }

    /**
     * Establishes a relationship link between two different customers (e.g., parent-subsidiary, guarantor).
     *
     * @param primaryCustomerId the unique identifier of the primary customer
     * @param relatedCustomerId the unique identifier of the associated customer
     * @param relationshipType the nature of the relationship being established
     * @param createdBy the identifier of the user or system creating the relationship
     * @return the newly created customer relationship entity
     * @throws IllegalArgumentException if either customer is not found, they are the same customer, or the relationship already exists
     */
    public CustomerRelationship createCustomerRelationship(UUID primaryCustomerId, UUID relatedCustomerId,
            CustomerRelationshipType relationshipType, String createdBy) {
        if (primaryCustomerId.equals(relatedCustomerId)) {
            throw new IllegalArgumentException("Cannot create relationship with self");
        }

        Customer primaryCustomer = customerRepository.findById(primaryCustomerId)
                .orElseThrow(() -> new IllegalArgumentException("Primary customer not found: " + primaryCustomerId));

        Customer relatedCustomer = customerRepository.findById(relatedCustomerId)
                .orElseThrow(() -> new IllegalArgumentException("Related customer not found: " + relatedCustomerId));

        // Check if relationship already exists
        if (relationshipRepository.existsActiveRelationship(primaryCustomerId, relatedCustomerId, relationshipType)) {
            throw new IllegalArgumentException("Relationship already exists between customers");
        }

        CustomerRelationship relationship = new CustomerRelationship(
                primaryCustomer,
                relatedCustomer,
                relationshipType,
                createdBy);

        return relationshipRepository.save(relationship);
    }

    /**
     * Retrieves all active relationships where the specified customer is the primary party.
     *
     * @param customerId the unique identifier of the primary customer
     * @return a list of active customer relationships
     */
    @Transactional(readOnly = true)
    public List<CustomerRelationship> getCustomerRelationships(UUID customerId) {
        return relationshipRepository.findActiveRelationshipsByCustomerId(customerId);
    }

    /**
     * Deactivates an existing customer relationship.
     *
     * @param relationshipId the unique identifier of the relationship to deactivate
     * @param removedBy the identifier of the user or system removing the relationship
     * @throws IllegalArgumentException if the relationship is not found
     */
    public void removeCustomerRelationship(UUID relationshipId, String removedBy) {
        CustomerRelationship relationship = relationshipRepository.findById(relationshipId)
                .orElseThrow(() -> new IllegalArgumentException("Relationship not found: " + relationshipId));

        relationship.deactivate(removedBy);
        relationshipRepository.save(relationship);
    }

    /**
     * Retrieves all customers related to the specified customer, optionally filtering by relationship type.
     * Considers both directions of the relationship.
     *
     * @param customerId the unique identifier of the customer
     * @param relationshipType the specific type of relationship to filter by, or null for all types
     * @return a list of related customer entities
     */
    @Transactional(readOnly = true)
    public List<Customer> getRelatedCustomers(UUID customerId, CustomerRelationshipType relationshipType) {
        List<CustomerRelationship> relationships;

        if (relationshipType != null) {
            relationships = relationshipRepository.findByCustomerIdAndRelationshipType(customerId, relationshipType);
        } else {
            relationships = relationshipRepository.findActiveRelationshipsByCustomerId(customerId);
        }

        return relationships.stream().map(rel -> {
            if (rel.getPrimaryCustomer().getId().equals(customerId)) {
                return rel.getRelatedCustomer();
            } else {
                return rel.getPrimaryCustomer();
            }
        }).collect(Collectors.toList());
    }

    /**
     * Applies a set of profile updates to a customer record.
     * Only non-null values in the update object will be applied.
     *
     * @param customerId the unique identifier of the customer to update
     * @param profileUpdate an object containing the requested profile modifications
     * @param updatedBy the identifier of the user or system applying the update
     * @return the updated customer entity
     * @throws IllegalArgumentException if the customer is not found or a new tax ID already exists
     */
    public Customer updateCustomerProfile(UUID customerId, CustomerProfileUpdate profileUpdate, String updatedBy) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        if (profileUpdate.getFirstName() != null) {
            customer.setFirstName(profileUpdate.getFirstName());
        }
        if (profileUpdate.getLastName() != null) {
            customer.setLastName(profileUpdate.getLastName());
        }
        if (profileUpdate.getBusinessName() != null) {
            customer.setBusinessName(profileUpdate.getBusinessName());
        }
        if (profileUpdate.getDateOfBirth() != null) {
            customer.setDateOfBirth(profileUpdate.getDateOfBirth());
        }
        if (profileUpdate.getTaxId() != null && !profileUpdate.getTaxId().equals(customer.getTaxId())) {
            if (customerRepository.existsByTaxId(profileUpdate.getTaxId())) {
                throw new IllegalArgumentException("Tax ID already exists: " + profileUpdate.getTaxId());
            }
            customer.setTaxId(profileUpdate.getTaxId());
        }

        return customerRepository.save(customer);
    }
}
