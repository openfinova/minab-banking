package com.openfinova.banking.tp.mapper;

import com.openfinova.banking.tp.api.dto.TransactionResponse;
import com.openfinova.banking.tp.entity.Transaction;
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
}
