package com.openfinova.banking.gl.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional comments when approving a pending GL transaction")
public class GlApproveTransactionRequest {

    @Size(max = 500)
    @Schema(description = "Optional notes for the approval audit trail")
    private String comments;

    public GlApproveTransactionRequest() {
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }
}
