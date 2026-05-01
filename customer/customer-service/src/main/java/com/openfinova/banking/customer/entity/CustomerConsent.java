package com.openfinova.banking.customer.entity;

import com.openfinova.banking.customer.api.entity.ConsentType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity recording an individual customer consent decision.
 *
 * <p>Each row represents one grant or revocation of a specific consent type.
 * The full history is preserved — never delete records.
 * The latest record per (customer, consentType) dictates the current status.</p>
 *
 * <p>Required for GDPR Articles 6/7, ePrivacy Directive, and equivalent
 * data protection regulations across jurisdictions.</p>
 */
@Entity
@Table(name = "customer_consents", indexes = { @Index(name = "idx_consent_customer", columnList = "customer_id"),
        @Index(name = "idx_consent_type", columnList = "consent_type"),
        @Index(name = "idx_consent_granted", columnList = "granted"),
        @Index(name = "idx_consent_recorded_at", columnList = "recorded_at") }, uniqueConstraints = {
// Enforced at the application layer via latest-record logic, not DB unique constraint,
// because history must be preserved.
})
public class CustomerConsent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;

    /**
     * The category of consent being recorded.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "consent_type", nullable = false, length = 50)
    @NotNull(message = "Consent type is required")
    private ConsentType consentType;

    /**
     * True = consent granted; false = consent withdrawn/revoked.
     */
    @Column(name = "granted", nullable = false)
    private boolean granted;

    /**
     * The version of the policy or terms document the customer consented to.
     * For example: "PP-2025-01", "TC-2024-03".
     */
    @Column(name = "policy_version", length = 50)
    private String policyVersion;

    /**
     * The channel through which the consent was captured
     * (e.g., "WEB_PORTAL", "MOBILE_APP", "BRANCH", "CALL_CENTER").
     */
    @Column(name = "capture_channel", length = 50)
    private String captureChannel;

    /**
     * IP address from which the consent was submitted (for audit and legal evidence).
     */
    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    /**
     * User or system that recorded this consent change.
     */
    @Column(name = "recorded_by", length = 100)
    private String recordedBy;

    /**
     * Scheduled expiry of the consent (null = does not expire automatically).
     * GDPR requires re-consent when data processing purpose changes or periodically for some categories.
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * Optional note (e.g., reason for revocation, reference to a complaint).
     */
    @Column(name = "notes", length = 500)
    private String notes;

    @CreationTimestamp
    @Column(name = "recorded_at", nullable = false, updatable = false)
    private LocalDateTime recordedAt;

    public CustomerConsent() {
    }

    public CustomerConsent(Customer customer, ConsentType consentType, boolean granted, String policyVersion,
            String captureChannel, String recordedBy) {
        this.customer = customer;
        this.consentType = consentType;
        this.granted = granted;
        this.policyVersion = policyVersion;
        this.captureChannel = captureChannel;
        this.recordedBy = recordedBy;
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

    public ConsentType getConsentType() {
        return consentType;
    }

    public void setConsentType(ConsentType consentType) {
        this.consentType = consentType;
    }

    public boolean isGranted() {
        return granted;
    }

    public void setGranted(boolean granted) {
        this.granted = granted;
    }

    public String getPolicyVersion() {
        return policyVersion;
    }

    public void setPolicyVersion(String policyVersion) {
        this.policyVersion = policyVersion;
    }

    public String getCaptureChannel() {
        return captureChannel;
    }

    public void setCaptureChannel(String captureChannel) {
        this.captureChannel = captureChannel;
    }

    public String getIpAddress() {
        return ipAddress;
    }

    public void setIpAddress(String ipAddress) {
        this.ipAddress = ipAddress;
    }

    public String getRecordedBy() {
        return recordedBy;
    }

    public void setRecordedBy(String recordedBy) {
        this.recordedBy = recordedBy;
    }

    public LocalDateTime getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(LocalDateTime expiresAt) {
        this.expiresAt = expiresAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getRecordedAt() {
        return recordedAt;
    }

    public void setRecordedAt(LocalDateTime recordedAt) {
        this.recordedAt = recordedAt;
    }
}
