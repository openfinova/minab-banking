package com.openfinova.banking.gl.controller;

import java.time.YearMonth;
import java.util.LinkedHashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.CreateFiscalPeriodRequest;
import com.openfinova.banking.gl.api.dto.SystemInitRequest;
import com.openfinova.banking.gl.service.FiscalPeriodService;
import com.openfinova.banking.gl.service.GLAccountService;
import com.openfinova.banking.gl.service.OperationalGLAccountService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Administrative REST controller for bootstrapping a fresh GL deployment.
 *
 * On a clean database these three steps must be completed in order before
 * any transactions can be posted:
 * 1. Standard chart of accounts — supplies all GL accounts referenced by the
 *    operational mappings and by journal entries.
 * 2. Operational account mappings — wires each {@code OperationalGLAccountType}
 *    to its designated GL account (requires chart to exist).
 * 3. Fiscal periods — at least one OPEN period covering today's date must
 *    exist; GL transactions are rejected without one.
 *
 * Use {@code POST /api/gl/setup/initialize} to execute all three steps in a
 * single idempotent request, or call the individual endpoints when only one step
 * is needed.
 */
@RestController
@RequestMapping("/api/v1/gl/setup")
@Tag(name = "GL System Setup", description = "One-time initialization endpoints for bootstrapping the GL on a fresh deployment")
public class SystemSetupController {

    private static final Logger log = LoggerFactory.getLogger(SystemSetupController.class);

    private final GLAccountService glAccountService;
    private final OperationalGLAccountService operationalGLAccountService;
    private final FiscalPeriodService fiscalPeriodService;

    public SystemSetupController(GLAccountService glAccountService,
            OperationalGLAccountService operationalGLAccountService, FiscalPeriodService fiscalPeriodService) {
        this.glAccountService = glAccountService;
        this.operationalGLAccountService = operationalGLAccountService;
        this.fiscalPeriodService = fiscalPeriodService;
    }

