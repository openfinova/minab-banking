package com.openfinova.banking.tp.api.dto;

import jakarta.validation.constraints.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Request DTO for modifying existing balance reservations.
 */
public class ReservationModificationRequest {

    @NotNull(message = "Reservation ID is required")
    private UUID reservationId;

    @NotNull(message = "New amount is required")
    @DecimalMin(value = "0.01", message = "New amount must be greater than 0")
    @Digits(integer = 15, fraction = 4, message = "New amount must have at most 15 integer digits and 4 decimal places")
    private BigDecimal newAmount;

    @NotBlank(message = "Reason is required")
    @Size(max = 255, message = "Reason must not exceed 255 characters")
    private String reason;

    private LocalDateTime newExpirationTime;

    // Constructors
    public ReservationModificationRequest() {
    }

    public ReservationModificationRequest(UUID reservationId, BigDecimal newAmount, String reason) {
        this.reservationId = reservationId;
        this.newAmount = newAmount;
        this.reason = reason;
    }

    // Getters and Setters
    public UUID getReservationId() {
        return reservationId;
    }

    public void setReservationId(UUID reservationId) {
        this.reservationId = reservationId;
    }

    public BigDecimal getNewAmount() {
        return newAmount;
    }

    public void setNewAmount(BigDecimal newAmount) {
        this.newAmount = newAmount;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getNewExpirationTime() {
        return newExpirationTime;
    }

    public void setNewExpirationTime(LocalDateTime newExpirationTime) {
        this.newExpirationTime = newExpirationTime;
    }

    @Override
    public String toString() {
        return "ReservationModificationRequest{" + "reservationId=" + reservationId + ", newAmount=" + newAmount
                + ", reason='" + reason + '\'' + ", newExpirationTime=" + newExpirationTime + '}';
    }
}