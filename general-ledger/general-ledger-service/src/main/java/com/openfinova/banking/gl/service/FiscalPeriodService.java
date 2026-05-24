package com.openfinova.banking.gl.service;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.gl.api.dto.CreateFiscalPeriodRequest;
import com.openfinova.banking.gl.api.entity.FiscalPeriodStatus;
import com.openfinova.banking.gl.api.entity.GLAuditAction;
import com.openfinova.banking.gl.api.entity.GLEntityType;
import com.openfinova.banking.gl.entity.FiscalPeriod;
import com.openfinova.banking.gl.mapper.FiscalPeriodMapper;
import com.openfinova.banking.gl.repository.FiscalPeriodRepository;

/**
* Service implementation for managing fiscal periods in the general ledger system.
 *
 * This service handles the lifecycle of fiscal periods including:
 * - Finding active periods for specific dates
 * - Opening new fiscal periods with validation
 * - Closing periods with balance consistency checks
 * - Creating balance snapshots during period closure
 */
@Service
@Transactional
public class FiscalPeriodService {

    private static final Logger logger = LoggerFactory.getLogger(FiscalPeriodService.class);

    private final FiscalPeriodRepository fiscalPeriodRepository;
    private final AuditService auditService;
    private final FiscalPeriodMapper fiscalPeriodMapper;

    public FiscalPeriodService(FiscalPeriodRepository fiscalPeriodRepository, AuditService auditService,
            FiscalPeriodMapper fiscalPeriodMapper) {
        this.fiscalPeriodRepository = fiscalPeriodRepository;
        this.auditService = auditService;
        this.fiscalPeriodMapper = fiscalPeriodMapper;
    }

    /**
     * Retrieves all fiscal periods in the system.
     *
     * @return a list of all fiscal periods ordered by start date
     */
    public List<FiscalPeriod> getAllFiscalPeriods() {
        logger.debug("Getting all fiscal periods");
        return fiscalPeriodRepository.findAllByOrderByStartDateAscFiscalYearAscPeriodNumberAsc();
    }

    /**
     * Finds the active fiscal period for a given date.
     *
     * @param date the date to find the active period for
     * @return Optional containing the active fiscal period, or empty if none found
     */
    @Transactional(readOnly = true)
    public Optional<FiscalPeriod> findActivePeriod(LocalDate date) {
        logger.debug("Finding active fiscal period for date: {}", date);
        return fiscalPeriodRepository.findActivePeriodForDate(date);
    }

    /**
     * Retrieves fiscal periods filtered by their status.
     *
     * @param status the fiscal period status to filter by
     * @return a list of fiscal periods with the specified status
     */
    public List<FiscalPeriod> getFiscalPeriodsByStatus(FiscalPeriodStatus status) {
        logger.debug("Getting fiscal periods by status: {}", status);
        return fiscalPeriodRepository.findByStatus(status);
    }

    /**
     * Retrieves the fiscal period that contains a specific date.
     *
     * @param date the date to find the fiscal period for
     * @return an Optional containing the fiscal period if found
     */
    public Optional<FiscalPeriod> getFiscalPeriodForDate(LocalDate date) {
        logger.debug("Getting fiscal period for date: {}", date);
        return fiscalPeriodRepository.findActivePeriodForDate(date);
    }

    public Optional<FiscalPeriod> getFiscalPeriodById(UUID id) {
        logger.debug("Getting fiscal period by ID: {}", id);
        return fiscalPeriodRepository.findById(id);
    }

    /**
     * Returns all fiscal periods for a given fiscal year, ordered by period number.
     * This is the primary method for regulatory reporting and year-end processing;
     * it hits the {@code fiscal_year} index directly instead of scanning dates.
     *
     * @param fiscalYear the fiscal year (e.g. 2024)
     * @return periods belonging to that year, sorted by periodNumber ascending
     */
    @Transactional(readOnly = true)
    public List<FiscalPeriod> getFiscalPeriodsByYear(int fiscalYear) {
        logger.debug("Getting fiscal periods for year: {}", fiscalYear);
        return fiscalPeriodRepository.findByFiscalYearOrderByPeriodNumberAsc(fiscalYear);
    }