    @PostMapping("/initialize")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Full GL initialization", description = "Bootstraps a fresh deployment in a single call: "
            + "(1) creates the standard chart of accounts for the supplied currency, "
            + "(2) wires the standard operational GL account mappings, "
            + "(3) opens 12 monthly fiscal periods for the requested fiscal year. "
            + "All three steps are idempotent: existing records are skipped, not overwritten.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Initialization completed (see body for per-step counts)"),
            @ApiResponse(responseCode = "400", description = "Invalid request body") })
    public ResponseEntity<Map<String, Object>> initialize(@Valid @RequestBody SystemInitRequest request) {

        log.info(
                "GL system initialization: currency={}, fiscalYear={}, createdBy={}",
                request.getCurrency(),
                request.getFiscalYear(),
                request.getCreatedBy());

        int chartCount = initChartOfAccounts(request.getCurrency(), request.getCreatedBy());
        int operationalCount = initOperationalAccounts(request.getCreatedBy());
        int[] periodResults = initFiscalPeriods(request.getFiscalYear(), request.getCreatedBy());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("currency", request.getCurrency());
        result.put("fiscalYear", request.getFiscalYear());
        result.put("glAccountsCreated", chartCount);
        result.put("operationalAccountsWired", operationalCount);
        result.put("fiscalPeriodsCreated", periodResults[0]);
        result.put("fiscalPeriodsAlreadyExisted", periodResults[1]);

        log.info(
                "GL initialization complete: {} GL accounts, {} operational mappings, "
                        + "{} fiscal periods created ({} already existed)",
                chartCount,
                operationalCount,
                periodResults[0],
                periodResults[1]);

        return ResponseEntity.ok(result);
    }

    @PostMapping("/chart-of-accounts")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Initialize standard chart of accounts", description = "Creates the standard banking chart of accounts for the given currency. "
            + "Accounts that already exist (by code) are skipped.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Chart of accounts initialized"),
            @ApiResponse(responseCode = "400", description = "Invalid request") })
    public ResponseEntity<Map<String, Object>> initializeChartOfAccounts(
            @Parameter(description = "ISO 4217 base currency code", required = true, example = "USD") @RequestParam String currency,
            @Parameter(description = "Operator username for audit trail", required = true, example = "admin") @RequestParam String createdBy) {

        log.info("Initializing chart of accounts: currency={}, createdBy={}", currency, createdBy);

        int count = initChartOfAccounts(currency, createdBy);

        log.info("Chart of accounts initialization complete: {} accounts created", count);
        return ResponseEntity.ok(Map.of("currency", currency, "glAccountsCreated", count));
    }

    @PostMapping("/operational-accounts")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Initialize standard operational account mappings", description = "Wires each OperationalGLAccountType to its designated GL account "
            + "from the standard chart. The chart of accounts must exist first. "
            + "Mappings that already exist are skipped.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Operational accounts initialized"),
            @ApiResponse(responseCode = "400", description = "Invalid request"),
            @ApiResponse(responseCode = "409", description = "Chart of accounts not present — run chart-of-accounts init first") })
    public ResponseEntity<Map<String, Object>> initializeOperationalAccounts(
            @Parameter(description = "Operator username for audit trail", required = true, example = "admin") @RequestParam String createdBy) {

        log.info("Initializing operational account mappings: createdBy={}", createdBy);

        int count = initOperationalAccounts(createdBy);

        log.info("Operational account initialization complete: {} mappings created", count);
        return ResponseEntity.ok(Map.of("operationalAccountsWired", count));
    }

    @PostMapping("/fiscal-periods")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Bootstrap fiscal periods for a year", description = "Creates 12 monthly OPEN fiscal periods for the given fiscal year "
            + "(period 1 = January … period 12 = December). " + "Periods that already exist are skipped.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Fiscal periods initialized"),
            @ApiResponse(responseCode = "400", description = "Invalid request") })
    public ResponseEntity<Map<String, Object>> initializeFiscalPeriods(
            @Parameter(description = "Fiscal year to bootstrap (e.g. 2026)", required = true, example = "2026") @RequestParam int fiscalYear,
            @Parameter(description = "Operator username for audit trail", required = true, example = "admin") @RequestParam String createdBy) {

        log.info("Initializing fiscal periods for year={}, createdBy={}", fiscalYear, createdBy);

        int[] results = initFiscalPeriods(fiscalYear, createdBy);

        log.info("Fiscal period initialization complete: {} created, {} already existed", results[0], results[1]);
        return ResponseEntity.ok(
                Map.of(
                        "fiscalYear",
                        fiscalYear,
                        "fiscalPeriodsCreated",
                        results[0],
                        "fiscalPeriodsAlreadyExisted",
                        results[1]));
    }

    private int initChartOfAccounts(String currency, String createdBy) {
        return glAccountService.createStandardChartOfAccounts(currency, createdBy);
    }

    private int initOperationalAccounts(String createdBy) {
        return operationalGLAccountService.createStandardOperationalAccounts(createdBy);
    }

    /**
     * Creates 12 monthly fiscal periods for {@code fiscalYear}.
     *
     * @return int[2] where [0] = created, [1] = already existed (skipped)
     */
    private int[] initFiscalPeriods(int fiscalYear, String createdBy) {
        int created = 0;
        int skipped = 0;

        for (int month = 1; month <= 12; month++) {
            YearMonth ym = YearMonth.of(fiscalYear, month);
            String name = ym.getMonth().getDisplayName(java.time.format.TextStyle.FULL, java.util.Locale.ENGLISH) + " "
                    + fiscalYear;

            CreateFiscalPeriodRequest req = new CreateFiscalPeriodRequest(
                    name,
                    ym.atDay(1),
                    ym.atEndOfMonth(),
                    fiscalYear,
                    month);

            try {
                fiscalPeriodService.createFiscalPeriod(req);
                created++;
                log.debug("Created fiscal period: {}", name);
            } catch (IllegalArgumentException e) {
                // Period already exists or dates overlap — skip gracefully
                skipped++;
                log.debug("Skipped fiscal period (already exists or overlap): {} — {}", name, e.getMessage());
            }
        }
        return new int[] { created, skipped };
    }
}
