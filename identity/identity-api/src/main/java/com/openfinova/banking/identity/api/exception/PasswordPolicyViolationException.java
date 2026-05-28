package com.openfinova.banking.identity.api.exception;

import java.util.List;

/**
 * Exception carrying one or more human-readable password policy violation messages.
 * Exposed via {@code identity-api} so banking-app can handle it without depending on identity-service internals.
 */
public class PasswordPolicyViolationException extends RuntimeException {

    private final List<String> violations;

    public PasswordPolicyViolationException(List<String> violations) {
        super(String.join(" ", violations));
        this.violations = List.copyOf(violations);
    }

    public List<String> getViolations() {
        return violations;
    }
}
