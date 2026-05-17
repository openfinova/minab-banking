package com.openfinova.banking.customer.controller;

import com.openfinova.banking.customer.api.entity.CustomerRelationshipType;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.entity.CustomerRelationship;
import com.openfinova.banking.customer.service.CustomerService;
import com.openfinova.banking.identity.api.principal.CallerContextResolver;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

/**
 * REST Controller for managing customer relationships.
 * Exposes endpoints for:
 * - Creating relationships between customers (spouse, business partners, etc.)
 * - Viewing customer relationships
 * - Removing relationships
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/relationships")
@Tag(name = "Customer Relationships", description = "APIs for managing relationships between customers")
public class CustomerRelationshipController {

    private static final Logger log = LoggerFactory.getLogger(CustomerRelationshipController.class);

    private final CustomerService customerService;

    public CustomerRelationshipController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Create customer relationship", description = "Links two customers in a relationship (e.g., spouse, business partners)")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Relationship created successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "409", description = "Relationship already exists") })
    public ResponseEntity<CustomerRelationship> createCustomerRelationship(Authentication authentication,
            @Parameter(description = "Primary customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Related customer ID") @RequestParam UUID relatedCustomerId,
            @Parameter(description = "Relationship type") @RequestParam CustomerRelationshipType relationshipType) {

        String createdBy = CallerContextResolver.resolveUsername(authentication);

        log.info(
                "Creating relationship: primary={}, related={}, type={}, createdBy={}",
                customerId,
                relatedCustomerId,
                relationshipType,
                createdBy);

        CustomerRelationship relationship = customerService
                .createCustomerRelationship(customerId, relatedCustomerId, relationshipType, createdBy);

        log.info("Successfully created relationship with ID: {}", relationship.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(relationship);
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get customer relationships", description = "Retrieves all relationships for a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Relationships retrieved successfully") })
    public ResponseEntity<List<CustomerRelationship>> getCustomerRelationships(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId) {

        log.info("Fetching relationships for customer: {}", customerId);

        List<CustomerRelationship> relationships = customerService.getCustomerRelationships(customerId);

        log.info("Found {} relationships for customer: {}", relationships.size(), customerId);

        return ResponseEntity.ok(relationships);
    }

    @GetMapping("/related-customers")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get related customers", description = "Retrieves customers related to a specific customer with optional type filtering")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Related customers retrieved successfully") })
    public ResponseEntity<List<Customer>> getRelatedCustomers(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Optional relationship type filter") @RequestParam(required = false) CustomerRelationshipType relationshipType) {

        log.info("Fetching related customers for: {}, type filter: {}", customerId, relationshipType);

        List<Customer> relatedCustomers = customerService.getRelatedCustomers(customerId, relationshipType);

        log.info("Found {} related customers", relatedCustomers.size());

        return ResponseEntity.ok(relatedCustomers);
    }

    @DeleteMapping("/{relationshipId}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Remove customer relationship", description = "Removes a relationship between customers")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Relationship removed successfully"),
            @ApiResponse(responseCode = "404", description = "Relationship not found") })
    public ResponseEntity<Void> removeCustomerRelationship(Authentication authentication,
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Relationship ID", required = true) @PathVariable UUID relationshipId) {

        String removedBy = CallerContextResolver.resolveUsername(authentication);

        log.info("Removing relationship: {}, removedBy: {}", relationshipId, removedBy);

        customerService.removeCustomerRelationship(relationshipId, removedBy);

        log.info("Successfully removed relationship: {}", relationshipId);

        return ResponseEntity.ok().build();
    }
}
