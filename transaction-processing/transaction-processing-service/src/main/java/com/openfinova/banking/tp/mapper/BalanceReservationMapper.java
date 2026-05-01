package com.openfinova.banking.tp.mapper;

import com.openfinova.banking.tp.api.dto.BalanceReservationResponse;
import com.openfinova.banking.tp.entity.BalanceReservation;
import org.springframework.stereotype.Component;

@Component
public class BalanceReservationMapper {

    public BalanceReservationResponse toResponse(BalanceReservation reservation) {
        if (reservation == null) {
            return null;
        }

        BalanceReservationResponse response = new BalanceReservationResponse();
        response.setId(reservation.getId());
        response.setAccountId(reservation.getAccountId());
        response.setTransactionId(reservation.getTransaction() != null ? reservation.getTransaction().getId() : null);
        response.setAmount(reservation.getReservedAmount());
        response.setCurrency(reservation.getCurrency());
        response.setType(reservation.getReservationType());
        response.setStatus(reservation.getStatus());
        response.setExpiresAt(reservation.getExpiresAt());
        response.setCreatedAt(reservation.getCreatedAt());
        response.setReleasedAt(reservation.getReleasedAt());

        return response;
    }
}
