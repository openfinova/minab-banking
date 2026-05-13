package com.openfinova.banking.customer.dto;

import java.time.LocalDate;

import com.openfinova.banking.customer.api.entity.DocumentType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * DTO for submitting KYC documents.
 * Uses opaque file references (e.g. upload ID) instead of raw paths to avoid path traversal.
 */
public class KYCDocumentSubmission {

    @NotNull(message = "Document type is required")
    private DocumentType documentType;

    @NotBlank
    @Size(max = 100)
    private String documentNumber;

    @NotBlank(message = "Issuing country is required")
    @Pattern(regexp = "[A-Za-z]{2}", message = "Issuing country must be ISO 3166-1 alpha-2 (2 letters)")
    @Size(min = 2, max = 2, message = "Issuing country must be ISO 3166-1 alpha-2 (2 characters)")
    private String issuingCountry;

    @Size(max = 100)
    private String issuingAuthority;

    @Past(message = "Issue date must be in the past")
    private LocalDate issueDate;

    private LocalDate expiryDate;

    /**
     * Opaque file reference (upload ID or storage key), not a filesystem path.
     */
    @Size(max = 255)
    private String filePath;

    public KYCDocumentSubmission() {
    }

    public DocumentType getDocumentType() {
        return documentType;
    }

    public void setDocumentType(DocumentType documentType) {
        this.documentType = documentType;
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

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }
}
