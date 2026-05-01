package com.openfinova.banking.setup.controller;

import java.time.LocalDate;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.setup.api.HolidayService;
import com.openfinova.banking.setup.api.dto.HolidayDTO;
import com.openfinova.banking.setup.dto.HolidayCheckResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for holiday management operations.
 *
 * Exposes endpoints for:
 * - Holiday lookups (customer-facing)
 * - Holiday creation (administrative)
 * - Holiday deletion (administrative)
 */
@RestController
@RequestMapping("/api/v1/holidays")
@Tag(name = "Holidays", description = "APIs for holiday management")
public class HolidayController {

    private static final Logger log = LoggerFactory.getLogger(HolidayController.class);

    private final HolidayService holidayService;

    public HolidayController(HolidayService holidayService) {
        this.holidayService = holidayService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('holiday:read')")
    @Operation(summary = "Get holidays for a specific year", description = "Retrieves all holidays for a specified country, region, and year")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Holidays retrieved successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "500", description = "Error retrieving holidays") })
    public ResponseEntity<List<HolidayDTO>> getHolidays(
            @Parameter(description = "Year to retrieve holidays for", required = true) @RequestParam Integer year,
            @Parameter(description = "ISO 3166-1 alpha-2 country code (e.g., US, GB, CA)", required = true) @RequestParam String countryCode,
            @Parameter(description = "Optional region code (e.g., NY for New York)") @RequestParam(required = false) String regionCode) {

        log.info("Fetching holidays for year: {}, country: {}, region: {}", year, countryCode, regionCode);

        List<HolidayDTO> holidays = holidayService.getHolidays(year, countryCode, regionCode);

        log.info("Retrieved {} holidays for {}/{}", holidays.size(), countryCode, regionCode);

        return ResponseEntity.ok(holidays);
    }

    @GetMapping("/check")
    @PreAuthorize("hasAuthority('holiday:read')")
    @Operation(summary = "Check if a date is a holiday", description = "Determines if a specific date is a holiday for the given country and region")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Holiday check completed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "500", description = "Error checking holiday") })
    public ResponseEntity<HolidayCheckResponse> isHoliday(
            @Parameter(description = "Date to check (format: yyyy-MM-dd)", required = true) @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "ISO 3166-1 alpha-2 country code (e.g., US, GB, CA)", required = true) @RequestParam String countryCode,
            @Parameter(description = "Optional region code (e.g., NY for New York)") @RequestParam(required = false) String regionCode) {

        log.info("Checking if {} is a holiday for {}/{}", date, countryCode, regionCode);

        boolean isHoliday = holidayService.isHoliday(date, countryCode, regionCode);

        HolidayCheckResponse response = new HolidayCheckResponse(date, countryCode, regionCode, isHoliday);

        // If it's a holiday, populate the holiday name
        if (isHoliday) {
            holidayService.getHoliday(date, countryCode, regionCode)
                    .ifPresent(holiday -> response.setHolidayName(holiday.getName()));
        }

        return ResponseEntity.ok(response);
    }

    @GetMapping("/{countryCode}/{date}")
    @PreAuthorize("hasAuthority('holiday:read')")
    @Operation(summary = "Get holiday details by date", description = "Retrieves holiday details for a specific date and country")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Holiday details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Holiday not found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters") })
    public ResponseEntity<HolidayDTO> getHolidayByDate(
            @Parameter(description = "ISO 3166-1 alpha-2 country code", required = true) @PathVariable String countryCode,
            @Parameter(description = "Date to look up (format: yyyy-MM-dd)", required = true) @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Optional region code") @RequestParam(required = false) String regionCode) {

        log.info("Fetching holiday details for {}: {}", countryCode, date);

        return holidayService.getHoliday(date, countryCode, regionCode).map(holiday -> {
            log.info("Holiday details retrieved: {}", holiday.getName());
            return ResponseEntity.ok(holiday);
        }).orElseGet(() -> {
            log.info("Holiday not found for date: {}, country: {}, region: {}", date, countryCode, regionCode);
            return ResponseEntity.notFound().build();
        });
    }

    @PostMapping
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Create a new holiday", description = "Creates a new holiday entry for a specific country and region")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Holiday created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid holiday data"),
            @ApiResponse(responseCode = "409", description = "Holiday already exists"),
            @ApiResponse(responseCode = "500", description = "Error creating holiday") })
    public ResponseEntity<HolidayDTO> createHoliday(@Valid @RequestBody HolidayDTO holidayDTO) {
        log.info(
                "Creating new holiday: {} for {}/{}",
                holidayDTO.getName(),
                holidayDTO.getCountryCode(),
                holidayDTO.getRegionCode());

        holidayService.addHoliday(holidayDTO);

        log.info("Holiday created successfully: {} on {}", holidayDTO.getName(), holidayDTO.getDate());

        return ResponseEntity.status(HttpStatus.CREATED).body(holidayDTO);
    }

    @DeleteMapping("/{countryCode}/{date}")
    @PreAuthorize("hasAuthority('admin:config:write')")
    @Operation(summary = "Delete a holiday", description = "Deletes a holiday entry for a specific date, country, and region")
    @ApiResponses(value = { @ApiResponse(responseCode = "204", description = "Holiday deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Holiday not found"),
            @ApiResponse(responseCode = "400", description = "Invalid parameters"),
            @ApiResponse(responseCode = "500", description = "Error deleting holiday") })
    public ResponseEntity<Void> deleteHoliday(
            @Parameter(description = "ISO 3166-1 alpha-2 country code", required = true) @PathVariable String countryCode,
            @Parameter(description = "Date of the holiday to delete (format: yyyy-MM-dd)", required = true) @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Parameter(description = "Optional region code") @RequestParam(required = false) String regionCode) {

        log.info("Deleting holiday for date: {}, country: {}, region: {}", date, countryCode, regionCode);

        boolean removed = holidayService.removeHoliday(date, countryCode, regionCode);

        if (removed) {
            log.info("Holiday deleted successfully for date: {}", date);
            return ResponseEntity.noContent().build();
        } else {
            log.info("Holiday not found for deletion: {}, {}, {}", date, countryCode, regionCode);
            return ResponseEntity.notFound().build();
        }
    }
}
