package com.openfinova.banking.customer.controller;

import com.openfinova.banking.customer.dto.AddressRequest;
import com.openfinova.banking.customer.dto.AddressResponse;
import com.openfinova.banking.customer.entity.CustomerAddress;
import com.openfinova.banking.customer.mapper.CustomerMapper;
import com.openfinova.banking.customer.service.AddressService;
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
 * REST Controller for managing customer addresses.
 *
 * Exposes endpoints for:
 * - Adding addresses to customer profiles
 * - Viewing customer addresses
 * - Updating addresses
 * - Setting primary addresses
 * - Deleting addresses
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/addresses")
@Tag(name = "Customer Addresses", description = "APIs for managing customer physical and mailing addresses")
public class AddressController {

    private static final Logger log = LoggerFactory.getLogger(AddressController.class);

    private final AddressService addressService;

    public AddressController(AddressService addressService) {
        this.addressService = addressService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Add address", description = "Adds a new address to a customer profile")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Address added successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid address data") })
    public ResponseEntity<AddressResponse> addAddress(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody AddressRequest address) {

        log.info("Adding address for customer: {}, type: {}", customerId, address.getType());

        CustomerAddress created = addressService.addAddress(customerId, CustomerMapper.toAddressEntity(address));

        log.info("Successfully added address with ID: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerMapper.toAddressResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get customer addresses", description = "Retrieves all addresses for a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Addresses retrieved successfully") })
    public ResponseEntity<List<AddressResponse>> getAddresses(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId) {

        log.info("Fetching addresses for customer: {}", customerId);

        List<CustomerAddress> addresses = addressService.getAddressesByCustomerId(customerId);

        log.info("Found {} addresses for customer: {}", addresses.size(), customerId);

        return ResponseEntity.ok(CustomerMapper.toAddressResponseList(addresses));
    }

    @GetMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customer:read')")
    @Operation(summary = "Get address by ID", description = "Retrieves a specific address by its ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Address retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found") })
    public ResponseEntity<AddressResponse> getAddress(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Address ID", required = true) @PathVariable UUID addressId) {

        log.info("Fetching address: {}", addressId);

        CustomerAddress address = addressService.getAddressById(customerId, addressId);

        return ResponseEntity.ok(CustomerMapper.toAddressResponse(address));
    }

    @PutMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Update address", description = "Updates an existing address")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Address updated successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found"),
            @ApiResponse(responseCode = "400", description = "Invalid address data") })
    public ResponseEntity<AddressResponse> updateAddress(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Address ID", required = true) @PathVariable UUID addressId,
            @Valid @RequestBody AddressRequest addressDetails) {

        log.info("Updating address: {}", addressId);

        CustomerAddress updated = addressService
                .updateAddress(customerId, addressId, CustomerMapper.toAddressEntity(addressDetails));

        log.info("Successfully updated address: {}", addressId);

        return ResponseEntity.ok(CustomerMapper.toAddressResponse(updated));
    }

    @PutMapping("/{addressId}/set-primary")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Set primary address", description = "Designates an address as the primary address for its type")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Primary address set successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found") })
    public ResponseEntity<Void> setPrimaryAddress(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Address ID", required = true) @PathVariable UUID addressId) {

        log.info("Setting primary address: {}", addressId);

        addressService.setPrimaryAddress(customerId, addressId);

        log.info("Successfully set primary address: {}", addressId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{addressId}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Delete address", description = "Removes an address from a customer profile")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Address deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Address not found") })
    public ResponseEntity<Void> deleteAddress(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Address ID", required = true) @PathVariable UUID addressId) {

        log.info("Deleting address: {}", addressId);

        addressService.deleteAddress(customerId, addressId);

        log.info("Successfully deleted address: {}", addressId);

        return ResponseEntity.ok().build();
    }
}
