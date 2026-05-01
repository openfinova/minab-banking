package com.openfinova.banking.customer.account.controller;

import com.openfinova.banking.customer.account.api.dto.*;
import com.openfinova.banking.customer.account.api.entity.AccountPermission;
import com.openfinova.banking.customer.account.entity.AccountRelationship;
import com.openfinova.banking.customer.account.mapper.AccountRelationshipMapper;
import com.openfinova.banking.customer.account.service.AccountRelationshipService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/accounts")
@Tag(name = "Account Relationships", description = "APIs for managing account relationships and permissions")
/**
 * REST controller for account relationship management.
 * Handles adding relationships, managing beneficiaries, updating permissions, and checking permissions on customer accounts.
 */
public class AccountRelationshipController {

    private static final Logger log = LoggerFactory.getLogger(AccountRelationshipController.class);

    private final AccountRelationshipService relationshipService;
    private final AccountRelationshipMapper relationshipMapper;

    public AccountRelationshipController(AccountRelationshipService relationshipService,
            AccountRelationshipMapper relationshipMapper) {
        this.relationshipService = relationshipService;
        this.relationshipMapper = relationshipMapper;
    }

    @PostMapping("/{id}/relationships")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Add relationship", description = "Creates a new relationship between a user and an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Relationship created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data") })
    public ResponseEntity<AccountRelationshipResponse> addRelationship(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody AddRelationshipRequest request) {

        log.info(
                "Adding relationship to account {}: user={}, type={}",
                id,
                request.getUserProfileId(),
                request.getRelationshipType());

        AccountRelationship relationship = relationshipService
                .addRelationship(id, request.getUserProfileId(), request.getRelationshipType(), request.getCreatedBy());

        log.info("Successfully created relationship with ID: {}", relationship.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(relationshipMapper.toResponse(relationship));
    }

    @GetMapping("/{id}/relationships")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Get account relationships", description = "Retrieves all relationships for an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Relationships retrieved successfully") })
    public ResponseEntity<List<AccountRelationshipResponse>> getAccountRelationships(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id) {

        log.info("Fetching relationships for account: {}", id);

        List<AccountRelationship> relationships = relationshipService.getRelationshipsByAccount(id);
        List<AccountRelationshipResponse> response = relationships.stream().map(relationshipMapper::toResponse)
                .toList();

        log.info("Found {} relationships for account: {}", response.size(), id);

        return ResponseEntity.ok(response);
    }

    @DeleteMapping("/{id}/relationships/{userProfileId}")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Remove relationship", description = "Removes a relationship between a user and an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Relationship removed successfully"),
            @ApiResponse(responseCode = "404", description = "Relationship not found") })
    public ResponseEntity<Void> removeRelationship(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "User profile ID", required = true) @PathVariable UUID userProfileId) {

        log.info("Removing relationship for account {} and user {}", id, userProfileId);

        // Find the relationship first
        List<AccountRelationship> relationships = relationshipService.getRelationshipsByAccount(id);
        relationships.stream().filter(r -> r.getUserProfileId().equals(userProfileId)).findFirst()
                .ifPresent(r -> relationshipService.removeRelationship(r.getId()));

        log.info("Successfully removed relationship for account {} and user {}", id, userProfileId);

        return ResponseEntity.ok().build();
    }

    @PatchMapping("/relationships/{relationshipId}/permissions")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Update permissions", description = "Updates permissions for an account relationship")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Permissions updated successfully"),
            @ApiResponse(responseCode = "404", description = "Relationship not found") })
    public ResponseEntity<Void> updatePermissions(
            @Parameter(description = "Relationship ID", required = true) @PathVariable UUID relationshipId,
            @Valid @RequestBody UpdatePermissionsRequest request) {

        log.info("Updating permissions for relationship: {}", relationshipId);

        relationshipService.updatePermissions(relationshipId, request.getPermissions());

        log.info("Successfully updated permissions for relationship: {}", relationshipId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{id}/beneficiaries")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Add beneficiary", description = "Adds a beneficiary to an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Beneficiary added successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid input data") })
    public ResponseEntity<AccountRelationshipResponse> addBeneficiary(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody AddBeneficiaryRequest request) {

        log.info("Adding beneficiary to account {}: user={}", id, request.getUserProfileId());

        AccountRelationship relationship = relationshipService.addBeneficiary(id, request);

        log.info("Successfully added beneficiary with ID: {}", relationship.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(relationshipMapper.toResponse(relationship));
    }

    @DeleteMapping("/{id}/beneficiaries/{userProfileId}")
    @PreAuthorize("hasAuthority('account:write')")
    @Operation(summary = "Remove beneficiary", description = "Removes a beneficiary from an account")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Beneficiary removed successfully"),
            @ApiResponse(responseCode = "404", description = "Beneficiary not found") })
    public ResponseEntity<Void> removeBeneficiary(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "User profile ID", required = true) @PathVariable UUID userProfileId) {

        log.info("Removing beneficiary from account {}: user={}", id, userProfileId);

        relationshipService.removeBeneficiary(id, userProfileId);

        log.info("Successfully removed beneficiary from account: {}", id);

        return ResponseEntity.ok().build();
    }

    @GetMapping("/user/{userProfileId}/all")
    @PreAuthorize("hasAuthority('account:read:own')")
    @Operation(summary = "Get all accounts for user", description = "Retrieves all accounts where a user has any relationship")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Accounts retrieved successfully") })
    public ResponseEntity<List<AccountRelationshipResponse>> getAllAccountsForUser(
            @Parameter(description = "User profile ID", required = true) @PathVariable UUID userProfileId) {

        log.info("Fetching all accounts for user: {}", userProfileId);

        List<AccountRelationship> relationships = relationshipService.getRelationshipsByUserProfile(userProfileId);
        List<AccountRelationshipResponse> response = relationships.stream().map(relationshipMapper::toResponse)
                .toList();

        log.info("Found {} account relationships for user: {}", response.size(), userProfileId);

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{id}/permissions/check")
    @PreAuthorize("hasAuthority('account:read')")
    @Operation(summary = "Check permission", description = "Validates if a user has permission to perform an action")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Permission check completed") })
    public ResponseEntity<ValidationResult> checkPermission(
            @Parameter(description = "Account ID", required = true) @PathVariable UUID id,
            @Parameter(description = "User profile ID") @RequestParam UUID userProfileId,
            @Parameter(description = "Required permission") @RequestParam AccountPermission permission) {

        log.info("Checking permission for account {} and user {}: {}", id, userProfileId, permission);

        boolean hasPermission = relationshipService.hasPermission(id, userProfileId, permission);

        ValidationResult result = new ValidationResult();
        result.setValid(hasPermission);
        result.setMessage(
                hasPermission ? "User has the required permission"
                        : "User does not have the required permission: " + permission);

        return ResponseEntity.ok(result);
    }
}
