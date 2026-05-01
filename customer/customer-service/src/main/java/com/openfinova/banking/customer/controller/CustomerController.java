package com.openfinova.banking.customer.controller;

import com.openfinova.banking.customer.api.entity.CustomerStatus;
import com.openfinova.banking.customer.dto.CustomerProfileUpdate;
import com.openfinova.banking.customer.dto.CustomerResponse;
import com.openfinova.banking.customer.entity.Customer;
import com.openfinova.banking.customer.mapper.CustomerMapper;
import com.openfinova.banking.customer.service.CustomerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;
import java.util.UUID;

/**
 * REST Controller for customer profile management.
 * Exposes customer-facing and administrative endpoints for:
 * - Customer profile CRUD operations
 * - Customer status management
 * - Customer listing and search
 */
@RestController
@RequestMapping("/api/v1/customers")
@Tag(name = "Customer Management", description = "APIs for managing customer profiles")
public class CustomerController {

    private static final Logger log = LoggerFactory.getLogger(CustomerController.class);

    private final CustomerService customerService;

    public CustomerController(CustomerService customerService) {
        this.customerService = customerService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Create customer", description = "Creates a new customer profile. This is typically called during customer onboarding.")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Customer created successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid customer data"),
            @ApiResponse(responseCode = "409", description = "Customer with tax ID already exists") })
    public ResponseEntity<CustomerResponse> createCustomer(@Valid @RequestBody Customer customer) {

        log.info("Creating customer");

        Customer created = customerService.createCustomer(customer);

        log.info("Successfully created customer with ID: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerMapper.toCustomerResponse(created));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get customer by ID", description = "Retrieves customer profile details by ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Customer retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<CustomerResponse> getCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID id) {

        log.info("Fetching customer: {}", id);

        Optional<Customer> customer = customerService.getCustomerById(id);

        return customer.map(c -> ResponseEntity.ok(CustomerMapper.toCustomerResponse(c))).orElseGet(() -> {
            log.warn("Customer not found: {}", id);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping("/tax-id/{taxId}")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Get customer by tax ID", description = "Retrieves customer profile by tax identification number")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Customer retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<CustomerResponse> getCustomerByTaxId(
            @Parameter(description = "Tax ID", required = true) @PathVariable String taxId) {

        log.info("Fetching customer by tax ID");

        Optional<Customer> customer = customerService.getCustomerByTaxId(taxId);

        return customer.map(c -> ResponseEntity.ok(CustomerMapper.toCustomerResponse(c))).orElseGet(() -> {
            log.warn("Customer not found for tax ID lookup");
            return ResponseEntity.notFound().build();
        });
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Update customer", description = "Updates customer basic information")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Customer updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid customer data") })
    public ResponseEntity<CustomerResponse> updateCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody Customer customerDetails) {

        log.info("Updating customer: {}", id);

        Customer updated = customerService.updateCustomer(id, customerDetails);

        log.info("Successfully updated customer: {}", id);

        return ResponseEntity.ok(CustomerMapper.toCustomerResponse(updated));
    }

    @PutMapping("/{id}/profile")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Update customer profile", description = "Updates customer profile information with audit trail")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Profile updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<CustomerResponse> updateCustomerProfile(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID id,
            @Valid @RequestBody CustomerProfileUpdate profileUpdate,
            @Parameter(description = "User performing the update") @RequestParam String updatedBy) {

        log.info("Updating customer profile: {}, updatedBy: {}", id, updatedBy);

        Customer updated = customerService.updateCustomerProfile(id, profileUpdate, updatedBy);

        log.info("Successfully updated customer profile: {}", id);

        return ResponseEntity.ok(CustomerMapper.toCustomerResponse(updated));
    }

    @PutMapping("/{id}/status")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Update customer status", description = "Updates customer status (e.g., PROSPECT to ACTIVE, ACTIVE to SUSPENDED)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Status updated successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<Void> updateCustomerStatus(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID id,
            @Parameter(description = "New customer status") @RequestParam CustomerStatus status) {

        log.info("Updating customer status: {}, newStatus: {}", id, status);

        customerService.updateCustomerStatus(id, status);

        log.info("Successfully updated customer status: {}", id);

        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "List customers", description = "Lists customers with optional status filtering and pagination")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Customers retrieved successfully") })
    public ResponseEntity<Page<CustomerResponse>> listCustomers(
            @Parameter(description = "Optional status filter") @RequestParam(required = false) CustomerStatus status,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("Listing customers with status filter: {}, page: {}", status, pageable.getPageNumber());

        Page<Customer> customers = customerService.listCustomers(status, pageable);
        Page<CustomerResponse> responses = customers.map(CustomerMapper::toCustomerResponse);

        log.info("Found {} customers (page {})", responses.getNumberOfElements(), pageable.getPageNumber());

        return ResponseEntity.ok(responses);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Delete customer", description = "Deletes a customer profile (typically a logical delete)")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Customer deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found") })
    public ResponseEntity<Void> deleteCustomer(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID id) {

        log.info("Deleting customer: {}", id);

        customerService.deleteCustomer(id);

        log.info("Successfully deleted customer: {}", id);

        return ResponseEntity.ok().build();
    }
}
