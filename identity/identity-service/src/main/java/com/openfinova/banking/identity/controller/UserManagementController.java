package com.openfinova.banking.identity.controller;

import java.util.Map;
import java.util.Set;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.identity.dto.CreateUserRequest;
import com.openfinova.banking.identity.dto.DeprovisionUserRequest;
import com.openfinova.banking.identity.dto.LockUserRequest;
import com.openfinova.banking.identity.dto.RejectProvisioningRequest;
import com.openfinova.banking.identity.dto.SuspendUserRequest;
import com.openfinova.banking.identity.dto.UpdateUserAccessRequest;
import com.openfinova.banking.identity.dto.UserResponse;
import com.openfinova.banking.identity.dto.UserSearchCriteria;
import com.openfinova.banking.identity.service.UserManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/identity/users")
@Tag(name = "User Management", description = """
        Administrative lifecycle for **BankingUser** records: create, search, roles, lock/unlock, \
        provisioning approval, suspension, deprovisioning, password reset, and soft delete. \
        All routes require `admin:users:read` or `admin:users:write` (see each operation).""")
@SecurityRequirement(name = "oauth2")
public class UserManagementController {

    private final UserManagementService userService;

    public UserManagementController(UserManagementService userService) {
        this.userService = userService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Create a new user account", description = "Assigns initial roles and enforces password policy on the provided password.")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "User created"),
            @ApiResponse(responseCode = "400", description = "Validation failure or duplicate username"),
            @ApiResponse(responseCode = "401", description = "Not authenticated"),
            @ApiResponse(responseCode = "403", description = "Missing admin:users:write") })
    public ResponseEntity<UserResponse> createUser(Authentication auth, @Valid @RequestBody CreateUserRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(UserResponse.from(userService.createUser(request, actor)));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin:users:read')")
    @Operation(summary = "List all users (paginated)")
    public ResponseEntity<Page<UserResponse>> listUsers(Pageable pageable) {
        return ResponseEntity.ok(userService.listUsers(pageable).map(UserResponse::from));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:users:read')")
    @Operation(summary = "Get user by ID")
    public ResponseEntity<UserResponse> getUser(
            @Parameter(description = "User primary key", required = true) @PathVariable UUID id) {
        return ResponseEntity.ok(UserResponse.from(userService.getUser(id)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('admin:users:read')")
    @Operation(summary = "Search users by criteria", description = "Combines optional filters (username, email, type, flags, role, branch) with Spring Data pagination.")
    public ResponseEntity<Page<UserResponse>> searchUsers(@ModelAttribute UserSearchCriteria criteria,
            @Parameter(description = "Pagination: page, size, sort") Pageable pageable) {
        return ResponseEntity.ok(userService.searchUsers(criteria, pageable).map(UserResponse::from));
    }

    @GetMapping("/by-customer/{customerPartyId}")
    @PreAuthorize("hasAuthority('admin:users:read')")
    @Operation(summary = "Find user linked to a customer party record")
    public ResponseEntity<UserResponse> getUserByCustomerPartyId(
            @Parameter(description = "Customer party UUID", required = true) @PathVariable UUID customerPartyId) {
        return ResponseEntity.ok(UserResponse.from(userService.getUserByCustomerPartyId(customerPartyId)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Update user access fields (email, branch, employee ID, etc.)")
    public ResponseEntity<UserResponse> updateUserAccess(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody UpdateUserAccessRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(UserResponse.from(userService.updateUserAccess(id, request, actor)));
    }

    @PutMapping("/{id}/roles")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Assign roles to a user", description = "Replaces the user's role set entirely with the given role names (must exist).")
    public ResponseEntity<UserResponse> assignRoles(Authentication auth,
            @Parameter(description = "User primary key", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "Set of role names to assign", required = true) @RequestBody Set<String> roleNames) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(UserResponse.from(userService.assignRoles(id, roleNames, actor)));
    }

    @PatchMapping("/{id}/enabled")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Enable or disable a user account")
    public ResponseEntity<UserResponse> setEnabled(Authentication auth,
            @Parameter(description = "User primary key", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON body `{ \"enabled\": true|false }`", required = true) @RequestBody Map<String, Boolean> body) {
        boolean enabled = Boolean.TRUE.equals(body.get("enabled"));
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(UserResponse.from(userService.setEnabled(id, enabled, actor)));
    }

    @PatchMapping("/{id}/lock")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Lock a user account with a reason")
    public ResponseEntity<UserResponse> lockUser(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody LockUserRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(UserResponse.from(userService.lockUser(id, request.getReason(), actor)));
    }

    @PatchMapping("/{id}/unlock")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Unlock a user account and reset failed login attempts")
    public ResponseEntity<UserResponse> unlockUser(Authentication auth, @PathVariable UUID id) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(UserResponse.from(userService.unlockUser(id, actor)));
    }

    @PatchMapping("/{id}/password")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Reset a user's password (admin action)")
    public ResponseEntity<Void> resetPassword(Authentication auth,
            @Parameter(description = "User primary key", required = true) @PathVariable UUID id,
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "JSON body `{ \"password\": \"...\" }`", required = true) @RequestBody Map<String, String> body) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        userService.resetPassword(id, body.get("password"), actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/force-password-change")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Force a user to change their password on next login")
    public ResponseEntity<Void> forcePasswordChange(Authentication auth, @PathVariable UUID id) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        userService.forcePasswordChange(id, actor);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Soft-delete a user account (disables and timestamps)")
    public ResponseEntity<Void> softDeleteUser(Authentication auth, @PathVariable UUID id) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        userService.softDeleteUser(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/provisioning/approve")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Approve pending account provisioning (enables login)")
    public ResponseEntity<UserResponse> approveProvisioning(Authentication auth, @PathVariable UUID id) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(UserResponse.from(userService.approveProvisioning(id, actor)));
    }

    @PostMapping("/{id}/provisioning/reject")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Reject pending account provisioning")
    public ResponseEntity<UserResponse> rejectProvisioning(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody RejectProvisioningRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(UserResponse.from(userService.rejectProvisioning(id, request.getReason(), actor)));
    }

    @PatchMapping("/{id}/suspend")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Suspend sign-in with reason and optional end time")
    public ResponseEntity<UserResponse> suspendUser(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody SuspendUserRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(
                UserResponse
                        .from(userService.suspendUser(id, request.getReason(), request.getSuspensionUntil(), actor)));
    }

    @PatchMapping("/{id}/reactivate")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Clear administrative suspension")
    public ResponseEntity<UserResponse> reactivateUser(Authentication auth, @PathVariable UUID id) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(UserResponse.from(userService.reactivateUser(id, actor)));
    }

    @PostMapping("/{id}/deprovision")
    @PreAuthorize("hasAuthority('admin:users:write')")
    @Operation(summary = "Deprovision a user account", description = "Revokes roles and MFA, invalidates password, disables the account, and publishes "
            + "UserAccountDeprovisionedEvent for downstream systems.")
    public ResponseEntity<Void> deprovisionUser(Authentication auth, @PathVariable UUID id,
            @RequestBody(required = false) DeprovisionUserRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        String reason = request != null ? request.getReason() : null;
        userService.deprovisionUser(id, reason, actor);
        return ResponseEntity.noContent().build();
    }
}
