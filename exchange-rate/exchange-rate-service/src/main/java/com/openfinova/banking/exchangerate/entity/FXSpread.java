package com.openfinova.banking.exchangerate.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import com.openfinova.banking.common.lib.validation.ValidCurrency;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Represents a Foreign Exchange (FX) spread configuration between a specific pair of currencies.
 *
 * A spread is an additional percentage or margin applied to the base exchange rate
 * during currency conversion. It acts as the markup or fee charged by the banking system
 * for facilitating the foreign exchange transaction. For example, a spread rate of
 * 0.0025 represents 25 basis points or a 0.25% markup.
 *
 * Usage considerations:
 * - Directional: The spread is specific to the direction of conversion, defined
 *   from sourceCurrency to targetCurrency.
 * - Precision: spreadRate is stored as a decimal value (e.g., 0.0025
 *   for 0.25%) and must be between 0 and 1.
 * - Concurrency: Uses optimistic locking (via version) to handle
 *   concurrent modifications safely.
 * - Auditing: Includes creation and update timestamps, as well as the
 *   user who created the configuration.
 */
@Entity
@Table(name = "fx_spreads", indexes = {
        @Index(name = "idx_fx_spreads_pair", columnList = "source_currency, target_currency") })
public class FXSpread {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    private Long version;

    @Column(name = "source_currency", nullable = false, length = 3)
    @NotNull
    @ValidCurrency
    private String sourceCurrency;

    @Column(name = "target_currency", nullable = false, length = 3)
    @NotNull
    @ValidCurrency
    private String targetCurrency;

    /**
     * Spread rate applied to amount (e.g. 0.0025 = 0.25% = 25 bps).
     */
    @Column(name = "spread_rate", nullable = false, precision = 19, scale = 6)
    @NotNull
    @DecimalMin("0")
    @DecimalMax("1")
    private BigDecimal spreadRate;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @Column(name = "created_by", nullable = false, length = 100)
    @NotNull
    @Size(max = 100)
    private String createdBy;

    public FXSpread() {
    }

    public FXSpread(String sourceCurrency, String targetCurrency, BigDecimal spreadRate, String createdBy) {
        this.sourceCurrency = sourceCurrency;
        this.targetCurrency = targetCurrency;
        this.spreadRate = spreadRate;
        this.createdBy = createdBy;
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
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

    public BigDecimal getSpreadRate() {
        return spreadRate;
    }

    public void setSpreadRate(BigDecimal spreadRate) {
        this.spreadRate = spreadRate;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
}
