package com.openfinova.banking.compliance.service;

public interface ScreeningService {
    /**
     * Checks if a name and country combination appears on a sanctions list.
     */
    boolean checkSanctions(String name, String country);

    /**
     * Checks if a name and country combination appears on a PEP list.
     */
    boolean checkPep(String name, String country);
}
