package com.openfinova.banking.tan.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ConfirmDeviceRequest(
        @NotBlank @Size(min = 8, max = 8) String confirmationCode
) {
}
