package com.openfinova.banking.config;

/**
 * Central registry of Spring cache names used across domain modules loaded into banking-app.
 */
public final class BankingCacheNames {

    public static final String EXCHANGE_RATES = "exchangeRates";
    public static final String LOAN_PRODUCTS = "loanProducts";
    public static final String FEE_RULES = "feeRules";
    public static final String CUSTOMER_TIERS = "customerTiers";
    public static final String FEE_WAIVERS = "feeWaivers";
    public static final String GL_ACCOUNTS = "glAccounts";
    public static final String OPERATIONAL_GL_CONFIG = "operationalGlConfig";
    public static final String GL_AUTH_LIMITS = "glAuthLimits";
    public static final String HOLIDAYS = "holidays";
    public static final String BANKING_ROLES = "bankingRoles";
    public static final String COMPLIANCE_RULES = "complianceRules";

    /** All caches registered by {@link CacheConfiguration}. */
    public static final String[] ALL = { EXCHANGE_RATES, LOAN_PRODUCTS, FEE_RULES, CUSTOMER_TIERS, FEE_WAIVERS,
            GL_ACCOUNTS, OPERATIONAL_GL_CONFIG, GL_AUTH_LIMITS, HOLIDAYS, BANKING_ROLES, COMPLIANCE_RULES, };

    private BankingCacheNames() {
    }
}
