package com.openfinova.banking.gl.dto;

import java.util.List;

/**
 * Chart of accounts import data.
 */
public class ChartOfAccountsImport {
    private List<AccountExportData> accounts;
    private boolean overwriteExisting;
    private String importFormat;

    public ChartOfAccountsImport() {
    }

    public List<AccountExportData> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountExportData> accounts) {
        this.accounts = accounts;
    }

    public boolean isOverwriteExisting() {
        return overwriteExisting;
    }

    public void setOverwriteExisting(boolean overwriteExisting) {
        this.overwriteExisting = overwriteExisting;
    }

    public String getImportFormat() {
        return importFormat;
    }

    public void setImportFormat(String importFormat) {
        this.importFormat = importFormat;
    }
}
