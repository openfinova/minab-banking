package com.openfinova.banking.tan.dto;

import java.time.Instant;
import java.util.UUID;

import com.openfinova.banking.tan.entity.TanDeviceStatus;

public record TanDeviceResponse(UUID id, String deviceName, TanDeviceStatus status, Instant enrolledAt,
        Instant lastUsedAt) {
}
