package com.openfinova.banking.identity.controller;

import java.util.Arrays;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.permission.BankingPermission;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.identity.dto.CreateRoleRequest;
import com.openfinova.banking.identity.dto.PermissionModificationRequest;
import com.openfinova.banking.identity.dto.RoleResponse;
import com.openfinova.banking.identity.dto.UpdateRoleRequest;
import com.openfinova.banking.identity.service.RoleManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/identity/roles")
@Tag(name = "Role Management", description = """
        CRUD for **BankingRole** and permission catalogue inspection. \
        System roles may be protected from deletion; permission values accept enum names or authority strings.""")
@SecurityRequirement(name = "oauth2")
public class RoleManagementController {

    private final RoleManagementService roleService;

    public RoleManagementController(RoleManagementService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('admin:roles:read')")
    @Operation(summary = "List all roles and their permissions")
    public ResponseEntity<List<RoleResponse>> listRoles() {
        return ResponseEntity.ok(roleService.listRoles().stream().map(RoleResponse::from).toList());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:roles:read')")
    @Operation(summary = "Get a role by ID")
    public ResponseEntity<RoleResponse> getRole(@PathVariable UUID id) {
        return ResponseEntity.ok(RoleResponse.from(roleService.getRole(id)));
    }

    @GetMapping("/permissions")
    @PreAuthorize("hasAuthority('admin:roles:read')")
    @Operation(summary = "List all available permissions from the permission catalogue")
    public ResponseEntity<List<PermissionInfo>> listAvailablePermissions() {
        List<PermissionInfo> perms = Arrays.stream(BankingPermission.values())
                .map(p -> new PermissionInfo(p.name(), p.getAuthority())).toList();
        return ResponseEntity.ok(perms);
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:roles:write')")
    @Operation(summary = "Create a new custom role")
    public ResponseEntity<RoleResponse> createRole(Authentication auth, @Valid @RequestBody CreateRoleRequest request) {
        Set<BankingPermission> permissions = resolvePermissions(request.getPermissions());
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.status(HttpStatus.CREATED).body(
                RoleResponse.from(
                        roleService.createRole(
                                request.getName(),
                                request.getDisplayName(),
                                request.getDescription(),
                                permissions,
                                actor)));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:roles:write')")
    @Operation(summary = "Update role metadata (display name, description)")
    public ResponseEntity<RoleResponse> updateRole(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody UpdateRoleRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(
                RoleResponse
                        .from(roleService.updateRole(id, request.getDisplayName(), request.getDescription(), actor)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('admin:roles:write')")
    @Operation(summary = "Delete a non-system role")
    public ResponseEntity<Void> deleteRole(Authentication auth, @PathVariable UUID id) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        roleService.deleteRole(id, actor);
        return ResponseEntity.noContent().build();
    }

    @PutMapping("/{id}/permissions")
    @PreAuthorize("hasAuthority('admin:roles:write')")
    @Operation(summary = "Replace the entire permission set of a role")
    public ResponseEntity<RoleResponse> setPermissions(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody PermissionModificationRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(
                RoleResponse.from(roleService.setPermissions(id, resolvePermissions(request.getPermissions()), actor)));
    }

    @PatchMapping("/{id}/permissions/add")
    @PreAuthorize("hasAuthority('admin:roles:write')")
    @Operation(summary = "Add permissions to a role")
    public ResponseEntity<RoleResponse> addPermissions(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody PermissionModificationRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(
                RoleResponse.from(roleService.addPermissions(id, resolvePermissions(request.getPermissions()), actor)));
    }

    @PatchMapping("/{id}/permissions/remove")
    @PreAuthorize("hasAuthority('admin:roles:write')")
    @Operation(summary = "Remove permissions from a role")
    public ResponseEntity<RoleResponse> removePermissions(Authentication auth, @PathVariable UUID id,
            @Valid @RequestBody PermissionModificationRequest request) {
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        return ResponseEntity.ok(
                RoleResponse
                        .from(roleService.removePermissions(id, resolvePermissions(request.getPermissions()), actor)));
    }

    private Set<BankingPermission> resolvePermissions(Set<String> permissionStrings) {
        if (permissionStrings == null || permissionStrings.isEmpty()) {
            return EnumSet.noneOf(BankingPermission.class);
        }
        return permissionStrings.stream().map(s -> {
            for (BankingPermission p : BankingPermission.values()) {
                if (p.name().equals(s) || p.getAuthority().equals(s)) {
                    return p;
                }
            }
            throw new IllegalArgumentException("Unknown permission: " + s);
        }).collect(Collectors.toCollection(() -> EnumSet.noneOf(BankingPermission.class)));
    }

    /**
     * Lightweight DTO for the permission catalogue listing.
     *
     * @param name      enum constant name (e.g. {@code LOAN_READ})
     * @param authority value placed in JWT {@code permissions} claim (e.g. {@code loan:read})
     */
    @Schema(name = "PermissionInfo", description = "Single entry from the banking permission catalogue")
    public record PermissionInfo(
            @Schema(description = "Permission enum constant name", example = "LOAN_READ") String name,
            @Schema(description = "Authority string used in JWT and @PreAuthorize", example = "loan:read") String authority) {
    }
}
