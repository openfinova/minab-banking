package com.openfinova.banking.loan.dto;

import java.util.ArrayList;
import java.util.List;

/**
 * Summary of a bulk payment import run.
 */
public class BulkPaymentImportResult {
    private int totalRecords;
    private int successfulRecords;
    private int failedRecords;
    private List<String> errors = new ArrayList<>();

    public int getTotalRecords() {
        return totalRecords;
    }

    public void setTotalRecords(int totalRecords) {
        this.totalRecords = totalRecords;
    }

    public int getSuccessfulRecords() {
        return successfulRecords;
    }

    public void setSuccessfulRecords(int successfulRecords) {
        this.successfulRecords = successfulRecords;
    }

    public int getFailedRecords() {
        return failedRecords;
    }

    public void setFailedRecords(int failedRecords) {
        this.failedRecords = failedRecords;
    }

    public List<String> getErrors() {
        return errors;
    }

    public void setErrors(List<String> errors) {
        this.errors = errors;
    }
}
