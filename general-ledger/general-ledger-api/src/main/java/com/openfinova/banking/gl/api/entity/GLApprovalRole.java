package com.openfinova.banking.gl.api.entity;

/**
 * User roles for GL transaction authorization and approval workflow.
 * Defines the hierarchy of roles that can create and approve GL transactions.
 *
 * Roles are ordered from lowest to highest authority level.
 */
public enum GLApprovalRole {

    /**
     * Junior accountant - lowest authorization level.
     * Typically limited to small transaction amounts.
     */
    ACCOUNTANT("ROLE_ACCOUNTANT", "Accountant"),

    /**
     * Senior accountant - moderate authorization level.
     * Can create and approve medium-sized transactions.
     */
    SENIOR_ACCOUNTANT("ROLE_SENIOR_ACCOUNTANT", "Senior Accountant"),

    /**
     * Accounting manager - elevated authorization level.
     * Can approve larger transactions and manage accounting team.
     */
    MANAGER("ROLE_MANAGER", "Manager"),

    /**
     * Financial controller - high authorization level.
     * Oversees all accounting operations, can approve significant transactions.
     */
    CONTROLLER("ROLE_CONTROLLER", "Controller"),

    /**
     * Chief Financial Officer - highest authorization level.
     * Can approve unlimited amounts, typically requires dual approval for material transactions.
     */
    CFO("ROLE_CFO", "Chief Financial Officer");

    private final String authority;
    private final String displayName;

    GLApprovalRole(String authority, String displayName) {
        this.authority = authority;
        this.displayName = displayName;
    }

    /**
     * Get the Spring Security authority name (e.g., "ROLE_ACCOUNTANT").
     * Used for integration with Spring Security role-based access control.
     *
     * @return the authority string
     */
    public String getAuthority() {
        return authority;
    }

    /**
     * Get the human-readable display name.
     *
     * @return the display name
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Find GLApprovalRole by authority string.
     *
     * @param authority the authority string (e.g., "ROLE_ACCOUNTANT")
     * @return the matching GLApprovalRole
     * @throws IllegalArgumentException if no matching role found
     */
    public static GLApprovalRole fromAuthority(String authority) {
        if (authority == null) {
            throw new IllegalArgumentException("Authority cannot be null");
        }

        for (GLApprovalRole role : values()) {
            if (role.authority.equals(authority)) {
                return role;
            }
        }

        throw new IllegalArgumentException("Unknown authority: " + authority);
    }

    /**
     * Check if this role has higher or equal authority than another role.
     * Based on ordinal position in enum.
     *
     * @param other the role to compare against
     * @return true if this role has higher or equal authority
     */
    public boolean hasAuthorityOf(GLApprovalRole other) {
        return this.ordinal() >= other.ordinal();
    }
}
