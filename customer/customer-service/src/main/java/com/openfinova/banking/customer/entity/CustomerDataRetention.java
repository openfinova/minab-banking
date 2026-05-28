package com.openfinova.banking.customer.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking the data retention schedule and anonymization state for a customer.
 *
 * Created when a customer account is closed. Records the mandatory retention
 * period end date calculated from the account closure date, per the applicable
 * legal basis (typically 5–10 years under AML/CFT regulations).
 *
 * The {@link com.openfinova.banking.customer.scheduler.AnonymizationScheduler}
 * queries this table daily and triggers anonymization for records past their
 * {@link #retentionExpiresAt} date.
 *
 * Legal references:
 * FATF Recommendation 11 — 5-year record-keeping minimum
 * EU 5th AMLD Art. 40 — 5 years from end of business relationship
 * GDPR Art. 5(1)(e) — storage limitation principle
 * GDPR Art. 17(3)(b) — legal obligation exemption from erasure
 */
@Entity
@Table(name = "customer_data_retention", indexes = {
        @Index(name = "idx_retention_customer", columnList = "customer_id"),
        @Index(name = "idx_retention_expires", columnList = "retention_expires_at"),
        @Index(name = "idx_retention_anonymized", columnList = "anonymized") })
public class CustomerDataRetention {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    /**
     * The customer this retention record belongs to. One customer → one retention record.
     */
    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false, unique = true)
    @NotNull(message = "Customer is required")
    private Customer customer;

    /**
     * Date on which the business relationship/account was formally terminated.
     * Used as the base date for retention period calculation.
     */
    @Column(name = "relationship_ended_at", nullable = false)
    @NotNull(message = "Relationship end date is required")
    private LocalDate relationshipEndedAt;

    /**
     * Date after which personal data may be anonymized/erased.
     * = relationshipEndedAt + retentionYears.
     */
    @Column(name = "retention_expires_at", nullable = false)
    @NotNull(message = "Retention expiry date is required")
    private LocalDate retentionExpiresAt;

    /**
     * Number of years data must be retained.
     * Default is 5 years per FATF Rec. 11 / EU 5AMLD.
     * Some jurisdictions or record types require longer (up to 10 years).
     */
    @Column(name = "retention_years", nullable = false)
    private int retentionYears = 5;

    /**
     * The legal basis justifying this retention period.
     * (e.g., "EU_5AMLD_ART40", "FATF_REC11", "LOCAL_AML_LAW_2020", "GDPR_ART17_3B")
     */
    @Column(name = "legal_basis", nullable = false, length = 100)
    private String legalBasis;

    /**
     * Whether the customer's PII has been anonymized.
     */
    @Column(name = "anonymized", nullable = false)
    private boolean anonymized = false;

    /**
     * Timestamp of when anonymization was performed. Null if not yet anonymized.
     */
    @Column(name = "anonymized_at")
    private LocalDateTime anonymizedAt;

    /**
     * User or system identifier that performed the anonymization.
     * (e.g., a staff member's user ID or "SYSTEM_SCHEDULER")
     */
    @Column(name = "anonymized_by", length = 100)
    private String anonymizedBy;

    /**
     * Batch job reference ID if anonymization was triggered by the scheduler.
     * Null if manually triggered.
     */
    @Column(name = "anonymization_job_ref", length = 100)
    private String anonymizationJobReference;

    /**
     * Optional notes by the Data Protection Officer or compliance team.
     */
    @Column(name = "notes", length = 1000)
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public CustomerDataRetention() {
    }

    public CustomerDataRetention(Customer customer, LocalDate relationshipEndedAt, int retentionYears,
            String legalBasis) {
        this.customer = customer;
        this.relationshipEndedAt = relationshipEndedAt;
        this.retentionYears = retentionYears;
        this.legalBasis = legalBasis;
        this.retentionExpiresAt = relationshipEndedAt.plusYears(retentionYears);
    }

    // Business logic

    /**
     * Returns true if the retention period has expired and anonymization is permitted.
     */
    public boolean isRetentionExpired(LocalDate currentDate) {
        return !anonymized && currentDate.isAfter(retentionExpiresAt);
    }

    /**
     * Records the completion of anonymization.
     *
     * @param anonymizedBy user or system that performed the anonymization
     * @param jobReference optional batch job reference
     */
    public void recordAnonymization(String anonymizedBy, String jobReference, LocalDateTime anonymizedAt) {
        this.anonymized = true;
        this.anonymizedAt = anonymizedAt;
        this.anonymizedBy = anonymizedBy;
        this.anonymizationJobReference = jobReference;
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public LocalDate getRelationshipEndedAt() {
        return relationshipEndedAt;
    }

    public void setRelationshipEndedAt(LocalDate relationshipEndedAt) {
        this.relationshipEndedAt = relationshipEndedAt;
        if (this.retentionYears > 0) {
            this.retentionExpiresAt = relationshipEndedAt.plusYears(this.retentionYears);
        }
    }

    public LocalDate getRetentionExpiresAt() {
        return retentionExpiresAt;
    }

    public void setRetentionExpiresAt(LocalDate retentionExpiresAt) {
        this.retentionExpiresAt = retentionExpiresAt;
    }

    public int getRetentionYears() {
        return retentionYears;
    }

    public void setRetentionYears(int retentionYears) {
        this.retentionYears = retentionYears;
    }

    public String getLegalBasis() {
        return legalBasis;
    }

    public void setLegalBasis(String legalBasis) {
        this.legalBasis = legalBasis;
    }

    public boolean isAnonymized() {
        return anonymized;
    }

    public void setAnonymized(boolean anonymized) {
        this.anonymized = anonymized;
    }

    public LocalDateTime getAnonymizedAt() {
        return anonymizedAt;
    }

    public void setAnonymizedAt(LocalDateTime anonymizedAt) {
        this.anonymizedAt = anonymizedAt;
    }

    public String getAnonymizedBy() {
        return anonymizedBy;
    }

    public void setAnonymizedBy(String anonymizedBy) {
        this.anonymizedBy = anonymizedBy;
    }

    public String getAnonymizationJobReference() {
        return anonymizationJobReference;
    }

    public void setAnonymizationJobReference(String anonymizationJobReference) {
        this.anonymizationJobReference = anonymizationJobReference;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
