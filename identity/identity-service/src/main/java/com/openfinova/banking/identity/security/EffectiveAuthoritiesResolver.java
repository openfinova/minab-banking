package com.openfinova.banking.identity.security;

import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;

import com.openfinova.banking.identity.api.permission.BankingPermission;
import com.openfinova.banking.identity.entity.BankingRole;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.ScopedPermissionGrant;

/**
 * Resolves which {@link BankingPermission} values are effective for a user at a
 * given instant: unconditional role permissions plus scoped grants whose
 * time and org constraints match.
 */
public final class EffectiveAuthoritiesResolver {

    private EffectiveAuthoritiesResolver() {
    }

    public static Set<BankingPermission> resolveEffectivePermissions(BankingUser user, Clock clock) {
        ZoneId zone = ZoneId.systemDefault();
        LocalTime now = LocalTime.now(clock.withZone(zone));
        EnumSet<BankingPermission> out = EnumSet.noneOf(BankingPermission.class);
        for (BankingRole role : user.getRoles()) {
            if (!role.isEnabled()) {
                continue;
            }
            out.addAll(role.getPermissions());
            for (ScopedPermissionGrant grant : role.getScopedPermissionGrants()) {
                if (grant.getPermission() != null && isGrantEffective(grant, user, now)) {
                    out.add(grant.getPermission());
                }
            }
        }
        return out;
    }

    static boolean isGrantEffective(ScopedPermissionGrant grant, BankingUser user, LocalTime now) {
        if (!matchesBranch(grant.getBranchCode(), user.getBranchCode())) {
            return false;
        }
        if (!matchesDepartment(grant.getDepartmentCode(), user.getDepartmentCode())) {
            return false;
        }
        return isWithinTimeWindow(now, grant.getTimeWindowStart(), grant.getTimeWindowEnd());
    }

    private static boolean matchesBranch(String grantBranch, String userBranch) {
        if (grantBranch == null || grantBranch.isBlank()) {
            return true;
        }
        return grantBranch.equals(userBranch);
    }

    private static boolean matchesDepartment(String grantDept, String userDept) {
        if (grantDept == null || grantDept.isBlank()) {
            return true;
        }
        return Objects.equals(grantDept, userDept);
    }

    /**
     * Inclusive daily window in local time. {@code null} start/end means unbounded on that side.
     * If start is after end, the window crosses midnight.
     */
    static boolean isWithinTimeWindow(LocalTime now, LocalTime start, LocalTime end) {
        if (start == null && end == null) {
            return true;
        }
        if (start != null && end == null) {
            return !now.isBefore(start);
        }
        if (start == null) {
            return !now.isAfter(end);
        }
        if (!start.isAfter(end)) {
            return !now.isBefore(start) && !now.isAfter(end);
        }
        return !now.isBefore(start) || !now.isAfter(end);
    }
}
