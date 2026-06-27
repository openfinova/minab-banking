package com.openfinova.banking.tan.controller;

import java.util.UUID;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.openfinova.banking.identity.api.principal.BankingPrincipal;
import com.openfinova.banking.tan.dto.PaymentQrResponse;
import com.openfinova.banking.tan.dto.PendingTransactionResponse;
import com.openfinova.banking.tan.dto.VerifyTanRequest;
import com.openfinova.banking.tan.dto.VerifyTanResponse;
import com.openfinova.banking.tan.service.TanAuthorizationService;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/tan")
@Tag(name = "TAN Authorization", description = "Transaction-bound TAN authorization (SCA)")
public class TanAuthorizationController {

    private final TanAuthorizationService tanAuthorizationService;

    public TanAuthorizationController(TanAuthorizationService tanAuthorizationService) {
        this.tanAuthorizationService = tanAuthorizationService;
    }

    @GetMapping("/qr/{txnId}")
    @PreAuthorize("hasAuthority('payment:initiate') or hasAuthority('payment:initiate:own')")
    public ResponseEntity<PaymentQrResponse> paymentQr(Authentication authentication, @PathVariable UUID txnId) {
        return ResponseEntity.ok(tanAuthorizationService.buildPaymentQr(BankingPrincipal.from(authentication), txnId));
    }

    @GetMapping("/pending/{txnId}")
    @PreAuthorize("hasAuthority('tan:generate')")
    public ResponseEntity<PendingTransactionResponse> pendingTransaction(Authentication authentication,
            @PathVariable UUID txnId) {
        return ResponseEntity
                .ok(tanAuthorizationService.getPendingTransaction(BankingPrincipal.from(authentication), txnId));
    }

    @PostMapping("/verify")
    @PreAuthorize("hasAuthority('payment:initiate') or hasAuthority('payment:initiate:own')")
    public ResponseEntity<VerifyTanResponse> verifyTan(Authentication authentication,
            @Valid @RequestBody VerifyTanRequest request) {
        return ResponseEntity.ok(tanAuthorizationService.verifyTan(BankingPrincipal.from(authentication), request));
    }
}
