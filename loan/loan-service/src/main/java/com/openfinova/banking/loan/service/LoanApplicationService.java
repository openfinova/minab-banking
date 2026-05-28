package com.openfinova.banking.loan.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.loan.api.entity.ApplicationStatus;
import com.openfinova.banking.loan.dto.AffordabilityAssessment;
import com.openfinova.banking.loan.dto.ApplicationValidationResult;
import com.openfinova.banking.loan.entity.LoanApplication;
import com.openfinova.banking.loan.repository.LoanApplicationRepository;
import com.openfinova.banking.setup.api.DateTimeService;

@Service
@Transactional
public class LoanApplicationService {

    private final LoanApplicationRepository applicationRepository;
    private final DateTimeService dateTimeService;

    public LoanApplicationService(LoanApplicationRepository applicationRepository, DateTimeService dateTimeService) {
        this.applicationRepository = applicationRepository;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Creates a new loan application with DRAFT status and auto-generated application number.
     *
     * @param application the loan application to create
     * @param createdBy the user creating the application
     * @return the created loan application with assigned number and status
     */
    public LoanApplication createApplication(LoanApplication application, String createdBy) {
        application.setApplicationNumber(generateApplicationNumber());
        application.setStatus(ApplicationStatus.DRAFT);
        return applicationRepository.save(application);
    }

    /**
     * Retrieves a loan application by its unique identifier.
     *
     * @param id the application ID
     * @return an Optional containing the application if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<LoanApplication> getApplicationById(UUID id) {
        return applicationRepository.findById(id);
    }

    /**
     * Retrieves a loan application by its application number.
     *
     * @param applicationNumber the unique application number
     * @return an Optional containing the application if found, empty otherwise
     */
    @Transactional(readOnly = true)
    public Optional<LoanApplication> getApplicationByNumber(String applicationNumber) {
        return applicationRepository.findByApplicationNumber(applicationNumber);
    }

    /**
     * Retrieves all loan applications for a specific customer with pagination.
     *
     * @param customerId the customer ID
     * @param pageable pagination parameters
     * @return a page of loan applications for the customer
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getApplicationsByCustomer(UUID customerId, Pageable pageable) {
        return applicationRepository.findByCustomerId(customerId, pageable);
    }

    /**
     * Retrieves all loan applications with a specific status with pagination.
     *
     * @param status the application status to filter by
     * @param pageable pagination parameters
     * @return a page of loan applications with the specified status
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getApplicationsByStatus(ApplicationStatus status, Pageable pageable) {
        return applicationRepository.findByStatus(status, pageable);
    }

    /**
     * Submits a draft loan application for review.
     * Changes the application status from DRAFT to SUBMITTED.
     *
     * @param applicationId the application ID
     * @param submittedBy the user submitting the application
     * @return the updated loan application
     * @throws IllegalArgumentException if application not found
     * @throws IllegalStateException if application is not in DRAFT status
     */
    public LoanApplication submitApplication(UUID applicationId, String submittedBy) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        if (!application.canTransitionTo(ApplicationStatus.SUBMITTED)) {
            throw new IllegalStateException(application.getTransitionErrorMessage(ApplicationStatus.SUBMITTED));
        }

        application.setStatus(ApplicationStatus.SUBMITTED);
        return applicationRepository.save(application);
    }

    /**
     * Assigns a loan application to an underwriter for review.
     * Changes the application status to UNDER_REVIEW and records assignment details.
     *
     * @param applicationId the application ID
     * @param underwriterId the ID of the underwriter to assign
     * @param assignedBy the user performing the assignment
     * @return the updated loan application
     * @throws IllegalArgumentException if application not found
     * @throws IllegalStateException if transition to UNDER_REVIEW is not allowed
     */
    public LoanApplication assignToUnderwriter(UUID applicationId, String underwriterId, String assignedBy) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        if (!application.canTransitionTo(ApplicationStatus.UNDER_REVIEW)) {
            throw new IllegalStateException(application.getTransitionErrorMessage(ApplicationStatus.UNDER_REVIEW));
        }

        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        application.setUnderwriterId(underwriterId);
        application.setUnderwriterAssignedBy(assignedBy);
        application.setUnderwriterAssignedAt(dateTimeService.instant());

