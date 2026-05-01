package com.openfinova.banking.customer.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.customer.account.api.entity.HoldStatus;

@Schema(description = "Account hold response")
public class AccountHoldResponse {

    @Schema(description = "Hold ID")
    private UUID id;

    @Schema(description = "Account ID")
    private UUID accountId;

    @Schema(description = "Hold amount")
    private BigDecimal amount;

    @Schema(description = "Currency")
    private String currency;

    @Schema(description = "Hold status")
    private HoldStatus status;

    @Schema(description = "Reason for hold")
    private String reason;

    @Schema(description = "Expiration date/time")
    private LocalDateTime expiresAt;

    @Schema(description = "Creation timestamp")
    private Instant createdAt;

    @Schema(description = "Released timestamp")
    private LocalDateTime releasedAt;

    // Getters and setters
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

    public BigDecimal getAmount() {
        return amount;
    }

    public void setAmount(BigDecimal amount) {
        this.amount = amount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public HoldStatus getStatus() {
        return status;
    }

    public void setStatus(HoldStatus status) {
        this.status = status;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getReleasedAt() {
        return releasedAt;
    }

    public void setReleasedAt(LocalDateTime releasedAt) {
        this.releasedAt = releasedAt;
    }
}
