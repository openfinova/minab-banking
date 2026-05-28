package com.openfinova.banking.customer.api;

import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.customer.api.dto.CustomerInfo;
import com.openfinova.banking.customer.api.dto.CustomerValidationResult;
import com.openfinova.banking.customer.api.entity.KYCStatus;

/**
 * Public contract for read-only customer lookups and identity linkage used by other modules.
 *
 * <p>
 * This facade is the only entry point for cross-module customer queries. Callers obtain party
 * validation, KYC posture, and identity linkage without depending on customer-service internals.
 * Identity writes ({@link #linkIdentityUser}, {@link #unlinkIdentityUser}) are invoked by the
 * identity module after provisioning lifecycle events.
 */
public interface CustomerInfoService {

    /**
     * Validates whether the customer party may participate in banking operations.
     *
     * @param customerId the customer party identifier
     * @return validation outcome with reasons when the party is not eligible
     */
    CustomerValidationResult validateCustomer(UUID customerId);

    /**
     * Returns {@code true} when the customer exists and is in an active lifecycle state.
     */
    boolean isCustomerActive(UUID customerId);

    /**
     * Returns {@code true} when KYC has been verified for the customer party.
     */
    boolean isKYCVerified(UUID customerId);

    /**
     * Current KYC status for the customer party, if the customer exists.
     * Used by identity at token issuance (no profile payload).
     */
    Optional<KYCStatus> getKycStatus(UUID customerId);

    /**
     * Returns the customer profile snapshot when the party exists.
     */
    Optional<CustomerInfo> getCustomer(UUID customerId);

    /**
     * Returns {@code true} when a customer with the given tax identifier is already registered.
     */
    boolean existsByTaxId(String taxId);

    /**
     * Resolves a customer by tax identifier when one exists.
     */
    Optional<CustomerInfo> getCustomerByTaxId(String taxId);

    /**
     * Returns {@code true} when a customer record exists for the given party id.
     */
    boolean customerExists(UUID customerId);

    /**
     * Records that an identity user has been created and linked to this customer.
     * Called by the identity module after a CUSTOMER-type user account is provisioned.
     *
     * @param customerId      the customer party that the identity account belongs to
     * @param identityUserId  the UUID of the newly created identity user
     * @param username        the login username chosen for the identity user
     */
    void linkIdentityUser(UUID customerId, UUID identityUserId, String username);

    /**
     * Clears the identity user link.
     * Called by the identity module when the linked identity account is deprovisioned.
     *
     * @param customerId the customer party whose identity link should be cleared
     */
    void unlinkIdentityUser(UUID customerId);

    /**
     * Returns the identity user ID currently linked to this customer, if any.
     *
     * @param customerId the customer party to look up
     * @return an {@link Optional} containing the linked identity user ID, or empty if none
     */
    Optional<UUID> getLinkedIdentityUserId(UUID customerId);

    /**
     * Resolves the core banking {@code Customer} identifier from an identity-linked user id,
     * as stored on accounts as primary holder ({@code primary_user_profile_id}).
     */
    Optional<UUID> getCustomerIdByLinkedIdentityUserId(UUID linkedIdentityUserId);
}
