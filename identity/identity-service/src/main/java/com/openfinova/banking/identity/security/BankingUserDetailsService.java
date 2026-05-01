package com.openfinova.banking.identity.security;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.identity.config.PasswordPolicyProperties;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Spring Security UserDetailsService implementation that loads user accounts from the banking
 * identity database and wraps them in BankingUserDetails for use during authentication.
 *
 * This service is invoked by the Spring Security authentication machinery whenever a username
 * and password need to be verified — primarily during the OAuth2 authorization code flow's
 * form login step and during any direct password-grant authentication.
 *
 * The returned BankingUserDetails object carries the user's encoded password, granted authorities
 * (derived from assigned roles and their permissions), and account status flags such as whether
 * the account is locked, expired, or requires a password change. Spring Security evaluates these
 * flags as part of the standard authentication checks that follow this service's call.
 *
 * Security note: when a username is not found, a UsernameNotFoundException is thrown with a
 * generic message ("Bad credentials") and the actual username is never included. This prevents
 * user enumeration attacks where an attacker could distinguish between "username not found" and
 * "wrong password" by inspecting error messages.
 */
@Service
public class BankingUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(BankingUserDetailsService.class);

    private final UserRepository userRepository;
    private final DateTimeService dateTimeService;
    private final PasswordPolicyProperties passwordPolicy;

    public BankingUserDetailsService(UserRepository userRepository, DateTimeService dateTimeService,
            PasswordPolicyProperties passwordPolicy) {
        this.userRepository = userRepository;
        this.dateTimeService = dateTimeService;
        this.passwordPolicy = passwordPolicy;
    }

    /**
     * Loads the user account matching the given username from the database.
     *
     * The query fetches the user together with their assigned roles in a single read-only
     * transaction to avoid lazy-loading issues when Spring Security subsequently accesses
     * the granted authorities. The result is wrapped in a BankingUserDetails instance which
     * evaluates password expiry and account lock status at construction time using the
     * configured password policy and the application clock from DateTimeService.
     *
     * If no user with the given username exists, a warning is logged without including the
     * username in the log message or the exception, and a UsernameNotFoundException is thrown.
     * Spring Security will translate this to a generic authentication failure response so the
     * caller cannot distinguish a missing account from a wrong password.
     *
     * @param username the username supplied by the authentication request; must not be null
     * @return a fully populated BankingUserDetails instance for the matching user
     * @throws UsernameNotFoundException if no user with the given username exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        return userRepository.findByUsernameWithRoles(username)
                .map(user -> new BankingUserDetails(user, dateTimeService.clock(), passwordPolicy)).orElseThrow(() -> {
                    log.warn("Authentication attempt for unknown username");
                    return new UsernameNotFoundException("Bad credentials");
                });
    }
}
