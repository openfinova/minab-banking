package com.openfinova.banking.gl.api.dto;

import com.openfinova.banking.gl.api.entity.GLAccountStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;

@Schema(description = "Request to update GL account status")
public class UpdateGLAccountStatusRequest {

    @NotNull(message = "Status is required")
    @Schema(description = "New account status", example = "INACTIVE")
    private GLAccountStatus status;

    // Constructors
    public UpdateGLAccountStatusRequest() {
    }

    public UpdateGLAccountStatusRequest(GLAccountStatus status) {
        this.status = status;
    }

    // Getters and Setters
    public GLAccountStatus getStatus() {
        return status;
    }

    public void setStatus(GLAccountStatus status) {
        this.status = status;
    }
}
