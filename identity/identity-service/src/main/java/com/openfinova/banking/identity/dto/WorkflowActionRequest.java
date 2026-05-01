package com.openfinova.banking.identity.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;

@Schema(description = "Optional comment on approve / reject")
public class WorkflowActionRequest {

    @Size(max = 500)
    private String comment;

    public String getComment() {
        return comment;
    }

    public void setComment(String v) {
        this.comment = v;
    }
}
