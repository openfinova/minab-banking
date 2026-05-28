package com.openfinova.banking.customer.entity;

import com.openfinova.banking.customer.api.entity.DataSubjectRequestStatus;
import com.openfinova.banking.customer.api.entity.DataSubjectRequestType;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Entity tracking a Data Subject Request (DSAR) submitted by a customer
 * (data subject) under GDPR Articles 15–22.
 *
 * The bank is legally required to:
 * Acknowledge receipt immediately.
 * Verify the identity of the requester before fulfilling.
 * Respond within 30 calendar days (Art. 12(3)), or notify of extension
 * (up to 90 days) for complex/numerous requests.
 * Document the outcome and retain the record for accountability (Art. 5(2)).
 */
@Entity
@Table(name = "data_subject_requests", indexes = { @Index(name = "idx_dsar_customer", columnList = "customer_id"),
        @Index(name = "idx_dsar_type", columnList = "request_type"),
        @Index(name = "idx_dsar_status", columnList = "status"),
        @Index(name = "idx_dsar_due_by", columnList = "due_by"),
        @Index(name = "idx_dsar_received_at", columnList = "received_at") })
public class DataSubjectRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Version
    @Column(name = "version", nullable = false)
    private Long version = 0L;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    @NotNull(message = "Customer is required")
    private Customer customer;

    /**
     * Type of GDPR request (access, erasure, portability, etc.).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "request_type", nullable = false, length = 30)
    @NotNull(message = "Request type is required")
    private DataSubjectRequestType requestType;

    /**
     * Current lifecycle status of this request.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @NotNull(message = "Status is required")
    private DataSubjectRequestStatus status = DataSubjectRequestStatus.RECEIVED;

    /**
     * Date the request was formally received (SLA clock starts here).
     */
    @Column(name = "received_at", nullable = false)
    @NotNull(message = "Received date is required")
    private LocalDate receivedAt;

    /**
     * Deadline for responding (receivedAt + 30 days, extendable to +90 for complex requests).
     */
    @Column(name = "due_by", nullable = false)
    @NotNull(message = "Due date is required")
    private LocalDate dueBy;

    /**
     * Date the response was provided to the customer. Null if still in progress.
     */
    @Column(name = "fulfilled_at")
    private LocalDate fulfilledAt;

    /**
     * Channel through which the request was received.
     * (e.g., "EMAIL", "PORTAL", "BRANCH", "PHONE", "POST")
     */
    @Column(name = "channel", length = 30)
    private String channel;

    /**
     * Reference number provided to the customer for tracking their request.
     */
    @Column(name = "reference_number", unique = true, length = 30)
    private String referenceNumber;

    /**
     * Free-text description or additional context supplied by the customer.
     */
    @Column(name = "customer_notes", columnDefinition = "TEXT")
    private String customerNotes;

    /**
     * If DEFERRED: the legal basis justifying the deferral.
     * If REJECTED: the reason and legal basis for the rejection.
     * (e.g., "Legal obligation under EU 5AMLD Art. 40 — retention until 2031-03-15")
     */
    @Column(name = "outcome_reason", columnDefinition = "TEXT")
    private String outcomeReason;

    /**
     * Date on which the deferred request will be automatically reconsidered.
     * Populated only when status = DEFERRED. Links to CustomerDataRetention.retentionExpiresAt.
     */
    @Column(name = "deferred_until")
    private LocalDate deferredUntil;

    /**
     * Whether the SLA deadline has been extended (Art. 12(3) allows +60 days for complex requests).
     */
    @Column(name = "extended", nullable = false)
    private boolean extended = false;

    /**
     * Date extended deadline was communicated to the customer (if extended = true).
     */
    @Column(name = "extension_notified_at")
    private LocalDate extensionNotifiedAt;

    /**
     * Staff member or service that handled this request.
     */
    @Column(name = "handled_by", length = 100)
    private String handledBy;

    @CreationTimestamp
    @Column(name = "received_at_ts", nullable = false, updatable = false)
    private LocalDateTime receivedAtTimestamp;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public DataSubjectRequest() {
    }

    public DataSubjectRequest(Customer customer, DataSubjectRequestType requestType, String channel,
            String customerNotes, LocalDate receivedAt) {
        this.customer = customer;
        this.requestType = requestType;
        this.channel = channel;
        this.customerNotes = customerNotes;
        this.receivedAt = receivedAt;
        this.dueBy = this.receivedAt.plusDays(30);
        this.status = DataSubjectRequestStatus.RECEIVED;
    }

    // Business logic

    /**
     * Returns true if the response deadline has passed and the request is still open.
     */
    public boolean isOverdue(LocalDate evaluatedAt) {
        return status != DataSubjectRequestStatus.FULFILLED && status != DataSubjectRequestStatus.REJECTED
                && status != DataSubjectRequestStatus.WITHDRAWN && evaluatedAt.isAfter(dueBy);
    }

    /**
     * Extends the SLA deadline by up to 60 additional days (total max 90 days from receipt).
     * Must notify the customer within the original 30-day window.
     */
    public void extendDeadline(int additionalDays, LocalDate extensionNotifiedAt) {
        if (additionalDays > 60) {
            throw new IllegalArgumentException("GDPR allows a maximum extension of 60 days");
        }
        this.dueBy = this.dueBy.plusDays(additionalDays);
        this.extended = true;
        this.extensionNotifiedAt = extensionNotifiedAt;
    }

    /**
     * Marks the request as fulfilled.
     */
    public void markFulfilled(String handledBy, LocalDate fulfilledAt) {
        this.status = DataSubjectRequestStatus.FULFILLED;
        this.fulfilledAt = fulfilledAt;
        this.handledBy = handledBy;
    }

    /**
     * Defers the request to a future date due to legal retention obligations.
     */
    public void defer(LocalDate deferredUntil, String legalBasisReason, String handledBy) {
        this.status = DataSubjectRequestStatus.DEFERRED;
        this.deferredUntil = deferredUntil;
        this.outcomeReason = legalBasisReason;
        this.handledBy = handledBy;
    }

    /**
     * Rejects the request with a documented reason.
     */
    public void reject(String reason, String handledBy, LocalDate fulfilledAt) {
        this.status = DataSubjectRequestStatus.REJECTED;
        this.outcomeReason = reason;
        this.handledBy = handledBy;
        this.fulfilledAt = fulfilledAt;
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

    public DataSubjectRequestType getRequestType() {
        return requestType;
    }

    public void setRequestType(DataSubjectRequestType requestType) {
        this.requestType = requestType;
    }

    public DataSubjectRequestStatus getStatus() {
        return status;
    }

    public void setStatus(DataSubjectRequestStatus status) {
        this.status = status;
    }

    public LocalDate getReceivedAt() {
        return receivedAt;
    }

    public void setReceivedAt(LocalDate receivedAt) {
        this.receivedAt = receivedAt;
    }

    public LocalDate getDueBy() {
        return dueBy;
    }

    public void setDueBy(LocalDate dueBy) {
        this.dueBy = dueBy;
    }

    public LocalDate getFulfilledAt() {
        return fulfilledAt;
    }

    public void setFulfilledAt(LocalDate fulfilledAt) {
        this.fulfilledAt = fulfilledAt;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String channel) {
        this.channel = channel;
    }

    public String getReferenceNumber() {
        return referenceNumber;
    }

    public void setReferenceNumber(String referenceNumber) {
        this.referenceNumber = referenceNumber;
    }

    public String getCustomerNotes() {
        return customerNotes;
    }

    public void setCustomerNotes(String customerNotes) {
        this.customerNotes = customerNotes;
    }

    public String getOutcomeReason() {
        return outcomeReason;
    }

    public void setOutcomeReason(String outcomeReason) {
        this.outcomeReason = outcomeReason;
    }

    public LocalDate getDeferredUntil() {
        return deferredUntil;
    }

    public void setDeferredUntil(LocalDate deferredUntil) {
        this.deferredUntil = deferredUntil;
    }

    public boolean isExtended() {
        return extended;
    }

    public void setExtended(boolean extended) {
        this.extended = extended;
    }

    public LocalDate getExtensionNotifiedAt() {
        return extensionNotifiedAt;
    }

    public void setExtensionNotifiedAt(LocalDate extensionNotifiedAt) {
        this.extensionNotifiedAt = extensionNotifiedAt;
    }

    public String getHandledBy() {
        return handledBy;
    }

    public void setHandledBy(String handledBy) {
        this.handledBy = handledBy;
    }

    public LocalDateTime getReceivedAtTimestamp() {
        return receivedAtTimestamp;
    }

    public void setReceivedAtTimestamp(LocalDateTime receivedAtTimestamp) {
        this.receivedAtTimestamp = receivedAtTimestamp;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
