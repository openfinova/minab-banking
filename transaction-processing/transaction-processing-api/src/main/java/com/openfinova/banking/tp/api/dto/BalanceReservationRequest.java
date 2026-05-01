package com.openfinova.banking.tp.api.dto;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.tp.api.entity.ReservationType;
import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;

/**
 * Request DTO for creating balance reservations in batch operations.
 */
public class BalanceReservationRequest {

    @NotNull(message = "Account ID is required")
    private UUID accountId;

    @NotNull(message = "Amount is required")
    @DecimalMin(value = "0.01", message = "Amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "Amount must have at most 15 integer digits and 4 decimal places")
    private BigDecimal amount;

    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency = "USD";

    @NotNull(message = "Reservation type is required")
    private ReservationType type;

    private LocalDateTime expiresAt;

    private String reservationReference;

    @NotBlank(message = "Reservation key is required for idempotency")
    @Size(max = 255, message = "Reservation key must not exceed 255 characters")
    private String reservationKey;

    private Map<String, Object> metadata;

    public BalanceReservationRequest() {
    }

    public BalanceReservationRequest(UUID accountId, BigDecimal amount, String currency, ReservationType type,
            String reservationKey) {
        this.accountId = accountId;
        this.amount = amount;
        this.currency = currency;
        this.type = type;
        this.reservationKey = reservationKey;
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

    public ReservationType getType() {
        return type;
    }

    public void setType(ReservationType type) {
        this.type = type;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getReservationReference() {
        return reservationReference;
    }

    public void setReservationReference(String reservationReference) {
        this.reservationReference = reservationReference;
    }

    public String getReservationKey() {
        return reservationKey;
    }

    public void setReservationKey(String reservationKey) {
        this.reservationKey = reservationKey;
    }

    public Map<String, Object> getMetadata() {
        return metadata;
    }

    public void setMetadata(Map<String, Object> metadata) {
        this.metadata = metadata;
    }

    @Override
    public String toString() {
        return "BalanceReservationRequest{" + "accountId=" + accountId + ", amount=" + amount + ", currency='"
                + currency + '\'' + ", type=" + type + ", reservationKey='" + reservationKey + '\'' + '}';
    }
}