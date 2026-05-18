package com.openfinova.banking.compliance.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class MockScreeningServiceImpl implements ScreeningService {

    private static final Logger logger = LoggerFactory.getLogger(MockScreeningServiceImpl.class);

    @Override
    public boolean checkSanctions(String name, String country) {
        logger.debug("Running mock sanctions check for {}, {}", name, country);
        // Mock logic: return true for any name containing "Sanctioned"
        if (name != null && name.toLowerCase().contains("sanctioned")) {
            return true;
        }
        // Mock logic: return true for any country in a mock list, e.g. "NK", "SY"
        if ("NK".equalsIgnoreCase(country) || "SY".equalsIgnoreCase(country)) {
            return true;
        }
        return false;
    }

    @Override
    public boolean checkPep(String name, String country) {
        logger.debug("Running mock PEP check for {}, {}", name, country);
        // Mock logic: return true for any name containing "Politician"
        if (name != null && name.toLowerCase().contains("politician")) {
            return true;
        }
        return false;
    }
}
