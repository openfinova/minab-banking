package com.openfinova.banking.identity.entity;

import java.time.LocalDateTime;
import java.util.EnumSet;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.identity.api.permission.BankingPermission;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * A named bundle of {@link BankingPermission} values.
 *
 * Users are assigned one or more roles; at token issuance time the union of all permissions across
 * all roles is flattened into the JWT {@code permissions} claim. This means endpoint guards
 * ({@code hasAuthority}) never need to enumerate role names.
 *
 * <p>
 * {@link #parentRole}, when set, points to the directly more-privileged role (toward {@code ADMIN})
 * for assignment validation: an assigner may grant only roles at or below their own position in
 * this chain unless they hold the configured privileged assigner role (see {@code identity.rbac}).
 */
@Entity
@Table(name = "identity_roles", indexes = {
        @Index(name = "idx_identity_roles_name", columnList = "name", unique = true) })
public class BankingRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(nullable = false)
    private Long version = 0L;

    @NotBlank
    @Size(max = 60)
    @Column(nullable = false, unique = true, length = 60)
    private String name;

    @Size(max = 120)
    @Column(name = "display_name", length = 120)
    private String displayName;

    @Size(max = 500)
    @Column(length = 500)
    private String description;

    /** True for roles seeded by the system; prevents deletion and permission stripping. */
    @Column(name = "system_role", nullable = false)
    private boolean systemRole = false;

    @Column(nullable = false)
    private boolean enabled = true;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "identity_role_permissions", joinColumns = @JoinColumn(name = "role_id"))
    @Column(name = "permission", length = 80)
    @Enumerated(EnumType.STRING)
    @Fetch(FetchMode.SUBSELECT)
    private Set<BankingPermission> permissions = EnumSet.noneOf(BankingPermission.class);

    /**
     * Additional permission grants constrained by time-of-day and/or branch/department.
     * Unconditional permissions remain in {@link #permissions}.
     */
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "identity_role_scoped_grants", joinColumns = @JoinColumn(name = "role_id"))
    @Fetch(FetchMode.SUBSELECT)
    private Set<ScopedPermissionGrant> scopedPermissionGrants = new HashSet<>();

    /**
     * More-privileged role directly above this one; {@code null} for roots such as {@code ADMIN}.
     * Eagerly loaded so hierarchy checks do not issue extra lazy queries during assignment.
     */
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "parent_role_id")
    private BankingRole parentRole;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    protected BankingRole() {
    }

    public BankingRole(String name, String displayName) {
        this.name = name;
        this.displayName = displayName;
    }

    public BankingRole withPermissions(BankingPermission... perms) {
        this.permissions = EnumSet.copyOf(Set.of(perms));
        return this;
    }

    public UUID getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String n) {
        this.displayName = n;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String d) {
        this.description = d;
    }

    public boolean isSystemRole() {
        return systemRole;
    }

    public void setSystemRole(boolean v) {
        this.systemRole = v;
    }

    public boolean isEnabled() {
        return enabled;
    }

    public void setEnabled(boolean v) {
        this.enabled = v;
    }

    public Set<BankingPermission> getPermissions() {
        return permissions;
    }

    public void setPermissions(Set<BankingPermission> p) {
        this.permissions = p;
    }

    public Set<ScopedPermissionGrant> getScopedPermissionGrants() {
        return scopedPermissionGrants;
    }

    public void setScopedPermissionGrants(Set<ScopedPermissionGrant> scopedPermissionGrants) {
        this.scopedPermissionGrants = scopedPermissionGrants;
    }

    public BankingRole getParentRole() {
        return parentRole;
    }

    public void setParentRole(BankingRole parentRole) {
        this.parentRole = parentRole;
    }

    public Long getVersion() {
        return version;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