    /**
     * Looks up a specific period by its natural key (fiscal year + period number).
     * Useful for period-specific regulatory extracts and bulk import validation.
     *
     * @param fiscalYear   the fiscal year (e.g. 2024)
     * @param periodNumber the period number within the year (1–13)
     * @return the matching period, or empty if not found
     */
    @Transactional(readOnly = true)
    public Optional<FiscalPeriod> getFiscalPeriod(int fiscalYear, int periodNumber) {
        logger.debug("Getting fiscal period for year: {}, period: {}", fiscalYear, periodNumber);
        return fiscalPeriodRepository.findByFiscalYearAndPeriodNumber(fiscalYear, periodNumber);
    }

    /**
     * Marks a fiscal period as closed and records the audit event.
     *
     * <p>This is a thin domain operation; all pre-close orchestration
     * (revaluation, closing entries, balance validation) is performed by
     * {@link FiscalPeriodWorkflowService#closePeriod} before this method is called.
     *
     * @param periodId      the UUID of the fiscal period to mark closed
     * @param closedBy      the user performing the close
     * @param reason        the business reason for the close
     * @param oldValues     snapshot of the period state before closing (for audit)
     * @param correlationId links all operations that belong to this close event
     * @throws IllegalArgumentException if the fiscal period does not exist
     */
    public void markClosed(UUID periodId, String closedBy, String reason, Map<String, Object> oldValues,
            UUID correlationId) {
        FiscalPeriod period = fiscalPeriodRepository.findById(periodId)
                .orElseThrow(() -> new IllegalArgumentException("Fiscal period not found: " + periodId));

        period.close(closedBy);
        fiscalPeriodRepository.save(period);

        Map<String, Object> newValues = Map.of(
                "status",
                period.getStatus().toString(),
                "closedAt",
                period.getClosedAt() != null ? period.getClosedAt().toString() : "",
                "closedBy",
                closedBy);

        auditService.logAudit(
                GLEntityType.FISCAL_PERIOD,
                periodId,
                GLAuditAction.PERIOD_CLOSE,
                closedBy,
                oldValues,
                newValues,
                reason,
                null,
                null,
                correlationId,
                null,
                null);
        logger.info("Fiscal period marked as closed: {}", periodId);
    }

    /**
     * Creates (opens) a new fiscal period from a validated request DTO.
     *
     * <p>This is the primary entry point for the HTTP layer. It delegates to
     * {@link #openPeriod(FiscalPeriod)} after mapping the DTO to an entity.
     *
     * @param request the validated creation request
     * @return the persisted fiscal period with generated ID and OPEN status
     * @throws IllegalArgumentException if dates are invalid or overlap with an existing period
     */
    public FiscalPeriod createFiscalPeriod(CreateFiscalPeriodRequest request) {
        logger.info(
                "Creating fiscal period '{}' ({}/{}) from {} to {}",
                request.getName(),
                request.getFiscalYear(),
                request.getPeriodNumber(),
                request.getStartDate(),
                request.getEndDate());
        FiscalPeriod period = fiscalPeriodMapper.toEntity(request);
        return openPeriod(period);
    }

    /**
     * Opens a new fiscal period after performing validation checks.
     *
     * This method performs the following operations:
     * 1. Validates that the period dates don't overlap with existing periods
     * 2. Ensures the start date is before the end date
     * 3. Sets the period status to OPEN
     * 4. Persists the period to the database
     *
     * @param period the fiscal period to open
     * @return the saved fiscal period with generated ID
     * @throws IllegalArgumentException if the period dates are invalid or overlap with existing periods
     */
    public FiscalPeriod openPeriod(FiscalPeriod period) {
        logger.info("Opening new fiscal period: {}", period.getName());

        // Validate that the period doesn't overlap with existing periods
        validatePeriodDates(period);

        // Set status to open
        period.setStatus(FiscalPeriodStatus.OPEN);

        FiscalPeriod savedPeriod = fiscalPeriodRepository.save(period);
        logger.info("Fiscal period opened successfully: {}", savedPeriod.getId());

        return savedPeriod;
    }

