package com.openfinova.banking.customer.account.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Schema(description = "Request to place a hold on account funds")
public class PlaceHoldRequest {

    @NotNull(message = "Amount is required")
    @Schema(description = "Hold amount", required = true)
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @Schema(description = "Currency code", required = true)
    private String currency;

    @NotBlank(message = "Reason is required")
    @Schema(description = "Reason for the hold", required = true)
    private String reason;

    @Schema(description = "Expiration date/time (null for indefinite)")
    private LocalDateTime expiresAt;

    // Getters and setters
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
}
