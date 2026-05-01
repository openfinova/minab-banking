package com.openfinova.banking.gl.controller;

import java.time.LocalDate;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.BalanceSheetResponse;
import com.openfinova.banking.gl.api.dto.CashFlowStatementResponse;
import com.openfinova.banking.gl.api.dto.IncomeStatementResponse;
import com.openfinova.banking.gl.service.FinancialReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * REST controller exposing period-end financial statement reports:
 * <ul>
 *   <li>Income Statement (Profit &amp; Loss)</li>
 *   <li>Balance Sheet (Statement of Financial Position)</li>
 *   <li>Statement of Cash Flows (simplified indirect method)</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/v1/gl/reports")
@Tag(name = "Financial Statements", description = "Period-end financial statement reports (income statement, balance sheet, cash flow)")
public class FinancialStatementController {

    private static final Logger log = LoggerFactory.getLogger(FinancialStatementController.class);

    private final FinancialReportService financialReportService;

    public FinancialStatementController(FinancialReportService financialReportService) {
        this.financialReportService = financialReportService;
    }

    @GetMapping("/income-statement")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get income statement", description = "Generates an income statement (profit and loss statement) for the specified period. "
            + "Revenue is derived from REVENUE-type accounts and expenses from EXPENSE-type accounts. "
            + "Net income = total revenue − total expenses.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Income statement generated successfully", content = @Content(schema = @Schema(implementation = IncomeStatementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range – startDate must be on or before endDate") })
    public ResponseEntity<IncomeStatementResponse> getIncomeStatement(
            @Parameter(description = "First day of the reporting period (inclusive)", required = true, example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Last day of the reporting period (inclusive)", required = true, example = "2026-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/gl/reports/income-statement  startDate={} endDate={}", startDate, endDate);

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }

        IncomeStatementResponse response = financialReportService.getIncomeStatement(startDate, endDate);

        log.info(
                "Income statement: {} revenue lines, {} expense lines, netIncome={}",
                response.getRevenueLines().size(),
                response.getExpenseLines().size(),
                response.getNetIncome());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/balance-sheet")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get balance sheet", description = "Generates a balance sheet (statement of financial position) as of the specified date. "
            + "Shows ASSET, LIABILITY, and EQUITY account balances. "
            + "The 'balanced' flag indicates whether Assets = Liabilities + Equity.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Balance sheet generated successfully", content = @Content(schema = @Schema(implementation = BalanceSheetResponse.class))) })
    public ResponseEntity<BalanceSheetResponse> getBalanceSheet(
            @Parameter(description = "Snapshot date", required = true, example = "2026-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate asOfDate) {

        log.info("GET /api/gl/reports/balance-sheet  asOfDate={}", asOfDate);

        BalanceSheetResponse response = financialReportService.getBalanceSheet(asOfDate);

        log.info(
                "Balance sheet: {} asset lines, {} liability lines, {} equity lines, balanced={}",
                response.getAssetLines().size(),
                response.getLiabilityLines().size(),
                response.getEquityLines().size(),
                response.isBalanced());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/cash-flow")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get statement of cash flows", description = "Generates a simplified statement of cash flows for the specified period using the indirect method. "
            + "Operating activities = net income. "
            + "Investing activities = net change in ASSET accounts (increase = outflow). "
            + "Financing activities = net change in LIABILITY and EQUITY accounts (increase = inflow). "
            + "Net cash change = operating + investing + financing.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Cash flow statement generated successfully", content = @Content(schema = @Schema(implementation = CashFlowStatementResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid date range – startDate must be on or before endDate") })
    public ResponseEntity<CashFlowStatementResponse> getCashFlowStatement(
            @Parameter(description = "First day of the reporting period (inclusive)", required = true, example = "2026-01-01") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "Last day of the reporting period (inclusive)", required = true, example = "2026-12-31") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("GET /api/gl/reports/cash-flow  startDate={} endDate={}", startDate, endDate);

        if (startDate.isAfter(endDate)) {
            return ResponseEntity.badRequest().build();
        }

        CashFlowStatementResponse response = financialReportService.getCashFlowStatement(startDate, endDate);

        log.info(
                "Cash flow statement: operating={}, investing={}, financing={}, netCash={}",
                response.getTotalOperating(),
                response.getTotalInvesting(),
                response.getTotalFinancing(),
                response.getNetCashChange());

        return ResponseEntity.ok(response);
    }
}
