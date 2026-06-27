package com.openfinova.banking.tan.exception;

/**
 * Thrown when a TAN code is submitted more than once for the same transaction within the validity window.
 */
public class TanCodeAlreadyUsedException extends RuntimeException {

    public TanCodeAlreadyUsedException(String message) {
        super(message);
    }
}
