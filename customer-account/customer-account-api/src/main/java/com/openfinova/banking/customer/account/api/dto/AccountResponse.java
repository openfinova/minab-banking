package com.openfinova.banking.customer.account.api.dto;

import com.openfinova.banking.customer.account.api.entity.AccountProductType;
import com.openfinova.banking.customer.account.api.entity.AccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

@Schema(description = "Account response")
public class AccountResponse {

    @Schema(description = "Account unique identifier")
    private UUID id;

    @Schema(description = "Account number")
    private String accountNumber;

    @Schema(description = "IBAN")
    private String iban;

    @Schema(description = "Primary user profile ID")
    private UUID primaryUserProfileId;

    @Schema(description = "Product type")
    private AccountProductType productType;

    @Schema(description = "Account status")
    private AccountStatus status;

    @Schema(description = "Display name")
    private String displayName;

    @Schema(description = "Description")
    private String description;

    @Schema(description = "Currency (ISO 4217 3-letter code)")
    private String currency;

    @Schema(description = "Ledger balance")
    private BigDecimal ledgerBalance;

    @Schema(description = "Available balance")
    private BigDecimal availableBalance;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Created by")
    private String createdBy;

    @Schema(description = "Last update timestamp")
    private Instant updatedAt;

    @Schema(description = "Closure timestamp")
    private LocalDateTime closedAt;

    @Schema(description = "Closure reason")
    private String closureReason;

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public String getIban() {
        return iban;
    }

    public void setIban(String iban) {
        this.iban = iban;
    }

    public UUID getPrimaryUserProfileId() {
        return primaryUserProfileId;
    }

    public void setPrimaryUserProfileId(UUID primaryUserProfileId) {
        this.primaryUserProfileId = primaryUserProfileId;
    }

    public AccountProductType getProductType() {
        return productType;
    }

    public void setProductType(AccountProductType productType) {
        this.productType = productType;
    }

    public AccountStatus getStatus() {
        return status;
    }

    public void setStatus(AccountStatus status) {
        this.status = status;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public BigDecimal getLedgerBalance() {
        return ledgerBalance;
    }

    public void setLedgerBalance(BigDecimal ledgerBalance) {
        this.ledgerBalance = ledgerBalance;
    }

    public BigDecimal getAvailableBalance() {
        return availableBalance;
    }

    public void setAvailableBalance(BigDecimal availableBalance) {
        this.availableBalance = availableBalance;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public LocalDateTime getClosedAt() {
        return closedAt;
    }

    public void setClosedAt(LocalDateTime closedAt) {
        this.closedAt = closedAt;
    }

    public String getClosureReason() {
        return closureReason;
    }

    public void setClosureReason(String closureReason) {
        this.closureReason = closureReason;
    }
}
