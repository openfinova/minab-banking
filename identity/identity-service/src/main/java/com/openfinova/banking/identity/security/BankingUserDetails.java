package com.openfinova.banking.identity.security;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Objects;
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
 *
 * Callers must supply a {@link Clock} from {@link com.openfinova.banking.setup.api.DateTimeService#clock()}
 * (typically {@code dateTimeService.clock()}) so account lock, expiry, and permission resolution align
 * with platform time. This type is not a Spring bean and cannot resolve {@code DateTimeService} itself.
 */
public class BankingUserDetails implements UserDetails {

    private final BankingUser user;
    private final Set<GrantedAuthority> authorities;
    private final PasswordPolicyProperties passwordPolicy;
    private final Clock clock;

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
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        return expiresAt == null || expiresAt.isAfter(now);
    }

    @Override
    public boolean isAccountNonLocked() {
        if (user.isAccountLocked()) {
            return false;
        }
        LocalDateTime autoLockUntil = user.getFailedLoginLockedUntil();
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
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
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
        LocalDateTime effective = PasswordLifecycleEvaluator.effectivePasswordExpiresAt(user, passwordPolicy);
        return effective == null || effective.isAfter(now);
    }

    @Override
    public boolean isEnabled() {
        LocalDateTime now = LocalDateTime.ofInstant(clock.instant(), clock.getZone());
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

    /**
     * Identity is the persistent user id so that two instances loaded from different requests
     * (form login vs. JSON-deserialized OAuth2 authorization row) compare equal. Spring
     * Authorization Server stores this object as the key in {@link
     * org.springframework.security.core.session.SessionRegistry SessionRegistry} at
     * {@code /oauth2/authorize}; when {@code /oauth2/token} regenerates the id_token from a fresh
     * deserialized instance, lookup must still match so the {@code sid} claim is populated and
     * RP-initiated logout can validate it.
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof BankingUserDetails other)) {
            return false;
        }
        return Objects.equals(getUserId(), other.getUserId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getUserId());
    }
}
