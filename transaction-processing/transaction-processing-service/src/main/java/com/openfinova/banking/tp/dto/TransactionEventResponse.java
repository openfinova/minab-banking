package com.openfinova.banking.tp.dto;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

import com.openfinova.banking.tp.api.entity.TransactionStatus;

public class TransactionEventResponse {

    private UUID id;
    private UUID transactionId;
    private String eventType;
    private Integer eventSequence;
    private TransactionStatus previousStatus;
    private TransactionStatus newStatus;
    private Map<String, Object> eventData;
    private String errorCode;
    private String errorMessage;
    private Instant createdAt;
    private String createdBy;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(UUID transactionId) {
        this.transactionId = transactionId;
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
}
