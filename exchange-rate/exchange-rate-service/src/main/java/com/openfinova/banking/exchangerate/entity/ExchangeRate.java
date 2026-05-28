package com.openfinova.banking.exchangerate.entity;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.exchangerate.api.entity.RateType;
import com.openfinova.banking.exchangerate.api.entity.TradeDirection;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * Entity representing an exchange rate between two currencies for a specific
 * date and type.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "exchange_rates", indexes = {
        @Index(name = "idx_exchange_rates_lookup", columnList = "source_currency, target_currency, rate_date, rate_type"),
        @Index(name = "idx_exchange_rates_date", columnList = "rate_date") }, uniqueConstraints = {
                @UniqueConstraint(name = "uk_exchange_rate", columnNames = { "source_currency", "target_currency",
                        "rate_date", "rate_type" }) })
public class ExchangeRate {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "source_currency", nullable = false, length = 3)
    @NotBlank(message = "Source currency is required")
    @ValidCurrency
    private String sourceCurrency;

    @Column(name = "target_currency", nullable = false, length = 3)
    @NotBlank(message = "Target currency is required")
    @ValidCurrency
    private String targetCurrency;

    /**
     * Mid-market rate. Always required.
     */
    @Column(nullable = false, precision = 19, scale = 8)
    @NotNull(message = "Rate is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Rate must be positive")
    private BigDecimal rate;

    /**
     * Bank's buying rate (bid) — below mid. Null when spread is not applicable
     * (e.g. EOD, AVG_MONTH).
     */
    @Column(name = "bid_rate", precision = 19, scale = 8)
    private BigDecimal bidRate;

    /**
     * Bank's selling rate (ask) — above mid. Null when spread is not
     * applicable.
     */
    @Column(name = "ask_rate", precision = 19, scale = 8)
    private BigDecimal askRate;

    @Column(name = "rate_date", nullable = false)
    @NotNull(message = "Rate date is required")
    private LocalDate rateDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "rate_type", nullable = false, length = 20)
    @NotNull(message = "Rate type is required")
    private RateType rateType;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version", nullable = false)
    private long version;

    // Constructors
    public ExchangeRate() {
    }

    public ExchangeRate(String sourceCurrency, String targetCurrency, BigDecimal rate, LocalDate rateDate,
            RateType rateType) {
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.rate = rate;
        this.rateDate = rateDate;
        this.rateType = rateType;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getSourceCurrency() {
        return sourceCurrency;
    }

    public void setSourceCurrency(String sourceCurrency) {
        this.sourceCurrency = sourceCurrency;
    }

    public String getTargetCurrency() {
        return targetCurrency;
    }

    public void setTargetCurrency(String targetCurrency) {
        this.targetCurrency = targetCurrency;
    }

    public BigDecimal getRate() {
        return rate;
    }

    public void setRate(BigDecimal rate) {
        this.rate = rate;
    }

    public BigDecimal getBidRate() {
        return bidRate;
    }

    public void setBidRate(BigDecimal bidRate) {
        this.bidRate = bidRate;
    }

    public BigDecimal getAskRate() {
        return askRate;
    }

    public void setAskRate(BigDecimal askRate) {
        this.askRate = askRate;
    }

    /**
     * Returns the rate appropriate for the given trade direction. Falls back to
     * the mid-rate if the directional rate has not been populated.
     *
     * @param direction the trade direction from the bank's perspective
     * @return bid rate for BUY, ask rate for SELL, mid rate for MID or if
     * directional rate is absent
     */
    public BigDecimal getRateForDirection(TradeDirection direction) {
        return switch (direction) {
            case BUY -> bidRate != null ? bidRate : rate;
            case SELL -> askRate != null ? askRate : rate;
            case MID -> rate;
        };
    }

    public LocalDate getRateDate() {
        return rateDate;
    }

    public void setRateDate(LocalDate rateDate) {
        this.rateDate = rateDate;
    }

    public RateType getRateType() {
        return rateType;
    }

    public void setRateType(RateType rateType) {
        this.rateType = rateType;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public long getVersion() {
        return version;
    }

    public void setVersion(long version) {
        this.version = version;
    }
}
