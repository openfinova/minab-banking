package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

/**
 * DTO for GL Journal Entry information exposed to external modules.
 * Represents a single debit or credit entry within a transaction.
 */
@Schema(description = "Journal entry representing a debit or credit in a transaction")
public class GLJournalEntryDTO {
    @Schema(description = "Unique entry identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @Schema(description = "Transaction ID this entry belongs to", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID transactionId;

    @Schema(description = "GL account ID", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID accountId;

    @Schema(description = "GL account code", example = "1000")
    private String accountCode;

    @Schema(description = "Debit amount (0 if credit entry)", example = "1000.00")
    private BigDecimal debitAmount;

    @Schema(description = "Credit amount (0 if debit entry)", example = "0.00")
    private BigDecimal creditAmount;

    @Schema(description = "Entry description", example = "Cash deposit")
    private String description;

    @Schema(description = "Line number in transaction", example = "1")
    private Integer lineNumber;

    @Schema(description = "Value date for the entry", example = "2026-02-14")
    private LocalDate valueDate;

    @Schema(description = "Entry currency", example = "USD")
    private String currency;

    public GLJournalEntryDTO() {
    }

    public GLJournalEntryDTO(UUID id, UUID transactionId, UUID accountId, String accountCode, BigDecimal debitAmount,
            BigDecimal creditAmount, String description, Integer lineNumber, LocalDate valueDate, String currency) {
        this.id = id;
        this.transactionId = transactionId;
        this.accountId = accountId;
        this.accountCode = accountCode;
        this.debitAmount = debitAmount;
        this.creditAmount = creditAmount;
        this.description = description;
        this.lineNumber = lineNumber;
        this.valueDate = valueDate;
        this.currency = currency;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
    }

    public UUID getAccountId() {
        return accountId;
    }

    public void setAccountId(UUID accountId) {
        this.accountId = accountId;
    }

    public String getAccountCode() {
        return accountCode;
    }

    public void setAccountCode(String accountCode) {
        this.accountCode = accountCode;
    }

    public BigDecimal getDebitAmount() {
        return debitAmount;
    }

    public void setDebitAmount(BigDecimal debitAmount) {
        this.debitAmount = debitAmount;
    }

    public BigDecimal getCreditAmount() {
        return creditAmount;
    }

    public void setCreditAmount(BigDecimal creditAmount) {
        this.creditAmount = creditAmount;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }

    public LocalDate getValueDate() {
        return valueDate;
    }

    public void setValueDate(LocalDate valueDate) {
        this.valueDate = valueDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }
}
