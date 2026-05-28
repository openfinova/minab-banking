package com.openfinova.banking.setup.api;

import com.openfinova.banking.setup.api.dto.BankProperties;

/**
 * Public contract for institution-wide configuration consumed by other modules.
 *
 * <p>
 * Exposes the configured legal entity name, home currency, and related bank metadata loaded from
 * setup configuration. Callers use this facade rather than reading setup properties directly.
 */
public interface BankService {

    /**
     * Returns the full bank configuration snapshot (name, currency, identifiers, etc.).
     */
    BankProperties getBankDetails();

    /**
     * Returns the configured legal or trading name of the institution.
     */
    String getBankName();

    /**
     * Returns the bank's primary accounting currency as a three-letter ISO code.
     */
    String getCurrency();
}
