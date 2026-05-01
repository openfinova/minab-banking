package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.AgingBracket;
import com.openfinova.banking.gl.api.entity.SuspenseReasonCode;
import com.openfinova.banking.gl.api.entity.SuspenseStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Filter criteria for searching suspense items.
 */
public class SuspenseItemFilterDTO {

    private List<SuspenseStatus> statuses;
    private List<SuspenseReasonCode> reasonCodes;
    private List<AgingBracket> agingBrackets;
    private String assignedTo;
    private String sourceSystem;
    private String currency;
    private LocalDate postingDateFrom;
    private LocalDate postingDateTo;
    private BigDecimal minAmount;
    private BigDecimal maxAmount;
    private Boolean requiresAMLReview;
    private Boolean isEscalated;
    private String searchText; // Search in description, external reference

    // Constructors

    public SuspenseItemFilterDTO() {
    }

    // Getters and Setters

    public List<SuspenseStatus> getStatuses() {
        return statuses;
    }

    public void setStatuses(List<SuspenseStatus> statuses) {
        this.statuses = statuses;
    }

    public List<SuspenseReasonCode> getReasonCodes() {
        return reasonCodes;
    }

    public void setReasonCodes(List<SuspenseReasonCode> reasonCodes) {
        this.reasonCodes = reasonCodes;
    }

    public List<AgingBracket> getAgingBrackets() {
        return agingBrackets;
    }

    public void setAgingBrackets(List<AgingBracket> agingBrackets) {
        this.agingBrackets = agingBrackets;
    }

    public String getAssignedTo() {
        return assignedTo;
    }

    public void setAssignedTo(String assignedTo) {
        this.assignedTo = assignedTo;
    }

    public String getSourceSystem() {
        return sourceSystem;
    }

    public void setSourceSystem(String sourceSystem) {
        this.sourceSystem = sourceSystem;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getPostingDateFrom() {
        return postingDateFrom;
    }

    public void setPostingDateFrom(LocalDate postingDateFrom) {
        this.postingDateFrom = postingDateFrom;
    }

    public LocalDate getPostingDateTo() {
        return postingDateTo;
    }

    public void setPostingDateTo(LocalDate postingDateTo) {
        this.postingDateTo = postingDateTo;
    }

    public BigDecimal getMinAmount() {
        return minAmount;
    }

    public void setMinAmount(BigDecimal minAmount) {
        this.minAmount = minAmount;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Boolean getRequiresAMLReview() {
        return requiresAMLReview;
    }

    public void setRequiresAMLReview(Boolean requiresAMLReview) {
        this.requiresAMLReview = requiresAMLReview;
    }

    public Boolean getIsEscalated() {
        return isEscalated;
    }

    public void setIsEscalated(Boolean isEscalated) {
        this.isEscalated = isEscalated;
    }

    public String getSearchText() {
        return searchText;
    }

    public void setSearchText(String searchText) {
        this.searchText = searchText;
    }

    @Override
    public String toString() {
        return "SuspenseItemFilterDTO{" + "statuses=" + statuses + ", reasonCodes=" + reasonCodes + ", currency='"
                + currency + '\'' + ", assignedTo='" + assignedTo + '\'' + '}';
    }
}
