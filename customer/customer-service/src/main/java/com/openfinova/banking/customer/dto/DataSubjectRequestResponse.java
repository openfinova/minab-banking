package com.openfinova.banking.customer.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

import com.openfinova.banking.customer.api.entity.DataSubjectRequestStatus;
import com.openfinova.banking.customer.api.entity.DataSubjectRequestType;

public class DataSubjectRequestResponse {

    private UUID id;
    private UUID customerId;
    private DataSubjectRequestType requestType;
    private DataSubjectRequestStatus status;
    private LocalDate receivedAt;
    private LocalDate dueBy;
    private LocalDate fulfilledAt;
    private String channel;
    private String referenceNumber;
    private String customerNotes;
    private String outcomeReason;
    private LocalDate deferredUntil;
    private boolean extended;
    private LocalDate extensionNotifiedAt;
    private String handledBy;
    private LocalDateTime updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getCustomerId() {
        return customerId;
    }

    public void setCustomerId(UUID customerId) {
        this.customerId = customerId;
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

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
