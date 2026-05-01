package com.openfinova.banking.setup.dto;

import java.time.LocalDate;

/**
 * Response DTO for holiday check operation.
 */
public class HolidayCheckResponse {
    private LocalDate date;
    private String countryCode;
    private String regionCode;
    private boolean isHoliday;
    private String holidayName;

    public HolidayCheckResponse() {
    }

    public HolidayCheckResponse(LocalDate date, String countryCode, String regionCode, boolean isHoliday) {
        this.date = date;
        this.countryCode = countryCode;
        this.regionCode = regionCode;
        this.isHoliday = isHoliday;
    }

    public HolidayCheckResponse(LocalDate date, String countryCode, String regionCode, boolean isHoliday,
            String holidayName) {
        this.date = date;
        this.countryCode = countryCode;
        this.regionCode = regionCode;
        this.isHoliday = isHoliday;
        this.holidayName = holidayName;
    }

    // Getters and Setters
    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
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

    public boolean isHoliday() {
        return isHoliday;
    }

    public void setHoliday(boolean holiday) {
        isHoliday = holiday;
    }

    public String getHolidayName() {
        return holidayName;
    }

    public void setHolidayName(String holidayName) {
        this.holidayName = holidayName;
    }
}
