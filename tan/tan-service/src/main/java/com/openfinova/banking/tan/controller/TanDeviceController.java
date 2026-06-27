package com.openfinova.banking.tan.controller;

import java.util.List;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.tan.dto.AttestationNonceResponse;
import com.openfinova.banking.tan.dto.ConfirmDeviceRequest;
import com.openfinova.banking.tan.dto.EnrollDeviceRequest;
import com.openfinova.banking.tan.dto.EnrollDeviceResponse;
import com.openfinova.banking.tan.dto.EnrollmentQrResponse;
import com.openfinova.banking.tan.dto.TanDeviceResponse;
import com.openfinova.banking.tan.service.TanDeviceService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tan/devices")
@Tag(name = "TAN Devices", description = "TAN app device enrollment and management")
public class TanDeviceController {

    private final TanDeviceService tanDeviceService;

    public TanDeviceController(TanDeviceService tanDeviceService) {
        this.tanDeviceService = tanDeviceService;
    }

    @PostMapping("/enrollment-qr")
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    public ResponseEntity<EnrollmentQrResponse> createEnrollmentQr(Authentication authentication) {
        return ResponseEntity.ok(tanDeviceService.createEnrollmentQr(BankingPrincipal.from(authentication)));
    }

    @GetMapping("/attestation-nonce")
    public ResponseEntity<AttestationNonceResponse> attestationNonce(
            @RequestHeader("Enrollment-Token") String enrollmentToken) {
        return ResponseEntity.ok(tanDeviceService.issueAttestationNonce(enrollmentToken));
    }

    @PostMapping
    public ResponseEntity<EnrollDeviceResponse> enrollDevice(@Valid @RequestBody EnrollDeviceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tanDeviceService.enrollDevice(request));
    }

    @PostMapping("/{id}/confirm")
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    public ResponseEntity<Void> confirmDevice(Authentication authentication, @PathVariable("id") UUID deviceId,
            @Valid @RequestBody ConfirmDeviceRequest request) {
        tanDeviceService.confirmDevice(BankingPrincipal.from(authentication), deviceId, request);
        return ResponseEntity.ok().build();
    }

    @GetMapping
    @PreAuthorize("hasAuthority('mfa:manage:own') or hasAuthority('tan:generate')")
    public ResponseEntity<List<TanDeviceResponse>> listDevices(Authentication authentication) {
        return ResponseEntity.ok(tanDeviceService.listDevices(BankingPrincipal.from(authentication)));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('mfa:manage:own')")
    public ResponseEntity<Void> revokeDevice(Authentication authentication, @PathVariable("id") UUID deviceId) {
        tanDeviceService.revokeDevice(BankingPrincipal.from(authentication), deviceId);
        return ResponseEntity.noContent().build();
    }
}
