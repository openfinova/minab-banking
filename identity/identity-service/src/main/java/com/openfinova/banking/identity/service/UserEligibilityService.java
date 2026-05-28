package com.openfinova.banking.identity.service;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.identity.api.model.UserSummary;
import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.entity.AccountProvisioningStatus;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.security.EffectiveAuthoritiesResolver;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Resolves user eligibility, active status, and effective permissions for cross-module identity checks.
 */
@Service
@Transactional(readOnly = true)
public class UserEligibilityService {

    private final UserRepository userRepository;
    private final DateTimeService dateTimeService;

    public UserEligibilityService(UserRepository userRepository, DateTimeService dateTimeService) {
        this.userRepository = userRepository;
        this.dateTimeService = dateTimeService;
    }

    public boolean isUserActive(UUID userId) {
        return userRepository.findById(userId).map(this::isActive).orElse(false);
    }

    public boolean isUsernameActive(String username) {
        return userRepository.findByUsername(username).map(this::isActive).orElse(false);
    }

    public boolean hasUserPermission(String username, String permission) {
        return userRepository.findByUsernameWithRoles(username).filter(this::isActive)
                .map(
                        user -> EffectiveAuthoritiesResolver.resolveEffectivePermissions(user, dateTimeService.clock())
                                .stream().anyMatch(p -> p.getAuthority().equals(permission)))
                .orElse(false);
    }

    public Optional<UUID> getUserIdByUsername(String username) {
        return userRepository.findByUsername(username).map(BankingUser::getId);
    }

    public Optional<String> getUsernameById(UUID userId) {
        return userRepository.findById(userId).map(BankingUser::getUsername);
    }

    public Optional<UserType> getUserType(String username) {
        return userRepository.findByUsername(username).map(BankingUser::getUserType);
    }

    public Set<String> getUserPermissions(String username) {
        return userRepository.findByUsernameWithRoles(username)
                .map(
                        user -> EffectiveAuthoritiesResolver.resolveEffectivePermissions(user, dateTimeService.clock())
                                .stream().map(p -> p.getAuthority()).collect(Collectors.toSet()))
                .orElse(Set.of());
    }

    public Optional<UUID> getUserIdByCustomerPartyId(UUID customerPartyId) {
        return userRepository.findByCustomerPartyId(customerPartyId).map(BankingUser::getId);
    }

    public Optional<UUID> getCustomerPartyIdByUserId(UUID userId) {
        return userRepository.findById(userId).map(BankingUser::getCustomerPartyId);
    }

    public boolean customerHasActiveLogin(UUID customerPartyId) {
        return userRepository.findByCustomerPartyId(customerPartyId).map(this::isActive).orElse(false);
    }

    public Map<String, UserSummary> resolveUsers(Set<String> usernames) {
        if (usernames == null || usernames.isEmpty()) {
            return Map.of();
        }
        List<BankingUser> users = userRepository.findByUsernameInWithRoles(usernames);
        Map<String, UserSummary> result = new HashMap<>();
        for (BankingUser user : users) {
            result.put(
                    user.getUsername(),
                    new UserSummary(user.getId(), user.getUsername(), user.getUserType(), isActive(user)));
        }
        return result;
    }

    /**
     * Returns whether the user must change their password before using banking APIs.
     */
    public boolean isForcePasswordChangeRequired(UUID userId) {
        return userRepository.findById(userId).map(BankingUser::isForcePasswordChange).orElse(false);
    }

    private boolean isActive(BankingUser user) {
        LocalDateTime now = dateTimeService.now();
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
        if (user.isAccountLocked()) {
            return false;
        }
        if (user.getAccountExpiresAt() != null && !user.getAccountExpiresAt().isAfter(now)) {
            return false;
        }
        LocalDateTime autoLock = user.getFailedLoginLockedUntil();
        return autoLock == null || !autoLock.isAfter(now);
    }
}
