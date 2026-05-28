package com.openfinova.banking.customer.controller;

import com.openfinova.banking.customer.api.entity.DataSubjectRequestType;
import com.openfinova.banking.customer.dto.CustomerDataExport;
import com.openfinova.banking.customer.dto.CustomerDataRetentionResponse;
import com.openfinova.banking.customer.dto.DataSubjectRequestResponse;
import com.openfinova.banking.customer.mapper.CustomerMapper;
import com.openfinova.banking.customer.service.AnonymizationService;
import com.openfinova.banking.customer.service.DataExportService;
import com.openfinova.banking.customer.service.DataSubjectRequestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * REST Controller for GDPR compliance operations.
 *
 * Exposes endpoints for:
 *   Submitting and managing Data Subject Requests (DSARs)
 *   Exporting customer data (Art. 15 access / Art. 20 portability)
 *   Manually triggering anonymization (admin/DPO only)
 *   Managing retention records
 *
 * Security note: In production, all endpoints under this controller
 * must be protected. Endpoints modifying data (fulfill, reject, anonymize) require
 * elevated privileges (ROLE_DPO or ROLE_COMPLIANCE_OFFICER). Endpoints that expose
 * personal data require identity verification before serving the response.
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}")
@Tag(name = "GDPR Compliance", description = "Data Subject Requests, data export, and anonymization")
public class DataSubjectRequestController {

    private static final Logger log = LoggerFactory.getLogger(DataSubjectRequestController.class);

    private final DataSubjectRequestService dsarService;
    private final DataExportService dataExportService;
    private final AnonymizationService anonymizationService;

    public DataSubjectRequestController(DataSubjectRequestService dsarService, DataExportService dataExportService,
            AnonymizationService anonymizationService) {
        this.dsarService = dsarService;
        this.dataExportService = dataExportService;
        this.anonymizationService = anonymizationService;
    }

