package com.openfinova.banking.identity.dto;

import java.util.List;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "TOTP enrollment material; recovery codes are shown once — store securely")
public class MfaSetupResponse {

    private String secret;
    private String qrUri;
    private List<String> recoveryCodes;

    public MfaSetupResponse(String secret, String qrUri, List<String> recoveryCodes) {
        this.secret = secret;
        this.qrUri = qrUri;
        this.recoveryCodes = recoveryCodes;
    }

    public String getSecret() {
        return secret;
    }

    public String getQrUri() {
        return qrUri;
    }

    public List<String> getRecoveryCodes() {
        return recoveryCodes;
    }
}
