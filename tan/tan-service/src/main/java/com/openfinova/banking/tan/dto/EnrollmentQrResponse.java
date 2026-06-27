package com.openfinova.banking.tan.dto;

import java.time.Instant;

public record EnrollmentQrResponse(
        String qrPayload, Instant expiresAt
) {
}
