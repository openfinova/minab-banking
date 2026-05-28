package com.openfinova.banking.identity.service;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import com.openfinova.banking.identity.config.PasswordPolicyProperties;
import com.openfinova.banking.identity.api.exception.PasswordPolicyViolationException;

/**
 * Validates passwords against configurable complexity rules and prevents
 * reuse of recent passwords. Called from user-creation, admin-reset, and
 * self-service change-password flows.
 */
@Service
@EnableConfigurationProperties(PasswordPolicyProperties.class)
public class PasswordPolicyService {

    private final PasswordPolicyProperties props;
    private final PasswordEncoder passwordEncoder;

    public PasswordPolicyService(PasswordPolicyProperties props, PasswordEncoder passwordEncoder) {
        this.props = props;
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Validate password complexity. Throws {@link PasswordPolicyViolationException}
     * if any rule is violated, listing all violated rules at once.
     *
     * @param rawPassword the plaintext password to validate against the complexity rules
     * @throws PasswordPolicyViolationException if any complexity rule is violated
     */
    public void validate(String rawPassword) {
        List<String> violations = new ArrayList<>();

        if (rawPassword == null || rawPassword.length() < props.getMinLength()) {
            violations.add("Password must be at least " + props.getMinLength() + " characters.");
        }

        if (rawPassword != null && rawPassword.length() > props.getMaxLength()) {
            violations.add("Password must not exceed " + props.getMaxLength() + " characters.");
        }

        if (props.isRequireUppercase()
                && (rawPassword == null || !rawPassword.chars().anyMatch(Character::isUpperCase))) {
            violations.add("Password must contain at least one uppercase letter.");
        }

        if (props.isRequireLowercase()
                && (rawPassword == null || !rawPassword.chars().anyMatch(Character::isLowerCase))) {
            violations.add("Password must contain at least one lowercase letter.");
        }

        if (props.isRequireDigit() && (rawPassword == null || !rawPassword.chars().anyMatch(Character::isDigit))) {
            violations.add("Password must contain at least one digit.");
        }

        if (props.isRequireSpecialChar()
                && (rawPassword == null || rawPassword.chars().allMatch(ch -> Character.isLetterOrDigit(ch)))) {
            violations.add("Password must contain at least one special character.");
        }

        if (!violations.isEmpty()) {
            throw new PasswordPolicyViolationException(violations);
        }
    }

    /**
     * Check the candidate password against recent password hashes.
     * Throws if the password was recently used.
     *
     * The in-memory list is already capped by {@link #getHistoryCount()} when maintained by
     * user management. Matching runs <strong>newest-first</strong> so immediate reuse of the
     * current password fails on the first {@code matches} call in the common case.
     *
     * @param rawPassword the plaintext password to check against the history
     * @param pastHashes the list of recent password hashes to compare against
     * @throws PasswordPolicyViolationException if the password was used recently
     */
    public void checkHistory(String rawPassword, List<String> pastHashes) {
        if (pastHashes == null || pastHashes.isEmpty()) {
            return;
        }
        ListIterator<String> it = pastHashes.listIterator(pastHashes.size());
        while (it.hasPrevious()) {
            String hash = it.previous();
            if (hash != null && passwordEncoder.matches(rawPassword, hash)) {
                throw new PasswordPolicyViolationException(
                        List.of("Password was used recently. Choose a different password."));
            }
        }
    }

    /**
     * Maximum number of past hashes to retain.
     */
    public int getHistoryCount() {
        return props.getHistoryCount();
    }

    /**
     * Maximum password age in days before expiry.
     */
    public int getMaxAgeDays() {
        return props.getMaxAgeDays();
    }

}
