package com.openfinova.banking.gl.controller;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.CreateFiscalPeriodRequest;
import com.openfinova.banking.gl.api.dto.FiscalPeriodResponse;
import com.openfinova.banking.gl.api.entity.FiscalPeriodStatus;
import com.openfinova.banking.gl.entity.FiscalPeriod;
import com.openfinova.banking.gl.mapper.FiscalPeriodMapper;
import com.openfinova.banking.gl.service.FiscalPeriodService;
import com.openfinova.banking.gl.service.FiscalPeriodWorkflowService;
import com.openfinova.banking.identity.api.principal.CallerContextResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gl/fiscal-periods")
@Tag(name = "Fiscal Period Management", description = "APIs for managing accounting periods")
public class FiscalPeriodController {

    private static final Logger log = LoggerFactory.getLogger(FiscalPeriodController.class);

    private final FiscalPeriodService fiscalPeriodService;
    private final FiscalPeriodWorkflowService fiscalPeriodWorkflowService;
    private final FiscalPeriodMapper fiscalPeriodMapper;

    public FiscalPeriodController(FiscalPeriodService fiscalPeriodService,
            FiscalPeriodWorkflowService fiscalPeriodWorkflowService, FiscalPeriodMapper fiscalPeriodMapper) {
        this.fiscalPeriodService = fiscalPeriodService;
        this.fiscalPeriodWorkflowService = fiscalPeriodWorkflowService;
        this.fiscalPeriodMapper = fiscalPeriodMapper;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get all fiscal periods", description = "Retrieves all fiscal periods ordered by start date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Periods retrieved successfully") })
    public ResponseEntity<List<FiscalPeriodResponse>> getAllFiscalPeriods() {
        log.info("Fetching all fiscal periods");

        List<FiscalPeriod> periods = fiscalPeriodService.getAllFiscalPeriods();
        List<FiscalPeriodResponse> response = periods.stream().map(fiscalPeriodMapper::toResponse).toList();

        log.info("Retrieved {} fiscal periods", response.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get periods by status", description = "Retrieves fiscal periods filtered by status")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Periods retrieved successfully") })
    public ResponseEntity<List<FiscalPeriodResponse>> getFiscalPeriodsByStatus(
            @Parameter(description = "Period status", required = true) @PathVariable FiscalPeriodStatus status) {

        log.info("Fetching fiscal periods with status: {}", status);

        List<FiscalPeriod> periods = fiscalPeriodService.getFiscalPeriodsByStatus(status);
        List<FiscalPeriodResponse> response = periods.stream().map(fiscalPeriodMapper::toResponse).toList();

        log.info("Retrieved {} fiscal periods with status: {}", response.size(), status);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/year/{fiscalYear}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get periods by fiscal year", description = "Retrieves all fiscal periods for a given fiscal year, ordered by period number. "
            + "Use this for regulatory reporting and year-end processing instead of filtering by date.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Periods retrieved successfully") })
    public ResponseEntity<List<FiscalPeriodResponse>> getFiscalPeriodsByYear(
            @Parameter(description = "Fiscal year (e.g. 2024)", required = true, example = "2024") @PathVariable int fiscalYear) {

        log.info("Fetching fiscal periods for year: {}", fiscalYear);

        List<FiscalPeriodResponse> response = fiscalPeriodService.getFiscalPeriodsByYear(fiscalYear).stream()
                .map(fiscalPeriodMapper::toResponse).toList();

        log.info("Retrieved {} fiscal periods for year {}", response.size(), fiscalYear);

        return ResponseEntity.ok(response);
    }

    @GetMapping("/year/{fiscalYear}/period/{periodNumber}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get period by fiscal year and period number", description = "Retrieves a specific fiscal period by its natural key (fiscal year + period number). "
            + "Period numbers: 1-12 for monthly, 1-4 for quarterly, 13 for year-end adjustments.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Period found", content = @Content(schema = @Schema(implementation = FiscalPeriodResponse.class))),
            @ApiResponse(responseCode = "404", description = "No period found for the given year and number") })
    public ResponseEntity<FiscalPeriodResponse> getFiscalPeriodByYearAndNumber(
            @Parameter(description = "Fiscal year (e.g. 2024)", required = true, example = "2024") @PathVariable int fiscalYear,
            @Parameter(description = "Period number within the fiscal year (1-13)", required = true, example = "3") @PathVariable int periodNumber) {

        log.info("Fetching fiscal period for year: {}, period: {}", fiscalYear, periodNumber);

        return fiscalPeriodService.getFiscalPeriod(fiscalYear, periodNumber).map(period -> {
            log.info("Found fiscal period: {}", period.getName());
            return ResponseEntity.ok(fiscalPeriodMapper.toResponse(period));
        }).orElseGet(() -> {
            log.warn("No fiscal period found for year: {}, period: {}", fiscalYear, periodNumber);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get active period for date", description = "Retrieves the active fiscal period for a specific date")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active period found", content = @Content(schema = @Schema(implementation = FiscalPeriodResponse.class))),
            @ApiResponse(responseCode = "404", description = "No active period found for date") })
    public ResponseEntity<FiscalPeriodResponse> getActivePeriod(
            @Parameter(description = "Reference date", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Fetching active fiscal period for date: {}", date);

        return fiscalPeriodService.findActivePeriod(date).map(period -> {
            log.info("Found active period: {}", period.getName());
            return ResponseEntity.ok(fiscalPeriodMapper.toResponse(period));
        }).orElseGet(() -> {
            log.warn("No active fiscal period found for date: {}", date);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/for-date")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get period for date", description = "Retrieves the fiscal period that contains a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Period found"),
            @ApiResponse(responseCode = "404", description = "No period found for date") })
    public ResponseEntity<FiscalPeriodResponse> getFiscalPeriodForDate(
            @Parameter(description = "Date to find period for", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {

        log.info("Fetching fiscal period for date: {}", date);

        return fiscalPeriodService.getFiscalPeriodForDate(date).map(period -> {
            log.info("Found fiscal period: {}", period.getName());
            return ResponseEntity.ok(fiscalPeriodMapper.toResponse(period));
        }).orElseGet(() -> {
            log.warn("No fiscal period found for date: {}", date);
            return ResponseEntity.notFound().build();
        });
    }

    @PostMapping("/{id}/close")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Close fiscal period", description = "Closes a fiscal period, preventing further postings")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Period closed successfully"),
            @ApiResponse(responseCode = "404", description = "Period not found"),
            @ApiResponse(responseCode = "400", description = "Period cannot be closed") })
    public ResponseEntity<Map<String, String>> closePeriod(Authentication authentication,
            @Parameter(description = "Period ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Reason for closing the period", required = true) @RequestParam String reason) {

        String closedBy = CallerContextResolver.resolveUsername(authentication);

        log.info("Closing fiscal period: {} by {}", id, closedBy);

        fiscalPeriodWorkflowService.closePeriod(id, closedBy, reason);
        log.info("Successfully closed fiscal period: {}", id);

        return ResponseEntity.ok(Map.of("message", "Fiscal period closed successfully", "periodId", id.toString()));
    }

    @PostMapping("/{id}/reopen")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Reopen fiscal period", description = "Reopens a closed fiscal period (HIGH RISK - requires mandatory reason)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Period reopened successfully"),
            @ApiResponse(responseCode = "404", description = "Period not found"),
            @ApiResponse(responseCode = "400", description = "Period cannot be reopened or reason insufficient") })
    public ResponseEntity<FiscalPeriodResponse> reopenFiscalPeriod(Authentication authentication,
            @Parameter(description = "Period ID", required = true) @PathVariable UUID id,
            @Parameter(description = "Business justification for reopening (min 10 characters)", required = true) @RequestParam String reason) {

        String reopenedBy = CallerContextResolver.resolveUsername(authentication);

        log.info("Reopening fiscal period: {} by {}", id, reopenedBy);

        FiscalPeriod period = fiscalPeriodService.reopenFiscalPeriod(id, reopenedBy, reason);
        log.info("Successfully reopened fiscal period: {}", period.getName());

        return ResponseEntity.ok(fiscalPeriodMapper.toResponse(period));
    }

    @GetMapping("/posting-allowed")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Check if posting is allowed", description = "Validates if posting is allowed for a specific date")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Validation result") })
    public ResponseEntity<Map<String, Object>> isPostingAllowed(
            @Parameter(description = "Posting date to validate", required = true, example = "2024-01-15") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate postingDate) {

        log.info("Checking if posting is allowed for date: {}", postingDate);

        boolean allowed = fiscalPeriodService.isPostingAllowedForDate(postingDate);
        log.info("Posting allowed for {}: {}", postingDate, allowed);

        return ResponseEntity.ok(Map.of("postingDate", postingDate, "allowed", allowed));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Open new fiscal period", description = "Creates and opens a new fiscal period")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Period created successfully", content = @Content(schema = @Schema(implementation = FiscalPeriodResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid period data or date overlap with existing period"),
            @ApiResponse(responseCode = "409", description = "A period with the same fiscal year and period number already exists") })
    public ResponseEntity<FiscalPeriodResponse> createFiscalPeriod(
            @Valid @RequestBody CreateFiscalPeriodRequest request) {

        log.info(
                "Creating fiscal period: {} ({}/{})",
                request.getName(),
                request.getFiscalYear(),
                request.getPeriodNumber());

        FiscalPeriod created = fiscalPeriodService.createFiscalPeriod(request);
        log.info("Successfully created fiscal period with ID: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(fiscalPeriodMapper.toResponse(created));
    }
}
