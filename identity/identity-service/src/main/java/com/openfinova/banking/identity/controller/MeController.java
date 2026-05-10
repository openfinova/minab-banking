package com.openfinova.banking.identity.controller;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.common.lib.exception.ResourceNotFoundException;
import com.openfinova.banking.identity.api.audit.AuditActor;
import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.identity.dto.ChangePasswordRequest;
import com.openfinova.banking.identity.dto.MfaDisableRequest;
import com.openfinova.banking.identity.dto.MfaSetupResponse;
import com.openfinova.banking.identity.dto.MfaVerifyRequest;
import com.openfinova.banking.identity.dto.MyAccessInfoResponse;
import com.openfinova.banking.identity.dto.SecurityAuditEventResponse;
import com.openfinova.banking.identity.entity.BankingUser;
import com.openfinova.banking.identity.entity.SecurityAuditEventType;
import com.openfinova.banking.identity.repository.UserRepository;
import com.openfinova.banking.identity.service.MfaService;
import com.openfinova.banking.identity.service.SecurityAuditService;
import com.openfinova.banking.identity.service.UserManagementService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

/**
 * Self-service endpoints for the currently authenticated user.
 * Handles credential management (password change, MFA enrollment) --
 * profile data is managed in the customer / employee modules.
 */
@RestController
@RequestMapping("/api/v1/identity/me")
@Tag(name = "Self-Service", description = """
        Credential and TOTP MFA operations for the **currently authenticated** user (JWT subject). \
        Requires OAuth2 access tokens with the appropriate fine-grained permissions \
        (`profile:read:own`, `password:change:own`, `mfa:manage:own`, `audit:read:own`). \
        See the global API description for the MFA enrollment sequence.""")
@SecurityRequirement(name = "oauth2")
public class MeController {

    private static final Logger log = LoggerFactory.getLogger(MeController.class);

    private static final String OPAQUE_NOT_FOUND = "The requested resource was not found.";

    private final UserRepository userRepository;
    private final UserManagementService userService;
    private final MfaService mfaService;
    private final SecurityAuditService auditService;
    private final PasswordEncoder passwordEncoder;

