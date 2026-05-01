package com.openfinova.banking.customer.account.mapper;

import com.openfinova.banking.customer.account.api.dto.AccountResponse;
import com.openfinova.banking.customer.account.entity.Account;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link Account} entities and {@link AccountResponse} DTOs.
 */
@Component
public class AccountMapper {

    public AccountResponse toResponse(Account account) {
        if (account == null) {
            return null;
        }

        AccountResponse response = new AccountResponse();
        response.setId(account.getId());
        response.setAccountNumber(account.getAccountNumber());
        response.setIban(account.getIban());
        response.setPrimaryUserProfileId(account.getPrimaryUserProfileId());
        response.setProductType(account.getProductType());
        response.setStatus(account.getStatus());
        response.setDisplayName(account.getDisplayName());
        response.setDescription(account.getDescription());
        response.setCurrency(account.getCurrency());
        response.setLedgerBalance(account.getLedgerBalance());
        response.setAvailableBalance(account.getAvailableBalance());
        response.setCreatedAt(account.getCreatedAt());
        response.setCreatedBy(account.getCreatedBy());
        response.setUpdatedAt(account.getUpdatedAt());
        response.setClosedAt(account.getClosedAt());
        response.setClosureReason(account.getClosureReason());

        return response;
    }
}
