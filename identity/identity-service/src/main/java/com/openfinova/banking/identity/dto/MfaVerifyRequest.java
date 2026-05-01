package com.openfinova.banking.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "TOTP code to confirm MFA enrollment (6 digits)")
public class MfaVerifyRequest {

    @NotBlank
    @Size(min = 6, max = 6)
    private String code;

    public String getCode() {
        return code;
    }

    public void setCode(String v) {
        this.code = v;
    }
}
