package com.openfinova.banking.compliance.api.event;

import java.util.UUID;
import com.openfinova.banking.compliance.api.entity.RiskRating;

public class CustomerRiskEvaluatedEvent {
    private final UUID customerId;
    private final RiskRating riskRating;
    private final boolean pepMatch;
    private final boolean sanctionMatch;

    public CustomerRiskEvaluatedEvent(UUID customerId, RiskRating riskRating, boolean pepMatch, boolean sanctionMatch) {
        this.customerId = customerId;
        this.riskRating = riskRating;
        this.pepMatch = pepMatch;
        this.sanctionMatch = sanctionMatch;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public RiskRating getRiskRating() {
        return riskRating;
    }

    public boolean isPepMatch() {
        return pepMatch;
    }

    public boolean isSanctionMatch() {
        return sanctionMatch;
    }
}
