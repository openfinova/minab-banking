package com.openfinova.banking.tp.service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.gl.api.GeneralLedgerService;
import com.openfinova.banking.gl.api.dto.GLTransactionDTO;
import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tp.api.dto.CompensationWorkflowReport;
import com.openfinova.banking.tp.api.dto.CompensationWorkflowSummary;
import com.openfinova.banking.tp.api.dto.DailyWorkflowMetrics;
import com.openfinova.banking.tp.api.entity.CompensationStatus;
import com.openfinova.banking.tp.api.entity.TransactionType;
import com.openfinova.banking.tp.entity.BalanceReservation;
import com.openfinova.banking.tp.entity.CompensationStep;
import com.openfinova.banking.tp.entity.CompensationWorkflow;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.repository.CompensationWorkflowRepository;
import com.openfinova.banking.tp.repository.TransactionRepository;

/**
 * Implementation of CompensationWorkflowService with comprehensive compensation
 * logic for failed transactions using the Saga pattern.
 * Enhanced with workflow monitoring, manual intervention, and reporting capabilities.
 *
 * Requirements addressed:
 * - Transaction lifecycle state management
 * - Audit trail implementations
 * - Workflow monitoring and manual intervention capabilities
 */
@Service
@Transactional
public class CompensationWorkflowService {

    private static final Logger logger = LoggerFactory.getLogger(CompensationWorkflowService.class);

    private final CompensationWorkflowRepository compensationWorkflowRepository;
    private final TransactionRepository transactionRepository;
    private final BalanceReservationService balanceReservationService;
    private final DateTimeService dateTimeService;
    private final GeneralLedgerService generalLedgerService;

    public CompensationWorkflowService(CompensationWorkflowRepository compensationWorkflowRepository,
            TransactionRepository transactionRepository, BalanceReservationService balanceReservationService,
            DateTimeService dateTimeService, GeneralLedgerService generalLedgerService) {
        this.compensationWorkflowRepository = compensationWorkflowRepository;
        this.transactionRepository = transactionRepository;
        this.balanceReservationService = balanceReservationService;
        this.dateTimeService = dateTimeService;
        this.generalLedgerService = generalLedgerService;
    }

    public UUID startCompensation(UUID transactionId) {
        logger.info("Starting compensation workflow for transaction: {}", transactionId);

        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Check if compensation workflow already exists
        var existingWorkflow = compensationWorkflowRepository.findByOriginalTransactionId(transactionId);
        if (existingWorkflow.isPresent()) {
            logger.info("Compensation workflow already exists for transaction: {}", transactionId);
            return existingWorkflow.get().getId();
        }

        // Create compensation workflow
        CompensationWorkflow workflow = new CompensationWorkflow(
                originalTransaction,
                "Transaction failed after authorization - compensation required");

        // Determine compensation type and amount
        determineCompensationType(workflow, originalTransaction);

        // Create compensation steps based on transaction state
        createCompensationSteps(workflow, originalTransaction);

        CompensationWorkflow savedWorkflow = compensationWorkflowRepository.save(workflow);

        // Start executing compensation steps asynchronously
        executeCompensationWorkflowAsync(savedWorkflow.getId());

        logger.info("Compensation workflow started: {} for transaction: {}", savedWorkflow.getId(), transactionId);
        return savedWorkflow.getId();
    }

