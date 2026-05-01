package com.openfinova.banking.identity.validation;

/**
 * Shared Bean Validation patterns for the identity module.
 */
public final class IdentityValidation {

    private IdentityValidation() {
    }

    /**
     * Optional branch / cost-centre code: empty, or 1–20 chars starting with a letter or digit,
     * then letters, digits, dots, underscores, or hyphens.
     */
    public static final String BRANCH_CODE_PATTERN = "^([A-Za-z0-9][A-Za-z0-9._-]{0,19})?$";

    public static final String BRANCH_CODE_MESSAGE = "Branch code must be empty or 1-20 characters: start with a letter or digit; "
            + "only letters, digits, '.', '_', '-' allowed";
}
