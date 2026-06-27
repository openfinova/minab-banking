package com.openfinova.banking.tan.dto;

import java.time.Instant;

public record PaymentQrResponse(
        String qrPayload, Instant expiresAt
) {
}
