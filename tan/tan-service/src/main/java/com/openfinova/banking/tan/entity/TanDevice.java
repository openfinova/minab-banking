package com.openfinova.banking.tan.entity;

import java.time.Instant;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.openfinova.banking.tan.crypto.TanSecretAttributeConverter;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

/**
 * Enrolled TAN mobile device bound to a banking user for dynamic transaction authorization.
 *
 * Each record represents one physical phone running the TAN app. The device generates and holds
 * a shared secret; the server stores an encrypted copy and uses it to validate time-based TAN
 * codes during Strong Customer Authentication (SCA) for payments.
 *
 * Lifecycle
 *   Created in {@code PENDING_ENROLLMENT} when the mobile app completes attestation and submits
 *   its secret ({@link com.openfinova.banking.tan.service.TanDeviceService#enrollDevice}).
 *   Transitions to {@code ACTIVE} after the user confirms enrollment on a second channel
 *   ({@link com.openfinova.banking.tan.service.TanDeviceService#confirmDevice}).
 *   {@code lastUsedAt} is updated on each successful TAN verification for a payment.
 *   Transitions to {@code REVOKED} when the user deregisters the device; revoked rows are
 *   retained for audit but excluded from active device counts and authorization.
 *
 * Invariants
 *   {@code userId} and {@code id} are immutable after creation.
 *   {@code tanSecret} decodes to exactly 32 bytes when enrollment is accepted.
 *   Only {@code ACTIVE} devices may authorize payments.
 *   Per-user device count is capped by configuration; non-revoked devices count toward the limit.
 *
 * @see TanDeviceStatus
 * @see com.openfinova.banking.tan.service.TanDeviceService
 * @see com.openfinova.banking.tan.service.TanAuthorizationService
 */
@Entity
@Table(name = "tan_devices")
@EntityListeners(AuditingEntityListener.class)
public class TanDevice {

    /** Surrogate primary key; assigned at enrollment and never changed. */
    @Id
    @Column(nullable = false, updatable = false)
    private UUID id;

    /** Identity of the banking user who owns this device; immutable after enrollment. */
    @Column(name = "user_id", nullable = false, updatable = false)
    private UUID userId;

    /** User-visible label (e.g. "John's iPhone") shown in device management and notifications. */
    @Column(name = "device_name", nullable = false, length = 200)
    private String deviceName;

    /**
     * Enrollment and operational state. Only {@link TanDeviceStatus#ACTIVE} devices participate
     * in payment authorization; {@link TanDeviceStatus#REVOKED} rows are kept for audit only.
     */
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private TanDeviceStatus status;

    /**
     * Base64-encoded 32-byte TAN shared secret generated on the device. Encrypted at rest via
     * {@link TanSecretAttributeConverter}; never exposed through the API after enrollment.
     */
    @Convert(converter = TanSecretAttributeConverter.class)
    @Column(name = "tan_secret", length = 512)
    private String tanSecret;

    /**
     * One-time challenge issued at enrollment and returned to the mobile app. The app derives a
     * confirmation code from {@code tanSecret} and this nonce; the user enters that code on a
     * trusted channel to prove possession before the device becomes {@code ACTIVE}.
     */
    @Column(name = "enrollment_nonce", nullable = false, length = 128)
    private String enrollmentNonce;

    /**
     * Opaque identifier from the mobile platform (e.g. Android instance ID) used to correlate
     * attestation verdicts with this enrollment. Optional when attestation is not enforced.
     */
    @Column(name = "platform_device_id", length = 256)
    private String platformDeviceId;

    /** Timestamp when enrollment was confirmed and the device transitioned to {@code ACTIVE}. */
    @Column(name = "enrolled_at")
    private Instant enrolledAt;

    /** Timestamp of the most recent successful TAN verification against this device; null until first use. */
    @Column(name = "last_used_at")
    private Instant lastUsedAt;

    /** Optimistic-lock version; incremented on every update to detect concurrent modifications. */
    @Version
    @Column(nullable = false)
    private Long version = 0L;

    /** Principal that created this record; populated by JPA auditing at insert time. */
    @CreatedBy
    @Column(name = "created_by", updatable = false)
    private String createdBy;

    /** Instant this record was first persisted; populated by JPA auditing at insert time. */
    @CreatedDate
    @Column(name = "created_at", updatable = false)
    private Instant createdAt;

    /** Principal that last modified this record; populated by JPA auditing on each update. */
    @LastModifiedBy
    @Column(name = "updated_by")
    private String updatedBy;

    /** Instant of the most recent mutation; populated by JPA auditing on each update. */
    @LastModifiedDate
    @Column(name = "updated_at")
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getDeviceName() {
        return deviceName;
    }

    public void setDeviceName(String deviceName) {
        this.deviceName = deviceName;
    }

    public TanDeviceStatus getStatus() {
        return status;
    }

    public void setStatus(TanDeviceStatus status) {
        this.status = status;
    }

    public String getTanSecret() {
        return tanSecret;
    }

    public void setTanSecret(String tanSecret) {
        this.tanSecret = tanSecret;
    }

    public String getEnrollmentNonce() {
        return enrollmentNonce;
    }

    public void setEnrollmentNonce(String enrollmentNonce) {
        this.enrollmentNonce = enrollmentNonce;
    }

    public String getPlatformDeviceId() {
        return platformDeviceId;
    }

    public void setPlatformDeviceId(String platformDeviceId) {
        this.platformDeviceId = platformDeviceId;
    }

    public Instant getEnrolledAt() {
        return enrolledAt;
    }

    public void setEnrolledAt(Instant enrolledAt) {
        this.enrolledAt = enrolledAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public String getUpdatedBy() {
        return updatedBy;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }
}