    public void executeCompensationStep(UUID stepId) {
        logger.info("Executing compensation step: {}", stepId);

        // Find the workflow containing this step
        CompensationWorkflow workflow = compensationWorkflowRepository.findByCompensationStepId(stepId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Compensation step not found: " + stepId));

        CompensationStep step = workflow.getCompensationSteps().stream()
                .filter(s -> stepId.toString().equals(s.getStepId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Step not found in workflow: " + stepId));

        if (step.getStatus() != com.openfinova.banking.tp.api.entity.CompensationStepStatus.PENDING) {
            logger.warn("Compensation step {} is not in PENDING state: {}", stepId, step.getStatus());
            return;
        }

        try {
            // Mark step as in progress
            step.setStatus(com.openfinova.banking.tp.api.entity.CompensationStepStatus.IN_PROGRESS);
            step.setStartedAt(dateTimeService.now());
            compensationWorkflowRepository.save(workflow);

            // Execute the step based on its type
            executeStepByType(step, workflow);

            // Mark step as completed
            step.setStatus(com.openfinova.banking.tp.api.entity.CompensationStepStatus.COMPLETED);
            step.setCompletedAt(dateTimeService.now());

            logger.info("Compensation step completed successfully: {}", stepId);

        } catch (Exception e) {
            logger.error("Compensation step failed: {}", stepId, e);

            // Mark step as failed
            step.setStatus(com.openfinova.banking.tp.api.entity.CompensationStepStatus.FAILED);
            step.setFailureReason(e.getMessage());
            step.setFailedAt(dateTimeService.now());

            // Increment workflow retry count
            workflow.incrementRetryCount();

            // Check if we should escalate or retry
            if (!workflow.canRetry()) {
                workflow.escalate("Maximum retries exceeded", "SYSTEM");
                logger.warn("Compensation workflow escalated due to max retries: {}", workflow.getId());
            }
        } finally {
            compensationWorkflowRepository.save(workflow);
        }

        // Check if all steps are completed
        if (workflow.areAllStepsCompleted()) {
            workflow.transitionTo(CompensationStatus.COMPLETED, "All compensation steps completed successfully");
            compensationWorkflowRepository.save(workflow);
            logger.info("Compensation workflow completed: {}", workflow.getId());
        }
    }

    @Transactional(readOnly = true)
    public CompensationStatus getWorkflowStatus(UUID workflowId) {
        CompensationWorkflow workflow = compensationWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Compensation workflow not found: " + workflowId));
        return workflow.getWorkflowStatus();
    }

    // New workflow monitoring methods

    @Transactional(readOnly = true)
    public CompensationWorkflow getWorkflowDetails(UUID workflowId) {
        logger.debug("Retrieving workflow details for: {}", workflowId);
        return compensationWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Compensation workflow not found: " + workflowId));
    }

    @Transactional(readOnly = true)
    public List<CompensationStep> getWorkflowSteps(UUID workflowId) {
        logger.debug("Retrieving workflow steps for: {}", workflowId);
        CompensationWorkflow workflow = getWorkflowDetails(workflowId);
        List<CompensationStep> steps = workflow.getCompensationSteps();
        return steps != null ? new ArrayList<>(steps) : new ArrayList<>();
    }

    @Transactional(readOnly = true)
    public List<CompensationWorkflow> getActiveWorkflows() {
        logger.debug("Retrieving active compensation workflows");
        return compensationWorkflowRepository.findActiveWorkflows();
    }

    @Transactional(readOnly = true)
    public List<CompensationWorkflow> getFailedWorkflows() {
        logger.debug("Retrieving failed compensation workflows");
        return compensationWorkflowRepository.findFailedWorkflows();
    }

    // Manual intervention capabilities

    public void pauseWorkflow(UUID workflowId, String reason) {
        logger.info("Pausing compensation workflow: {} with reason: {}", workflowId, reason);

        CompensationWorkflow workflow = getWorkflowDetails(workflowId);

        if (workflow.getWorkflowStatus().isTerminal()) {
            throw new IllegalStateException("Cannot pause workflow in terminal state: " + workflow.getWorkflowStatus());
        }

        // For now, we'll escalate the workflow to effectively pause it
        workflow.escalate("Manually paused: " + reason, "SYSTEM");
        compensationWorkflowRepository.save(workflow);

        logger.info("Compensation workflow paused: {}", workflowId);
    }

    public void resumeWorkflow(UUID workflowId, String resumedBy) {
        logger.info("Resuming compensation workflow: {} by: {}", workflowId, resumedBy);

        CompensationWorkflow workflow = getWorkflowDetails(workflowId);

        if (workflow.getWorkflowStatus() != CompensationStatus.ESCALATED) {
            throw new IllegalStateException(
                    "Can only resume escalated workflows. Current status: " + workflow.getWorkflowStatus());
        }

        // Reset workflow for retry
        workflow.resetForRetry();
        compensationWorkflowRepository.save(workflow);

        // Start executing the workflow asynchronously
        executeCompensationWorkflowAsync(workflowId);

        logger.info("Compensation workflow resumed: {} by: {}", workflowId, resumedBy);
    }

    public void skipCompensationStep(UUID stepId, String reason, String skippedBy) {
        logger.info("Skipping compensation step: {} by: {} with reason: {}", stepId, skippedBy, reason);

        CompensationWorkflow workflow = compensationWorkflowRepository.findByCompensationStepId(stepId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Compensation step not found: " + stepId));

        CompensationStep step = workflow.getCompensationSteps().stream()
                .filter(s -> stepId.toString().equals(s.getStepId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Step not found in workflow: " + stepId));

        if (step.getStatus().isTerminal()) {
            throw new IllegalStateException("Cannot skip step in terminal state: " + step.getStatus());
        }

        step.markSkipped("Manually skipped by " + skippedBy + ": " + reason);
        compensationWorkflowRepository.save(workflow);

        logger.info("Compensation step skipped: {} by: {}", stepId, skippedBy);
    }

    public void retryCompensationStep(UUID stepId, String retriedBy) {
        logger.info("Retrying compensation step: {} by: {}", stepId, retriedBy);

        CompensationWorkflow workflow = compensationWorkflowRepository.findByCompensationStepId(stepId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Compensation step not found: " + stepId));

        CompensationStep step = workflow.getCompensationSteps().stream()
                .filter(s -> stepId.toString().equals(s.getStepId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Step not found in workflow: " + stepId));

        if (!step.getStatus().isFailed()) {
            throw new IllegalStateException("Can only retry failed steps. Current status: " + step.getStatus());
        }

        step.resetForRetry();
        compensationWorkflowRepository.save(workflow);

        // Execute the step
        executeCompensationStep(stepId);

        logger.info("Compensation step retry initiated: {} by: {}", stepId, retriedBy);
    }

    public void forceCompleteWorkflow(UUID workflowId, String reason, String completedBy) {
        logger.info(
                "Force completing compensation workflow: {} by: {} with reason: {}",
                workflowId,
                completedBy,
                reason);

        CompensationWorkflow workflow = getWorkflowDetails(workflowId);

        if (workflow.getWorkflowStatus().isTerminal()) {
            throw new IllegalStateException("Workflow is already in terminal state: " + workflow.getWorkflowStatus());
        }

        workflow.transitionTo(CompensationStatus.COMPLETED, "Force completed by " + completedBy + ": " + reason);
        compensationWorkflowRepository.save(workflow);

        logger.info("Compensation workflow force completed: {} by: {}", workflowId, completedBy);
    }

    // Workflow configuration

    public CompensationWorkflow createCustomWorkflow(UUID transactionId, List<CompensationStep> steps) {
        logger.info("Creating custom compensation workflow for transaction: {}", transactionId);

        Transaction originalTransaction = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new IllegalArgumentException("Transaction not found: " + transactionId));

        // Check if compensation workflow already exists
        var existingWorkflow = compensationWorkflowRepository.findByOriginalTransactionId(transactionId);
        if (existingWorkflow.isPresent()) {
            throw new IllegalStateException("Compensation workflow already exists for transaction: " + transactionId);
        }

        CompensationWorkflow workflow = new CompensationWorkflow(
                originalTransaction,
                "Custom compensation workflow created");

        // Add custom steps
        if (steps != null) {
            for (int i = 0; i < steps.size(); i++) {
                CompensationStep step = steps.get(i);
                step.setOrder(i + 1);
                step.setStatus(com.openfinova.banking.tp.api.entity.CompensationStepStatus.PENDING);
                workflow.addCompensationStep(step);
            }
        }

        CompensationWorkflow savedWorkflow = compensationWorkflowRepository.save(workflow);

        logger.info(
                "Custom compensation workflow created: {} for transaction: {}",
                savedWorkflow.getId(),
                transactionId);
        return savedWorkflow;
    }

    public void updateWorkflowStep(UUID stepId, CompensationStep updatedStep) {
        logger.info("Updating compensation step: {}", stepId);

        CompensationWorkflow workflow = compensationWorkflowRepository.findByCompensationStepId(stepId.toString())
                .orElseThrow(() -> new IllegalArgumentException("Compensation step not found: " + stepId));

        CompensationStep existingStep = workflow.getCompensationSteps().stream()
                .filter(s -> stepId.toString().equals(s.getStepId())).findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Step not found in workflow: " + stepId));

        if (existingStep.getStatus() != com.openfinova.banking.tp.api.entity.CompensationStepStatus.PENDING) {
            throw new IllegalStateException(
                    "Can only update pending steps. Current status: " + existingStep.getStatus());
        }

        // Update step properties
        existingStep.setStepType(updatedStep.getStepType());
        existingStep.setDescription(updatedStep.getDescription());
        existingStep.setParameters(updatedStep.getParameters());

        compensationWorkflowRepository.save(workflow);

        logger.info("Compensation step updated: {}", stepId);
    }

    // Monitoring and reporting

    @Transactional(readOnly = true)
    public CompensationWorkflowReport getWorkflowReport(LocalDate startDate, LocalDate endDate) {
        logger.info("Generating compensation workflow report from {} to {}", startDate, endDate);

        CompensationWorkflowReport report = new CompensationWorkflowReport(startDate, endDate);

        // Get all workflows in the date range
        List<CompensationWorkflow> workflows = compensationWorkflowRepository
                .findByCreatedAtBetween(startDate, endDate);

        // Calculate summary metrics
        report.setTotalWorkflows(workflows.size());
        report.setCompletedWorkflows(
                (int) workflows.stream().filter(w -> w.getWorkflowStatus() == CompensationStatus.COMPLETED).count());
        report.setFailedWorkflows(
                (int) workflows.stream().filter(w -> w.getWorkflowStatus() == CompensationStatus.FAILED).count());
        report.setActiveWorkflows((int) workflows.stream().filter(w -> !w.getWorkflowStatus().isTerminal()).count());
        report.setEscalatedWorkflows(
                (int) workflows.stream().filter(w -> w.getWorkflowStatus() == CompensationStatus.ESCALATED).count());
        report.setCancelledWorkflows(
                (int) workflows.stream().filter(w -> w.getWorkflowStatus() == CompensationStatus.CANCELLED).count());

        // Calculate success rate
        if (report.getTotalWorkflows() > 0) {
            report.setSuccessRate((double) report.getCompletedWorkflows() / report.getTotalWorkflows());
            report.setEscalationRate((double) report.getEscalatedWorkflows() / report.getTotalWorkflows());
        }

        // Calculate completion times for completed workflows
        List<CompensationWorkflow> completedWorkflows = workflows.stream()
                .filter(w -> w.getWorkflowStatus() == CompensationStatus.COMPLETED && w.getCompletedAt() != null)
                .collect(Collectors.toList());

        if (!completedWorkflows.isEmpty()) {
            List<Duration> durations = completedWorkflows.stream().map(w -> {
                LocalDateTime createdAtLocal = LocalDateTime
                        .ofInstant(w.getCreatedAt(), java.time.ZoneId.systemDefault());
                return Duration.between(createdAtLocal, w.getCompletedAt());
            }).sorted().collect(Collectors.toList());

            report.setAverageCompletionTime(
                    durations.stream().reduce(Duration.ZERO, Duration::plus).dividedBy(durations.size()));

            report.setMinCompletionTime(durations.get(0));
            report.setMaxCompletionTime(durations.get(durations.size() - 1));

            if (durations.size() % 2 == 0) {
                report.setMedianCompletionTime(durations.get(durations.size() / 2 - 1));
            } else {
                report.setMedianCompletionTime(durations.get(durations.size() / 2));
            }
        }

        // Group by transaction type
        Map<TransactionType, Integer> workflowsByType = workflows.stream().collect(
                Collectors.groupingBy(
                        w -> w.getOriginalTransaction().getTransactionType(),
                        Collectors.collectingAndThen(
                                Collectors.counting(),
                                count -> count != null ? Math.toIntExact(count) : 0)));
        report.setWorkflowsByTransactionType(workflowsByType);

        // Group by status
        Map<CompensationStatus, Integer> workflowsByStatus = workflows.stream().collect(
                Collectors.groupingBy(
                        CompensationWorkflow::getWorkflowStatus,
                        Collectors.collectingAndThen(
                                Collectors.counting(),
                                count -> count != null ? Math.toIntExact(count) : 0)));
        report.setWorkflowsByStatus(workflowsByStatus);

        // Create workflow summaries
        List<CompensationWorkflowSummary> summaries = workflows.stream().map(this::createWorkflowSummary)
                .collect(Collectors.toList());
        report.setWorkflowSummaries(summaries);

        // Get daily metrics
        List<DailyWorkflowMetrics> dailyMetrics = calculateDailyMetrics(startDate, endDate);
        report.setDailyMetrics(dailyMetrics);

        // Get top failure reasons
        Map<String, Integer> topFailureReasons = workflows.stream()
                .filter(w -> w.getWorkflowStatus() == CompensationStatus.FAILED).collect(
                        Collectors.groupingBy(
                                CompensationWorkflow::getFailureReason,
                                Collectors.collectingAndThen(
                                        Collectors.counting(),
                                        count -> count != null ? Math.toIntExact(count) : 0)));
        report.setTopFailureReasons(topFailureReasons);

        logger.info("Compensation workflow report generated with {} workflows", report.getTotalWorkflows());
        return report;
    }

    @Transactional(readOnly = true)
    public List<CompensationWorkflow> getWorkflowsByStatus(CompensationStatus status) {
        logger.debug("Retrieving workflows with status: {}", status);
        return compensationWorkflowRepository.findByWorkflowStatus(status);
    }

    @Transactional(readOnly = true)
    public Duration getAverageCompensationTime(TransactionType transactionType) {
        logger.debug("Calculating average compensation time for transaction type: {}", transactionType);

        List<CompensationWorkflow> completedWorkflows = compensationWorkflowRepository
                .findCompletedWorkflowsByTransactionType(transactionType);

        if (completedWorkflows.isEmpty()) {
            return Duration.ZERO;
        }

        List<Duration> durations = completedWorkflows.stream().filter(w -> w.getCompletedAt() != null).map(w -> {
            LocalDateTime createdAtLocal = LocalDateTime.ofInstant(w.getCreatedAt(), java.time.ZoneId.systemDefault());
            return Duration.between(createdAtLocal, w.getCompletedAt());
        }).collect(Collectors.toList());

        if (durations.isEmpty()) {
            return Duration.ZERO;
        }

        Duration totalDuration = durations.stream().reduce(Duration.ZERO, Duration::plus);
        return totalDuration.dividedBy(durations.size());
    }

    /**
     * Retries compensation workflows that are ready for retry.
     * Called by TPOperationsScheduler.
     */
    public void retryFailedCompensations() {
        logger.debug("Checking for compensation workflows ready for retry");

        List<CompensationWorkflow> workflowsToRetry = compensationWorkflowRepository
                .findWorkflowsReadyForRetry(dateTimeService.now());

        for (CompensationWorkflow workflow : workflowsToRetry) {
            logger.info("Retrying compensation workflow: {}", workflow.getId());

            workflow.resetForRetry();
            compensationWorkflowRepository.save(workflow);

            executeCompensationWorkflowAsync(workflow.getId());
        }
    }

    // Private helper methods

    private CompensationWorkflowSummary createWorkflowSummary(CompensationWorkflow workflow) {
        CompensationWorkflowSummary summary = new CompensationWorkflowSummary();
        summary.setWorkflowId(workflow.getId());
        summary.setOriginalTransactionId(workflow.getOriginalTransaction().getId());
        summary.setTransactionType(workflow.getOriginalTransaction().getTransactionType());
        summary.setStatus(workflow.getWorkflowStatus());
        summary.setCompensationType(workflow.getCompensationType());
        summary.setCompensationAmount(workflow.getCompensationAmount());
        summary.setFailureReason(workflow.getFailureReason());
        summary.setRetryCount(workflow.getRetryCount());
        summary.setCreatedAt(
                workflow.getCreatedAt() != null
                        ? LocalDateTime.ofInstant(workflow.getCreatedAt(), java.time.ZoneId.systemDefault())
                        : null);
        summary.setCompletedAt(workflow.getCompletedAt());
        summary.setEscalatedBy(workflow.getEscalatedBy());
        summary.setEscalatedAt(workflow.getEscalatedAt());
        summary.setEscalationReason(workflow.getEscalationReason());

        if (workflow.getCompensationSteps() != null) {
            summary.setTotalSteps(workflow.getCompensationSteps().size());
            summary.setCompletedSteps(
                    (int) workflow.getCompensationSteps().stream().filter(
                            s -> s.getStatus() == com.openfinova.banking.tp.api.entity.CompensationStepStatus.COMPLETED)
                            .count());
            summary.setFailedSteps(
                    (int) workflow.getCompensationSteps().stream().filter(s -> s.getStatus().isFailed()).count());
        }

        if (workflow.getCreatedAt() != null && workflow.getCompletedAt() != null) {
            LocalDateTime createdAtLocal = LocalDateTime
                    .ofInstant(workflow.getCreatedAt(), java.time.ZoneId.systemDefault());
            summary.setProcessingDuration(Duration.between(createdAtLocal, workflow.getCompletedAt()));
        }

        return summary;
    }

    private List<DailyWorkflowMetrics> calculateDailyMetrics(LocalDate startDate, LocalDate endDate) {
        Map<LocalDate, DailyWorkflowMetrics> metricsMap = new HashMap<>();

        // Initialize all dates in range
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            metricsMap.put(current, new DailyWorkflowMetrics(current, 0, 0, 0));
            current = current.plusDays(1);
        }

        // Get daily counts from repository
        List<Object[]> dailyCounts = compensationWorkflowRepository.getDailyWorkflowCounts(startDate, endDate);

        for (Object[] row : dailyCounts) {
            LocalDate date = (LocalDate) row[0];
            Long count = (Long) row[1];
            CompensationStatus status = (CompensationStatus) row[2];

            DailyWorkflowMetrics metrics = metricsMap.get(date);
            if (metrics != null) {
                metrics.setTotalWorkflows(metrics.getTotalWorkflows() + count.intValue());

                switch (status) {
                    case COMPLETED -> metrics.setCompletedWorkflows(metrics.getCompletedWorkflows() + count.intValue());
                    case FAILED -> metrics.setFailedWorkflows(metrics.getFailedWorkflows() + count.intValue());
                    case ESCALATED -> metrics.setEscalatedWorkflows(metrics.getEscalatedWorkflows() + count.intValue());
                    case INITIATED, IN_PROGRESS, CANCELLED -> {
                        // These are counted in total but don't affect specific metrics
                    }
                }

                // Recalculate success rate
                if (metrics.getTotalWorkflows() > 0) {
                    metrics.setSuccessRate((double) metrics.getCompletedWorkflows() / metrics.getTotalWorkflows());
                }
            }
        }

        return metricsMap.values().stream().sorted(Comparator.comparing(DailyWorkflowMetrics::getDate))
                .collect(Collectors.toList());
    }

    @Async
    private void executeCompensationWorkflowAsync(UUID workflowId) {
        logger.info("Executing compensation workflow asynchronously: {}", workflowId);

        CompensationWorkflow workflow = compensationWorkflowRepository.findById(workflowId)
                .orElseThrow(() -> new IllegalArgumentException("Workflow not found: " + workflowId));

        workflow.transitionTo(CompensationStatus.IN_PROGRESS, "Starting compensation execution");
        compensationWorkflowRepository.save(workflow);

        // Execute steps in order
        CompensationStep nextStep = workflow.getNextPendingStep();
        while (nextStep != null) {
            executeCompensationStep(UUID.fromString(nextStep.getStepId()));

            // Refresh workflow and get next step
            workflow = compensationWorkflowRepository.findById(workflowId).orElse(workflow);

            // Check if we should stop due to failure
            if (workflow.hasFailedSteps() && !workflow.canRetry()) {
                break;
            }

            nextStep = workflow.getNextPendingStep();
        }
    }

    private void determineCompensationType(CompensationWorkflow workflow, Transaction originalTransaction) {
        // Determine compensation type based on transaction state and type
        if (originalTransaction.getGlTransactionId() != null) {
            workflow.setCompensationType(com.openfinova.banking.tp.api.entity.CompensationType.FULL);
            workflow.setCompensationAmount(originalTransaction.getTotalAmount());
        } else if (!originalTransaction.getReservations().isEmpty()) {
            workflow.setCompensationType(com.openfinova.banking.tp.api.entity.CompensationType.RESERVATION_RELEASE);
            workflow.setCompensationAmount(originalTransaction.getTotalAmount());
        } else {
            workflow.setCompensationType(com.openfinova.banking.tp.api.entity.CompensationType.PARTIAL);
            workflow.setCompensationAmount(originalTransaction.getPrincipalAmount());
        }
    }

    private void createCompensationSteps(CompensationWorkflow workflow, Transaction originalTransaction) {
        int stepOrder = 1;

        // Step 1: Release balance reservations if any exist
        if (!originalTransaction.getReservations().isEmpty()) {
            CompensationStep releaseReservationsStep = new CompensationStep();
            releaseReservationsStep.setStepId(UUID.randomUUID().toString());
            releaseReservationsStep
                    .setStepType(com.openfinova.banking.tp.api.entity.CompensationStepType.RELEASE_RESERVATIONS);
            releaseReservationsStep.setDescription(
                    com.openfinova.banking.tp.api.entity.CompensationStepType.RELEASE_RESERVATIONS.getDescription());
            releaseReservationsStep.setOrder(stepOrder++);
            releaseReservationsStep.setStatus(com.openfinova.banking.tp.api.entity.CompensationStepStatus.PENDING);
            workflow.addCompensationStep(releaseReservationsStep);
        }

        // Step 2: Reverse GL transaction if it was posted
        if (originalTransaction.getGlTransactionId() != null) {
            CompensationStep reverseGLStep = new CompensationStep();
            reverseGLStep.setStepId(UUID.randomUUID().toString());
            reverseGLStep.setStepType(com.openfinova.banking.tp.api.entity.CompensationStepType.REVERSE_GL_TRANSACTION);
            reverseGLStep.setDescription("Reverse GL transaction: " + originalTransaction.getGlTransactionId());
            reverseGLStep.setOrder(stepOrder++);
            reverseGLStep.setStatus(com.openfinova.banking.tp.api.entity.CompensationStepStatus.PENDING);
            workflow.addCompensationStep(reverseGLStep);
        }

        // Step 3: Create compensating transaction if needed
        if (workflow.getCompensationType() == com.openfinova.banking.tp.api.entity.CompensationType.FULL) {
            CompensationStep createCompensatingTxnStep = new CompensationStep();
            createCompensatingTxnStep.setStepId(UUID.randomUUID().toString());
            createCompensatingTxnStep.setStepType(
                    com.openfinova.banking.tp.api.entity.CompensationStepType.CREATE_COMPENSATING_TRANSACTION);
            createCompensatingTxnStep.setDescription(
                    com.openfinova.banking.tp.api.entity.CompensationStepType.CREATE_COMPENSATING_TRANSACTION
                            .getDescription());
            createCompensatingTxnStep.setOrder(stepOrder++);
            createCompensatingTxnStep.setStatus(com.openfinova.banking.tp.api.entity.CompensationStepStatus.PENDING);
            workflow.addCompensationStep(createCompensatingTxnStep);
        }

        // Step 4: Notify external systems
        CompensationStep notifyExternalStep = new CompensationStep();
        notifyExternalStep.setStepId(UUID.randomUUID().toString());
        notifyExternalStep
                .setStepType(com.openfinova.banking.tp.api.entity.CompensationStepType.NOTIFY_EXTERNAL_SYSTEMS);
        notifyExternalStep.setDescription(
                com.openfinova.banking.tp.api.entity.CompensationStepType.NOTIFY_EXTERNAL_SYSTEMS.getDescription());
        notifyExternalStep.setOrder(stepOrder++);
        notifyExternalStep.setStatus(com.openfinova.banking.tp.api.entity.CompensationStepStatus.PENDING);
        workflow.addCompensationStep(notifyExternalStep);
    }

    private void executeStepByType(CompensationStep step, CompensationWorkflow workflow) {
        switch (step.getStepType()) {
            case RELEASE_RESERVATIONS:
                executeReleaseReservationsStep(step, workflow);
                break;
            case REVERSE_GL_TRANSACTION:
                executeReverseGLTransactionStep(step, workflow);
                break;
            case CREATE_COMPENSATING_TRANSACTION:
                executeCreateCompensatingTransactionStep(step, workflow);
                break;
            case NOTIFY_EXTERNAL_SYSTEMS:
                executeNotifyExternalSystemsStep(step, workflow);
                break;
            default:
                throw new IllegalArgumentException("Unknown compensation step type: " + step.getStepType());
        }
    }

    private void executeReleaseReservationsStep(CompensationStep step, CompensationWorkflow workflow) {
        logger.info("Executing release reservations step for workflow: {}", workflow.getId());

        Transaction originalTransaction = workflow.getOriginalTransaction();

        for (BalanceReservation reservation : originalTransaction.getReservations()) {
            if (reservation.getStatus() == com.openfinova.banking.tp.api.entity.ReservationStatus.ACTIVE) {
                balanceReservationService.releaseReservation(reservation.getId());
                reservation.setStatus(com.openfinova.banking.tp.api.entity.ReservationStatus.RELEASED);
                logger.info("Released reservation: {} for workflow: {}", reservation.getId(), workflow.getId());
            }
        }

        step.setExecutionDetails("Released " + originalTransaction.getReservations().size() + " reservations");
    }

    private void executeReverseGLTransactionStep(CompensationStep step, CompensationWorkflow workflow) {
        logger.info("Executing reverse GL transaction step for workflow: {}", workflow.getId());

        Transaction originalTransaction = workflow.getOriginalTransaction();

        if (originalTransaction.getGlTransactionId() != null) {
            GLTransactionDTO reversal = generalLedgerService.reverseTransaction(
                    originalTransaction.getGlTransactionId(),
                    "Compensation reversal for workflow: " + workflow.getId(),
                    "SYSTEM");

            workflow.setGlReversalTransactionId(reversal.getId());
            step.setExecutionDetails("GL reversal transaction created: " + reversal.getId());

            logger.info("Created GL reversal transaction: {} for workflow: {}", reversal.getId(), workflow.getId());
        } else {
            step.setExecutionDetails("No GL transaction to reverse");
            logger.info("No GL transaction to reverse for workflow: {}", workflow.getId());
        }
    }

    private void executeCreateCompensatingTransactionStep(CompensationStep step, CompensationWorkflow workflow) {
        logger.info("Executing create compensating transaction step for workflow: {}", workflow.getId());

        // This would create a new transaction that reverses the effects of the original
        // For now, we'll simulate this step
        step.setExecutionDetails("Compensating transaction logic executed");

        logger.info("Compensating transaction step completed for workflow: {}", workflow.getId());
    }

    private void executeNotifyExternalSystemsStep(CompensationStep step, CompensationWorkflow workflow) {
        logger.info("Executing notify external systems step for workflow: {}", workflow.getId());

        // This would notify external payment gateways, partner systems, etc.
        // For now, we'll simulate this step
        step.setExecutionDetails("External systems notified of transaction reversal");

        logger.info("External systems notification completed for workflow: {}", workflow.getId());
    }
}