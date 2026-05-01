package com.openfinova.banking.gl.dto;

import java.util.List;
import java.util.UUID;

/**
 * Result of hierarchy validation.
 */
public class AccountsHierarchyValidationResult {
    private boolean valid;
    private List<String> issues;
    private List<UUID> problematicAccounts;

    public AccountsHierarchyValidationResult() {
    }

    public AccountsHierarchyValidationResult(boolean valid, List<String> issues, List<UUID> problematicAccounts) {
        this.valid = valid;
        this.issues = issues;
        this.problematicAccounts = problematicAccounts;
    }

    public boolean isValid() {
        return valid;
    }

    public void setValid(boolean valid) {
        this.valid = valid;
    }

    public List<String> getIssues() {
        return issues;
    }

    public void setIssues(List<String> issues) {
        this.issues = issues;
    }

    public List<UUID> getProblematicAccounts() {
        return problematicAccounts;
    }

    public void setProblematicAccounts(List<UUID> problematicAccounts) {
        this.problematicAccounts = problematicAccounts;
    }
}
