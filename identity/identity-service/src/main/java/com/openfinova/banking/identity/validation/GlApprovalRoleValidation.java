package com.openfinova.banking.identity.validation;

import java.util.Set;

/**
 * Known GL approval role codes aligned with {@code com.openfinova.banking.gl.api.entity.GLApprovalRole}
 * (identity-service does not depend on general-ledger-api). Users' {@code glApprovalRole} must match
 * one of these when set, so JWT claims and downstream GL approval checks stay consistent.
 */
public final class GlApprovalRoleValidation {

    /** Ordered from lowest to highest authority (ordinal semantics must match GL enum). */
    public static final String[] ORDERED_CODES = { "ACCOUNTANT", "SENIOR_ACCOUNTANT", "MANAGER", "CONTROLLER", "CFO" };

    private static final Set<String> KNOWN = Set.of(ORDERED_CODES);

    private GlApprovalRoleValidation() {
    }

    public static void requireValidOrNull(String glApprovalRole) {
        if (glApprovalRole == null || glApprovalRole.isBlank()) {
            return;
        }
        String code = glApprovalRole.strip();
        if (!KNOWN.contains(code)) {
            throw new IllegalArgumentException("Unknown glApprovalRole: " + code + ". Expected one of: " + KNOWN);
        }
    }

    public static int hierarchyIndex(String glApprovalRole) {
        if (glApprovalRole == null || glApprovalRole.isBlank()) {
            return -1;
        }
        String code = glApprovalRole.strip();
        for (int i = 0; i < ORDERED_CODES.length; i++) {
            if (ORDERED_CODES[i].equals(code)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * @return true if {@code actorRole} is at or above {@code requiredRole} in the approval hierarchy
     */
    public static boolean satisfiesMinimumRole(String actorRole, String requiredRole) {
        int a = hierarchyIndex(actorRole);
        int r = hierarchyIndex(requiredRole);
        if (r < 0) {
            return false;
        }
        return a >= r;
    }
}