    @PostMapping("/data-requests")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Submit a Data Subject Request", description = "Creates a new GDPR data subject request (access, erasure, portability, etc.) "
            + "on behalf of the customer. An erasure request is automatically deferred if a legal "
            + "retention obligation (AML/CFT) is still in force.")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "DSAR submitted successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request payload"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<DataSubjectRequestResponse> submitRequest(
            @Parameter(description = "Customer ID") @PathVariable UUID customerId,
            @Valid @RequestBody SubmitDsarRequest body) {

        var created = dsarService.submitRequest(customerId, body.requestType(), body.channel(), body.notes());

        log.info("DSAR {} submitted for customer {}.", created.getReferenceNumber(), customerId);
        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerMapper.toDataSubjectRequestResponse(created));
    }

    @GetMapping("/data-requests")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "List Data Subject Requests", description = "Returns all DSARs for this customer, newest first.")
    @ApiResponse(responseCode = "200", description = "List of DSARs")
    public ResponseEntity<List<DataSubjectRequestResponse>> listRequests(@PathVariable UUID customerId) {
        return ResponseEntity
                .ok(CustomerMapper.toDataSubjectRequestResponseList(dsarService.getRequestsForCustomer(customerId)));
    }

    @PutMapping("/data-requests/{requestId}/identity-verified")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Confirm identity verification", description = "Advances the DSAR to IN_REVIEW after the requestor's identity has been verified. "
            + "Requires ROLE_COMPLIANCE_OFFICER.")
    public ResponseEntity<DataSubjectRequestResponse> confirmIdentityVerified(@PathVariable UUID customerId,
            @PathVariable UUID requestId, @RequestParam @NotBlank String verifiedBy) {
        return ResponseEntity.ok(
                CustomerMapper
                        .toDataSubjectRequestResponse(dsarService.confirmIdentityVerified(requestId, verifiedBy)));
    }

    @PutMapping("/data-requests/{requestId}/fulfill")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Fulfill a DSAR", description = "Marks the request as fulfilled. Requires ROLE_DPO.")
    public ResponseEntity<DataSubjectRequestResponse> fulfill(@PathVariable UUID customerId,
            @PathVariable UUID requestId, @RequestParam @NotBlank String handledBy) {
        return ResponseEntity
                .ok(CustomerMapper.toDataSubjectRequestResponse(dsarService.fulfill(requestId, handledBy)));
    }

    @PutMapping("/data-requests/{requestId}/reject")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Reject a DSAR", description = "Rejects the request with a documented reason and legal basis. "
            + "Requires ROLE_DPO.")
    public ResponseEntity<DataSubjectRequestResponse> reject(@PathVariable UUID customerId,
            @PathVariable UUID requestId, @RequestBody RejectDsarRequest body) {
        return ResponseEntity.ok(
                CustomerMapper
                        .toDataSubjectRequestResponse(dsarService.reject(requestId, body.reason(), body.handledBy())));
    }

    @PutMapping("/data-requests/{requestId}/extend")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Extend DSAR deadline", description = "Extends the 30-day SLA by up to 60 additional days for complex requests "
            + "(GDPR Art. 12(3)). Customer must be notified within original 30-day window.")
    public ResponseEntity<DataSubjectRequestResponse> extendDeadline(@PathVariable UUID customerId,
            @PathVariable UUID requestId, @RequestParam int additionalDays, @RequestParam @NotBlank String handledBy) {
        return ResponseEntity.ok(
                CustomerMapper.toDataSubjectRequestResponse(
                        dsarService.extendDeadline(requestId, additionalDays, handledBy)));
    }

    @DeleteMapping("/data-requests/{requestId}")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Withdraw a DSAR", description = "Allows the customer to withdraw their own pending request.")
    public ResponseEntity<DataSubjectRequestResponse> withdraw(@PathVariable UUID customerId,
            @PathVariable UUID requestId) {
        return ResponseEntity
                .ok(CustomerMapper.toDataSubjectRequestResponse(dsarService.withdraw(requestId, customerId)));
    }

    @GetMapping("/data-export")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Export customer personal data", description = "Returns a structured export of all personal data held for GDPR Art. 15 "
            + "(right of access) and Art. 20 (data portability). " + "Requires prior identity verification.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Data export returned successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<CustomerDataExport> exportData(@PathVariable UUID customerId,
            @RequestParam(required = false) UUID dataSubjectRequestId) {
        return ResponseEntity.ok(dataExportService.exportCustomerData(customerId, dataSubjectRequestId));
    }

    @PostMapping("/retention")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Create retention record", description = "Creates a data retention record when a customer account is closed. "
            + "Calculates the retention expiry date based on the legal basis. " + "Requires ROLE_COMPLIANCE_OFFICER.")
    @ApiResponses({ @ApiResponse(responseCode = "201", description = "Retention record created"),
            @ApiResponse(responseCode = "409", description = "Retention record already exists") })
    public ResponseEntity<CustomerDataRetentionResponse> createRetentionRecord(@PathVariable UUID customerId,
            @RequestBody CreateRetentionRequest body) {

        if (dsarService.retentionRecordExists(customerId)) {
            return ResponseEntity.status(HttpStatus.CONFLICT).build();
        }

        var retention = dsarService.createRetentionRecord(
                customerId,
                body.relationshipEndedAt(),
                body.retentionYears(),
                body.legalBasis());

        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerMapper.toDataRetentionResponse(retention));
    }

    @GetMapping("/retention")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Get retention record", description = "Returns the data retention record for a customer. "
            + "Requires ROLE_COMPLIANCE_OFFICER.")
    public ResponseEntity<CustomerDataRetentionResponse> getRetentionRecord(@PathVariable UUID customerId) {
        return dsarService.getRetentionRecord(customerId).map(CustomerMapper::toDataRetentionResponse)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    // =========================================================================
    // Manual Anonymization (DPO / Admin)
    // =========================================================================

    @PostMapping("/anonymize")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Manually trigger anonymization", description = "Immediately anonymizes a customer's personal data if the retention "
            + "period has expired. Requires ROLE_DPO.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Anonymization completed"),
            @ApiResponse(responseCode = "400", description = "Retention period has not yet expired"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<Map<String, String>> anonymizeCustomer(@PathVariable UUID customerId,
            @RequestParam @NotBlank String requestedBy) {
        try {
            anonymizationService.anonymizeCustomer(customerId, requestedBy, null);
            return ResponseEntity.ok(
                    Map.of("status", "ANONYMIZED", "customerId", customerId.toString(), "performedBy", requestedBy));
        } catch (IllegalStateException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    record SubmitDsarRequest(@NotNull DataSubjectRequestType requestType, String channel, String notes) {
    }

    record RejectDsarRequest(@NotBlank String reason, @NotBlank String handledBy) {
    }

    record CreateRetentionRequest(@NotNull LocalDate relationshipEndedAt, int retentionYears,
            @NotBlank String legalBasis) {
    }
}
