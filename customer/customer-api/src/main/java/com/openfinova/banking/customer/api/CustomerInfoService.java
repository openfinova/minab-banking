package com.openfinova.banking.customer.api;

import java.util.Optional;
import java.util.UUID;

import com.openfinova.banking.customer.api.dto.CustomerInfo;
import com.openfinova.banking.customer.api.dto.CustomerValidationResult;
import com.openfinova.banking.customer.api.entity.KYCStatus;

public interface CustomerInfoService {

    CustomerValidationResult validateCustomer(UUID customerId);

    boolean isCustomerActive(UUID customerId);

    boolean isKYCVerified(UUID customerId);

    /**
     * Current KYC status for the customer party, if the customer exists.
     * Used by identity at token issuance (no profile payload).
     */
    Optional<KYCStatus> getKycStatus(UUID customerId);

    Optional<CustomerInfo> getCustomer(UUID customerId);

    boolean existsByTaxId(String taxId);

    Optional<CustomerInfo> getCustomerByTaxId(String taxId);

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
}
