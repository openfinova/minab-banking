package com.openfinova.banking.loan.api;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.loan.api.dto.LoanAccountResponse;
import com.openfinova.banking.loan.api.dto.LoanDisbursementResponse;
import com.openfinova.banking.loan.api.dto.LoanPaymentResponse;
import com.openfinova.banking.loan.api.entity.PaymentMethod;

/**
 * Facade interface for loan operations consumed by other modules (for example transaction
 * processing, customer, or GL). Implementation resides in loan-service following the
 * module contract pattern: callers depend on this API and DTOs only, not on internal
 * entities or repositories.
 *
 * Covers two integration areas:
 *   - Account validation and read models for routing, limits, and eligibility checks
 *   - Posting hooks after payment rails complete (disbursement outcome, inbound repayments)
 */
public interface LoanService {

    // --- Account validation / read model ---

    /**
     * Returns loan account details for validation and enrichment.
     * Used by: TP and other modules that need a stable loan snapshot without depending on loan-service entities.
     */
    Optional<LoanAccountResponse> getLoanAccountById(UUID loanAccountId);

    /**
     * Resolves a loan by its business account number (customer-facing / reference data).
     */
    Optional<LoanAccountResponse> getLoanAccountByNumber(String loanAccountNumber);

    /**
     * Whether a loan account row exists.
     * Used by: TP (transaction validation against loan account id).
     */
    boolean loanAccountExists(UUID loanAccountId);

    /**
     * Owner customer id for the loan account, if the account exists.
     * Used by: TP (party resolution, limits tied to customer).
     */
    Optional<UUID> getCustomerIdForLoanAccount(UUID loanAccountId);

    /**
     * Whether the loan can accept a standard repayment posting (active or restructured).
     * Used by: TP before crediting loan repayment legs.
     */
    boolean isLoanAccountEligibleForRepayment(UUID loanAccountId);

    // --- Disbursement bridge (outgoing transfer completed or failed) ---

    /**
     * Locates a disbursement by its unique reference (e.g. correlation id from TP).
     */
    Optional<LoanDisbursementResponse> findDisbursementByReference(String disbursementReference);

    /**
     * Marks disbursement completed, stores the payment-system transaction reference, and
     activates the loan account on first successful disbursement when still in APPROVED state.
     * Used by: TP after successful outgoing disbursement transfer.
     */
    LoanDisbursementResponse completeDisbursementAfterTransfer(UUID disbursementId, String transactionReference,
            String completedBy);

    /**
     * Marks disbursement failed after TP or payment rails report failure.
     */
    LoanDisbursementResponse failDisbursementAfterTransfer(UUID disbursementId, String failureReason, String failedBy);

    // --- Repayment bridge (incoming payment matched to loan) ---

    /**
     * Idempotency helper: whether a repayment was already recorded for this external transaction reference.
     * Used by: TP before posting duplicate repayment.
     */
    boolean repaymentExistsForTransactionReference(String transactionReference);

    /**
     * Records a repayment allocated with the standard waterfall after funds are confirmed.
     * Used by: TP after successful inbound transfer to loan collection account.
     */
    LoanPaymentResponse recordRepaymentFromPaymentSystem(UUID loanAccountId, BigDecimal amount, LocalDate valueDate,
            PaymentMethod paymentMethod, String transactionReference, String recordedBy);
}