        return applicationRepository.save(application);
    }

    /**
     * Performs credit scoring for a loan application based on debt-to-income ratio.
     * Base score is 600, adjusted by DTI:
     * - DTI < 30%: +100 points
     * - DTI > 50%: -100 points
     *
     * @param applicationId the application ID
     * @return the calculated credit score
     * @throws IllegalArgumentException if application not found
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public BigDecimal performCreditScoring(UUID applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        // Simplified credit scoring logic
        BigDecimal score = BigDecimal.valueOf(600);

        if (application.getMonthlyIncome() != null && application.getExistingObligations() != null
                && application.getMonthlyIncome().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal dti = application.getExistingObligations()
                    .divide(application.getMonthlyIncome(), 4, RoundingMode.HALF_UP);

            if (dti.compareTo(BigDecimal.valueOf(0.3)) < 0) {
                score = score.add(BigDecimal.valueOf(100));
            } else if (dti.compareTo(BigDecimal.valueOf(0.5)) > 0) {
                score = score.subtract(BigDecimal.valueOf(100));
            }
        }

        application.setCreditScore(score);
        applicationRepository.save(application);
        return score;
    }

    /**
     * Performs risk assessment for a loan application based on credit score.
     * Risk ratings:
     * - A: Score >= 750 (Low risk)
     * - B: Score >= 650 (Medium risk)
     * - C: Score >= 550 (High risk)
     * - D: Score < 550 (Very high risk)
     *
     * @param applicationId the application ID
     * @return the assigned risk rating (A, B, C, or D)
     * @throws IllegalArgumentException if application not found
     */
    @PreAuthorize("hasAuthority('loan:write')")
    public String performRiskAssessment(UUID applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        BigDecimal creditScore = application.getCreditScore();
        String riskRating;

        if (creditScore == null) {
            riskRating = "C";
        } else if (creditScore.compareTo(BigDecimal.valueOf(750)) >= 0) {
            riskRating = "A";
        } else if (creditScore.compareTo(BigDecimal.valueOf(650)) >= 0) {
            riskRating = "B";
        } else if (creditScore.compareTo(BigDecimal.valueOf(550)) >= 0) {
            riskRating = "C";
        } else {
            riskRating = "D";
        }

        application.setRiskRating(riskRating);
        applicationRepository.save(application);
        return riskRating;
    }

    /**
     * Approves a loan application with specified terms.
     * Sets the status to APPROVED and records approval details.
     *
     * @param applicationId the application ID
     * @param approvedAmount the approved loan amount
     * @param approvedTenorMonths the approved loan tenor in months
     * @param approvedInterestRate the approved interest rate
     * @param guarantorsRequired the number of guarantors required
     * @param approvedBy the user approving the application
     * @return the approved loan application
     * @throws IllegalArgumentException if application not found
     * @throws IllegalStateException if transition to APPROVED is not allowed
     */
    public LoanApplication approveApplication(UUID applicationId, BigDecimal approvedAmount,
            Integer approvedTenorMonths, BigDecimal approvedInterestRate, Integer guarantorsRequired,
            String approvedBy) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        if (!application.canTransitionTo(ApplicationStatus.APPROVED)) {
            throw new IllegalStateException(application.getTransitionErrorMessage(ApplicationStatus.APPROVED));
        }

        application.setStatus(ApplicationStatus.APPROVED);
        application.setApprovedAmount(approvedAmount);
        application.setApprovedTenorMonths(approvedTenorMonths);
        application.setApprovedInterestRate(approvedInterestRate);
        application.setGuarantorsRequired(guarantorsRequired);
        application.setApprovalDate(dateTimeService.today());
        application.setApprovedBy(approvedBy);

        return applicationRepository.save(application);
    }

    /**
     * Rejects a loan application with a specified reason.
     * Sets the status to REJECTED and records rejection details.
     *
     * @param applicationId the application ID
     * @param rejectionReason the reason for rejection
     * @param rejectedBy the user rejecting the application
     * @return the rejected loan application
     * @throws IllegalArgumentException if application not found
     * @throws IllegalStateException if transition to REJECTED is not allowed
     */
    public LoanApplication rejectApplication(UUID applicationId, String rejectionReason, String rejectedBy) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        if (!application.canTransitionTo(ApplicationStatus.REJECTED)) {
            throw new IllegalStateException(application.getTransitionErrorMessage(ApplicationStatus.REJECTED));
        }

        application.setStatus(ApplicationStatus.REJECTED);
        application.setRejectionReason(rejectionReason);
        application.setRejectionDate(dateTimeService.today());
        application.setRejectedBy(rejectedBy);

        return applicationRepository.save(application);
    }

    /**
     * Requests additional information for a loan application.
     * Sets the status to UNDER_REVIEW and records the information required.
     *
     * @param applicationId the application ID
     * @param informationRequired description of the additional information needed
     * @param requestedBy the user requesting the information
     * @return the updated loan application
     * @throws IllegalArgumentException if application not found
     * @throws IllegalStateException if transition to UNDER_REVIEW is not allowed
     */
    public LoanApplication requestAdditionalInformation(UUID applicationId, String informationRequired,
            String requestedBy) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        if (!application.canTransitionTo(ApplicationStatus.UNDER_REVIEW)) {
            throw new IllegalStateException(application.getTransitionErrorMessage(ApplicationStatus.UNDER_REVIEW));
        }

        application.setStatus(ApplicationStatus.UNDER_REVIEW);
        application.setRemarks(informationRequired);

        return applicationRepository.save(application);
    }

    /**
     * Checks if a loan application has the required number of guarantors.
     *
     * @param applicationId the application ID
     * @return true if the required number of guarantors is met, false otherwise
     * @throws IllegalArgumentException if application not found
     */
    @Transactional(readOnly = true)
    public boolean hasRequiredGuarantors(UUID applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        return application.hasRequiredGuarantors();
    }

    /**
     * Retrieves all pending loan applications with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of pending loan applications
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getPendingApplications(Pageable pageable) {
        return applicationRepository.findPendingApplications(pageable);
    }

    /**
     * Retrieves all approved loan applications with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of approved loan applications
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getApprovedApplications(Pageable pageable) {
        return applicationRepository.findApprovedApplications(pageable);
    }

    /**
     * Retrieves all rejected loan applications with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of rejected loan applications
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getRejectedApplications(Pageable pageable) {
        return applicationRepository.findRejectedApplications(pageable);
    }

    /**
     * Retrieves loan applications created within a date range with pagination.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination parameters
     * @return a page of loan applications created within the date range
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getApplicationsCreatedBetween(Instant startDate, Instant endDate, Pageable pageable) {
        return applicationRepository.findApplicationsCreatedBetween(startDate, endDate, pageable);
    }

    /**
     * Retrieves loan applications approved within a date range with pagination.
     *
     * @param startDate the start date (inclusive)
     * @param endDate the end date (inclusive)
     * @param pageable pagination parameters
     * @return a page of loan applications approved within the date range
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getApplicationsApprovedBetween(LocalDate startDate, LocalDate endDate,
            Pageable pageable) {
        return applicationRepository.findApplicationsApprovedBetween(startDate, endDate, pageable);
    }

    /**
     * Retrieves loan applications that require guarantors with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of loan applications requiring guarantors
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getApplicationsRequiringGuarantors(Pageable pageable) {
        return applicationRepository.findApplicationsRequiringGuarantors(pageable);
    }

    /**
     * Retrieves loan applications that have all required guarantors with pagination.
     *
     * @param pageable pagination parameters
     * @return a page of loan applications with complete guarantor requirements
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getApplicationsWithCompleteGuarantors(Pageable pageable) {
        return applicationRepository.findApplicationsWithCompleteGuarantors(pageable);
    }

    /**
     * Counts the number of pending loan applications for a specific customer.
     *
     * @param customerId the customer ID
     * @return the count of pending applications
     */
    @Transactional(readOnly = true)
    public long countPendingApplicationsByCustomer(UUID customerId) {
        return applicationRepository.countPendingApplicationsByCustomer(customerId);
    }

    /**
     * Calculates the total requested amount for all applications with a specific status.
     *
     * @param status the application status
     * @return the sum of requested amounts
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalRequestedAmountByStatus(ApplicationStatus status) {
        return applicationRepository.sumRequestedAmountByStatus(status);
    }

    /**
     * Calculates the total approved amount across all approved applications.
     *
     * @return the sum of approved amounts
     */
    @Transactional(readOnly = true)
    public BigDecimal calculateTotalApprovedAmount() {
        return applicationRepository.sumApprovedAmounts();
    }

    /**
     * Retrieves stale loan applications that haven't been updated since the cutoff date.
     *
     * @param cutoffDate the cutoff date for identifying stale applications
     * @param pageable pagination parameters
     * @return a page of stale loan applications
     */
    @Transactional(readOnly = true)
    public Page<LoanApplication> getStaleApplications(Instant cutoffDate, Pageable pageable) {
        return applicationRepository.findStaleApplications(cutoffDate, pageable);
    }

    /**
     * Validates a loan application for submission.
     * Checks that required fields like requested amount and tenor are valid.
     *
     * @param applicationId the application ID
     * @return validation result with errors if any
     * @throws IllegalArgumentException if application not found
     */
    @Transactional(readOnly = true)
    public ApplicationValidationResult validateForSubmission(UUID applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        List<String> errors = new ArrayList<>();

        if (application.getRequestedAmount() == null
                || application.getRequestedAmount().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add("Requested amount must be positive");
        }
        if (application.getRequestedTenorMonths() == null || application.getRequestedTenorMonths() <= 0) {
            errors.add("Requested tenor must be positive");
        }

        boolean valid = errors.isEmpty();
        ApplicationValidationResult result = new ApplicationValidationResult(
                valid,
                valid ? "Application is valid for submission" : "Application has validation errors");
        result.setErrors(errors);

        return result;
    }

    /**
     * Validates a loan application for approval.
     * Checks that the application is in UNDERWRITING status and has credit score and risk rating.
     *
     * @param applicationId the application ID
     * @return validation result with errors if any
     * @throws IllegalArgumentException if application not found
     */
    @Transactional(readOnly = true)
    public ApplicationValidationResult validateForApproval(UUID applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        List<String> errors = new ArrayList<>();

        if (!ApplicationStatus.UNDERWRITING.equals(application.getStatus())) {
            errors.add("Application must be in underwriting status");
        }
        if (application.getCreditScore() == null) {
            errors.add("Credit score is required");
        }
        if (application.getRiskRating() == null) {
            errors.add("Risk rating is required");
        }
        if (application.getGuarantorsRequired() != null && application.getGuarantorsRequired() > 0) {
            if (!application.hasRequiredGuarantors()) {
                errors.add(
                        String.format(
                                "Required guarantors: %d, Provided: %d",
                                application.getGuarantorsRequired(),
                                application.getGuarantorsProvided()));
            }
        }

        boolean valid = errors.isEmpty();
        ApplicationValidationResult result = new ApplicationValidationResult(
                valid,
                valid ? "Application is valid for approval" : "Application has validation errors");
        result.setErrors(errors);

        return result;
    }

    /**
     * Calculates affordability assessment for a loan application.
     * Computes debt-to-income ratio and determines if the loan is affordable (DTI <= 40%).
     *
     * @param applicationId the application ID
     * @return affordability assessment with DTI ratio and recommendation
     * @throws IllegalArgumentException if application not found
     */
    @Transactional(readOnly = true)
    public AffordabilityAssessment calculateAffordability(UUID applicationId) {
        LoanApplication application = applicationRepository.findById(applicationId).orElseThrow(
                () -> new IllegalArgumentException(String.format("Application not found: %s", applicationId)));

        AffordabilityAssessment assessment = new AffordabilityAssessment();
        assessment.setMonthlyIncome(application.getMonthlyIncome());
        assessment.setExistingObligations(application.getExistingObligations());

        if (application.getMonthlyIncome() != null && application.getRequestedAmount() != null) {
            BigDecimal monthlyRate = application.getApprovedInterestRate() != null
                    ? application.getApprovedInterestRate().divide(BigDecimal.valueOf(1200), 6, RoundingMode.HALF_UP)
                    : BigDecimal.valueOf(0.01);

            BigDecimal proposedInstallment = calculateMonthlyInstallment(
                    application.getRequestedAmount(),
                    monthlyRate,
                    application.getRequestedTenorMonths());

            assessment.setProposedInstallment(proposedInstallment);

            BigDecimal totalObligations = (application.getExistingObligations() != null
                    ? application.getExistingObligations()
                    : BigDecimal.ZERO).add(proposedInstallment);

            BigDecimal dti = totalObligations.divide(application.getMonthlyIncome(), 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            assessment.setDebtToIncomeRatio(dti);
            assessment.setAffordable(dti.compareTo(BigDecimal.valueOf(40)) <= 0);
            assessment.setRecommendation(
                    dti.compareTo(BigDecimal.valueOf(40)) <= 0 ? "Affordable" : "DTI ratio exceeds 40%");
        }

        return assessment;
    }

    /**
     * Calculates the monthly installment for a loan using the amortization formula.
     * Formula: PMT = P * r * (1 + r)^n / ((1 + r)^n - 1)
     * where P = principal, r = monthly rate, n = number of months
     *
     * @param principal the loan principal amount
     * @param monthlyRate the monthly interest rate (as decimal)
     * @param months the loan tenor in months
     * @return the calculated monthly installment amount
     */
    private BigDecimal calculateMonthlyInstallment(BigDecimal principal, BigDecimal monthlyRate, Integer months) {
        if (monthlyRate.compareTo(BigDecimal.ZERO) == 0) {
            return principal.divide(BigDecimal.valueOf(months), 2, RoundingMode.HALF_UP);
        }

        BigDecimal onePlusR = BigDecimal.ONE.add(monthlyRate);
        BigDecimal numerator = principal.multiply(monthlyRate).multiply(onePlusR.pow(months));
        BigDecimal denominator = onePlusR.pow(months).subtract(BigDecimal.ONE);

        return numerator.divide(denominator, 2, RoundingMode.HALF_UP);
    }

    /**
     * Generates a unique application number using timestamp.
     * Format: APP-{timestamp}
     *
     * @return the generated application number
     */
    private String generateApplicationNumber() {
        return String.format("APP-%s", System.currentTimeMillis());
    }
}
