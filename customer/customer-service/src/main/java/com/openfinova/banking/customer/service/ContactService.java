package com.openfinova.banking.customer.service;

import com.openfinova.banking.customer.api.entity.ContactType;
import com.openfinova.banking.customer.entity.ContactDetail;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.repository.ContactDetailRepository;
import com.openfinova.banking.customer.repository.CustomerRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Service class for managing customer contact details in the core banking system.
 * This service handles operations such as adding, retrieving, updating, verifying,
 * and deleting customer contact details.
 * It enforces business rules, such as ensuring a customer has only one primary
 * contact per type and resetting verification status upon value updates.
 * All operations are transactional to guarantee data consistency.
 */
@Service
@Transactional
public class ContactService {

    private final ContactDetailRepository contactRepository;
    private final CustomerRepository customerRepository;

    /**
     * Constructs a new ContactService with the necessary repositories.
     *
     * @param contactRepository the repository used for accessing and managing customer contact data
     * @param customerRepository the repository used for accessing customer data
     */
    public ContactService(ContactDetailRepository contactRepository, CustomerRepository customerRepository) {
        this.contactRepository = contactRepository;
        this.customerRepository = customerRepository;
    }

    /**
     * Adds a new contact detail for the specified customer.
     * If the new contact is marked as primary, this method will automatically
     * unset the primary flag on any existing contacts of the same type for this customer.
     *
     * @param customerId the unique identifier of the customer
     * @param contactDetail the contact detail to be added
     * @return the saved contact detail entity
     * @throws IllegalArgumentException if the customer is not found
     */
    public ContactDetail addContactDetail(UUID customerId, ContactDetail contactDetail) {
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found: " + customerId));

        contactDetail.setCustomer(customer);

        // If this is marked as primary, unset other primary contacts of the same type
        if (contactDetail.isPrimary()) {
            List<ContactDetail> existingContacts = contactRepository
                    .findByCustomerIdAndTypeAndDeletedAtIsNull(customerId, contactDetail.getType());
            existingContacts.stream().filter(ContactDetail::isPrimary).forEach(c -> {
                c.setPrimary(false);
                contactRepository.save(c);
            });
        }

        return contactRepository.save(contactDetail);
    }

    /**
     * Retrieves all active (non-deleted) contact details associated with a specific customer.
     *
     * @param customerId the unique identifier of the customer
     * @return a list of active contact details belonging to the customer
     */
    @Transactional(readOnly = true)
    public List<ContactDetail> getContactDetailsByCustomerId(UUID customerId) {
        return contactRepository.findByCustomerIdAndDeletedAtIsNull(customerId);
    }

    /**
     * Retrieves all active (non-deleted) contact details of a specific type for a customer.
     *
     * @param customerId the unique identifier of the customer
     * @param type the specific type of contact to retrieve
     * @return a list of active contact details matching the specified type
     */
    @Transactional(readOnly = true)
    public List<ContactDetail> getContactDetailsByType(UUID customerId, ContactType type) {
        return contactRepository.findByCustomerIdAndTypeAndDeletedAtIsNull(customerId, type);
    }

    /**
     * Retrieves a specific contact detail by its unique identifier, verifying that it belongs
     * to the specified customer and has not been deleted.
     *
     * @param customerId the unique identifier of the customer who owns the contact detail
     * @param contactId the unique identifier of the contact detail to retrieve
     * @return the requested contact detail
     * @throws IllegalArgumentException if the contact is not found, has been deleted, or does not belong to the specified customer
     */
    @Transactional(readOnly = true)
    public ContactDetail getContactById(UUID customerId, UUID contactId) {
        ContactDetail contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact detail not found: " + contactId));
        if (contact.isDeleted()) {
            throw new IllegalArgumentException("Contact detail not found: " + contactId);
        }
        validateContactOwnership(contact, customerId);
        return contact;
    }

