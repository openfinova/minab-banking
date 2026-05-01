package com.openfinova.banking.tp.controller;

import java.time.Duration;
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
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.tp.api.dto.CompensationWorkflowReport;
import com.openfinova.banking.tp.api.dto.CompensationWorkflowResponse;
import com.openfinova.banking.tp.api.entity.CompensationStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.CompensationStep;
import com.openfinova.banking.tp.entity.CompensationWorkflow;
import com.openfinova.banking.tp.mapper.CompensationWorkflowMapper;
import com.openfinova.banking.tp.service.CompensationWorkflowService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * REST Controller for compensation workflow monitoring and manual intervention.
 *
 * NOTE: This controller exposes monitoring and manual intervention endpoints.
 * Internal workflow initiation methods (startCompensation, executeCompensationStep)
 * are called internally by TransactionService and should NOT be exposed via REST API.
 *
 * Monitoring endpoints:
 * - View workflow details and status
 * - View active and failed workflows
 * - Generate workflow reports
 *
 * Manual intervention endpoints:
 * - Pause/resume workflows
 * - Skip/retry steps
 * - Force complete workflows
 */
@RestController
@RequestMapping("/api/v1/compensation/workflows")
@Tag(name = "Compensation Workflows", description = "APIs for monitoring and managing transaction compensation workflows (Saga pattern)")
public class CompensationWorkflowController {

    private static final Logger log = LoggerFactory.getLogger(CompensationWorkflowController.class);

    private final CompensationWorkflowService compensationService;
    private final CompensationWorkflowMapper workflowMapper;

    public CompensationWorkflowController(CompensationWorkflowService compensationService,
            CompensationWorkflowMapper workflowMapper) {
        this.compensationService = compensationService;
        this.workflowMapper = workflowMapper;
    }

    // Monitoring Endpoints

    @GetMapping("/{workflowId}")
    @PreAuthorize("hasAuthority('compensation:read')")
    @Operation(summary = "Get workflow details", description = "Retrieves comprehensive details of a compensation workflow including all steps")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Workflow details retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found") })
    public ResponseEntity<CompensationWorkflowResponse> getWorkflowDetails(
            @Parameter(description = "Workflow ID", required = true) @PathVariable UUID workflowId) {

        log.info("Fetching workflow details: {}", workflowId);

        CompensationWorkflow workflow = compensationService.getWorkflowDetails(workflowId);

        return ResponseEntity.ok(workflowMapper.toResponse(workflow));
    }

    @GetMapping("/{workflowId}/status")
    @PreAuthorize("hasAuthority('compensation:read')")
    @Operation(summary = "Get workflow status", description = "Retrieves the current status of a compensation workflow")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Status retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found") })
    public ResponseEntity<Map<String, CompensationStatus>> getWorkflowStatus(
            @Parameter(description = "Workflow ID", required = true) @PathVariable UUID workflowId) {

        log.info("Fetching workflow status: {}", workflowId);

        CompensationStatus status = compensationService.getWorkflowStatus(workflowId);

        return ResponseEntity.ok(Map.of("status", status));
    }

    @GetMapping("/{workflowId}/steps")
    @PreAuthorize("hasAuthority('compensation:read')")
    @Operation(summary = "Get workflow steps", description = "Retrieves all compensation steps for a workflow in execution order")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Steps retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found") })
    public ResponseEntity<List<CompensationStep>> getWorkflowSteps(
            @Parameter(description = "Workflow ID", required = true) @PathVariable UUID workflowId) {

        log.info("Fetching workflow steps: {}", workflowId);

        List<CompensationStep> steps = compensationService.getWorkflowSteps(workflowId);

        log.info("Found {} steps for workflow: {}", steps.size(), workflowId);

        return ResponseEntity.ok(steps);
    }

