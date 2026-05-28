package com.openfinova.banking.loan.service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.entity.GuarantorStatus;
import com.openfinova.banking.loan.api.entity.GuarantorType;
import com.openfinova.banking.loan.dto.GuarantorValidationResult;
import com.openfinova.banking.loan.entity.Guarantor;
import com.openfinova.banking.loan.entity.LoanAccount;
import com.openfinova.banking.loan.repository.GuarantorRepository;
import com.openfinova.banking.loan.repository.LoanAccountRepository;
import com.openfinova.banking.setup.api.DateTimeService;

/**
 * Implementation of GuarantorService for managing loan guarantors.
 *
 * This service handles:
 * - Adding and managing guarantors for loans
 * - Verifying guarantor eligibility and documentation
 * - Tracking guarantor status throughout loan lifecycle
 * - Releasing guarantors when loans are settled
 * - Validating guarantor requirements
 *
 * A guarantor is a person or entity that agrees to repay a loan if the
 * borrower defaults. Guarantors provide additional security for lenders
 * and can help borrowers qualify for loans they might not otherwise receive.
 *
 * Guarantor Lifecycle:
 * - PENDING: Guarantor added but not yet verified
 * - ACTIVE: Guarantor verified and actively guaranteeing the loan
 * - RELEASED: Guarantor released after loan settlement or other conditions
 * - REMOVED: Guarantor removed from the loan (e.g., replaced by another)
 *
 * @see GuarantorService
 * @see Guarantor
 * @see com.openfinova.banking.loan.api.entity.GuarantorStatus
 */
@Service
@Transactional
public class GuarantorService {

    private final GuarantorRepository guarantorRepository;
    private final LoanAccountRepository loanAccountRepository;
    private final DateTimeService dateTimeService;

