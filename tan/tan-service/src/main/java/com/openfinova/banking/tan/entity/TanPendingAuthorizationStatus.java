package com.openfinova.banking.tan.entity;

/**
 * Status of a {@link TanPendingAuthorization} in the authorization process.
 *
 * Transitions are forward-only except that {@code EXPIRED} is terminal: an expired
 * authorization is never reactivated; the user must initiate a new authorization process.
 *
 * @see TanPendingAuthorization
 * @see com.openfinova.banking.tan.service.TanPendingAuthorizationService
 */
public enum TanPendingAuthorizationStatus {
    /**
     * The authorization is pending verification.
     */
    PENDING,

    /**
     * The authorization has been verified.
     */
    SCA_VERIFIED,

    /**
     * The authorization has expired.
     */
    EXPIRED
}
