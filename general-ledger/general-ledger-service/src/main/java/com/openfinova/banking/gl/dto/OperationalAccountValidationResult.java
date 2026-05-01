package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;

import java.util.List;

/**
 * Result of operational account validation.
 */
public class OperationalAccountValidationResult {
    private final boolean isValid;
    private final List<OperationalGLAccountType> missingTypes;

    public OperationalAccountValidationResult(boolean isValid, List<OperationalGLAccountType> missingTypes) {
        this.isValid = isValid;
        this.missingTypes = missingTypes;
    }

    public boolean isValid() {
        return isValid;
    }

    public List<OperationalGLAccountType> getMissingTypes() {
        return missingTypes;
    }

    @Override
    public String toString() {
        if (isValid) {
            return "All operational accounts configured";
        }
        return "Missing operational accounts: " + missingTypes;
    }
}