    public GuarantorService(GuarantorRepository guarantorRepository, LoanAccountRepository loanAccountRepository,
            DateTimeService dateTimeService) {
        this.guarantorRepository = guarantorRepository;
        this.loanAccountRepository = loanAccountRepository;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Adds a new guarantor to a loan.
     *
     * The guarantor is initially set to PENDING status and must be verified
     * before becoming active. This allows time for documentation review and
     * eligibility verification.
     *
     * @param guarantor the guarantor entity to add
     * @param addedBy the user adding the guarantor
     * @return the saved guarantor with PENDING status
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Guarantor addGuarantor(UUID loanAccountId, Guarantor guarantor, String addedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        LoanAccount loanAccount = loanAccountRepository.findById(loanAccountId)
                .orElseThrow(() -> new IllegalArgumentException("Loan account not found: " + loanAccountId));
        guarantor.setLoanAccount(loanAccount);
        guarantor.setStatus(GuarantorStatus.PENDING);
        return guarantorRepository.save(guarantor);
    }

    /**
     * Retrieves a guarantor by its unique identifier.
     *
     * @param id the guarantor ID
     * @return Optional containing the guarantor if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<Guarantor> getGuarantorById(UUID id) {
        return guarantorRepository.findById(id);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Optional<Guarantor> getGuarantorForLoanAccount(UUID loanAccountId, UUID guarantorId) {
        return guarantorRepository.findById(guarantorId)
                .filter(g -> g.getLoanAccount() != null && loanAccountId.equals(g.getLoanAccount().getId()));
    }

    /**
     * Retrieves all guarantors for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return list of all guarantors for the loan account (all statuses)
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public List<Guarantor> getGuarantorsByLoanAccount(UUID loanAccountId) {
        return guarantorRepository.findByLoanAccountId(loanAccountId);
    }

    /**
     * Retrieves only active guarantors for a loan account.
     *
     * Active guarantors are those currently guaranteeing the loan.
     *
     * @param loanAccountId the loan account ID
     * @return list of active guarantors for the loan account
     */
    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public List<Guarantor> getActiveGuarantorsByLoanAccount(UUID loanAccountId) {
        return guarantorRepository.findByLoanAccountIdAndStatus(loanAccountId, GuarantorStatus.ACTIVE);
    }

    /**
     * Retrieves all guarantees provided by a customer with pagination.
     *
     * Shows all loans where the customer is acting as a guarantor.
     *
     * @param customerId the customer ID
     * @param pageable pagination parameters
     * @return page of guarantor records for the customer
     */
    @Transactional(readOnly = true)
    public Page<Guarantor> getGuarantorsByCustomer(UUID customerId, Pageable pageable) {
        return guarantorRepository.findByCustomerId(customerId, pageable);
    }

    /**
     * Retrieves active guarantees provided by a customer.
     *
     * Shows all loans where the customer is currently an active guarantor.
     * Useful for assessing a customer's total guarantee exposure.
     *
     * @param customerId the customer ID
     * @return list of active guarantor records for the customer
     */
    @Transactional(readOnly = true)
    public List<Guarantor> getActiveGuaranteesByCustomer(UUID customerId) {
        return guarantorRepository.findByCustomerIdAndStatus(customerId, GuarantorStatus.ACTIVE);
    }

    /**
     * Retrieves guarantors by type with pagination.
     *
     * @param guarantorType the type of guarantor to filter by
     * @param pageable pagination parameters
     * @return page of guarantors matching the specified type
     */
    @Transactional(readOnly = true)
    public Page<Guarantor> getGuarantorsByType(GuarantorType guarantorType, Pageable pageable) {
        return guarantorRepository.findByGuarantorType(guarantorType, pageable);
    }

    /**
     * Retrieves guarantors by status with pagination.
     *
     * @param status the guarantor status to filter by
     * @param pageable pagination parameters
     * @return page of guarantors matching the specified status
     */
    @Transactional(readOnly = true)
    public Page<Guarantor> getGuarantorsByStatus(GuarantorStatus status, Pageable pageable) {
        return guarantorRepository.findByStatus(status, pageable);
    }

    @Transactional(readOnly = true)
    @PreAuthorize("hasAuthority('loan:read')")
    public Page<Guarantor> getGuarantorsByLoanAccountAndStatus(UUID loanAccountId, GuarantorStatus status,
            Pageable pageable) {
        return guarantorRepository.findByLoanAccount_IdAndStatus(loanAccountId, status, pageable);
    }

    /**
     * Updates the status of a guarantor with state transition validation.
     *
     * This method validates that the status transition is allowed according
     * to the guarantor state machine rules:
     * - PENDING → ACTIVE, REMOVED
     * - ACTIVE → INVOKED, RELEASED, REMOVED
     * - INVOKED → RELEASED
     * - RELEASED → (no transitions allowed)
     * - REMOVED → (no transitions allowed)
     *
     * @param guarantorId the ID of the guarantor to update
     * @param newStatus the new status to transition to
     * @param updatedBy the user performing the update
     * @return the updated guarantor
     * @throws IllegalArgumentException if guarantorId is null, newStatus is null, updatedBy is null/empty, or guarantor not found
     * @throws IllegalStateException if the status transition is not allowed
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Guarantor updateGuarantorStatus(UUID loanAccountId, UUID guarantorId, GuarantorStatus newStatus,
            String updatedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (guarantorId == null) {
            throw new IllegalArgumentException("Guarantor ID cannot be null");
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("New status cannot be null");
        }
        if (updatedBy == null || updatedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Updated by cannot be null or empty");
        }

        Guarantor guarantor = guarantorRepository.findById(guarantorId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Guarantor not found: %s", guarantorId)));

        // Validate state transition
        if (!guarantor.canTransitionTo(newStatus)) {
            throw new IllegalStateException(guarantor.getTransitionErrorMessage(newStatus));
        }

        guarantor.setStatus(newStatus);
        return guarantorRepository.save(guarantor);
    }

    /**
     * Verifies a guarantor and activates their guarantee.
     *
     * This method should be called after:
     * - Verifying guarantor identity and documentation
     * - Confirming guarantor's financial capacity
     * - Obtaining guarantor's signed agreement
     * - Completing any required legal formalities
     *
     * Once verified, the guarantor becomes ACTIVE and their guarantee is enforceable.
     * Only guarantors in PENDING status can be verified.
     *
     * @param guarantorId the ID of the guarantor to verify
     * @param verifiedBy the user verifying the guarantor
     * @return the verified guarantor with ACTIVE status
     * @throws IllegalArgumentException if guarantorId is null, verifiedBy is null/empty, or guarantor not found
     * @throws IllegalStateException if guarantor is not in PENDING status
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Guarantor verifyGuarantor(UUID loanAccountId, UUID guarantorId, String verifiedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (guarantorId == null) {
            throw new IllegalArgumentException("Guarantor ID cannot be null");
        }
        if (verifiedBy == null || verifiedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Verified by cannot be null or empty");
        }

        Guarantor guarantor = guarantorRepository.findById(guarantorId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Guarantor not found: %s", guarantorId)));
        assertGuarantorOnLoan(guarantor, loanAccountId);

        // Validate state transition
        if (!guarantor.canTransitionTo(GuarantorStatus.ACTIVE)) {
            throw new IllegalStateException(guarantor.getTransitionErrorMessage(GuarantorStatus.ACTIVE));
        }

        guarantor.setStatus(GuarantorStatus.ACTIVE);
        guarantor.setVerifiedDate(dateTimeService.instant());
        guarantor.setVerifiedBy(verifiedBy);
        return guarantorRepository.save(guarantor);
    }

    /**
     * Releases a guarantor from their guarantee obligation.
     *
     * Guarantors are typically released when:
     * - The loan is fully repaid
     * - The loan is settled early
     * - The guarantee period expires
     * - The guarantee is replaced by another form of security
     *
     * Once released, the guarantor is no longer liable for the loan.
     * Only guarantors in ACTIVE or INVOKED status can be released.
     *
     * @param guarantorId the ID of the guarantor to release
     * @param releasedBy the user releasing the guarantor
     * @return the released guarantor with RELEASED status
     * @throws IllegalArgumentException if guarantorId is null, releasedBy is null/empty, or guarantor not found
     * @throws IllegalStateException if guarantor cannot be released from current status
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public Guarantor releaseGuarantor(UUID loanAccountId, UUID guarantorId, String releasedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (guarantorId == null) {
            throw new IllegalArgumentException("Guarantor ID cannot be null");
        }
        if (releasedBy == null || releasedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Released by cannot be null or empty");
        }

        Guarantor guarantor = guarantorRepository.findById(guarantorId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Guarantor not found: %s", guarantorId)));
        assertGuarantorOnLoan(guarantor, loanAccountId);

        // Validate state transition
        if (!guarantor.canTransitionTo(GuarantorStatus.RELEASED)) {
            throw new IllegalStateException(guarantor.getTransitionErrorMessage(GuarantorStatus.RELEASED));
        }

        guarantor.setStatus(GuarantorStatus.RELEASED);
        guarantor.setReleasedDate(dateTimeService.instant());
        guarantor.setReleasedBy(releasedBy);
        return guarantorRepository.save(guarantor);
    }

    /**
     * Removes a guarantor from a loan.
     *
     * Guarantors may be removed when:
     * - They are replaced by another guarantor
     * - They no longer meet eligibility requirements
     * - They request to be removed (with lender approval)
     * - The loan structure changes
     *
     * Removed guarantors are no longer associated with the loan.
     * Guarantors can be removed from PENDING or ACTIVE status.
     *
     * @param guarantorId the ID of the guarantor to remove
     * @param reason the reason for removal
     * @param removedBy the user removing the guarantor
     * @throws IllegalArgumentException if guarantorId is null, reason is null/empty, removedBy is null/empty, or guarantor not found
     * @throws IllegalStateException if guarantor cannot be removed from current status
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public void removeGuarantor(UUID loanAccountId, UUID guarantorId, String reason, String removedBy) {
        if (loanAccountId == null) {
            throw new IllegalArgumentException("Loan account ID cannot be null");
        }
        if (guarantorId == null) {
            throw new IllegalArgumentException("Guarantor ID cannot be null");
        }
        if (reason == null || reason.trim().isEmpty()) {
            throw new IllegalArgumentException("Reason cannot be null or empty");
        }
        if (removedBy == null || removedBy.trim().isEmpty()) {
            throw new IllegalArgumentException("Removed by cannot be null or empty");
        }

        Guarantor guarantor = guarantorRepository.findById(guarantorId)
                .orElseThrow(() -> new IllegalArgumentException(String.format("Guarantor not found: %s", guarantorId)));
        assertGuarantorOnLoan(guarantor, loanAccountId);

        // Validate state transition
        if (!guarantor.canTransitionTo(GuarantorStatus.REMOVED)) {
            throw new IllegalStateException(guarantor.getTransitionErrorMessage(GuarantorStatus.REMOVED));
        }

        guarantor.setStatus(GuarantorStatus.REMOVED);
        guarantor.setRemovedDate(dateTimeService.instant());
        guarantor.setRemovedBy(removedBy);
        guarantor.setRemovalReason(reason);
        guarantor.setRemarks(reason);
        guarantorRepository.save(guarantor);
    }

    /**
     * Counts the total number of guarantors for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return the count of guarantors (all statuses)
     */
    @Transactional(readOnly = true)
    public long countGuarantorsByLoanAccount(UUID loanAccountId) {
        return guarantorRepository.countByLoanAccountId(loanAccountId);
    }

    /**
     * Counts the number of active guarantors for a loan account.
     *
     * @param loanAccountId the loan account ID
     * @return the count of active guarantors
     */
    @Transactional(readOnly = true)
    public long countActiveGuarantorsByLoanAccount(UUID loanAccountId) {
        return guarantorRepository.countByLoanAccountIdAndStatus(loanAccountId, GuarantorStatus.ACTIVE);
    }

    /**
     * Checks if a loan account has any active guarantors.
     *
     * @param loanAccountId the loan account ID
     * @return true if at least one active guarantor exists, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasActiveGuarantors(UUID loanAccountId) {
        return guarantorRepository.existsByLoanAccountIdAndStatus(loanAccountId, GuarantorStatus.ACTIVE);
    }

    /**
     * Checks if a loan account has the required number of guarantors.
     *
     * Verifies that the loan has at least the required number of active guarantors.
     * This is typically used during loan approval to ensure guarantee requirements are met.
     *
     * @param loanAccountId the loan account ID
     * @param requiredCount the minimum number of guarantors required
     * @return true if the loan has at least the required number of active guarantors, false otherwise
     */
    @Transactional(readOnly = true)
    public boolean hasRequiredGuarantors(UUID loanAccountId, Integer requiredCount) {
        long activeCount = countActiveGuarantorsByLoanAccount(loanAccountId);
        return activeCount >= requiredCount;
    }

    /**
     * Validates if a customer is eligible to be a guarantor for a loan.
     *
     * Performs eligibility checks including:
     * - Customer is not already a guarantor for this loan
     *
     * Additional checks that could be added:
     * - Customer's credit score meets minimum requirements
     * - Customer's income is sufficient
     * - Customer is not the borrower on this loan
     * - Customer doesn't have too many existing guarantees
     * - Customer meets age requirements
     *
     * @param customerId the ID of the customer to validate
     * @param loanAccountId the ID of the loan account
     * @return validation result with eligibility status and reasons
     */
    @Transactional(readOnly = true)
    public GuarantorValidationResult validateGuarantorEligibility(UUID customerId, UUID loanAccountId) {
        List<String> reasons = new ArrayList<>();

        // Check if customer is already a guarantor for this loan
        if (guarantorRepository.existsByLoanAccountIdAndCustomerId(loanAccountId, customerId)) {
            reasons.add("Customer is already a guarantor for this loan");
        }

        boolean eligible = reasons.isEmpty();
        GuarantorValidationResult result = new GuarantorValidationResult(
                eligible,
                eligible ? "Customer is eligible to be a guarantor" : "Customer is not eligible");
        result.setReasons(reasons);

        return result;
    }

    private static void assertGuarantorOnLoan(Guarantor guarantor, UUID loanAccountId) {
        if (guarantor.getLoanAccount() == null || !loanAccountId.equals(guarantor.getLoanAccount().getId())) {
            throw new IllegalArgumentException("Guarantor does not belong to this loan account");
        }
    }
}
