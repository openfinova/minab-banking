package com.openfinova.banking.loan.entity;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import com.openfinova.banking.common.lib.validation.ValidCurrency;
import com.openfinova.banking.loan.api.entity.CollateralStatus;
import com.openfinova.banking.loan.api.entity.CollateralType;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Collateral entity representing security pledged against a loan.
 * Tracks collateral details, valuation, and insurance information.
 */
@Entity
@Table(name = "collaterals", indexes = {
        @Index(name = "idx_collaterals_reference", columnList = "collateral_reference"),
        @Index(name = "idx_collaterals_account", columnList = "loan_account_id"),
        @Index(name = "idx_collaterals_type", columnList = "collateral_type"),
        @Index(name = "idx_collaterals_status", columnList = "status") })
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE)
public class Collateral {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank(message = "Collateral reference is required")
    @Column(name = "collateral_reference", nullable = false, unique = true, length = 50)
    @Size(max = 50, message = "Collateral reference must not exceed 50 characters")
    private String collateralReference;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loan_account_id", nullable = false)
    @NotNull(message = "Loan account is required")
    private LoanAccount loanAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "collateral_type", nullable = false, length = 30)
    @NotNull(message = "Collateral type is required")
    private CollateralType collateralType;

    @NotBlank(message = "Description is required")
    @Column(nullable = false, length = 500)
    @Size(max = 500, message = "Description must not exceed 500 characters")
    private String description;

    /**
     * Current market value of the collateral.
     */
    @NotNull(message = "Valuation amount is required")
    @DecimalMin(value = "0.0", message = "Valuation amount must be positive")
    @Column(name = "valuation_amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal valuationAmount;

    @Column(nullable = false, length = 3)
    @NotBlank(message = "Currency is required")
    @ValidCurrency
    private String currency;

    @NotNull(message = "Valuation date is required")
    @Column(name = "valuation_date", nullable = false)
    private LocalDate valuationDate;

    /**
     * Name of the person or entity who performed the valuation.
     */
    @Column(name = "valued_by", length = 100)
    @Size(max = 100, message = "Valued by must not exceed 100 characters")
    private String valuedBy;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Status is required")
    private CollateralStatus status = CollateralStatus.ACTIVE;

    /**
     * Physical location of the collateral.
     */
    @Column(length = 200)
    @Size(max = 200, message = "Location must not exceed 200 characters")
    private String location;

    /**
     * Registration or identification number (e.g., vehicle VIN, property title number).
     */
    @Column(name = "registration_number", length = 100)
    @Size(max = 100, message = "Registration number must not exceed 100 characters")
    private String registrationNumber;

    /**
     * Insurance coverage amount for the collateral.
     */
    @Column(name = "insurance_expiry_date")
    private LocalDate insuranceExpiryDate;

    /**
     * Insurance policy number for the collateral.
     */
    @Column(name = "insurance_policy_number", length = 100)
    @Size(max = 100, message = "Insurance policy number must not exceed 100 characters")
    private String insurancePolicyNumber;

    @Column(name = "release_date")
    private LocalDate releaseDate;

    @Column(length = 1000)
    @Size(max = 1000, message = "Remarks must not exceed 1000 characters")
    private String remarks;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    // Constructors
    public Collateral() {
    }

    // Business Logic
    public boolean isActive() {
        return CollateralStatus.ACTIVE.equals(status);
    }

    public boolean isReleased() {
        return CollateralStatus.RELEASED.equals(status);
    }

    /**
     * Validates if the collateral can transition to the specified status.
     *
     * @param newStatus the target status
     * @return true if the transition is valid, false otherwise
     */
    public boolean canTransitionTo(CollateralStatus newStatus) {
        if (this.status == newStatus) {
            return true; // Same status is always valid
        }

        return switch (this.status) {
            case ACTIVE -> newStatus == CollateralStatus.UNDER_VALUATION || newStatus == CollateralStatus.RELEASED
                    || newStatus == CollateralStatus.LIQUIDATED;
            case UNDER_VALUATION -> newStatus == CollateralStatus.ACTIVE;
            case RELEASED, LIQUIDATED -> false; // Terminal states
        };
    }

    /**
     * Gets a descriptive error message for invalid status transitions.
     *
     * @param newStatus the attempted target status
     * @return error message describing why the transition is invalid
     */
    public String getTransitionErrorMessage(CollateralStatus newStatus) {
        if (this.status == newStatus) {
            return String.format("Collateral is already in %s status", newStatus);
        }

        return switch (this.status) {
            case ACTIVE -> String.format(
                    "Cannot transition from ACTIVE to %s. Valid transitions: UNDER_VALUATION, RELEASED, LIQUIDATED",
                    newStatus);
            case UNDER_VALUATION ->
                String.format("Cannot transition from UNDER_VALUATION to %s. Valid transition: ACTIVE", newStatus);
            case RELEASED -> "Cannot change status of RELEASED collateral. This is a terminal state";
            case LIQUIDATED -> "Cannot change status of LIQUIDATED collateral. This is a terminal state";
        };
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getCollateralReference() {
        return collateralReference;
    }

    public void setCollateralReference(String collateralReference) {
        this.collateralReference = collateralReference;
    }

    public LoanAccount getLoanAccount() {
        return loanAccount;
    }

    public void setLoanAccount(LoanAccount loanAccount) {
        this.loanAccount = loanAccount;
    }

    public CollateralType getCollateralType() {
        return collateralType;
    }

    public void setCollateralType(CollateralType collateralType) {
        this.collateralType = collateralType;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getValuationAmount() {
        return valuationAmount;
    }

    public void setValuationAmount(BigDecimal valuationAmount) {
        this.valuationAmount = valuationAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public LocalDate getValuationDate() {
        return valuationDate;
    }

    public void setValuationDate(LocalDate valuationDate) {
        this.valuationDate = valuationDate;
    }

    public String getValuedBy() {
        return valuedBy;
    }

    public void setValuedBy(String valuedBy) {
        this.valuedBy = valuedBy;
    }

    public CollateralStatus getStatus() {
        return status;
    }

    public void setStatus(CollateralStatus status) {
        this.status = status;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }

    public String getRegistrationNumber() {
        return registrationNumber;
    }

    public void setRegistrationNumber(String registrationNumber) {
        this.registrationNumber = registrationNumber;
    }

    public LocalDate getInsuranceExpiryDate() {
        return insuranceExpiryDate;
    }

    public void setInsuranceExpiryDate(LocalDate insuranceExpiryDate) {
        this.insuranceExpiryDate = insuranceExpiryDate;
    }

    public String getInsurancePolicyNumber() {
        return insurancePolicyNumber;
    }

    public void setInsurancePolicyNumber(String insurancePolicyNumber) {
        this.insurancePolicyNumber = insurancePolicyNumber;
    }

    public LocalDate getReleaseDate() {
        return releaseDate;
    }

    public void setReleaseDate(LocalDate releaseDate) {
        this.releaseDate = releaseDate;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
