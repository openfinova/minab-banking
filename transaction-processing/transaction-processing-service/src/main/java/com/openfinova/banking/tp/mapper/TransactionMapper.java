package com.openfinova.banking.tp.mapper;

import com.openfinova.banking.tp.api.dto.TransactionResponse;
import com.openfinova.banking.tp.dto.TransactionEventResponse;
import com.openfinova.banking.tp.entity.Transaction;
import com.openfinova.banking.tp.entity.TransactionEvent;
import org.springframework.stereotype.Component;

@Component
public class TransactionMapper {

    public TransactionResponse toResponse(Transaction transaction) {
        if (transaction == null) {
            return null;
        }

        TransactionResponse response = new TransactionResponse();
        response.setId(transaction.getId());
        response.setIdempotencyKey(transaction.getIdempotencyKey());
        response.setType(transaction.getRequest() != null ? transaction.getRequest().getTransactionType() : null);
        response.setAmount(transaction.getPrincipalAmount());
        response.setCurrency(transaction.getCurrency());
        response.setStatus(transaction.getStatus());
        response.setSourceAccountId(transaction.getSourceAccountId());
        response.setDestinationAccountId(transaction.getDestinationAccountId());
        response.setDescription(transaction.getRequest() != null ? transaction.getRequest().getDescription() : null);
        response.setFeeAmount(transaction.getFeeAmount());
        response.setCreatedAt(transaction.getCreatedAt());
        response.setUpdatedAt(transaction.getUpdatedAt());
        response.setCompletedAt(transaction.getCompletedAt());

        return response;
    }

    public TransactionEventResponse toEventResponse(TransactionEvent event) {
        if (event == null) {
            return null;
        }
        TransactionEventResponse response = new TransactionEventResponse();
        response.setId(event.getId());
        response.setTransactionId(event.getTransaction() != null ? event.getTransaction().getId() : null);
        response.setEventType(event.getEventType());
        response.setEventSequence(event.getEventSequence());
        response.setPreviousStatus(event.getPreviousStatus());
        response.setNewStatus(event.getNewStatus());
        response.setEventData(event.getEventData());
        response.setErrorCode(event.getErrorCode());
        response.setErrorMessage(event.getErrorMessage());
        response.setCreatedAt(event.getCreatedAt());
        response.setCreatedBy(event.getCreatedBy());
        return response;
    }

    public java.util.List<TransactionEventResponse> toEventResponseList(java.util.List<TransactionEvent> events) {
        if (events == null) {
            return java.util.List.of();
        }
        return events.stream().map(this::toEventResponse).toList();
    }
}
