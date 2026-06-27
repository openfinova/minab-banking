package com.openfinova.banking.tan.api;

import java.util.UUID;

/**
 * Facade for TAN (transaction authentication) operations consumed by other modules.
 */
public interface TanService {

    /**
     * Returns whether SCA has been completed for the given transaction.
     *
     * @param transactionId core banking transaction id
     * @return true when a valid TAN was verified for this transaction
     */
    boolean isScaVerified(UUID transactionId);
}
