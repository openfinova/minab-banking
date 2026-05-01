package com.openfinova.banking.customer.account.mapper;

import com.openfinova.banking.customer.account.api.dto.AccountTransactionResponse;
import com.openfinova.banking.customer.account.entity.AccountTransaction;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link AccountTransaction} entities and {@link AccountTransactionResponse} DTOs.
 */
@Component
public class AccountTransactionMapper {

    public AccountTransactionResponse toResponse(AccountTransaction transaction) {
        if (transaction == null) {
            return null;
        }

        AccountTransactionResponse response = new AccountTransactionResponse();
        response.setId(transaction.getId());
        response.setAccountId(transaction.getCustomerAccount().getId());
        response.setTransactionType(transaction.getTransactionType());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setDescription(transaction.getDescription());
        response.setReferenceId(transaction.getReferenceId());
        response.setGlTransactionId(transaction.getGlTransactionId());

        return response;
    }
}
