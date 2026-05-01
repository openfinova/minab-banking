package com.openfinova.banking.setup.api.dto;

import java.time.LocalDate;
import java.util.UUID;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Data Transfer Object for Holiday information.
 */
@Schema(description = "Holiday information including date, country, and region")
public class HolidayDTO {

    @Schema(description = "Unique holiday identifier", example = "123e4567-e89b-12d3-a456-426614174000")
    private UUID id;

    @NotNull(message = "Holiday date is required")
    @Schema(description = "Date of the holiday", example = "2026-12-25")
    private LocalDate date;

    @Schema(description = "Year of the holiday", example = "2026")
    private Integer year;

    @NotBlank(message = "Country code is required")
    @Size(min = 2, max = 2, message = "Country code must be 2 characters")
    @Schema(description = "ISO 3166-1 alpha-2 country code", example = "US")
    private String countryCode;

    @Size(max = 10, message = "Region code must not exceed 10 characters")
    @Schema(description = "Optional region/state code", example = "NY")
    private String regionCode;

    @NotBlank(message = "Holiday name is required")
    @Size(max = 100, message = "Holiday name must not exceed 100 characters")
    @Schema(description = "Name of the holiday", example = "Christmas Day")
    private String name;

    @Size(max = 500, message = "Description must not exceed 500 characters")
    @Schema(description = "Detailed description of the holiday", example = "Federal holiday celebrating Christmas")
    private String description;

    @NotNull(message = "Holiday type is required")
    @Schema(description = "Type of holiday", example = "NATIONAL")
    private String type;

    @Schema(description = "Whether this is a bank holiday", example = "true")
    private Boolean bankHoliday;

    @Schema(description = "Whether this is an observed holiday (e.g., Monday for Sunday holiday)", example = "false")
    private Boolean observedHoliday;

    // Constructors
    public HolidayDTO() {
    }

    // Getters and Setters
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

    public String getType() {
        return type;
    }

    public void setType(String type) {
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
}
