package com.openfinova.banking.customer.controller;

import com.openfinova.banking.customer.dto.IdentificationDocumentResponse;
import com.openfinova.banking.customer.entity.IdentificationDocument;
import com.openfinova.banking.customer.mapper.CustomerMapper;
import com.openfinova.banking.customer.service.IdentificationDocumentService;
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
 * REST Controller for managing customer identification documents.
 * Exposes endpoints for:
 * - Adding identification documents
 * - Viewing identification documents
 * - Updating identification documents
 * - Verifying documents
 * - Deleting documents
 */
@RestController
@RequestMapping("/api/v1/customers/{customerId}/documents")
@Tag(name = "Identification Documents", description = "APIs for managing customer identification documents for KYC compliance")
public class IdentificationDocumentController {

    private static final Logger log = LoggerFactory.getLogger(IdentificationDocumentController.class);

    private final IdentificationDocumentService documentService;

    public IdentificationDocumentController(IdentificationDocumentService documentService) {
        this.documentService = documentService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Add identification document", description = "Records a new identification document for a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Document added successfully"),
            @ApiResponse(responseCode = "404", description = "Customer not found"),
            @ApiResponse(responseCode = "400", description = "Invalid document data") })
    public ResponseEntity<IdentificationDocumentResponse> addIdentificationDocument(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Valid @RequestBody IdentificationDocument document) {

        log.info("Adding identification document for customer: {}, type: {}", customerId, document.getType());

        IdentificationDocument created = documentService.addIdentificationDocument(customerId, document);

        log.info("Successfully added identification document with ID: {}", created.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(CustomerMapper.toDocumentResponse(created));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Get customer documents", description = "Retrieves all identification documents for a customer")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Documents retrieved successfully") })
    public ResponseEntity<List<IdentificationDocumentResponse>> getDocuments(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId) {

        log.info("Fetching identification documents for customer: {}", customerId);

        List<IdentificationDocument> documents = documentService.getDocumentsByCustomerId(customerId);

        log.info("Found {} identification documents for customer: {}", documents.size(), customerId);

        return ResponseEntity.ok(CustomerMapper.toDocumentResponseList(documents));
    }

    @GetMapping("/{documentId}")
    @PreAuthorize("hasAuthority('customer:pii:read')")
    @Operation(summary = "Get document by ID", description = "Retrieves a specific identification document by its ID")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Document retrieved successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found") })
    public ResponseEntity<IdentificationDocumentResponse> getDocument(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Document ID", required = true) @PathVariable UUID documentId) {

        log.info("Fetching identification document: {}", documentId);

        IdentificationDocument document = documentService.getDocumentById(customerId, documentId);

        return ResponseEntity.ok(CustomerMapper.toDocumentResponse(document));
    }

    @PutMapping("/{documentId}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Update identification document", description = "Updates details of an existing identification document")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Document updated successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found"),
            @ApiResponse(responseCode = "400", description = "Invalid document data") })
    public ResponseEntity<IdentificationDocumentResponse> updateIdentificationDocument(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Document ID", required = true) @PathVariable UUID documentId,
            @Valid @RequestBody IdentificationDocument documentDetails) {

        log.info("Updating identification document: {}", documentId);

        IdentificationDocument updated = documentService
                .updateIdentificationDocument(customerId, documentId, documentDetails);

        log.info("Successfully updated identification document: {}", documentId);

        return ResponseEntity.ok(CustomerMapper.toDocumentResponse(updated));
    }

    @PutMapping("/{documentId}/verify")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Verify document", description = "Marks an identification document as verified by a back-office agent. Administrative operation.")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Document verified successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found") })
    public ResponseEntity<Void> verifyDocument(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Document ID", required = true) @PathVariable UUID documentId) {

        log.info("Verifying identification document: {}", documentId);

        documentService.verifyDocument(customerId, documentId);

        log.info("Successfully verified identification document: {}", documentId);

        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{documentId}")
    @PreAuthorize("hasAuthority('customer:write')")
    @Operation(summary = "Delete identification document", description = "Removes an identification document record")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Document deleted successfully"),
            @ApiResponse(responseCode = "404", description = "Document not found") })
    public ResponseEntity<Void> deleteIdentificationDocument(
            @Parameter(description = "Customer ID", required = true) @PathVariable UUID customerId,
            @Parameter(description = "Document ID", required = true) @PathVariable UUID documentId) {

        log.info("Deleting identification document: {}", documentId);

        documentService.deleteIdentificationDocument(customerId, documentId);

        log.info("Successfully deleted identification document: {}", documentId);

        return ResponseEntity.ok().build();
    }
}
