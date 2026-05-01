package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.util.UUID;

@Schema(description = "GL Journal Entry response")
public class GLJournalEntryResponse {

    @Schema(description = "Entry unique identifier")
    private UUID id;

    @Schema(description = "GL account ID")
    private UUID accountId;

    @Schema(description = "GL account code")
    private String accountCode;

    @Schema(description = "GL account name")
    private String accountName;

    @Schema(description = "Entry type (DEBIT or CREDIT)")
    private String entryType;

    @Schema(description = "Debit amount")
    private BigDecimal debitAmount;

    @Schema(description = "Credit amount")
    private BigDecimal creditAmount;

    @Schema(description = "Currency code")
    private String currency;

    @Schema(description = "Entry description")
    private String description;

    @Schema(description = "Line number within transaction")
    private Integer lineNumber;

    // Constructors
    public GLJournalEntryResponse() {
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
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

    public String getAccountName() {
        return accountName;
    }

    public void setAccountName(String accountName) {
        this.accountName = accountName;
    }

    public String getEntryType() {
        return entryType;
    }

    public void setEntryType(String entryType) {
        this.entryType = entryType;
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

    public Integer getLineNumber() {
        return lineNumber;
    }

    public void setLineNumber(Integer lineNumber) {
        this.lineNumber = lineNumber;
    }
}
