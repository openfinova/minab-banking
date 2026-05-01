package com.openfinova.banking.identity.config;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Role assignment rules: separation-of-duties pairs and the privileged assigner role name.
 */
@ConfigurationProperties(prefix = "identity.rbac")
public class RbacProperties {

    /**
     * Role name that may assign any role regardless of hierarchy (typically {@code ADMIN}).
     */
    private String privilegedAssignerRoleName = "ADMIN";

    /**
     * End-customer role name; only {@link com.openfinova.banking.identity.api.UserType#CUSTOMER}
     * users may hold it, and staff users must not.
     */
    private String customerPortalRoleName = "CUSTOMER";

    /**
     * Pairs of role names that must never be held by the same user together. Each entry is
     * {@code FIRST|SECOND} (order-independent at validation time).
     */
    private List<String> sodConflictingRolePairs = List.of("LOAN_OFFICER|LOAN_SUPERVISOR", "GL_ACCOUNTANT|GL_MANAGER");

    public String getPrivilegedAssignerRoleName() {
        return privilegedAssignerRoleName;
    }

    public void setPrivilegedAssignerRoleName(String privilegedAssignerRoleName) {
        this.privilegedAssignerRoleName = privilegedAssignerRoleName;
    }

    public String getCustomerPortalRoleName() {
        return customerPortalRoleName;
    }

    public void setCustomerPortalRoleName(String customerPortalRoleName) {
        this.customerPortalRoleName = customerPortalRoleName;
    }

    public List<String> getSodConflictingRolePairs() {
        return sodConflictingRolePairs;
    }

    public void setSodConflictingRolePairs(List<String> sodConflictingRolePairs) {
        this.sodConflictingRolePairs = sodConflictingRolePairs != null ? List.copyOf(sodConflictingRolePairs)
                : List.of();
    }

    /**
     * Parsed SoD pairs for runtime checks (immutable, uppercased names).
     */
    public List<Set<String>> parsedSodPairs() {
        List<Set<String>> out = new ArrayList<>();
        for (String raw : sodConflictingRolePairs) {
            if (raw == null || raw.isBlank()) {
                continue;
            }
            String[] parts = raw.trim().split("\\|");
            if (parts.length != 2) {
                continue;
            }
            String a = parts[0].trim().toUpperCase(Locale.ROOT);
            String b = parts[1].trim().toUpperCase(Locale.ROOT);
            if (a.isEmpty() || b.isEmpty() || a.equals(b)) {
                continue;
            }
            out.add(Set.of(a, b));
        }
        return List.copyOf(out);
    }

    /**
     * Returns true if the given role name set contains both roles of any configured pair.
     */
    public boolean violatesSeparationOfDuties(Set<String> roleNamesUpper) {
        Set<String> names = new LinkedHashSet<>(roleNamesUpper);
        for (Set<String> pair : parsedSodPairs()) {
            if (names.containsAll(pair)) {
                return true;
            }
        }
        return false;
    }

    /**
     * First conflicting pair found, or empty if none.
     */
    public Set<String> firstSodViolation(Set<String> roleNamesUpper) {
        Set<String> names = new LinkedHashSet<>(roleNamesUpper);
        for (Set<String> pair : parsedSodPairs()) {
            if (names.containsAll(pair)) {
                return pair;
            }
        }
        return Set.of();
    }
}
