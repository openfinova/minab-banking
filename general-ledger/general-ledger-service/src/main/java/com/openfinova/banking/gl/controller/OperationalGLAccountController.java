package com.openfinova.banking.gl.controller;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.gl.api.dto.ConfigureOperationalAccountRequest;
import com.openfinova.banking.gl.api.dto.OperationalGLConfigResponse;
import com.openfinova.banking.gl.api.entity.OperationalGLAccountType;
import com.openfinova.banking.gl.dto.OperationalAccountValidationResult;
import com.openfinova.banking.gl.entity.OperationalGLConfig;
import com.openfinova.banking.gl.mapper.OperationalGLConfigMapper;
import com.openfinova.banking.gl.service.OperationalGLAccountService;
import com.openfinova.banking.identity.api.principal.CallerContextResolver;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/gl/operational-accounts")
@Tag(name = "Operational GL Account Configuration", description = "APIs for managing operational GL account mappings")
public class OperationalGLAccountController {

    private static final Logger log = LoggerFactory.getLogger(OperationalGLAccountController.class);

    private final OperationalGLAccountService operationalAccountService;
    private final OperationalGLConfigMapper configMapper;

    public OperationalGLAccountController(OperationalGLAccountService operationalAccountService,
            OperationalGLConfigMapper configMapper) {
        this.operationalAccountService = operationalAccountService;
        this.configMapper = configMapper;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Configure operational account", description = "Creates or updates an operational GL account mapping")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "Configuration created successfully", content = @Content(schema = @Schema(implementation = OperationalGLConfigResponse.class))),
            @ApiResponse(responseCode = "400", description = "Invalid configuration data") })
    public ResponseEntity<OperationalGLConfigResponse> configureOperationalAccount(Authentication authentication,
            @Valid @RequestBody ConfigureOperationalAccountRequest request) {

        log.info("Configuring operational account: {}", request.getType());

        String createdBy = CallerContextResolver.resolveUsername(authentication);
        OperationalGLConfig config = operationalAccountService
                .configureOperationalAccount(request.getType(), request.getGlAccountId(), createdBy);

        log.info("Successfully configured operational account with ID: {}", config.getId());

        return ResponseEntity.status(HttpStatus.CREATED).body(configMapper.toResponse(config));
    }

    @GetMapping("/{type}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get operational account", description = "Retrieves the GL account ID for a specific operational account type")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Account found"),
            @ApiResponse(responseCode = "404", description = "Configuration not found") })
    public ResponseEntity<Map<String, Object>> getOperationalGLAccount(
            @Parameter(description = "Operational account type", required = true) @PathVariable OperationalGLAccountType type) {

        log.info("Fetching operational account: {}", type);

        UUID glAccountId = operationalAccountService.getOperationalGLAccountOrNull(type);

        if (glAccountId == null) {
            log.warn("Operational account not configured: {}", type);
            return ResponseEntity.notFound().build();
        }

        log.info("Found operational account: {} -> {}", type, glAccountId);

        return ResponseEntity.ok(Map.of("type", type, "glAccountId", glAccountId));
    }

    @GetMapping("/configuration/{type}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get configuration details", description = "Retrieves the full configuration for an operational account")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Configuration found", content = @Content(schema = @Schema(implementation = OperationalGLConfigResponse.class))),
            @ApiResponse(responseCode = "404", description = "Configuration not found") })
    public ResponseEntity<OperationalGLConfigResponse> getConfiguration(
            @Parameter(description = "Operational account type", required = true) @PathVariable OperationalGLAccountType type) {

        log.info("Fetching configuration for: {}", type);

        return operationalAccountService.getConfiguration(type).map(config -> {
            log.info("Found configuration with ID: {}", config.getId());
            return ResponseEntity.ok(configMapper.toResponse(config));
        }).orElseGet(() -> {
            log.warn("Configuration not found: {}", type);
            return ResponseEntity.notFound().build();
        });
    }

    @GetMapping
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Get all configurations", description = "Retrieves all active operational account configurations")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Configurations retrieved successfully") })
    public ResponseEntity<List<OperationalGLConfigResponse>> getAllActiveConfigurations() {
        log.info("Fetching all active operational account configurations");

        List<OperationalGLConfig> configs = operationalAccountService.getAllActiveConfigurations();
        List<OperationalGLConfigResponse> response = configs.stream().map(configMapper::toResponse).toList();

        log.info("Retrieved {} active configurations", response.size());

        return ResponseEntity.ok(response);
    }

    @PostMapping("/{configId}/deactivate")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Deactivate configuration", description = "Deactivates an operational account configuration")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Configuration deactivated successfully"),
            @ApiResponse(responseCode = "404", description = "Configuration not found") })
    public ResponseEntity<Map<String, String>> deactivateConfiguration(Authentication authentication,
            @Parameter(description = "Configuration ID", required = true) @PathVariable UUID configId) {

        String deactivatedBy = CallerContextResolver.resolveUsername(authentication);

        log.info("Deactivating configuration: {} by {}", configId, deactivatedBy);

        operationalAccountService.deactivateConfiguration(configId, deactivatedBy);
        log.info("Successfully deactivated configuration: {}", configId);

        return ResponseEntity
                .ok(Map.of("message", "Configuration deactivated successfully", "configId", configId.toString()));
    }

    @PostMapping("/{configId}/activate")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Activate configuration", description = "Activates an operational account configuration")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Configuration activated successfully"),
            @ApiResponse(responseCode = "404", description = "Configuration not found") })
    public ResponseEntity<Map<String, String>> activateConfiguration(Authentication authentication,
            @Parameter(description = "Configuration ID", required = true) @PathVariable UUID configId) {

        String activatedBy = CallerContextResolver.resolveUsername(authentication);

        log.info("Activating configuration: {} by {}", configId, activatedBy);

        operationalAccountService.activateConfiguration(configId, activatedBy);
        log.info("Successfully activated configuration: {}", configId);

        return ResponseEntity
                .ok(Map.of("message", "Configuration activated successfully", "configId", configId.toString()));
    }

    @GetMapping("/check/{type}")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Check if configured", description = "Checks if an operational account is configured")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Check result") })
    public ResponseEntity<Map<String, Object>> isConfigured(
            @Parameter(description = "Operational account type", required = true) @PathVariable OperationalGLAccountType type) {

        log.info("Checking if operational account is configured: {}", type);

        boolean configured = operationalAccountService.isConfigured(type);
        log.info("Operational account {} configured: {}", type, configured);

        return ResponseEntity.ok(Map.of("type", type, "configured", configured));
    }

    @PostMapping("/standard")
    @PreAuthorize("hasAuthority('gl:approve')")
    @Operation(summary = "Create standard configurations", description = "Creates standard operational account configurations")
    @ApiResponses(value = { @ApiResponse(responseCode = "201", description = "Standard configurations created"),
            @ApiResponse(responseCode = "400", description = "Invalid request") })
    public ResponseEntity<Map<String, Object>> createStandardOperationalAccounts(Authentication authentication) {

        String createdBy = CallerContextResolver.resolveUsername(authentication);

        log.info("Creating standard operational accounts by {}", createdBy);

        int count = operationalAccountService.createStandardOperationalAccounts(createdBy);
        log.info("Created {} standard operational account configurations", count);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(Map.of("message", "Standard operational accounts created", "count", count));
    }

    @GetMapping("/validate")
    @PreAuthorize("hasAuthority('gl:read')")
    @Operation(summary = "Validate operational accounts", description = "Validates that all required operational accounts are configured")
    @ApiResponses(value = { @ApiResponse(responseCode = "200", description = "Validation result") })
    public ResponseEntity<Map<String, Object>> validateOperationalAccounts() {

        log.info("Validating operational accounts");

        OperationalAccountValidationResult result = operationalAccountService.validateOperationalAccounts();

        log.info("Validation result: {}", result.isValid() ? "VALID" : "INVALID");

        return ResponseEntity.ok(Map.of("valid", result.isValid(), "missingTypes", result.getMissingTypes()));
    }
}
