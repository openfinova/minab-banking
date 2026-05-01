package com.openfinova.banking.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import com.openfinova.banking.customer.api.entity.AddressType;

/**
 * Entity representing a physical or mailing address associated with a customer.
 */
@Entity
@Table(name = "customer_addresses", indexes = { @Index(name = "idx_cust_addr_customer", columnList = "customer_id"),
        @Index(name = "idx_cust_addr_type", columnList = "type") })
public class CustomerAddress {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @NotNull(message = "Address type is required")
    private AddressType type;

    @Column(name = "line1", nullable = false, length = 100)
    @NotBlank(message = "Address line 1 is required")
    @Size(max = 100, message = "Line 1 must not exceed 100 characters")
    private String line1;

    @Column(name = "line2", length = 100)
    @Size(max = 100, message = "Line 2 must not exceed 100 characters")
    private String line2;

    @Column(name = "city", nullable = false, length = 100)
    @NotBlank(message = "City is required")
    @Size(max = 100, message = "City must not exceed 100 characters")
    private String city;

    @Column(name = "state", length = 100)
    @Size(max = 100, message = "State must not exceed 100 characters")
    private String state;

    @Column(name = "postal_code", nullable = false, length = 20)
    @NotBlank(message = "Postal code is required")
    @Size(max = 20, message = "Postal code must not exceed 20 characters")
    private String postalCode;

    @Column(name = "country", nullable = false, length = 2)
    @NotBlank(message = "Country is required")
    @Size(max = 2, message = "Country must be ISO 3166-1 alpha-2 (2 characters)")
    @jakarta.validation.constraints.Pattern(regexp = "[A-Z]{2}", message = "Country must be ISO 3166-1 alpha-2 country code")
    private String country;

    @Column(name = "is_primary", nullable = false)
    private boolean isPrimary = false;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * Date from which this address became valid.
     * Enables address history: do not update, instead add a new record.
     */
    @Column(name = "valid_from")
    private LocalDate validFrom;

    /**
     * Date on which this address ceased to be valid. Null means currently active.
     */
    @Column(name = "valid_to")
    private LocalDate validTo;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Soft-delete timestamp. When set, the address is considered deleted for display
     * but retained for regulatory audit. Null means active.
     */
    @Column(name = "deleted_at")
    private LocalDateTime deletedAt;

    // Constructors
    public CustomerAddress() {
    }

    public CustomerAddress(Customer customer, AddressType type, String line1, String city, String postalCode,
            String country) {
        this.customer = customer;
        this.type = type;
        this.line1 = line1;
        this.city = city;
        this.postalCode = postalCode;
        this.country = country;
    }

    // Getters and Setters
    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public AddressType getType() {
        return type;
    }

    public void setType(AddressType type) {
        this.type = type;
    }

    public String getLine1() {
        return line1;
    }

    public void setLine1(String line1) {
        this.line1 = line1;
    }

    public String getLine2() {
        return line2;
    }

    public void setLine2(String line2) {
        this.line2 = line2;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public String getPostalCode() {
        return postalCode;
    }

    public void setPostalCode(String postalCode) {
        this.postalCode = postalCode;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }

    public boolean isPrimary() {
        return isPrimary;
    }

    public void setPrimary(boolean primary) {
        isPrimary = primary;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidTo() {
        return validTo;
    }

    public void setValidTo(LocalDate validTo) {
        this.validTo = validTo;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Returns true if this address is currently active (not expired).
     */
    public boolean isCurrentlyValid() {
        LocalDate today = LocalDate.now();
        boolean afterStart = validFrom == null || !today.isBefore(validFrom);
        boolean beforeEnd = validTo == null || today.isBefore(validTo);
        return afterStart && beforeEnd;
    }

    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }

    public boolean isDeleted() {
        return deletedAt != null;
    }
}