    /**
     * Reopens a closed fiscal period, allowing postings again.
     * HIGH RISK operation requiring mandatory reason for regulatory compliance.
     *
     * @param periodId the UUID of the fiscal period to reopen
     * @param reopenedBy the user reopening the period
     * @param reason mandatory business justification for reopening (min 10 chars)
     * @return the updated fiscal period with open status
     * @throws IllegalArgumentException if the period is not found or reason is insufficient
     * @throws IllegalStateException if the period is locked
     */
    public FiscalPeriod reopenFiscalPeriod(UUID periodId, String reopenedBy, String reason) {
        if (reopenedBy == null || reopenedBy.isBlank()) {
            throw new IllegalArgumentException("reopenedBy is required when reopening a fiscal period");
        }
        if (reason == null || reason.isBlank()) {
            throw new IllegalArgumentException("A reason is mandatory when reopening a fiscal period");
        }
        if (reason.strip().length() < 10) {
            throw new IllegalArgumentException(
                    "Reopen reason must be at least 10 characters for regulatory compliance");
        }
        logger.info("Reopening fiscal period: {} by {}", periodId, reopenedBy);

        Optional<FiscalPeriod> periodOpt = fiscalPeriodRepository.findById(periodId);
        if (periodOpt.isEmpty()) {
            throw new ResourceNotFoundException("FiscalPeriod", periodId);
        }

        FiscalPeriod period = periodOpt.get();

        if (period.getStatus() == FiscalPeriodStatus.LOCKED) {
            throw new IllegalStateException("Cannot reopen locked fiscal period: " + periodId);
        }

        // Capture old status for audit
        Map<String, Object> oldValues = Map.of(
                "status",
                period.getStatus().toString(),
                "closedAt",
                period.getClosedAt() != null ? period.getClosedAt().toString() : "",
                "closedBy",
                period.getClosedBy() != null ? period.getClosedBy() : "");

        period.reopen(reopenedBy);
        FiscalPeriod updatedPeriod = fiscalPeriodRepository.save(period);

        logger.info("Successfully reopened fiscal period: {}", periodId);

        // Audit log: period reopen (HIGH RISK - mandatory reason)
        Map<String, Object> newValues = Map.of(
                "status",
                updatedPeriod.getStatus().toString(),
                "reopenedBy",
                reopenedBy,
                "reopenedAt",
                updatedPeriod.getReopenedAt().toString());
        auditService.logAudit(
                GLEntityType.FISCAL_PERIOD,
                periodId,
                GLAuditAction.PERIOD_REOPEN,
                reopenedBy,
                oldValues,
                newValues,
                reason, // Mandatory reason for PERIOD_REOPEN
                null, // no transaction amount
                null, // no currency
                null, // no correlation ID for single reopen
                null, // TODO: IP address from SecurityContext
                null // TODO: session ID from SecurityContext
        );

        return updatedPeriod;
    }

    /**
     * Validates that the new period doesn't overlap with existing periods.
     *
     * @param period the period to validate
     * @throws IllegalArgumentException if validation fails
     */
    private void validatePeriodDates(FiscalPeriod period) {
        // Require fiscalYear and periodNumber — they are the natural key used in
        // regulatory reports; without them the period cannot be queried by year.
        if (period.getFiscalYear() == null) {
            throw new IllegalArgumentException("Fiscal year is required when opening a period");
        }
        if (period.getPeriodNumber() == null) {
            throw new IllegalArgumentException("Period number is required when opening a period");
        }

        // Check for overlapping periods
        boolean hasOverlap = fiscalPeriodRepository.existsOverlappingPeriod(period.getStartDate(), period.getEndDate());

        if (hasOverlap) {
            throw new IllegalArgumentException(
                    "Fiscal period dates overlap with existing period: " + period.getStartDate() + " to "
                            + period.getEndDate());
        }

        // Validate that start date is before end date
        if (period.getStartDate().isAfter(period.getEndDate())) {
            throw new IllegalArgumentException("Fiscal period start date must be before end date");
        }
    }

    /**
     * Validates if posting is allowed for a specific date based on fiscal period rules.
     *
     * @param postingDate the date to validate for posting
     * @return true if posting is allowed, false if the period is closed or doesn't exist
     */
    public boolean isPostingAllowedForDate(LocalDate postingDate) {
        logger.debug("Checking if posting is allowed for date: {}", postingDate);

        Optional<FiscalPeriod> period = getFiscalPeriodForDate(postingDate);
        return period.isPresent() && period.get().isOpen();
    }

}
