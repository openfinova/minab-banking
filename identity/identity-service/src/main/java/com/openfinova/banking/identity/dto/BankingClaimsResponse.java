package com.openfinova.banking.identity.dto;

import java.util.List;

/**
 * Banking authorization snapshot returned to the Keycloak claims-mapper / KYC authenticator SPI
 * at token issuance and login.
 *
 * The banking platform is the source of truth for this data; Keycloak only authenticates. The
 * field names form the wire contract consumed by the {@code banking-keycloak-spi} module and must
 * not diverge from it.
 *
 * @param bankingUserId       persistent {@code identity_users} id; becomes the JWT {@code sub}
 * @param username            login name, emitted as {@code preferred_username}
 * @param userType            STAFF or CUSTOMER
 * @param permissions         effective permission authorities (already time/scope resolved)
 * @param customerPartyId     linked customer party id, or {@code null} for staff/system
 * @param glApprovalRole      general-ledger approval role, or {@code null}
 * @param branchCode          home branch code, or {@code null}
 * @param employeeId          employee identifier, or {@code null}
 * @param forcePasswordChange whether the resource-server filter must force a password change
 * @param kycStatus           KYC status name for customers, or {@code null}
 * @param mfaEnabled          whether MFA is configured (drives acr/amr gold vs silver)
 * @param eligible            whether the banking platform permits a token/login for this user
 * @param denyReason          reason surfaced to the login failure when {@code eligible} is false
 */
public record BankingClaimsResponse(String bankingUserId, String username, String userType, List<String> permissions,
        String customerPartyId, String glApprovalRole, String branchCode, String employeeId,
        boolean forcePasswordChange, String kycStatus, boolean mfaEnabled, boolean eligible, String denyReason) {
}
