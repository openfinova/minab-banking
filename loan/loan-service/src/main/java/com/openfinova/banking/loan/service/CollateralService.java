package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.loan.api.entity.CollateralStatus;
import com.openfinova.banking.loan.api.entity.CollateralType;
import com.openfinova.banking.loan.entity.Collateral;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.repository.CollateralRepository;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.setup.api.DateTimeService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of CollateralService for managing loan collateral.
 *
 * This service handles:
 * - Collateral registration and tracking
 * - Valuation updates and revaluations
 * - Status transitions with validation
 * - Collateral release and liquidation
 * - Loan-to-value (LTV) ratio calculations
 * - Collateral coverage analysis
 *
 * State Transition Rules:
 * - ACTIVE → UNDER_VALUATION, RELEASED, LIQUIDATED
 * - UNDER_VALUATION → ACTIVE
 * - RELEASED → (terminal state)
 * - LIQUIDATED → (terminal state)
 */
@Service
@Transactional
public class CollateralService {

    private final CollateralRepository collateralRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final DateTimeService dateTimeService;

    public CollateralService(CollateralRepository collateralRepository, LoanAccountRepository loanAccountRepository,
            DateTimeService dateTimeService) {
        this.collateralRepository = collateralRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Registers new collateral for a loan account.
     *
     * This method:
     * - Generates a unique collateral reference
     * - Sets initial status to ACTIVE
     * - Persists the collateral record
     *
     * @param collateral the collateral entity to register (must not be null)
     * @param registeredBy the user who registered the collateral (must not be null or empty)
     * @return the registered collateral with generated reference
     * @throws IllegalArgumentException if collateral is null or registeredBy is null/empty
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Collateral registerCollateral(UUID loanAccountId, Collateral collateral, String registeredBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (collateral == null) {
            throw new IllegalArgumentException("Collateral cannot be null");
        }
        if (registeredBy == null || registeredBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Registered by cannot be null or empty");
        }

        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));
        collateral.setLoanAccount(loanAccount);
        collateral.setCollateralReference(generateCollateralReference());
        collateral.setStatus(CollateralStatus.ACTIVE);
        return collateralRepository.save(collateral);
    }

    /**
     * Retrieves collateral by its unique identifier.
     *
     * @param id the collateral ID
     * @return Optional containing the collateral if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Collateral> getCollateralById(UUID id) {
        return collateralRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Optional<Collateral> getCollateralForLoanAccount(UUID loanAccountId, UUID collateralId) {
        return collateralRepository.findById(collateralId)
                .filter(c -> c.getLoanAccount() != null && loanAccountId.equals(c.getLoanAccount().getId()));
    }

    /**
     * Retrieves all collateral associated with a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of all collateral for the loan account (all statuses)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public List<Collateral> getCollateralByLoanAccount(UUID loanAccountId) {
        return collateralRepository.findByLoanAccountId(loanAccountId);
    }

    /**
     * Retrieves only active collateral for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of active collateral for the loan account
     */
    @Transactional(readOnly = true)
    public List<Collateral> getActiveCollateralByLoanAccount(UUID loanAccountId) {
        return collateralRepository.findByLoanAccountIdAndStatus(loanAccountId, CollateralStatus.ACTIVE);
    }

    /**
     * Retrieves collateral by type with pagination.
     *
     * @param collateralType the type of collateral to filter by
     * @param pageable pagination parameters
     * @return page of collateral matching the specified type
     */
    @Transactional(readOnly = true)
    public Page<Collateral> getCollateralByType(CollateralType collateralType, Pageable pageable) {
        return collateralRepository.findByCollateralType(collateralType, pageable);
    }

    /**
     * Retrieves collateral by status with pagination.
     *
     * @param status the collateral status to filter by
     * @param pageable pagination parameters
     * @return page of collateral matching the specified status
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<Collateral> getCollateralByStatus(CollateralStatus status, Pageable pageable) {
        return collateralRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    public Page<Collateral> getCollateralByLoanAccountAndStatus(UUID loanAccountId, CollateralStatus status,
            Pageable pageable) {
        return collateralRepository.findByLoanAccount_IdAndStatus(loanAccountId, status, pageable);
    }

    /**
     * Updates the valuation of collateral.
     *
     * Valuation can only be updated for collateral in ACTIVE or UNDER_VALUATION status.
     * This ensures that released or liquidated collateral cannot be revalued.
     *
     * Validation rules:
     * - Collateral ID must not be null
     * - Valuation amount must not be null or negative
     * - Valuation date must not be null or in the future
     * - Valued by must not be null or empty
     * - Collateral must be in ACTIVE or UNDER_VALUATION status
     *
     * @param collateralId the ID of the collateral to update
     * @param valuationAmount the new valuation amount (must be non-negative)
     * @param valuationDate the date of valuation (must not be in the future)
     * @param valuedBy the person/entity who performed the valuation
     * @return the updated collateral
     * @throws IllegalArgumentException if any parameter is invalid
     * @throws IllegalStateException if collateral is not in ACTIVE or UNDER_VALUATION status
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Collateral updateValuation(UUID loanAccountId, UUID collateralId, BigDecimal valuationAmount,
            LocalDate valuationDate, String valuedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (collateralId == null) {
            throw new IllegalArgumentException("Collateral ID cannot be null");
        }
        if (valuationAmount == null) {
            throw new IllegalArgumentException("Valuation amount cannot be null");
        }
        if (valuationAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Valuation amount cannot be negative");
        }
        if (valuationDate == null) {
            throw new IllegalArgumentException("Valuation date cannot be null");
        }
        if (valuationDate.isAfter(dateTimeService.today())) {
            throw new IllegalArgumentException("Valuation date cannot be in the future");
        }
        if (valuedBy == null || valuedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Valued by cannot be null or empty");
        }

        Collateral collateral = collateralRepository.findById(collateralId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Collateral not found: %s", collateralId)));
        assertCollateralOnLoan(collateral, loanAccountId);

        // Only allow valuation updates for ACTIVE or UNDER_VALUATION collateral
        if (collateral.getStatus() != CollateralStatus.ACTIVE
                && collateral.getStatus() != CollateralStatus.UNDER_VALUATION) {
            throw new IllegalStateException(
                    String.format(
                            "Cannot update valuation for collateral in %s status. Only ACTIVE or UNDER_VALUATION collateral can be revalued",
                            collateral.getStatus()));
        }

        collateral.setValuationAmount(valuationAmount);
        collateral.setValuationDate(valuationDate);
        collateral.setValuedBy(valuedBy);

        return collateralRepository.save(collateral);
    }

    /**
     * Updates the status of collateral with state transition validation.
     *
     * This method validates that the status transition is allowed according
     * to the collateral state machine rules:
     * - ACTIVE → UNDER_VALUATION, RELEASED, LIQUIDATED
     * - UNDER_VALUATION → ACTIVE
     * - RELEASED → (no transitions allowed)
     * - LIQUIDATED → (no transitions allowed)
     *
     * @param collateralId the ID of the collateral to update
     * @param newStatus the new status to transition to
     * @param updatedBy the user performing the status update
     * @return the updated collateral
     * @throws IllegalArgumentException if any parameter is invalid or collateral not found
     * @throws IllegalStateException if the status transition is not allowed
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Collateral updateCollateralStatus(UUID loanAccountId, UUID collateralId, CollateralStatus newStatus,
            String updatedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (collateralId == null) {
            throw new IllegalArgumentException("Collateral ID cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        if (updatedBy == null || updatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated by cannot be null or empty");
        }

        Collateral collateral = collateralRepository.findById(collateralId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Collateral not found: %s", collateralId)));
        assertCollateralOnLoan(collateral, loanAccountId);

        // Validate state transition
        if (!collateral.canTransitionTo(newStatus)) {
            throw new IllegalStateException(collateral.getTransitionErrorMessage(newStatus));
        }

        collateral.setStatus(newStatus);
        return collateralRepository.save(collateral);
    }

    /**
     * Releases collateral back to the borrower.
     *
     * This is typically done after the loan is fully repaid. The collateral
     * status is changed to RELEASED and the release date is recorded.
     *
     * Only ACTIVE collateral can be released. Once released, the collateral
     * enters a terminal state and cannot be transitioned to any other status.
     *
     * @param collateralId the ID of the collateral to release
     * @param releasedBy the user who authorized the release
     * @return the released collateral with updated status and release date
     * @throws IllegalArgumentException if collateralId or releasedBy is invalid
     * @throws IllegalStateException if collateral is not in ACTIVE status
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Collateral releaseCollateral(UUID loanAccountId, UUID collateralId, String releasedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (collateralId == null) {
            throw new IllegalArgumentException("Collateral ID cannot be null");
        }
        if (releasedBy == null || releasedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Released by cannot be null or empty");
        }

        Collateral collateral = collateralRepository.findById(collateralId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Collateral not found: %s", collateralId)));
        assertCollateralOnLoan(collateral, loanAccountId);

        // Validate state transition
        if (!collateral.canTransitionTo(CollateralStatus.RELEASED)) {
            throw new IllegalStateException(collateral.getTransitionErrorMessage(CollateralStatus.RELEASED));
        }

        collateral.setStatus(CollateralStatus.RELEASED);
        collateral.setReleaseDate(dateTimeService.today());

        return collateralRepository.save(collateral);
    }

    /**
     * Liquidates collateral to recover loan amount after default.
     *
     * This method is used when a loan defaults and the collateral needs to be
     * sold to recover the outstanding amount. The collateral status is changed to
     * LIQUIDATED and the valuation is updated to the liquidation amount.
     *
     * Only ACTIVE collateral can be liquidated. Once liquidated, the collateral
     * enters a terminal state and cannot be transitioned to any other status.
     *
     * @param collateralId the ID of the collateral to liquidate
     * @param liquidationAmount the amount realized from liquidation (must be non-negative)
     * @param liquidatedBy the user who authorized the liquidation
     * @return the liquidated collateral with updated status and valuation
     * @throws IllegalArgumentException if any parameter is invalid or liquidationAmount is negative
     * @throws IllegalStateException if collateral is not in ACTIVE status
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Collateral liquidateCollateral(UUID loanAccountId, UUID collateralId, BigDecimal liquidationAmount,
            String liquidatedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (collateralId == null) {
            throw new IllegalArgumentException("Collateral ID cannot be null");
        }
        if (liquidationAmount == null) {
            throw new IllegalArgumentException("Liquidation amount cannot be null");
        }
        if (liquidationAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw new IllegalArgumentException("Liquidation amount cannot be negative");
        }
        if (liquidatedBy == null || liquidatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Liquidated by cannot be null or empty");
        }

        Collateral collateral = collateralRepository.findById(collateralId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Collateral not found: %s", collateralId)));
        assertCollateralOnLoan(collateral, loanAccountId);

        // Validate state transition
        if (!collateral.canTransitionTo(CollateralStatus.LIQUIDATED)) {
            throw new IllegalStateException(collateral.getTransitionErrorMessage(CollateralStatus.LIQUIDATED));
        }

        collateral.setStatus(CollateralStatus.LIQUIDATED);
        collateral.setValuationAmount(liquidationAmount);

        return collateralRepository.save(collateral);
    }

    private static void assertCollateralOnLoan(Collateral collateral, UUID loanAccountId) {
        if (collateral.getLoanAccount() == null || !loanAccountId.equals(collateral.getLoanAccount().getId())) {
            throw new IllegalArgumentException("Collateral does not belong to this loan account");
        }
    }

    /**
     * Calculates the total value of all active collateral for a loan account.
     *
     * This sums the valuation amounts of all collateral associated with
     * the loan account, regardless of status.
     *
     * @param loanAccountId the loan account ID
     * @return the total collateral value, or zero if no collateral exists
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalCollateralValue(UUID loanAccountId) {
        return collateralRepository.sumCollateralValueByLoanAccount(loanAccountId);
    }

    /**
     * Calculates the Loan-to-Value (LTV) ratio for a loan account.
     *
     * The LTV ratio is calculated as:
     * LTV = (Outstanding Principal / Total Collateral Value) × 100
     *
     * A lower LTV indicates better collateral coverage. For example:
     * - LTV of 50% means collateral is worth twice the loan amount
     * - LTV of 100% means collateral equals the loan amount
     * - LTV > 100% means loan is under-collateralized
     *
     * @param loanAccountId the loan account ID
     * @return the LTV ratio as a percentage (e.g., 75.5000 for 75.5%), or zero if no collateral
     * @throws IllegalArgumentException if loan account not found
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateLoanToValueRatio(UUID loanAccountId) {
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));

        BigDecimal totalCollateralValue = calculateTotalCollateralValue(loanAccountId);

        if (totalCollateralValue.compareTo(BigDecimal.ZERO) == 0) {
            return BigDecimal.ZERO;
        }

        return loanAccount.getOutstandingPrincipal().divide(totalCollateralValue, 4, RoundingMode.HALF_UP)
                .multiply(BigDecimal.valueOf(100));
    }

    /**
     * Checks if a loan account has sufficient collateral coverage.
     *
     * This method compares the loan's LTV ratio against a required coverage ratio.
     * The loan has sufficient collateral if LTV ≤ required coverage ratio.
     *
     * Example: If requiredCoverageRatio is 80%, the loan must have LTV ≤ 80%
     * to be considered sufficiently collateralized.
     *
     * @param loanAccountId the loan account ID
     * @param requiredCoverageRatio the maximum acceptable LTV ratio (e.g., 80.00 for 80%)
     * @return true if LTV is at or below the required ratio, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasSufficientCollateral(UUID loanAccountId, BigDecimal requiredCoverageRatio) {
        BigDecimal ltv = calculateLoanToValueRatio(loanAccountId);
        return ltv.compareTo(requiredCoverageRatio) <= 0;
    }

    /**
     * Counts the total number of collateral items for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return the count of collateral items (all statuses)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public long countCollateralByLoanAccount(UUID loanAccountId) {
        return collateralRepository.countByLoanAccountId(loanAccountId);
    }

    /**
     * Checks if a loan account has any active collateral.
     *
     * @param loanAccountId the loan account ID
     * @return true if at least one active collateral exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasActiveCollateral(UUID loanAccountId) {
        return collateralRepository.hasActiveCollateral(loanAccountId);
    }

    /**
     * Generates a unique collateral reference number.
     *
     * Format: COL-{timestamp}
     *
     * @return a unique collateral reference string
     */
    private String generateCollateralReference() {
        return "COL-" + System.currentTimeMillis();
    }
}
