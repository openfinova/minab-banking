package com.openfinova.banking.customer.account.mapper;

import org.springframework.stereotype.Component;

import com.openfinova.banking.customer.account.api.dto.AccountTransactionResponse;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionDirection;
import com.openfinova.banking.customer.account.api.entity.AccountTransactionType;
import com.openfinova.banking.customer.account.entity.AccountTransaction;

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
        response.setAccountNumber(transaction.getCustomerAccount().getAccountNumber());
        response.setTransactionType(transaction.getTransactionType());
        response.setDirection(resolveDirection(transaction.getTransactionType()));
        response.setStatus(transaction.getStatus());
        response.setAmount(transaction.getAmount());
        response.setCurrency(transaction.getCurrency());
        response.setTransactionDate(transaction.getTransactionDate());
        response.setDescription(transaction.getDescription());
        response.setReferenceId(transaction.getReferenceId());
        response.setGlTransactionId(transaction.getGlTransactionId());

        return response;
    }

    static AccountTransactionDirection resolveDirection(AccountTransactionType type) {
        if (type == null) {
            return AccountTransactionDirection.DEBIT;
        }
        return switch (type) {
            case DEPOSIT, TRANSFER_IN, INTEREST_CREDIT -> AccountTransactionDirection.CREDIT;
            default -> AccountTransactionDirection.DEBIT;
        };
    }
}
