package com.openfinova.banking.tp.entity;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import com.openfinova.banking.common.lib.converter.MapToJsonConverter;
import com.openfinova.banking.tp.api.entity.TransactionStatus;

import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
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
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entity representing events in the transaction processing lifecycle.
 * Implements event sourcing pattern for complete audit trail.
 */
@Entity
@EntityListeners(AuditingEntityListener.class)
@Table(name = "transaction_events", uniqueConstraints = @UniqueConstraint(columnNames = { "transaction_id",
        "event_sequence" }), indexes = {
                @Index(name = "idx_transaction_events_transaction", columnList = "transaction_id, event_sequence"),
                @Index(name = "idx_transaction_events_type", columnList = "event_type"),
                @Index(name = "idx_transaction_events_created_at", columnList = "created_at") })
public class TransactionEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "transaction_id", nullable = false)
    @NotNull(message = "Transaction is required")
    private Transaction transaction;

    @Column(name = "event_type", nullable = false, length = 100)
    @NotBlank(message = "Event type is required")
    @Size(max = 100, message = "Event type must not exceed 100 characters")
    private String eventType;

    @Column(name = "event_sequence", nullable = false)
    @NotNull(message = "Event sequence is required")
    private Integer eventSequence;

    @Enumerated(EnumType.STRING)
    @Column(name = "previous_status", length = 50)
    private TransactionStatus previousStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "new_status", nullable = false, length = 50)
    @NotNull(message = "New status is required")
    private TransactionStatus newStatus;

    @Convert(converter = MapToJsonConverter.class)
    @Column(name = "event_data", columnDefinition = "jsonb")
    private Map<String, Object> eventData;

    @Column(name = "error_code", length = 50)
    @Size(max = 50, message = "Error code must not exceed 50 characters")
    private String errorCode;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @CreatedBy
    @Column(name = "created_by", length = 100)
    @Size(max = 100, message = "Created by must not exceed 100 characters")
    private String createdBy;

    // Constructors
    public TransactionEvent() {
    }

    public TransactionEvent(Transaction transaction, String eventType, TransactionStatus newStatus) {
        this.transaction = transaction;
        this.eventType = eventType;
        this.newStatus = newStatus;
    }

    // Business logic methods

    /**
     * Checks if this event represents an error condition
     *
     * @return true if error code or error message is present
     */
    public boolean isErrorEvent() {
        return errorCode != null || errorMessage != null;
    }

    /**
     * Checks if this event represents a state transition
     *
     * @return true if event type is STATE_TRANSITION
     */
    public boolean isStateTransition() {
        return "STATE_TRANSITION".equals(eventType);
    }

    /**
     * Gets a display-friendly description of this event
     *
     * @return formatted event description
     */
    public String getEventDescription() {
        StringBuilder description = new StringBuilder();
        description.append(eventType);

        if (isStateTransition() && previousStatus != null) {
            description.append(": ").append(previousStatus).append(" → ").append(newStatus);
        }

        if (isErrorEvent()) {
            description.append(" (ERROR");
            if (errorCode != null) {
                description.append(": ").append(errorCode);
            }
            description.append(")");
        }

        return description.toString();
    }

    // Getters and Setters

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Transaction getTransaction() {
        return transaction;
    }

    public void setTransaction(Transaction transaction) {
        this.transaction = transaction;
    }

    public String getEventType() {
        return eventType;
    }

    public void setEventType(String eventType) {
        this.eventType = eventType;
    }

    public Integer getEventSequence() {
        return eventSequence;
    }

    public void setEventSequence(Integer eventSequence) {
        this.eventSequence = eventSequence;
    }

    public TransactionStatus getPreviousStatus() {
        return previousStatus;
    }

    public void setPreviousStatus(TransactionStatus previousStatus) {
        this.previousStatus = previousStatus;
    }

    public TransactionStatus getNewStatus() {
        return newStatus;
    }

    public void setNewStatus(TransactionStatus newStatus) {
        this.newStatus = newStatus;
    }

    public Map<String, Object> getEventData() {
        return eventData;
    }

    public void setEventData(Map<String, Object> eventData) {
        this.eventData = eventData;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }

    // equals, hashCode, and toString

    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof TransactionEvent that))
            return false;
        return id != null && id.equals(that.id);
    }

    @Override
    public int hashCode() {
        return id != null ? id.hashCode() : 0;
    }

    @Override
    public String toString() {
        return "TransactionEvent{" + "id=" + id + ", eventType='" + eventType + '\'' + ", eventSequence="
                + eventSequence + ", previousStatus=" + previousStatus + ", newStatus=" + newStatus + ", createdAt="
                + createdAt + '}';
    }
}