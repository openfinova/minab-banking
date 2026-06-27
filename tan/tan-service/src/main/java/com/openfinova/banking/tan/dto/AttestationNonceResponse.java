package com.openfinova.banking.tan.dto;

import java.time.Instant;

public record AttestationNonceResponse(
        String nonce, Instant expiresAt
) {
}
