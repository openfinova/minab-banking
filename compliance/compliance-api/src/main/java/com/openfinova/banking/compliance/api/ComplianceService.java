package com.openfinova.banking.compliance.api;

import java.math.BigDecimal;
import java.util.UUID;

import com.openfinova.banking.compliance.api.entity.RiskRating;

/**
 * Façade interface for Compliance operations.
 */
public interface ComplianceService {

    /**
     * Triggers a risk evaluation for a customer and returns the calculated risk rating.
     * This will perform internal scoring and optional external screening.
     *
     * @param customerId the ID of the customer to evaluate
     * @return the newly calculated risk rating
     */
    RiskRating evaluateCustomerRisk(UUID customerId);

    /**
     * Checks a specific party against sanctions lists synchronously.
     * Used for real-time transaction processing checks.
     *
     * @param partyName the name of the party
     * @param country the ISO country code
     * @return true if there is a match (i.e. blocked), false otherwise
     */
    boolean performSanctionsScreening(String partyName, String country);

    /**
     * Runs post-settlement AML transaction monitoring for a single posted TP transaction.
     * Typically invoked asynchronously after TP commits (avoid blocking the ledger path).
     */
    void evaluatePostedTransaction(UUID transactionId, UUID sourceAccountId, BigDecimal amount, String currency,
            String transactionTypeName);
}
