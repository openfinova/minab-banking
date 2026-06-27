package com.openfinova.banking.tan.dto;

import java.util.UUID;

public record EnrollDeviceResponse(
        UUID deviceId, String confirmationInput
) {
}
