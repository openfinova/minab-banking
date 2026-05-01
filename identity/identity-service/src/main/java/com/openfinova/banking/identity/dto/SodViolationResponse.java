package com.openfinova.banking.identity.dto;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A user currently holding two conflicting roles that violate a Segregation of Duties rule")
public class SodViolationResponse {

    private UUID userId;
    private String username;
    private List<String> conflictingRoles;

    public SodViolationResponse() {
    }

    public SodViolationResponse(UUID userId, String username, List<String> conflictingRoles) {
        this.userId = userId;
        this.username = username;
        this.conflictingRoles = conflictingRoles;
    }

    public UUID getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public List<String> getConflictingRoles() {
        return conflictingRoles;
    }
}
