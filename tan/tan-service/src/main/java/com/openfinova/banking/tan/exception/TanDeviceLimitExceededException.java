package com.openfinova.banking.tan.exception;

/**
 * Thrown when a user attempts to enroll more TAN devices than the configured limit.
 */
public class TanDeviceLimitExceededException extends RuntimeException {

    public TanDeviceLimitExceededException(String message) {
        super(message);
    }
}
