package com.openfinova.banking.customer.dto;

import com.openfinova.banking.customer.api.entity.DocumentStatus;
import com.openfinova.banking.customer.api.entity.DocumentType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for identification document API endpoints.
 * Masks document number (shows last 4 digits only) to protect PII.
 */
public class IdentificationDocumentResponse {

    private UUID id;
    private UUID customerId;
    private DocumentType type;
    private String maskedDocumentNumber; // Last 4 digits only, e.g. "****1234"
    private String issuingCountry;
    private String issuingAuthority;
    private LocalDate issueDate;
    private LocalDate expiryDate;
    private boolean verified;
    private DocumentStatus documentStatus;
    private LocalDateTime verifiedAt;
    private String verifiedBy;

    public IdentificationDocumentResponse() {
    }

    public static String maskDocumentNumber(String documentNumber) {
        if (documentNumber == null || documentNumber.length() < 4) {
            return "****";
        }
        return "****" + documentNumber.substring(documentNumber.length() - 4);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public DocumentType getType() {
        return type;
    }

    public void setType(DocumentType type) {
        this.type = type;
    }

    public String getMaskedDocumentNumber() {
        return maskedDocumentNumber;
    }

    public void setMaskedDocumentNumber(String maskedDocumentNumber) {
        this.maskedDocumentNumber = maskedDocumentNumber;
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
        return verified;
    }

    public void setVerified(boolean verified) {
        this.verified = verified;
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
}
