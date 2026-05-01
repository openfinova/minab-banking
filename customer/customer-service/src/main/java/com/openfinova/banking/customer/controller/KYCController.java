package com.openfinova.banking.customer.controller;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.customer.api.entity.KYCDecision;
import com.openfinova.banking.customer.dto.KYCDocumentSubmission;
import com.openfinova.banking.customer.entity.KYCWorkflow;
import com.openfinova.banking.customer.service.CustomerService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for KYC (Know Your Customer) workflow management.
 * Exposes endpoints for:
 * - KYC process initiation
 * - Document submission
 * - KYC review and approval
 * - KYC status management
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/kyc")
@Tag(name = "KYC Management", description = "APIs for managing KYC workflows and compliance")
public class KYCController {

    private static final Logger log = LoggerFactory.getLogger(KYCController.class);

    private final CustomerService customerService;

    public KYCController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping("/initiate")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Initiate KYC process", description = "Initiates the KYC verification process for a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "KYC process initiated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "KYC process already in progress") })
    public ResponseEntity<KYCWorkflow> initiateKYCProcess(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "User initiating the process") @RequestParam String initiatedBy) {

        log.info("Initiating KYC process for customer: {}, initiatedBy: {}", customerId, initiatedBy);

        KYCWorkflow workflow = customerService.initiateKYCProcess(customerId, initiatedBy);

        log.info("Successfully initiated KYC workflow with ID: {}", workflow.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(workflow);
    }

    @PostMapping("/documents")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Submit KYC documents", description = "Submits identification documents for KYC verification")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Documents submitted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer or KYC workflow not found"),
            @ApiResponse(responseCode = "400", description = "Invalid document data") })
    public ResponseEntity<KYCWorkflow> submitKYCDocuments(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody List<KYCDocumentSubmission> documents,
            @Parameter(description = "User submitting the documents") @RequestParam String submittedBy) {

        log.info(
                "Submitting {} KYC documents for customer: {}, submittedBy: {}",
                documents.size(),
                customerId,
                submittedBy);

        KYCWorkflow workflow = customerService.submitKYCDocuments(customerId, documents, submittedBy);

        log.info("Successfully submitted KYC documents for customer: {}", customerId);

        return ResponseEntity.ok(workflow);
    }

    @PostMapping("/review")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Review KYC documents", description = "Reviews and approves/rejects KYC documents. Administrative operation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "KYC review completed successfully"),
            @ApiResponse(responseCode = "404", description = "Customer or KYC workflow not found") })
    public ResponseEntity<KYCWorkflow> reviewKYCDocuments(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "KYC decision") @RequestParam KYCDecision decision,
            @Parameter(description = "Reviewer comments") @RequestParam String comments,
            @Parameter(description = "User performing the review") @RequestParam String reviewedBy) {

        log.info("Reviewing KYC for customer: {}, decision: {}, reviewedBy: {}", customerId, decision, reviewedBy);

        KYCWorkflow workflow = customerService.reviewKYCDocuments(customerId, decision, comments, reviewedBy);

        log.info("Successfully reviewed KYC for customer: {}, decision: {}", customerId, decision);

        return ResponseEntity.ok(workflow);
    }

    @GetMapping("/workflow")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get current KYC workflow", description = "Retrieves the current KYC workflow status for a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "KYC workflow retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "KYC workflow not found") })
    public ResponseEntity<KYCWorkflow> getKYCWorkflow(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId) {

        log.info("Fetching KYC workflow for customer: {}", customerId);

        Optional<KYCWorkflow> workflow = customerService.getKYCWorkflow(customerId);

        return workflow.map(ResponseEntity::ok).orElseGet(() -> {
            log.warn("KYC workflow not found for customer: {}", customerId);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/history")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get KYC workflow history", description = "Retrieves all KYC workflows for a customer including historical records")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "KYC history retrieved successfully") })
    public ResponseEntity<List<KYCWorkflow>> getKYCWorkflowHistory(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId) {

        log.info("Fetching KYC workflow history for customer: {}", customerId);

        List<KYCWorkflow> history = customerService.getKYCWorkflowHistory(customerId);

        log.info("Found {} KYC workflows for customer: {}", history.size(), customerId);

        return ResponseEntity.ok(history);
    }

    @PostMapping("/re-verification")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Request KYC re-verification", description = "Requests KYC re-verification for a customer. Administrative operation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Re-verification requested successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<KYCWorkflow> requestKYCReVerification(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Reason for re-verification") @RequestParam String reason,
            @Parameter(description = "User requesting re-verification") @RequestParam String requestedBy) {

        log.info(
                "Requesting KYC re-verification for customer: {}, reason: {}, requestedBy: {}",
                customerId,
                reason,
                requestedBy);

        KYCWorkflow workflow = customerService.requestKYCReVerification(customerId, reason, requestedBy);

        log.info("Successfully requested KYC re-verification for customer: {}", customerId);

        return ResponseEntity.status(HttpStatus.CREATED).body(workflow);
    }

}
