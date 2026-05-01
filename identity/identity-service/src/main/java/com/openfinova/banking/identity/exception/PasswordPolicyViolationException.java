package com.openfinova.banking.identity.exception;

import java.util.List;

/**
 * Exception carrying one or more human-readable policy violation messages.
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
