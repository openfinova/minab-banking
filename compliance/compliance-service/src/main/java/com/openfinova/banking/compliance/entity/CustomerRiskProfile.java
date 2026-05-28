package com.openfinova.banking.compliance.entity;

import java.time.LocalDateTime;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import com.openfinova.banking.compliance.api.entity.RiskRating;

@Entity
@Table(name = "customer_risk_profiles")
public class CustomerRiskProfile {

    @Id
    private UUID customerId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RiskRating riskRating;

    @Column(nullable = false)
    private boolean pepMatch;

    @Column(nullable = false)
    private boolean sanctionMatch;

    @Column(nullable = false)
    private LocalDateTime lastEvaluatedAt;

    public CustomerRiskProfile() {
    }

    public CustomerRiskProfile(UUID customerId, RiskRating riskRating, boolean pepMatch, boolean sanctionMatch,
            LocalDateTime lastEvaluatedAt) {
        this.customerId = customerId;
        this.riskRating = riskRating;
        this.pepMatch = pepMatch;
        this.sanctionMatch = sanctionMatch;
        this.lastEvaluatedAt = lastEvaluatedAt;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
    }

    public RiskRating getRiskRating() {
        return riskRating;
    }

    public void setRiskRating(RiskRating riskRating) {
        this.riskRating = riskRating;
    }

    public boolean isPepMatch() {
        return pepMatch;
    }

    public void setPepMatch(boolean pepMatch) {
        this.pepMatch = pepMatch;
    }

    public boolean isSanctionMatch() {
        return sanctionMatch;
    }

    public void setSanctionMatch(boolean sanctionMatch) {
        this.sanctionMatch = sanctionMatch;
    }

    public LocalDateTime getLastEvaluatedAt() {
        return lastEvaluatedAt;
    }

    public void setLastEvaluatedAt(LocalDateTime lastEvaluatedAt) {
        this.lastEvaluatedAt = lastEvaluatedAt;
    }
}