    public MeController(UserRepository userRepository, UserManagementService userService, MfaService mfaService,
            SecurityAuditService auditService, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.userService = userService;
        this.mfaService = mfaService;
        this.auditService = auditService;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('profile:read:own')")
    @Operation(summary = "View own access information", description = "Returns roles, MFA status, password policy flags, and profile fields available to the caller.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Current user's access summary"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Missing permission profile:read:own") })
    public ResponseEntity<MyAccessInfoResponse> getMyInfo(Authentication auth) {
        BankingUser user = resolveUser(auth);
        return ResponseEntity.ok(MyAccessInfoResponse.from(user));
    }

    @PatchMapping("/password")
    @PreAuthorize("hasAuthority('password:change:own')")
    @Operation(summary = "Change own password", description = "Verifies the current password, then applies password policy rules to the new password.")
    @ApiResponses({ @ApiResponse(responseCode = "204", description = "Password updated"),
            @ApiResponse(responseCode = "400", description = "Validation or policy failure"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Missing permission password:change:own") })
    public ResponseEntity<Void> changePassword(Authentication auth, @Valid @RequestBody ChangePasswordRequest request) {
        BankingPrincipal principal = BankingPrincipal.from(auth);
        UUID userId = principal.userId();
        if (userId == null) {
            userId = resolveUser(auth).getId();
        }
        AuditActor actor = AuditActor.fromPrincipal(principal);
        userService.changeOwnPassword(userId, request.getCurrentPassword(), request.getNewPassword(), actor);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/mfa/setup")
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    @Transactional
    @Operation(summary = "Initiate TOTP MFA enrollment", description = """
            Generates a TOTP secret, otpauth URI for QR display, and hashed recovery codes stored on the user. \
            **Step 1 of 2** — call `/mfa/verify` with a valid 6-digit code to activate MFA. \
            Fails if MFA is already enabled (disable first to re-enroll).""")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Secret, QR URI, and plaintext recovery codes (show once)"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Missing permission mfa:manage:own"),
            @ApiResponse(responseCode = "409", description = "MFA already enabled or invalid state") })
    public ResponseEntity<MfaSetupResponse> setupMfa(Authentication auth) {
        BankingUser user = resolveUser(auth);
        if (user.isMfaEnabled()) {
            throw new IllegalStateException("MFA is already enabled. Disable it first to re-enroll.");
        }

        String secret = mfaService.generateSecret();
        String qrUri = mfaService.generateQrUri(secret, user.getUsername(), "OpenFinova Banking");
        List<String> recoveryCodes = mfaService.generateRecoveryCodes();

        user.setMfaSecret(secret);
        user.setMfaRecoveryCodes(mfaService.hashRecoveryCodes(recoveryCodes));
        userRepository.save(user);

        return ResponseEntity.ok(new MfaSetupResponse(secret, qrUri, recoveryCodes));
    }

    @PostMapping("/mfa/verify")
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    @Transactional
    @Operation(summary = "Confirm TOTP to finalize MFA enrollment", description = """
            **Step 2 of 2** — validates the TOTP code against the pending secret from `/mfa/setup`, \
            then sets MFA enabled and writes an audit event.""")
    @ApiResponses({ @ApiResponse(responseCode = "204", description = "MFA enabled"),
            @ApiResponse(responseCode = "400", description = "Invalid code or MFA not in setup state"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Missing permission mfa:manage:own") })
    public ResponseEntity<Void> verifyMfa(Authentication auth, @Valid @RequestBody MfaVerifyRequest request) {
        BankingUser user = resolveUser(auth);
        if (user.getMfaSecret() == null) {
            throw new IllegalStateException("MFA setup not initiated. Call POST /mfa/setup first.");
        }
        if (user.isMfaEnabled()) {
            throw new IllegalStateException("MFA is already enabled.");
        }
        if (!mfaService.verifyCode(user.getMfaSecret(), request.getCode())) {
            log.warn("MFA verify failed: invalid TOTP (userId={})", user.getId());
            throw new IllegalArgumentException("The verification code is invalid or has expired.");
        }

        user.setMfaEnabled(true);
        userRepository.save(user);
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        auditService.recordParticipating(
                SecurityAuditEventType.MFA_ENABLED,
                user.getId(),
                user.getUsername(),
                "MFA enrolled via self-service",
                actor);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/mfa")
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    @Transactional
    @Operation(summary = "Disable MFA", description = "Requires the account password to confirm. Clears the TOTP secret and recovery codes.")
    @ApiResponses({ @ApiResponse(responseCode = "204", description = "MFA disabled"),
            @ApiResponse(responseCode = "400", description = "Wrong password or MFA not enabled"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Missing permission mfa:manage:own") })
    public ResponseEntity<Void> disableMfa(Authentication auth, @Valid @RequestBody MfaDisableRequest request) {
        BankingUser user = resolveUser(auth);
        if (!user.isMfaEnabled()) {
            throw new IllegalStateException("MFA is not enabled.");
        }
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            log.warn("MFA disable failed: password mismatch (userId={})", user.getId());
            throw new IllegalArgumentException("Unable to verify the current password.");
        }

        user.setMfaEnabled(false);
        user.setMfaSecret(null);
        user.setMfaRecoveryCodes(Set.of());
        userRepository.save(user);
        AuditActor actor = AuditActor.fromPrincipal(BankingPrincipal.from(auth));
        auditService.recordParticipating(
                SecurityAuditEventType.MFA_DISABLED,
                user.getId(),
                user.getUsername(),
                "MFA disabled via self-service",
                actor);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/audit-events")
    @PreAuthorize("hasAuthority('audit:read:own')")
    @Operation(summary = "Search security audit events for the current user", description = "Same event model as the admin audit API, but results are restricted to the JWT subject's user id.")
    @ApiResponses({ @ApiResponse(responseCode = "200", description = "Paged audit events"),
            @ApiResponse(responseCode = "401", description = "Missing or invalid bearer token"),
            @ApiResponse(responseCode = "403", description = "Missing permission audit:read:own") })
    public ResponseEntity<Page<SecurityAuditEventResponse>> getMyAuditEvents(Authentication auth,
            @Parameter(description = "Filter by event type") @RequestParam(required = false) SecurityAuditEventType eventType,
            @Parameter(description = "Filter by client IP") @RequestParam(required = false) String ipAddress,
            @Parameter(description = "Inclusive lower bound (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime from,
            @Parameter(description = "Inclusive upper bound (ISO-8601)") @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime to,
            @Parameter(description = "Spring Data pagination (page, size, sort)") Pageable pageable) {
        UUID userId = BankingPrincipal.from(auth).userId();
        if (userId == null) {
            throw new IllegalStateException("Cannot resolve user ID from token");
        }
        Page<SecurityAuditEventResponse> page = auditService
                .search(userId, eventType, null, ipAddress, from, to, pageable).map(SecurityAuditEventResponse::from);
        return ResponseEntity.ok(page);
    }

    private BankingUser resolveUser(Authentication auth) {
        BankingPrincipal principal = BankingPrincipal.from(auth);
        String username = principal.username();
        return userRepository.findByUsername(username).orElseThrow(
                () -> ResourceNotFoundException
                        .opaque("Self-service: no user for authenticated username=" + username, OPAQUE_NOT_FOUND));
    }
}
