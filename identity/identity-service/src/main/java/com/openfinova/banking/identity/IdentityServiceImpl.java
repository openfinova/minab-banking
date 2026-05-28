package com.openfinova.banking.identity;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.identity.api.IdentityService;
import com.openfinova.banking.identity.api.model.UserSummary;
import com.openfinova.banking.identity.api.model.UserType;
import com.openfinova.banking.identity.service.UserEligibilityService;

@Service

@Transactional(readOnly = true)

public class IdentityServiceImpl implements IdentityService {

    private final UserEligibilityService userEligibilityService;

    public IdentityServiceImpl(UserEligibilityService userEligibilityService) {
        this.userEligibilityService = userEligibilityService;
    }

    @Override
    public boolean isUserActive(UUID userId) {
        return userEligibilityService.isUserActive(userId);
    }

    @Override
    public boolean isUsernameActive(String username) {
        return userEligibilityService.isUsernameActive(username);
    }

    @Override
    public boolean hasUserPermission(String username, String permission) {
        return userEligibilityService.hasUserPermission(username, permission);
    }

    @Override
    public Optional<UUID> getUserIdByUsername(String username) {
        return userEligibilityService.getUserIdByUsername(username);
    }

    @Override
    public Optional<String> getUsernameById(UUID userId) {
        return userEligibilityService.getUsernameById(userId);
    }

    @Override
    public Optional<UserType> getUserType(String username) {
        return userEligibilityService.getUserType(username);
    }

    @Override
    public Set<String> getUserPermissions(String username) {
        return userEligibilityService.getUserPermissions(username);
    }

    @Override
    public Optional<UUID> getUserIdByCustomerPartyId(UUID customerPartyId) {
        return userEligibilityService.getUserIdByCustomerPartyId(customerPartyId);
    }

    @Override
    public Optional<UUID> getCustomerPartyIdByUserId(UUID userId) {
        return userEligibilityService.getCustomerPartyIdByUserId(userId);
    }

    @Override
    public boolean customerHasActiveLogin(UUID customerPartyId) {
        return userEligibilityService.customerHasActiveLogin(customerPartyId);
    }

    @Override
    public Map<String, UserSummary> resolveUsers(Set<String> usernames) {
        return userEligibilityService.resolveUsers(usernames);
    }

    @Override
    public boolean isForcePasswordChangeRequired(UUID userId) {
        return userEligibilityService.isForcePasswordChangeRequired(userId);
    }

}
