package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.GLTransactionStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Schema(description = "GL Transaction response")
public class GLTransactionResponse {

    @Schema(description = "Transaction unique identifier")
    private UUID id;

    @Schema(description = "External reference ID")
    private String referenceId;

    @Schema(description = "Transaction date")
    private LocalDate transactionDate;

    @Schema(description = "Transaction description")
    private String description;

    @Schema(description = "Transaction status")
    private GLTransactionStatus status;

    @Schema(description = "Transaction currency")
    private String currency;

    @Schema(description = "User who created the transaction")
    private String createdBy;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Posting timestamp")
    private Instant postingDate;

    @Schema(description = "Journal entries")
    private List<GLJournalEntryResponse> journalEntries;

    // Constructors
    public GLTransactionResponse() {
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

    public LocalDate getTransactionDate() {
        return transactionDate;
    }

    public void setTransactionDate(LocalDate transactionDate) {
        this.transactionDate = transactionDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public GLTransactionStatus getStatus() {
        return status;
    }

    public void setStatus(GLTransactionStatus status) {
        this.status = status;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getPostingDate() {
        return postingDate;
    }

    public void setPostingDate(Instant postingDate) {
        this.postingDate = postingDate;
    }

    public List<GLJournalEntryResponse> getJournalEntries() {
        return journalEntries;
    }

    public void setJournalEntries(List<GLJournalEntryResponse> journalEntries) {
        this.journalEntries = journalEntries;
    }
}
