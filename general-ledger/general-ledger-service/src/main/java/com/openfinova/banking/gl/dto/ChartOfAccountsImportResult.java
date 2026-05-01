package com.openfinova.banking.gl.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Import result.
 */
public class ChartOfAccountsImportResult {
    private int totalRecords;
    private int successfulImports;
    private int failedImports;
    private List<String> errors;
    private LocalDateTime importedAt;

    public ChartOfAccountsImportResult() {
    }

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getSuccessfulImports() {
        return successfulImports;
    }

    public void setSuccessfulImports(int successfulImports) {
        this.successfulImports = successfulImports;
    }

    public int getFailedImports() {
        return failedImports;
    }

    public void setFailedImports(int failedImports) {
        this.failedImports = failedImports;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }

    public LocalDateTime getImportedAt() {
        return importedAt;
    }

    public void setImportedAt(LocalDateTime importedAt) {
        this.importedAt = importedAt;
    }
}
