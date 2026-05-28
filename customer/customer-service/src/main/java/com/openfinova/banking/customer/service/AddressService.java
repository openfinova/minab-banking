package com.openfinova.banking.customer.service;

import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.entity.CustomerAddress;
import com.openfinova.banking.customer.repository.CustomerAddressRepository;
import com.openfinova.banking.customer.repository.CustomerRepository;
import com.openfinova.banking.setup.api.DateTimeService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service class for managing customer addresses in the core banking system.
 * This service handles operations such as adding, retrieving, updating, and
 * deleting customer addresses. It also enforces business rules, such as ensuring
 * a customer has only one primary address of a specific type at any given time.
 * All operations are transactional to guarantee data consistency.
 */
@Service
@Transactional
public class AddressService {

    private final CustomerAddressRepository addressRepository;
    private final CustomerRepository customerRepository;
    private final DateTimeService dateTimeService;

    /**
     * Constructs a new AddressService with the necessary repositories.
     *
     * @param addressRepository the repository used for accessing and managing customer address data
     * @param customerRepository the repository used for accessing customer data
     */
    public AddressService(CustomerAddressRepository addressRepository, CustomerRepository customerRepository,
            DateTimeService dateTimeService) {
        this.addressRepository = addressRepository;
        this.customerRepository = customerRepository;
        this.dateTimeService = dateTimeService;
    }

    /**
     * Adds a new address for the specified customer.
     * If the new address is marked as primary, this method will automatically
     * unset the primary flag on any existing addresses of the same type for this customer.
     *
     * @param customerId the unique identifier of the customer
     * @param address the customer address details to be added
     * @return the saved customer address entity
     * @throws IllegalArgumentException if the customer is not found
     */
    public CustomerAddress addAddress(UUID customerId, CustomerAddress address) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        address.setCustomer(customer);

        // If this is marked as primary, unset other primary addresses of the same type
        if (address.isPrimary()) {
            List<CustomerAddress> existingAddresses = addressRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
            existingAddresses.stream().filter(a -> a.getType() == address.getType() && a.isPrimary()).forEach(a -> {
                a.setPrimary(false);
                addressRepository.save(a);
            });
        }

        return addressRepository.save(address);
    }

    /**
     * Retrieves all active (non-deleted) addresses associated with a specific customer.
     *
     * @param customerId the unique identifier of the customer
     * @return a list of active addresses belonging to the customer
     */
    @Transactional(readOnly = true)
    public List<CustomerAddress> getAddressesByCustomerId(UUID customerId) {
        return addressRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
    }

    /**
     * Retrieves a specific address by its unique identifier, verifying that it belongs
     * to the specified customer and has not been deleted.
     *
     * @param customerId the unique identifier of the customer who owns the address
     * @param addressId the unique identifier of the address to retrieve
     * @return the requested customer address
     * @throws IllegalArgumentException if the address is not found, has been deleted, or does not belong to the specified customer
     */
    @Transactional(readOnly = true)
    public CustomerAddress getAddressById(UUID customerId, UUID addressId) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        if (address.isDeleted()) {
            throw new IllegalArgumentException("Address not found: " + addressId);
        }
        validateAddressOwnership(address, customerId);
        return address;
    }

    /**
     * Updates the details of an existing customer address.
     * Only non-null fields in the provided address details object will be updated.
     * The address must belong to the specified customer and must not be deleted.
     *
     * @param customerId the unique identifier of the customer who owns the address
     * @param addressId the unique identifier of the address to update
     * @param addressDetails an object containing the new address details
     * @return the updated customer address entity
     * @throws IllegalArgumentException if the address is not found or does not belong to the specified customer
     */
    public CustomerAddress updateAddress(UUID customerId, UUID addressId, CustomerAddress addressDetails) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        validateAddressOwnership(address, customerId);

        if (addressDetails.getLine1() != null) {
            address.setLine1(addressDetails.getLine1());
        }
        if (addressDetails.getLine2() != null) {
            address.setLine2(addressDetails.getLine2());
        }
        if (addressDetails.getCity() != null) {
            address.setCity(addressDetails.getCity());
        }
        if (addressDetails.getState() != null) {
            address.setState(addressDetails.getState());
        }
        if (addressDetails.getPostalCode() != null) {
            address.setPostalCode(addressDetails.getPostalCode());
        }
        if (addressDetails.getCountry() != null) {
            address.setCountry(addressDetails.getCountry());
        }

        return addressRepository.save(address);
    }

    /**
     * Sets a specific address as the primary address for its type.
     * This operation will automatically remove the primary status from any other
     * active addresses of the same type belonging to the customer.
     *
     * @param customerId the unique identifier of the customer who owns the address
     * @param addressId the unique identifier of the address to be set as primary
     * @throws IllegalArgumentException if the address is not found or does not belong to the specified customer
     */
    public void setPrimaryAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        validateAddressOwnership(address, customerId);

        // Unset other primary addresses of the same type for this customer
        List<CustomerAddress> customerAddresses = addressRepository
                .findByCustomerIdAndDeletedAtIsNull(address.getCustomer().getId());

        customerAddresses.stream()
                .filter(a -> a.getType() == address.getType() && a.isPrimary() && !a.getId().equals(addressId))
                .forEach(a -> {
                    a.setPrimary(false);
                    addressRepository.save(a);
                });

        address.setPrimary(true);
        addressRepository.save(address);
    }

    /**
     * Soft deletes a specific customer address by setting its deletion timestamp.
     * The address must belong to the specified customer and must not already be deleted.
     *
     * @param customerId the unique identifier of the customer who owns the address
     * @param addressId the unique identifier of the address to delete
     * @throws IllegalArgumentException if the address is not found, already deleted, or does not belong to the customer
     */
    public void deleteAddress(UUID customerId, UUID addressId) {
        CustomerAddress address = addressRepository.findById(addressId)
                .orElseThrow(() -> new IllegalArgumentException("Address not found: " + addressId));
        validateAddressOwnership(address, customerId);
        if (address.isDeleted()) {
            throw new IllegalArgumentException("Address not found: " + addressId);
        }
        address.setDeletedAt(dateTimeService.now());
        addressRepository.save(address);
    }

    /**
     * Validates that a given address belongs to the specified customer.
     *
     * @param address the customer address to check
     * @param customerId the expected customer identifier
     * @throws IllegalArgumentException if the address does not belong to the customer
     */
    private void validateAddressOwnership(CustomerAddress address, UUID customerId) {
        if (!address.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "Address " + address.getId() + " does not belong to customer " + customerId);
        }
    }
}
