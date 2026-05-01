package com.openfinova.banking.gl.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Chart of accounts export data.
 */
public class ChartOfAccountsExport {
    private LocalDateTime exportedAt;
    private List<AccountExportData> accounts;
    private String exportFormat;

    public ChartOfAccountsExport() {
    }

    public LocalDateTime getExportedAt() {
        return exportedAt;
    }

    public void setExportedAt(LocalDateTime exportedAt) {
        this.exportedAt = exportedAt;
    }

    public List<AccountExportData> getAccounts() {
        return accounts;
    }

    public void setAccounts(List<AccountExportData> accounts) {
        this.accounts = accounts;
    }

    public String getExportFormat() {
        return exportFormat;
    }

    public void setExportFormat(String exportFormat) {
        this.exportFormat = exportFormat;
    }
}
