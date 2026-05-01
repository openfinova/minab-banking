package com.openfinova.banking.identity.security;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.config.PasswordPolicyProperties;
import com.openfinova.banking.identity.entity.AccountProvisioningStatus;
import com.openfinova.banking.identity.entity.BankingUser;

/**
 * Spring Security {@link UserDetails} adapter for {@link BankingUser}.
 *
 * Exposes additional banking-specific fields so that {@link TokenCustomizerConfig} can inject them
 * as JWT claims without making a second database call.
 */
public class BankingUserDetails implements UserDetails {

    private final BankingUser user;
    private final Set<GrantedAuthority> authorities;
    private final PasswordPolicyProperties passwordPolicy;
    private final Clock clock;

    public BankingUserDetails(BankingUser user) {
        this(user, Clock.systemDefaultZone(), new PasswordPolicyProperties());
    }

    public BankingUserDetails(BankingUser user, Clock clock) {
        this(user, clock, new PasswordPolicyProperties());
    }

    public BankingUserDetails(BankingUser user, Clock clock, PasswordPolicyProperties passwordPolicy) {
        this.user = user;
        this.clock = clock;
        this.passwordPolicy = passwordPolicy;
        this.authorities = EffectiveAuthoritiesResolver.resolveEffectivePermissions(user, clock).stream()
                .map(p -> new SimpleGrantedAuthority(p.getAuthority())).collect(Collectors.toUnmodifiableSet());
    }

    public UUID getUserId() {
        return user.getId();
    }

    public UserType getUserType() {
        return user.getUserType();
    }

    public String getBranchCode() {
        return user.getBranchCode();
    }

    public String getEmployeeId() {
        return user.getEmployeeId();
    }

    public UUID getCustomerPartyId() {
        return user.getCustomerPartyId();
    }

    public String getGlApprovalRole() {
        return user.getGlApprovalRole();
    }

    public boolean isMfaEnabled() {
        return user.isMfaEnabled();
    }

    public boolean isForcePasswordChange() {
        return user.isForcePasswordChange();
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public String getPassword() {
        return user.getPasswordHash();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        LocalDateTime expiresAt = user.getAccountExpiresAt();
        LocalDateTime now = LocalDateTime.now(clock);
        return expiresAt == null || expiresAt.isAfter(now);
    }

    @Override
    public boolean isAccountNonLocked() {
        if (user.isAccountLocked()) {
            return false;
        }
        LocalDateTime autoLockUntil = user.getFailedLoginLockedUntil();
        LocalDateTime now = LocalDateTime.now(clock);
        if (autoLockUntil != null && autoLockUntil.isAfter(now)) {
            return false;
        }
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        if (user.isForcePasswordChange()) {
            return true;
        }
        LocalDateTime now = LocalDateTime.now(clock);
        LocalDateTime effective = PasswordLifecycleEvaluator.effectivePasswordExpiresAt(user, passwordPolicy);
        return effective == null || effective.isAfter(now);
    }

    @Override
    public boolean isEnabled() {
        LocalDateTime now = LocalDateTime.now(clock);
        if (!user.isEnabled()) {
            return false;
        }
        if (user.getDisabledAt() != null) {
            return false;
        }
        if (user.getProvisioningStatus() != AccountProvisioningStatus.ACTIVE) {
            return false;
        }
        if (user.isEffectivelySuspended(now)) {
            return false;
        }
        return true;
    }
}
