package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.GLTransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * DTO for GL Transaction information exposed to external modules.
 * Contains transaction header information and associated journal entries.
 */
@Schema(description = "General Ledger transaction with journal entries")
public class GLTransactionDTO {
    @Schema(description = "Unique transaction identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "External reference ID", example = "TXN-2024-001")
    private String referenceId;

    @Schema(description = "Sequential transaction number", example = "1001")
    private Long transactionNumber;

    @Schema(description = "Transaction description", example = "Customer deposit")
    private String description;

    @Schema(description = "Transaction date", example = "2026-02-14")
    private LocalDate transactionDate;

    @Schema(description = "Transaction currency", example = "USD")
    private String currency;

    @Schema(description = "Transaction status", example = "POSTED")
    private GLTransactionStatus status;

    @Schema(description = "List of journal entries for this transaction")
    private List<GLJournalEntryDTO> journalEntries;

    public GLTransactionDTO() {
    }

    public GLTransactionDTO(UUID id, String referenceId, Long transactionNumber, String description,
            LocalDate transactionDate, String currency, GLTransactionStatus status,
            List<GLJournalEntryDTO> journalEntries) {
        this.id = id;
        this.referenceId = referenceId;
        this.transactionNumber = transactionNumber;
        this.description = description;
        this.transactionDate = transactionDate;
        this.currency = currency;
        this.status = status;
        this.journalEntries = journalEntries;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
    }

    public Long getTransactionNumber() {
        return transactionNumber;
    }

    public void setTransactionNumber(Long transactionNumber) {
        this.transactionNumber = transactionNumber;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public GLTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(GLTransactionStatus status) {
        this.status = status;
    }

    public List<GLJournalEntryDTO> getJournalEntries() {
        return journalEntries;
    }

    public void setJournalEntries(List<GLJournalEntryDTO> journalEntries) {
        this.journalEntries = journalEntries;
    }
}