    /**
     * Updates the value of an existing contact detail.
     * If the value is changed, the verification status of the contact will be reset to false.
     * The contact must belong to the specified customer and must not be deleted.
     *
     * @param customerId the unique identifier of the customer who owns the contact detail
     * @param contactId the unique identifier of the contact detail to update
     * @param contactDetailDetails an object containing the new contact detail values
     * @return the updated contact detail entity
     * @throws IllegalArgumentException if the contact is not found, has been deleted, or does not belong to the specified customer
     */
    public ContactDetail updateContactDetail(UUID customerId, UUID contactId, ContactDetail contactDetailDetails) {
        ContactDetail contactDetail = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact detail not found: " + contactId));
        if (contactDetail.isDeleted()) {
            throw new IllegalArgumentException("Contact detail not found: " + contactId);
        }
        validateContactOwnership(contactDetail, customerId);

        if (contactDetailDetails.getValue() != null) {
            contactDetail.setValue(contactDetailDetails.getValue());
            // Reset verification when value changes
            contactDetail.setVerified(false);
        }

        return contactRepository.save(contactDetail);
    }

    /**
     * Sets a specific contact detail as the primary contact for its type.
     * This operation will automatically remove the primary status from any other
     * active contacts of the same type belonging to the customer.
     *
     * @param customerId the unique identifier of the customer who owns the contact detail
     * @param contactId the unique identifier of the contact detail to be set as primary
     * @throws IllegalArgumentException if the contact is not found, has been deleted, or does not belong to the specified customer
     */
    public void setPrimaryContact(UUID customerId, UUID contactId) {
        ContactDetail contactDetail = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact detail not found: " + contactId));
        if (contactDetail.isDeleted()) {
            throw new IllegalArgumentException("Contact detail not found: " + contactId);
        }
        validateContactOwnership(contactDetail, customerId);

        // Unset other primary contacts of the same type for this customer
        List<ContactDetail> customerContacts = contactRepository.findByCustomerIdAndTypeAndDeletedAtIsNull(
                contactDetail.getCustomer().getId(),
                contactDetail.getType());

        customerContacts.stream().filter(c -> c.isPrimary() && !c.getId().equals(contactId)).forEach(c -> {
            c.setPrimary(false);
            contactRepository.save(c);
        });

        contactDetail.setPrimary(true);
        contactRepository.save(contactDetail);
    }

    /**
     * Marks a specific contact detail as verified.
     *
     * @param customerId the unique identifier of the customer who owns the contact detail
     * @param contactId the unique identifier of the contact detail to verify
     * @throws IllegalArgumentException if the contact is not found, has been deleted, or does not belong to the specified customer
     */
    public void verifyContact(UUID customerId, UUID contactId) {
        ContactDetail contactDetail = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact detail not found: " + contactId));
        if (contactDetail.isDeleted()) {
            throw new IllegalArgumentException("Contact detail not found: " + contactId);
        }
        validateContactOwnership(contactDetail, customerId);

        contactDetail.setVerified(true);
        contactRepository.save(contactDetail);
    }

    /**
     * Soft deletes a specific contact detail by setting its deletion timestamp.
     * The contact must belong to the specified customer and must not already be deleted.
     *
     * @param customerId the unique identifier of the customer who owns the contact detail
     * @param contactId the unique identifier of the contact detail to delete
     * @throws IllegalArgumentException if the contact is not found, already deleted, or does not belong to the customer
     */
    public void deleteContactDetail(UUID customerId, UUID contactId) {
        ContactDetail contact = contactRepository.findById(contactId)
                .orElseThrow(() -> new IllegalArgumentException("Contact detail not found: " + contactId));
        validateContactOwnership(contact, customerId);
        if (contact.isDeleted()) {
            throw new IllegalArgumentException("Contact detail not found: " + contactId);
        }
        contact.setDeletedAt(LocalDateTime.now());
        contactRepository.save(contact);
    }

    /**
     * Validates that a given contact detail belongs to the specified customer.
     *
     * @param contact the contact detail to check
     * @param customerId the expected customer identifier
     * @throws IllegalArgumentException if the contact detail does not belong to the customer
     */
    private void validateContactOwnership(ContactDetail contact, UUID customerId) {
        if (!contact.getCustomer().getId().equals(customerId)) {
            throw new IllegalArgumentException(
                    "Contact " + contact.getId() + " does not belong to customer " + customerId);
        }
    }
}
