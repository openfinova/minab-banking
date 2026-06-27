package com.openfinova.banking.tan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record EnrollDeviceRequest(
        @NotBlank String enrollmentToken, @NotBlank @Size(max = 200) String deviceName,
        @NotBlank String attestationToken, @NotBlank String tanSecret, @Size(max = 256) String platformDeviceId
) {
}
