package com.openfinova.banking.identity.security;

import java.time.LocalDateTime;

import com.openfinova.banking.identity.config.PasswordPolicyProperties;
import com.openfinova.banking.identity.entity.BankingUser;

/**
 * Shared password expiry rules for {@link BankingUserDetails}, token issuance, and audits.
 */
public final class PasswordLifecycleEvaluator {

    private PasswordLifecycleEvaluator() {
    }

    /**
     * Effective expiry instant: explicit {@link BankingUser#getPasswordExpiresAt()} if set,
     * otherwise derived from {@link BankingUser#getPasswordChangedAt()} and {@code maxAgeDays}
     * when policy age is positive.
     */
    public static LocalDateTime effectivePasswordExpiresAt(BankingUser user, PasswordPolicyProperties policy) {
        LocalDateTime explicit = user.getPasswordExpiresAt();
        if (explicit != null) {
            return explicit;
        }
        if (policy.getMaxAgeDays() <= 0 || user.getPasswordChangedAt() == null) {
            return null;
        }
        return user.getPasswordChangedAt().plusDays(policy.getMaxAgeDays());
    }

    public static boolean isPasswordExpired(BankingUser user, PasswordPolicyProperties policy, LocalDateTime now) {
        LocalDateTime exp = effectivePasswordExpiresAt(user, policy);
        return exp != null && !exp.isAfter(now);
    }
}
