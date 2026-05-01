package com.openfinova.banking.identity.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

@Schema(description = "Start a multi-step approval chain for a logical resource")
public class CreateApprovalWorkflowRequest {

    @NotNull
    @Size(max = 80)
    private String resourceType;

    @NotNull
    @Size(max = 120)
    private String resourceId;

    /**
     * Ordered minimum GL approval tiers (e.g. MANAGER then CFO). Each step must be satisfied in
     * sequence.
     */
    @NotEmpty
    private List<@Size(max = 30) String> requiredGlRolesInOrder;

    public String getResourceType() {
        return resourceType;
    }

    public void setResourceType(String v) {
        this.resourceType = v != null ? v.strip() : null;
    }

    public String getResourceId() {
        return resourceId;
    }

    public void setResourceId(String v) {
        this.resourceId = v != null ? v.strip() : null;
    }

    public List<String> getRequiredGlRolesInOrder() {
        return requiredGlRolesInOrder;
    }

    public void setRequiredGlRolesInOrder(List<String> v) {
        this.requiredGlRolesInOrder = v;
    }
}