    @GetMapping("/active")
    @PreAuthorize("hasAuthority('compensation:read')")
    @Operation(summary = "Get active workflows", description = "Retrieves all currently active compensation workflows")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Active workflows retrieved successfully") })
    public ResponseEntity<List<CompensationWorkflowResponse>> getActiveWorkflows() {

        log.info("Fetching active compensation workflows");

        List<CompensationWorkflow> workflows = compensationService.getActiveWorkflows();
        List<CompensationWorkflowResponse> response = workflows.stream().map(workflowMapper::toResponse).toList();

        log.info("Found {} active workflows", response.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/failed")
    @PreAuthorize("hasAuthority('compensation:read')")
    @Operation(summary = "Get failed workflows", description = "Retrieves all failed compensation workflows that may need attention")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Failed workflows retrieved successfully") })
    public ResponseEntity<List<CompensationWorkflowResponse>> getFailedWorkflows() {

        log.info("Fetching failed compensation workflows");

        List<CompensationWorkflow> workflows = compensationService.getFailedWorkflows();
        List<CompensationWorkflowResponse> response = workflows.stream().map(workflowMapper::toResponse).toList();

        log.info("Found {} failed workflows", response.size());

        return ResponseEntity.ok(response);
    }

    @GetMapping("/status/{status}")
    @PreAuthorize("hasAuthority('compensation:read')")
    @Operation(summary = "Get workflows by status", description = "Retrieves all workflows with a specific status")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Workflows retrieved successfully") })
    public ResponseEntity<List<CompensationWorkflowResponse>> getWorkflowsByStatus(
            @Parameter(description = "Compensation status", required = true) @PathVariable CompensationStatus status) {

        log.info("Fetching workflows with status: {}", status);

        List<CompensationWorkflow> workflows = compensationService.getWorkflowsByStatus(status);
        List<CompensationWorkflowResponse> response = workflows.stream().map(workflowMapper::toResponse).toList();

        log.info("Found {} workflows with status: {}", response.size(), status);

        return ResponseEntity.ok(response);
    }

    // Manual Intervention Endpoints

    @PostMapping("/{workflowId}/pause")
    @PreAuthorize("hasAuthority('payment:initiate')")
    @Operation(summary = "Pause workflow", description = "Pauses a compensation workflow to prevent further automatic execution")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Workflow paused successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found") })
    public ResponseEntity<Void> pauseWorkflow(
            @Parameter(description = "Workflow ID", required = true) @PathVariable UUID workflowId,
            @Parameter(description = "Reason for pausing") @RequestParam String reason) {

        log.info("Pausing workflow {}: reason={}", workflowId, reason);

        compensationService.pauseWorkflow(workflowId, reason);

        log.info("Successfully paused workflow: {}", workflowId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/resume")
    @PreAuthorize("hasAuthority('payment:initiate')")
    @Operation(summary = "Resume workflow", description = "Resumes a paused compensation workflow")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Workflow resumed successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found") })
    public ResponseEntity<Void> resumeWorkflow(
            @Parameter(description = "Workflow ID", required = true) @PathVariable UUID workflowId,
            @Parameter(description = "User who resumed the workflow") @RequestParam String resumedBy) {

        log.info("Resuming workflow {}: resumedBy={}", workflowId, resumedBy);

        compensationService.resumeWorkflow(workflowId, resumedBy);

        log.info("Successfully resumed workflow: {}", workflowId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/force-complete")
    @PreAuthorize("hasAuthority('payment:initiate')")
    @Operation(summary = "Force complete workflow", description = "Forces completion of a workflow regardless of step status. Use with caution.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Workflow force completed successfully"),
            @ApiResponse(responseCode = "404", description = "Workflow not found") })
    public ResponseEntity<Void> forceCompleteWorkflow(
            @Parameter(description = "Workflow ID", required = true) @PathVariable UUID workflowId,
            @Parameter(description = "Reason for force completion") @RequestParam String reason,
            @Parameter(description = "User who forced completion") @RequestParam String completedBy) {

        log.info("Force completing workflow {}: reason={}, completedBy={}", workflowId, reason, completedBy);

        compensationService.forceCompleteWorkflow(workflowId, reason, completedBy);

        log.info("Successfully force completed workflow: {}", workflowId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/steps/{stepId}/skip")
    @PreAuthorize("hasAuthority('payment:initiate')")
    @Operation(summary = "Skip compensation step", description = "Skips a specific compensation step and marks it as completed")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Step skipped successfully"),
            @ApiResponse(responseCode = "404", description = "Step not found") })
    public ResponseEntity<Void> skipCompensationStep(
            @Parameter(description = "Workflow ID", required = true) @PathVariable UUID workflowId,
            @Parameter(description = "Step ID", required = true) @PathVariable UUID stepId,
            @Parameter(description = "Reason for skipping") @RequestParam String reason,
            @Parameter(description = "User who skipped the step") @RequestParam String skippedBy) {

        log.info("Skipping step {} in workflow {}: reason={}, skippedBy={}", stepId, workflowId, reason, skippedBy);

        compensationService.skipCompensationStep(stepId, reason, skippedBy);

        log.info("Successfully skipped step: {}", stepId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/{workflowId}/steps/{stepId}/retry")
    @PreAuthorize("hasAuthority('payment:initiate')")
    @Operation(summary = "Retry compensation step", description = "Retries a failed compensation step")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Step retry initiated successfully"),
            @ApiResponse(responseCode = "404", description = "Step not found") })
    public ResponseEntity<Void> retryCompensationStep(
            @Parameter(description = "Workflow ID", required = true) @PathVariable UUID workflowId,
            @Parameter(description = "Step ID", required = true) @PathVariable UUID stepId,
            @Parameter(description = "User who initiated retry") @RequestParam String retriedBy) {

        log.info("Retrying step {} in workflow {}: retriedBy={}", stepId, workflowId, retriedBy);

        compensationService.retryCompensationStep(stepId, retriedBy);

        log.info("Successfully initiated retry for step: {}", stepId);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/custom")
    @PreAuthorize("hasAuthority('payment:initiate')")
    @Operation(summary = "Create custom workflow", description = "Creates a custom compensation workflow with specified steps")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Custom workflow created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid workflow configuration") })
    public ResponseEntity<CompensationWorkflowResponse> createCustomWorkflow(
            @Parameter(description = "Transaction ID") @RequestParam UUID transactionId,
            @Valid @RequestBody List<CompensationStep> steps) {

        log.info("Creating custom workflow for transaction {}: {} steps", transactionId, steps.size());

        CompensationWorkflow workflow = compensationService.createCustomWorkflow(transactionId, steps);

        log.info("Successfully created custom workflow with ID: {}", workflow.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(workflowMapper.toResponse(workflow));
    }

    // Reporting Endpoints

    @GetMapping("/report")
    @PreAuthorize("hasAuthority('compensation:read')")
    @Operation(summary = "Generate workflow report", description = "Generates a comprehensive workflow report for the specified date range")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Report generated successfully") })
    public ResponseEntity<CompensationWorkflowReport> getWorkflowReport(
            @Parameter(description = "Start date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "End date") @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        log.info("Generating workflow report from {} to {}", startDate, endDate);

        CompensationWorkflowReport report = compensationService.getWorkflowReport(startDate, endDate);

        log.info("Successfully generated workflow report");

        return ResponseEntity.ok(report);
    }

    @GetMapping("/average-time/{transactionType}")
    @PreAuthorize("hasAuthority('compensation:read')")
    @Operation(summary = "Get average compensation time", description = "Calculates the average compensation time for a specific transaction type")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Average time calculated successfully") })
    public ResponseEntity<Map<String, Object>> getAverageCompensationTime(
            @Parameter(description = "Transaction type", required = true) @PathVariable TransactionType transactionType) {

        log.info("Calculating average compensation time for type: {}", transactionType);

        Duration avgTime = compensationService.getAverageCompensationTime(transactionType);

        return ResponseEntity.ok(
                Map.of(
                        "transactionType",
                        transactionType,
                        "averageCompensationTime",
                        avgTime.toString(),
                        "averageMinutes",
                        avgTime.toMinutes()));
    }
}
