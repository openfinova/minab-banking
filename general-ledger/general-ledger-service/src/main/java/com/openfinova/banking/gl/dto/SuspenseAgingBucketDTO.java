package com.openfinova.banking.gl.dto;

import com.openfinova.banking.gl.api.entity.AgingBracket;

import java.math.BigDecimal;

/**
 * Represents a single aging bucket in the aging report.
 */
public class SuspenseAgingBucketDTO {

    private AgingBracket agingBracket;
    private String description;
    private Integer itemCount;
    private BigDecimal totalAmount;
    private String currency;

    // Constructors

    public SuspenseAgingBucketDTO() {
    }

    public SuspenseAgingBucketDTO(AgingBracket agingBracket, Integer itemCount, BigDecimal totalAmount,
            String currency) {
        this.agingBracket = agingBracket;
        this.description = agingBracket.getDescription();
        this.itemCount = itemCount;
        this.totalAmount = totalAmount;
        this.currency = currency;
    }

    // Getters and Setters

    public AgingBracket getAgingBracket() {
        return agingBracket;
    }

    public void setAgingBracket(AgingBracket agingBracket) {
        this.agingBracket = agingBracket;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getItemCount() {
        return itemCount;
    }

    public void setItemCount(Integer itemCount) {
        this.itemCount = itemCount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    @Override
    public String toString() {
        return "SuspenseAgingBucketDTO{" + "agingBracket=" + agingBracket + ", itemCount=" + itemCount
                + ", totalAmount=" + totalAmount + ", currency='" + currency + '\'' + '}';
    }
}
