package com.openfinova.banking.customer.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.api.entity.DocumentStatus;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.entity.IdentificationDocument;
import com.openfinova.banking.customer.repository.CustomerRepository;
import com.openfinova.banking.customer.repository.IdentificationDocumentRepository;

/**
 * Service class for managing customer identification documents in the core banking system.
 * This service handles operations such as adding, retrieving, updating, verifying,
 * and deleting identification documents (e.g., passports, national IDs).
 * It enforces business rules, such as resetting verification status upon updates
 * and ensuring documents belong to the correct customer.
 * All operations are transactional to guarantee data consistency.
 */
@Service
@Transactional
public class IdentificationDocumentService {

    private final IdentificationDocumentRepository documentRepository;
    private final CustomerRepository customerRepository;

    /**
     * Constructs a new IdentificationDocumentService with the necessary repositories.
     *
     * @param documentRepository the repository used for accessing and managing identification document data
     * @param customerRepository the repository used for accessing customer data
     */
    public IdentificationDocumentService(IdentificationDocumentRepository documentRepository,
            CustomerRepository customerRepository) {
        this.documentRepository = documentRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Adds a new identification document for the specified customer.
     * Newly added documents are marked as unverified by default.
     *
     * @param customerId the unique identifier of the customer
     * @param document the identification document details to be added
     * @return the saved identification document entity
     * @throws IllegalArgumentException if the customer is not found
     */
    public IdentificationDocument addIdentificationDocument(UUID customerId, IdentificationDocument document) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        document.setCustomer(customer);
        document.setVerified(false); // New documents start as unverified

        return documentRepository.save(document);
    }

    /**
     * Retrieves all active (non-deleted) identification documents associated with a specific customer.
     *
     * @param customerId the unique identifier of the customer
     * @return a list of active identification documents belonging to the customer
     */
    @Transactional(readOnly = true)
    public List<IdentificationDocument> getDocumentsByCustomerId(UUID customerId) {
        return documentRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
    }

    /**
     * Retrieves a specific identification document by its unique identifier, verifying that it belongs
     * to the specified customer and has not been deleted.
     *
     * @param customerId the unique identifier of the customer who owns the document
     * @param documentId the unique identifier of the document to retrieve
     * @return the requested identification document
     * @throws IllegalArgumentException if the document is not found, has been deleted, or does not belong to the customer
     */
    @Transactional(readOnly = true)
    public IdentificationDocument getDocumentById(UUID customerId, UUID documentId) {
        IdentificationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Identification document not found: " + documentId));
        if (document.isDeleted()) {
            throw new IllegalArgumentException("Identification document not found: " + documentId);
        }
        validateDocumentOwnership(document, customerId);
        return document;
    }

    /**
     * Updates the details of an existing identification document.
     * Only non-null fields in the provided document details object will be updated.
     * If any changes are made, the document's verification status is automatically reset to false.
     * The document must belong to the specified customer and must not be deleted.
     *
     * @param customerId the unique identifier of the customer who owns the document
     * @param documentId the unique identifier of the document to update
     * @param documentDetails an object containing the new document details
     * @return the updated identification document entity
     * @throws IllegalArgumentException if the document is not found, has been deleted, or does not belong to the customer
     */
    public IdentificationDocument updateIdentificationDocument(UUID customerId, UUID documentId,
            IdentificationDocument documentDetails) {
        IdentificationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Identification document not found: " + documentId));
        if (document.isDeleted()) {
            throw new IllegalArgumentException("Identification document not found: " + documentId);
        }
        validateDocumentOwnership(document, customerId);

        if (documentDetails.getDocumentNumber() != null) {
            document.setDocumentNumber(documentDetails.getDocumentNumber());
        }
        if (documentDetails.getIssuingCountry() != null) {
            document.setIssuingCountry(documentDetails.getIssuingCountry());
        }
        if (documentDetails.getIssuingAuthority() != null) {
            document.setIssuingAuthority(documentDetails.getIssuingAuthority());
        }
        if (documentDetails.getIssueDate() != null) {
            document.setIssueDate(documentDetails.getIssueDate());
        }
        if (documentDetails.getExpiryDate() != null) {
            document.setExpiryDate(documentDetails.getExpiryDate());
        }

        // Reset verification if document details change
        document.setVerified(false);

        return documentRepository.save(document);
    }

    /**
     * Marks a specific identification document as verified.
     *
     * @param customerId the unique identifier of the customer who owns the document
     * @param documentId the unique identifier of the document to verify
     * @throws IllegalArgumentException if the document is not found, has been deleted, or does not belong to the customer
     */
    public void verifyDocument(UUID customerId, UUID documentId) {
        IdentificationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Identification document not found: " + documentId));
        if (document.isDeleted()) {
            throw new IllegalArgumentException("Identification document not found: " + documentId);
        }
        validateDocumentOwnership(document, customerId);

        LocalDateTime now = LocalDateTime.now();
        document.setVerified(true);
        document.setDocumentStatus(DocumentStatus.VERIFIED);
        document.setVerifiedAt(now);
        documentRepository.save(document);
    }

    /**
     * Soft deletes a specific identification document by setting its deletion timestamp.
     * The document must belong to the specified customer and must not already be deleted.
     *
     * @param customerId the unique identifier of the customer who owns the document
     * @param documentId the unique identifier of the document to delete
     * @throws IllegalArgumentException if the document is not found, already deleted, or does not belong to the customer
     */
    public void deleteIdentificationDocument(UUID customerId, UUID documentId) {
        IdentificationDocument document = documentRepository.findById(documentId)
                .orElseThrow(() -> new IllegalArgumentException("Identification document not found: " + documentId));
        validateDocumentOwnership(document, customerId);
        if (document.isDeleted()) {
            throw new IllegalArgumentException("Identification document not found: " + documentId);
        }
        document.setDeletedAt(LocalDateTime.now());
        documentRepository.save(document);
    }

    /**
     * Validates that a given identification document belongs to the specified customer.
     *
     * @param document the identification document to check
     * @param customerId the expected customer identifier
     * @throws IllegalArgumentException if the document does not belong to the customer
     */
    private void validateDocumentOwnership(IdentificationDocument document, UUID customerId) {
        if (!document.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "Document " + document.getId() + " does not belong to customer " + customerId);
        }
    }
}
