package com.openfinova.banking.gl.api.dto;

import java.time.LocalDate;
import java.util.List;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Request to post a GL transaction")
public class PostTransactionRequest {

    @NotBlank(message = "Reference ID is required")
    @Size(max = 100, message = "Reference ID must not exceed 100 characters")
    @Schema(description = "External reference ID for idempotency", example = "TXN-2024-001")
    private String referenceId;

    @NotNull(message = "Transaction date is required")
    @Schema(description = "Transaction date", example = "2024-01-15")
    private LocalDate transactionDate;

    @NotBlank(message = "Currency is required")
    @Size(max = 3, message = "Currency must be 3 characters")
    @Schema(description = "Transaction currency", example = "USD")
    private String currency;

    @NotBlank(message = "Description is required")
    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Transaction description", example = "Customer deposit")
    private String description;

    @NotBlank(message = "Posted by is required")
    @Size(max = 100, message = "Posted by must not exceed 100 characters")
    @Schema(description = "User posting the transaction", example = "system")
    private String postedBy;

    @NotEmpty(message = "At least one journal entry is required")
    @Valid
    @Schema(description = "List of journal entries (must balance)")
    private List<JournalEntryRequest> entries;

    // Constructors
    public PostTransactionRequest() {
    }

    // Getters and Setters
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

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getPostedBy() {
        return postedBy;
    }

    public void setPostedBy(String postedBy) {
        this.postedBy = postedBy;
    }

    public List<JournalEntryRequest> getEntries() {
        return entries;
    }

    public void setEntries(List<JournalEntryRequest> entries) {
        this.entries = entries;
    }
}
