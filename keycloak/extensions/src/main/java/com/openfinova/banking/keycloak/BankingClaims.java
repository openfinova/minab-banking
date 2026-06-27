package com.openfinova.banking.keycloak;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Banking authorization data fetched from the banking platform at token issuance / login.
 *
 * The banking platform (not Keycloak) remains the source of truth for permissions, customer
 * linkage, and KYC. This is the wire shape of {@code GET /internal/identity/claims/{userId}}.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class BankingClaims {

    @JsonProperty("bankingUserId")
    public String bankingUserId;

    @JsonProperty("username")
    public String username;

    @JsonProperty("userType")
    public String userType;

    @JsonProperty("permissions")
    public List<String> permissions;

    @JsonProperty("customerPartyId")
    public String customerPartyId;

    @JsonProperty("glApprovalRole")
    public String glApprovalRole;

    @JsonProperty("branchCode")
    public String branchCode;

    @JsonProperty("employeeId")
    public String employeeId;

    @JsonProperty("forcePasswordChange")
    public boolean forcePasswordChange;

    @JsonProperty("kycStatus")
    public String kycStatus;

    @JsonProperty("mfaEnabled")
    public boolean mfaEnabled;

    /** True when the banking platform permits a token to be issued for this user (KYC, status, ...). */
    @JsonProperty("eligible")
    public boolean eligible;

    /** Human-readable reason when {@link #eligible} is false; surfaced to login failure. */
    @JsonProperty("denyReason")
    public String denyReason;
}
