package com.openfinova.banking.customer.controller;

import com.openfinova.banking.customer.api.entity.ContactType;
import com.openfinova.banking.customer.dto.ContactResponse;
import com.openfinova.banking.customer.entity.ContactDetail;
import com.openfinova.banking.customer.mapper.CustomerMapper;
import com.openfinova.banking.customer.service.ContactService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing customer contact details.
 *
 * Exposes endpoints for:
 * - Adding contact details (email, phone, etc.)
 * - Viewing contact details
 * - Updating contact details
 * - Verifying contact details
 * - Setting primary contacts
 * - Deleting contact details
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/contacts")
@Tag(name = "Customer Contacts", description = "APIs for managing customer contact details (email, phone, etc.)")
public class ContactController {

    private static final Logger log = LoggerFactory.getLogger(ContactController.class);

    private final ContactService contactService;

    public ContactController(ContactService contactService) {
        this.contactService = contactService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Add contact detail", description = "Adds a new contact detail to a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Contact detail added successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid contact data") })
    public ResponseEntity<ContactResponse> addContactDetail(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody ContactDetail contactDetail) {

        log.info("Adding contact detail for customer: {}, type: {}", customerId, contactDetail.getType());

        ContactDetail created = contactService.addContactDetail(customerId, contactDetail);

        log.info("Successfully added contact detail with ID: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerMapper.toContactResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get customer contacts", description = "Retrieves all contact details for a customer")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact details retrieved successfully") })
    public ResponseEntity<List<ContactResponse>> getContactDetails(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId) {

        log.info("Fetching contact details for customer: {}", customerId);

        List<ContactDetail> contacts = contactService.getContactDetailsByCustomerId(customerId);

        log.info("Found {} contact details for customer: {}", contacts.size(), customerId);

        return ResponseEntity.ok(CustomerMapper.toContactResponseList(contacts));
    }

    @GetMapping("/type/{type}")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get contacts by type", description = "Retrieves contact details for a customer filtered by type")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Contact details retrieved successfully") })
    public ResponseEntity<List<ContactResponse>> getContactDetailsByType(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Contact type", required = true) @PathVariable ContactType type) {

        log.info("Fetching contact details for customer: {}, type: {}", customerId, type);

        List<ContactDetail> contacts = contactService.getContactDetailsByType(customerId, type);

        log.info("Found {} contact details of type {} for customer: {}", contacts.size(), type, customerId);

        return ResponseEntity.ok(CustomerMapper.toContactResponseList(contacts));
    }

    @PutMapping("/{contactId}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Update contact detail", description = "Updates an existing contact detail")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Contact detail updated successfully"),
            @ApiResponse(responseCode = "404", description = "Contact detail not found"),
            @ApiResponse(responseCode = "400", description = "Invalid contact data") })
    public ResponseEntity<ContactResponse> updateContactDetail(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId,
            @Valid @RequestBody ContactDetail contactDetailDetails) {

        log.info("Updating contact detail: {}", contactId);

        ContactDetail updated = contactService.updateContactDetail(customerId, contactId, contactDetailDetails);

        log.info("Successfully updated contact detail: {}", contactId);

        return ResponseEntity.ok(CustomerMapper.toContactResponse(updated));
    }

    @PutMapping("/{contactId}/set-primary")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Set primary contact", description = "Sets a specific contact detail as the primary one for its type")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Primary contact set successfully"),
            @ApiResponse(responseCode = "404", description = "Contact detail not found") })
    public ResponseEntity<Void> setPrimaryContact(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId) {

        log.info("Setting primary contact: {}", contactId);

        contactService.setPrimaryContact(customerId, contactId);

        log.info("Successfully set primary contact: {}", contactId);

        return ResponseEntity.ok().build();
    }

    @PutMapping("/{contactId}/verify")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Verify contact", description = "Marks a contact detail as verified (e.g., after OTP or email confirmation)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Contact verified successfully"),
            @ApiResponse(responseCode = "404", description = "Contact detail not found") })
    public ResponseEntity<Void> verifyContact(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId) {

        log.info("Verifying contact: {}", contactId);

        contactService.verifyContact(customerId, contactId);

        log.info("Successfully verified contact: {}", contactId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{contactId}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Delete contact detail", description = "Removes a contact detail from a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Contact detail deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Contact detail not found") })
    public ResponseEntity<Void> deleteContactDetail(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Contact ID", required = true) @PathVariable UUID contactId) {

        log.info("Deleting contact detail: {}", contactId);

        contactService.deleteContactDetail(customerId, contactId);

        log.info("Successfully deleted contact detail: {}", contactId);

        return ResponseEntity.ok().build();
    }
}
