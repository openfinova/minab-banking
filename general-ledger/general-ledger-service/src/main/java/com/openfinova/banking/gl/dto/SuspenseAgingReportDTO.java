package com.openfinova.banking.gl.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Complete aging report for suspense items.
 * Groups items by aging bracket with totals.
 */
public class SuspenseAgingReportDTO {

    private LocalDate reportDate;
    private String currency;
    private List<SuspenseAgingBucketDTO> agingBuckets = new ArrayList<>();
    private Integer totalItemCount;
    private BigDecimal totalAmount;
    private Integer itemsRequiringAMLReview;
    private Integer escalatedItemCount;
    private BigDecimal escalatedTotalAmount;

    // Constructors

    public SuspenseAgingReportDTO() {
        this.reportDate = LocalDate.now();
    }

    public SuspenseAgingReportDTO(String currency) {
        this();
        this.currency = currency;
    }

    // Getters and Setters

    public LocalDate getReportDate() {
        return reportDate;
    }

    public void setReportDate(LocalDate reportDate) {
        this.reportDate = reportDate;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public List<SuspenseAgingBucketDTO> getAgingBuckets() {
        return agingBuckets;
    }

    public void setAgingBuckets(List<SuspenseAgingBucketDTO> agingBuckets) {
        this.agingBuckets = agingBuckets;
    }

    public Integer getTotalItemCount() {
        return totalItemCount;
    }

    public void setTotalItemCount(Integer totalItemCount) {
        this.totalItemCount = totalItemCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public Integer getItemsRequiringAMLReview() {
        return itemsRequiringAMLReview;
    }

    public void setItemsRequiringAMLReview(Integer itemsRequiringAMLReview) {
        this.itemsRequiringAMLReview = itemsRequiringAMLReview;
    }

    public Integer getEscalatedItemCount() {
        return escalatedItemCount;
    }

    public void setEscalatedItemCount(Integer escalatedItemCount) {
        this.escalatedItemCount = escalatedItemCount;
    }

    public BigDecimal getEscalatedTotalAmount() {
        return escalatedTotalAmount;
    }

    public void setEscalatedTotalAmount(BigDecimal escalatedTotalAmount) {
        this.escalatedTotalAmount = escalatedTotalAmount;
    }

    @Override
    public String toString() {
        return "SuspenseAgingReportDTO{" + "reportDate=" + reportDate + ", currency='" + currency + '\''
                + ", totalItemCount=" + totalItemCount + ", totalAmount=" + totalAmount + ", escalatedItemCount="
                + escalatedItemCount + '}';
    }
}
