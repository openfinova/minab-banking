package com.openfinova.banking.keycloak.kyc;

import java.util.List;

import org.keycloak.Config;
import org.keycloak.authentication.Authenticator;
import org.keycloak.authentication.AuthenticatorFactory;
import org.keycloak.models.AuthenticationExecutionModel.Requirement;
import org.keycloak.models.KeycloakSession;
import org.keycloak.models.KeycloakSessionFactory;
import org.keycloak.provider.ProviderConfigProperty;

/**
 * Factory for {@link BankingKycAuthenticator}. Registered under {@code banking-kyc-authenticator}
 * and wired into the realm browser flow in {@code keycloak/realm/openfinova.yaml}.
 */
public class BankingKycAuthenticatorFactory implements AuthenticatorFactory {

    public static final String PROVIDER_ID = "banking-kyc-authenticator";

    private static final BankingKycAuthenticator INSTANCE = new BankingKycAuthenticator();

    private static final Requirement[] REQUIREMENT_CHOICES = {
            Requirement.REQUIRED,
            Requirement.DISABLED
    };

    @Override
    public String getId() {
        return PROVIDER_ID;
    }

    @Override
    public Authenticator create(KeycloakSession session) {
        return INSTANCE;
    }

    @Override
    public String getDisplayType() {
        return "Banking KYC / Eligibility Gate";
    }

    @Override
    public String getReferenceCategory() {
        return "banking-eligibility";
    }

    @Override
    public boolean isConfigurable() {
        return false;
    }

    @Override
    public Requirement[] getRequirementChoices() {
        return REQUIREMENT_CHOICES;
    }

    @Override
    public boolean isUserSetupAllowed() {
        return false;
    }

    @Override
    public String getHelpText() {
        return "Denies login for users the banking platform reports as ineligible (e.g. unverified KYC).";
    }

    @Override
    public List<ProviderConfigProperty> getConfigProperties() {
        return List.of();
    }

    @Override
    public void init(Config.Scope config) {
        // No init.
    }

    @Override
    public void postInit(KeycloakSessionFactory factory) {
        // No post-init.
    }

    @Override
    public void close() {
        // Nothing to release.
    }
}
