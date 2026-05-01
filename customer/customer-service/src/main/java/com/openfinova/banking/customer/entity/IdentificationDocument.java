package com.openfinova.banking.customer.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.customer.api.entity.DocumentStatus;
import com.openfinova.banking.customer.api.entity.DocumentType;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing an identification document (Passport, ID Card) used for
 * KYC.
 */
@Entity
@Table(name = "identification_documents", indexes = { @Index(name = "idx_id_docs_customer", columnList = "customer_id"),
        @Index(name = "idx_id_docs_type", columnList = "type"),
        @Index(name = "idx_id_docs_number", columnList = "document_number") })
public class IdentificationDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @NotNull(message = "Document type is required")
    private DocumentType type;

    @Column(name = "document_number", nullable = false, length = 50)
    @NotBlank(message = "Document number is required")
    @Size(max = 50, message = "Document number must not exceed 50 characters")
    private String documentNumber;

    @Column(name = "issuing_country", nullable = false, length = 2)
    @NotBlank(message = "Issuing country is required")
    @Size(max = 2, message = "Issuing country must be ISO 3166-1 alpha-2 (2 characters)")
    @jakarta.validation.constraints.Pattern(regexp = "[A-Z]{2}", message = "Issuing country must be ISO 3166-1 alpha-2 country code")
    private String issuingCountry;

    @Column(name = "issuing_authority", length = 100)
    private String issuingAuthority;

    @Column(name = "issue_date")
    private LocalDate issueDate;

    @Column(name = "expiry_date")
    @Future(message = "Expiry date must be in the future")
    private LocalDate expiryDate;

    @Column(name = "is_verified", nullable = false)
    private boolean isVerified = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Lifecycle status of this document through the KYC pipeline.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "document_status", nullable = false, length = 20)
    @NotNull(message = "Document status is required")
    private DocumentStatus documentStatus = DocumentStatus.SUBMITTED;

    /**
     * Timestamp when the document was verified.
     */
    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    /**
     * User or system that performed the document verification.
     */
    @Column(name = "verified_by", length = 100)
    private String verifiedBy;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Soft-delete timestamp. When set, the document is considered deleted for display
     * but retained for regulatory audit. Null means active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Constructors
    public IdentificationDocument() {
    }

    public IdentificationDocument(Customer customer, DocumentType type, String documentNumber, String issuingCountry) {
        this.customer = customer;
        this.type = type;
        this.documentNumber = documentNumber;
        this.issuingCountry = issuingCountry;
    }

    // Business Logic
    public boolean isValid() {
        return expiryDate == null || expiryDate.isAfter(LocalDate.now());
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public String getIssuingCountry() {
        return issuingCountry;
    }

    public void setIssuingCountry(String issuingCountry) {
        this.issuingCountry = issuingCountry;
    }

    public String getIssuingAuthority() {
        return issuingAuthority;
    }

    public void setIssuingAuthority(String issuingAuthority) {
        this.issuingAuthority = issuingAuthority;
    }

    public LocalDate getIssueDate() {
        return issueDate;
    }

    public void setIssueDate(LocalDate issueDate) {
        this.issueDate = issueDate;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public boolean isVerified() {
        return isVerified;
    }

    public void setVerified(boolean verified) {
        isVerified = verified;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public DocumentStatus getDocumentStatus() {
        return documentStatus;
    }

    public void setDocumentStatus(DocumentStatus documentStatus) {
        this.documentStatus = documentStatus;
    }

    public LocalDateTime getVerifiedAt() {
        return verifiedAt;
    }

    public void setVerifiedAt(LocalDateTime verifiedAt) {
        this.verifiedAt = verifiedAt;
    }

    public String getVerifiedBy() {
        return verifiedBy;
    }

    public void setVerifiedBy(String verifiedBy) {
        this.verifiedBy = verifiedBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
