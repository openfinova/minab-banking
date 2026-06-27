package com.openfinova.banking.tan.dto;

import java.time.Instant;

public record VerifyTanResponse(boolean verified, Instant verifiedAt) {
}
