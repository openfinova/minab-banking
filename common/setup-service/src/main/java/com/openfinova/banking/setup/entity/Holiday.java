package com.openfinova.banking.setup.entity;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.persistence.Version;

/**
 * Entity representing a holiday in the system.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "holidays", uniqueConstraints = @UniqueConstraint(name = "uk_holiday_date_country_region", columnNames = {
        "holiday_date", "country_code", "region_code" }), indexes = {
                @Index(name = "idx_holiday_country_year", columnList = "country_code, holiday_year"),
                @Index(name = "idx_holiday_date", columnList = "holiday_date"),
                @Index(name = "idx_holiday_bank", columnList = "is_bank_holiday") })
public class Holiday {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id", updatable = false, nullable = false)
    private UUID id;

    @Column(name = "holiday_date", nullable = false)
    private LocalDate date;

    @Column(name = "holiday_year", nullable = false)
    private Integer year;

    @Column(name = "country_code", nullable = false, length = 2)
    private String countryCode;

    @Column(name = "region_code", length = 10)
    private String regionCode;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "description", length = 500)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "holiday_type", nullable = false, length = 20)
    private HolidayType type;

    @Column(name = "is_bank_holiday", nullable = false)
    private Boolean bankHoliday;

    @Column(name = "is_observed_holiday", nullable = false)
    private Boolean observedHoliday;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    private String createdBy;

    @LastModifiedBy
    @Column(name = "updated_by", length = 100)
    private String updatedBy;

    @Version
    @Column(name = "version")
    private Long version;

    public enum HolidayType {
        PUBLIC,
        BANK,
        RELIGIOUS,
        REGIONAL,
        OBSERVANCE
    }

    public Holiday() {
    }

    public Holiday(LocalDate date, String countryCode, String regionCode, String name) {
        this.date = date;
        this.year = date.getYear();
        this.countryCode = countryCode;
        this.regionCode = regionCode;
        this.name = name;
        this.type = HolidayType.PUBLIC;
        this.bankHoliday = true;
        this.observedHoliday = false;
    }

    @PrePersist
    protected void onCreate() {
        if (date != null) {
            year = date.getYear();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        if (date != null) {
            year = date.getYear();
        }
    }

    // Getters and setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
        if (date != null) {
            this.year = date.getYear();
        }
    }

    public Integer getYear() {
        return year;
    }

    public void setYear(Integer year) {
        this.year = year;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegionCode() {
        return regionCode;
    }

    public void setRegionCode(String regionCode) {
        this.regionCode = regionCode;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public HolidayType getType() {
        return type;
    }

    public void setType(HolidayType type) {
        this.type = type;
    }

    public Boolean getBankHoliday() {
        return bankHoliday;
    }

    public void setBankHoliday(Boolean bankHoliday) {
        this.bankHoliday = bankHoliday;
    }

    public Boolean getObservedHoliday() {
        return observedHoliday;
    }

    public void setObservedHoliday(Boolean observedHoliday) {
        this.observedHoliday = observedHoliday;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    @Override
    public String toString() {
        return String.format(
                "Holiday{id=%s, date=%s, country=%s, region=%s, name='%s'}",
                id,
                date,
                countryCode,
                regionCode,
                name);
    }
}
