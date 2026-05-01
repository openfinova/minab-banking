package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Command object for posting a GL transaction from external modules.
 * Contains only the essential data needed to create and post a transaction.
 */
@Schema(description = "Command to post a GL transaction from external modules")
public class PostTransactionCommand {
    @Schema(description = "External reference ID", example = "TXN-2024-001")
    private String referenceId;

    @Schema(description = "Transaction description", example = "Customer deposit")
    private String description;

    @Schema(description = "Transaction date", example = "2026-02-14")
    private LocalDate transactionDate;

    @Schema(description = "Transaction currency", example = "USD")
    private String currency;

    @Schema(description = "User creating the transaction", example = "system")
    private String createdBy;

    @Schema(description = "List of journal entry commands")
    private List<JournalEntryCommand> entries;

    public PostTransactionCommand() {
    }

    public PostTransactionCommand(String referenceId, String description, LocalDate transactionDate, String currency,
            String createdBy, List<JournalEntryCommand> entries) {
        this.referenceId = referenceId;
        this.description = description;
        this.transactionDate = transactionDate;
        this.currency = currency;
        this.createdBy = createdBy;
        this.entries = entries;
    }

    // Getters and Setters
    public String getReferenceId() {
        return referenceId;
    }

    public void setReferenceId(String referenceId) {
        this.referenceId = referenceId;
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

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public List<JournalEntryCommand> getEntries() {
        return entries;
    }

    public void setEntries(List<JournalEntryCommand> entries) {
        this.entries = entries;
    }

    /**
     * Command object for creating a journal entry within a transaction.
     */
    public static class JournalEntryCommand {
        private UUID accountId;
        private java.math.BigDecimal debitAmount;
        private java.math.BigDecimal creditAmount;
        private String description;
        private LocalDate valueDate;

        public JournalEntryCommand() {
        }

        private BigDecimal exchangeRate = BigDecimal.ONE;
        private String currency;

        public JournalEntryCommand(UUID accountId, BigDecimal debitAmount, BigDecimal creditAmount, String description,
                LocalDate valueDate) {
            this.accountId = accountId;
            this.debitAmount = debitAmount;
            this.creditAmount = creditAmount;
            this.description = description;
            this.valueDate = valueDate;
        }

        // Getters and Setters
        public UUID getAccountId() {
            return accountId;
        }

        public void setAccountId(UUID accountId) {
            this.accountId = accountId;
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

        public BigDecimal getExchangeRate() {
            return exchangeRate;
        }

        public void setExchangeRate(BigDecimal exchangeRate) {
            this.exchangeRate = exchangeRate;
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

        public LocalDate getValueDate() {
            return valueDate;
        }

        public void setValueDate(LocalDate valueDate) {
            this.valueDate = valueDate;
        }
    }
}
