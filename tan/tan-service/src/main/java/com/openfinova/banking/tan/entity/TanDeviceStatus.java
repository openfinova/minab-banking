package com.openfinova.banking.tan.entity;

/**
 * Lifecycle state of a {@link TanDevice} enrollment.
 *
 * Transitions are forward-only except that {@code REVOKED} is terminal: a revoked device is
 * never reactivated; the user must enroll a new device instead.
 *
 * @see TanDevice
 * @see com.openfinova.banking.tan.service.TanDeviceService
 */
public enum TanDeviceStatus {

    /**
     * Secret submitted by the mobile app but enrollment not yet confirmed on a trusted channel.
     * The device cannot authorize payments until the user verifies the confirmation code.
     */
    PENDING_ENROLLMENT,

    /**
     * Enrollment confirmed; the device may generate TAN codes and authorize pending payments.
     * Counts toward the per-user device limit.
     */
    ACTIVE,

    /**
     * User deregistered the device. Retained for audit; excluded from listings, device limits,
     * and payment authorization.
     */
    REVOKED
}
