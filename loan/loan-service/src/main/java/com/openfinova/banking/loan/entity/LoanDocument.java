package com.openfinova.banking.loan.entity;

import com.openfinova.banking.loan.api.entity.DocumentStatus;
import com.openfinova.banking.loan.api.entity.DocumentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

/**
 * Loan Document entity for managing loan-related documents.
 * Tracks document metadata, expiry dates, and status.
 */
@Entity
@Table(name = "loan_documents", indexes = { @Index(name = "idx_loan_documents_account", columnList = "loan_account_id"),
        @Index(name = "idx_loan_documents_type", columnList = "document_type"),
        @Index(name = "idx_loan_documents_status", columnList = "status"),
        @Index(name = "idx_loan_documents_expiry", columnList = "expiry_date") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class LoanDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "document_type", nullable = false, length = 30)
    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank(message = "Document name is required")
    @Column(name = "document_name", nullable = false, length = 200)
    @Size(max = 200, message = "Document name must not exceed 200 characters")
    private String documentName;

    /**
     * File path or URL where the document is stored.
     */
    @NotBlank(message = "Document path is required")
    @Column(name = "document_path", nullable = false, length = 500)
    @Size(max = 500, message = "Document path must not exceed 500 characters")
    private String documentPath;

    @Column(name = "document_number", length = 100)
    @Size(max = 100, message = "Document number must not exceed 100 characters")
    private String documentNumber;

    @Column(name = "expiry_date")
    private LocalDate expiryDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private DocumentStatus status = DocumentStatus.ACTIVE;

    @Column(length = 500)
    @Size(max = 500, message = "Remarks must not exceed 500 characters")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public LoanDocument() {
    }

    // Business Logic
    public boolean isActive() {
        return DocumentStatus.ACTIVE.equals(status);
    }

    public boolean isExpired() {
        return expiryDate != null && expiryDate.isBefore(LocalDate.now());
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
    }

    public String getDocumentName() {
        return documentName;
    }

    public void setDocumentName(String documentName) {
        this.documentName = documentName;
    }

    public String getDocumentPath() {
        return documentPath;
    }

    public void setDocumentPath(String documentPath) {
        this.documentPath = documentPath;
    }

    public String getDocumentNumber() {
        return documentNumber;
    }

    public void setDocumentNumber(String documentNumber) {
        this.documentNumber = documentNumber;
    }

    public LocalDate getExpiryDate() {
        return expiryDate;
    }

    public void setExpiryDate(LocalDate expiryDate) {
        this.expiryDate = expiryDate;
    }

    public DocumentStatus getStatus() {
        return status;
    }

    public void setStatus(DocumentStatus status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
