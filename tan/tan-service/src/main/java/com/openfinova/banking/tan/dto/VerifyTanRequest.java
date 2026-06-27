package com.openfinova.banking.tan.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record VerifyTanRequest(@NotNull UUID txnId, @NotBlank @Size(min = 8, max = 8) String tanCode) {
}
