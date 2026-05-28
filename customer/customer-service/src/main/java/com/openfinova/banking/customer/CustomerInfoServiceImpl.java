package com.openfinova.banking.customer;

import java.util.Optional;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.openfinova.banking.customer.api.CustomerInfoService;
import com.openfinova.banking.customer.api.dto.CustomerInfo;
import com.openfinova.banking.customer.api.dto.CustomerValidationResult;
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

    @Override
    public CustomerValidationResult validateCustomer(UUID customerId) {
        return customerService.validateCustomer(customerId);
    }

    @Override
    public boolean isCustomerActive(UUID customerId) {
        return customerService.isCustomerActive(customerId);
    }

    @Override
    public boolean isKYCVerified(UUID customerId) {
        return customerService.isKYCVerified(customerId);
    }

    @Override
    public Optional<KYCStatus> getKycStatus(UUID customerId) {
        return customerService.getKycStatus(customerId);
    }

    @Override
    public Optional<CustomerInfo> getCustomer(UUID customerId) {
        return customerService.getCustomerById(customerId).map(this::toCustomerInfo);
    }

    @Override
    public boolean existsByTaxId(String taxId) {
        return customerService.getCustomerByTaxId(taxId).isPresent();
    }

    @Override
    public Optional<CustomerInfo> getCustomerByTaxId(String taxId) {
        return customerService.getCustomerByTaxId(taxId).map(this::toCustomerInfo);
    }

    @Override
    public boolean customerExists(UUID customerId) {
        return customerService.customerExists(customerId);
    }

    @Override
    @Transactional
    public void linkIdentityUser(UUID customerId, UUID identityUserId, String username) {
        customerService.linkIdentityUser(customerId, identityUserId, username);
    }

    @Override
    @Transactional
    public void unlinkIdentityUser(UUID customerId) {
        customerService.unlinkIdentityUser(customerId);
    }

    @Override
    public Optional<UUID> getLinkedIdentityUserId(UUID customerId) {
        return customerService.getLinkedIdentityUserId(customerId);
    }

    @Override
    public Optional<UUID> getCustomerIdByLinkedIdentityUserId(UUID linkedIdentityUserId) {
        return customerService.findCustomerIdByLinkedIdentityUserId(linkedIdentityUserId);
    }

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
        info.setCreatedAt(customer.getCreatedAt());
        info.setUpdatedAt(customer.getUpdatedAt());
        return info;
    }
}
