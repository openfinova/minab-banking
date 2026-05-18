package com.openfinova.banking.compliance.service;

import org.springframework.stereotype.Component;

import com.openfinova.banking.compliance.api.entity.RiskRating;
import com.openfinova.banking.customer.api.dto.CustomerInfo;

@Component
public class RiskEngine {

    public RiskRating calculateRisk(CustomerInfo customer, boolean pepMatch, boolean sanctionMatch) {
        if (sanctionMatch) {
            return RiskRating.UNACCEPTABLE;
        }

        if (pepMatch) {
            return RiskRating.HIGH;
        }

        int riskScore = 0;

        // High risk country
        String country = customer.getResidenceCountry();
        if (country != null && ("IR".equalsIgnoreCase(country) || "SY".equalsIgnoreCase(country)
                || "NK".equalsIgnoreCase(country))) {
            return RiskRating.UNACCEPTABLE;
        }
        if (country != null && ("RU".equalsIgnoreCase(country) || "CU".equalsIgnoreCase(country)
                || "BY".equalsIgnoreCase(country))) {
            riskScore += 50;
        }

        if (riskScore >= 50) {
            return RiskRating.HIGH;
        } else if (riskScore >= 20) {
            return RiskRating.MEDIUM;
        }

        return RiskRating.LOW;
    }
}
