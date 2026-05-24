package com.openfinova.banking.customer;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.customer.api.dto.CustomerInfo;
import com.openfinova.banking.customer.api.dto.CustomerValidationResult;
import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.api.entity.KYCStatus;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.service.CustomerService;

/**
 * Facade providing minimal customer services needed by other modules.
 * This is the primary integration point for account, transaction, and other modules.
 */
@Service
@Transactional(readOnly = true)
public class CustomerInfoServiceImpl implements CustomerInfoService {

    private final CustomerService customerService;

    public CustomerInfoServiceImpl(CustomerService customerService) {
        this.customerService = customerService;
    }

    /**
     * Validates a customer for transaction processing.
     * Used by transaction and account modules before processing operations.
     *
     * @param customerId the customer ID to validate
     * @return validation result with status and any errors
     */
    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public CustomerValidationResult validateCustomer(UUID customerId) {
        Optional<Customer> customerOpt = customerService.getCustomerById(customerId);

        if (customerOpt.isEmpty()) {
            return CustomerValidationResult.failure(customerId, List.of("Customer not found: " + customerId));
        }

        Customer customer = customerOpt.get();
        List<String> errors = new ArrayList<>();

        // Check customer status
        if (customer.getStatus() != CustomerStatus.ACTIVE) {
            errors.add("Customer is not active. Current status: " + customer.getStatus());
        }

        if (null != customer.getKycStatus()) // Check KYC status
            switch (customer.getKycStatus()) {
                case REJECTED -> errors.add("Customer KYC verification has been rejected");
                case EXPIRED -> errors.add("Customer KYC verification has expired");
                case PENDING -> errors.add("Customer KYC verification is pending");
                default -> {
                }
            }

        // If there are errors, return failure
        if (!errors.isEmpty()) {
            return CustomerValidationResult.failure(customerId, errors);
        }

        // Return success with customer and KYC status
        return CustomerValidationResult
                .success(customerId, customer.getStatus().name(), customer.getKycStatus().name());
    }

    /**
     * Checks if a customer exists and is active.
     * Quick check for other modules before processing.
     *
     * @param customerId the customer ID to check
     * @return true if customer exists and is active
     */
    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public boolean isCustomerActive(UUID customerId) {
        Optional<Customer> customerOpt = customerService.getCustomerById(customerId);
        return customerOpt.isPresent() && customerOpt.get().getStatus() == CustomerStatus.ACTIVE;
    }

    /**
     * Checks if a customer has completed KYC verification.
     * Used by account module to determine account opening eligibility.
     *
     * @param customerId the customer ID to check
     * @return true if KYC is verified, false otherwise
     */
    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public boolean isKYCVerified(UUID customerId) {
        Optional<Customer> customerOpt = customerService.getCustomerById(customerId);
        return customerOpt.isPresent() && customerOpt.get().getKycStatus() == KYCStatus.VERIFIED;
    }

    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public Optional<KYCStatus> getKycStatus(UUID customerId) {
        return customerService.getCustomerById(customerId).map(Customer::getKycStatus);
    }

    /**
     * Gets customer information by ID.
     *
     * @param customerId the customer ID
     * @return optional containing customer info if found
     */
    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public Optional<CustomerInfo> getCustomer(UUID customerId) {
        return customerService.getCustomerById(customerId).map(this::toCustomerInfo);
    }

    /**
     * Checks if a customer exists by tax ID.
     * Used during customer onboarding to prevent duplicates.
     *
     * @param taxId the tax ID to check
     * @return true if customer exists with this tax ID
     */
    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public boolean existsByTaxId(String taxId) {
        return customerService.getCustomerByTaxId(taxId).isPresent();
    }

    /**
     * Gets customer information by tax ID.
     * Used for customer lookup during transactions.
     *
     * @param taxId the tax ID
     * @return optional containing customer info if found
     */
    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public Optional<CustomerInfo> getCustomerByTaxId(String taxId) {
        return customerService.getCustomerByTaxId(taxId).map(this::toCustomerInfo);
    }

    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public boolean customerExists(UUID customerId) {
        return customerService.getCustomerById(customerId).isPresent();
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:customer:write')")
    public void linkIdentityUser(UUID customerId, UUID identityUserId, String username) {
        customerService.linkIdentityUser(customerId, identityUserId, username);
    }

    @Override
    @Transactional
    @PreAuthorize("hasAuthority('service:customer:write')")
    public void unlinkIdentityUser(UUID customerId) {
        customerService.unlinkIdentityUser(customerId);
    }

    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public Optional<UUID> getLinkedIdentityUserId(UUID customerId) {
        return customerService.getLinkedIdentityUserId(customerId);
    }

    @Override
    @PreAuthorize("hasAuthority('service:customer:read')")
    public Optional<UUID> getCustomerIdByLinkedIdentityUserId(UUID linkedIdentityUserId) {
        return customerService.findCustomerIdByLinkedIdentityUserId(linkedIdentityUserId);
    }

    // Helper method to convert Customer entity to CustomerInfo DTO
    private CustomerInfo toCustomerInfo(Customer customer) {
        CustomerInfo info = new CustomerInfo();
        info.setCustomerId(customer.getId());
        info.setCustomerNumber(customer.getCustomerNumber());
        info.setCustomerType(customer.getType());
        info.setFirstName(customer.getFirstName());
        info.setLastName(customer.getLastName());
        info.setBusinessName(customer.getBusinessName());
        info.setTaxId(customer.getTaxId());
        info.setDateOfBirth(customer.getDateOfBirth());
        info.setStatus(customer.getStatus());
        info.setKycStatus(customer.getKycStatus());
        info.setNationality(customer.getNationality());
        info.setResidenceCountry(customer.getResidenceCountry());
        // riskRating intentionally not set here — will be populated by the future AML module
        info.setCreatedAt(customer.getCreatedAt());
        info.setUpdatedAt(customer.getUpdatedAt());
        return info;
    }
}
