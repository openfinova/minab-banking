package com.openfinova.banking.customer.account.mapper;

import com.openfinova.banking.customer.account.api.dto.AccountHoldResponse;
import com.openfinova.banking.customer.account.entity.AccountHold;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link AccountHold} entities and {@link AccountHoldResponse} DTOs.
 */
@Component
public class AccountHoldMapper {

    public AccountHoldResponse toResponse(AccountHold hold) {
        if (hold == null) {
            return null;
        }

        AccountHoldResponse response = new AccountHoldResponse();
        response.setId(hold.getId());
        response.setAccountId(hold.getCustomerAccount().getId());
        response.setAmount(hold.getAmount());
        response.setCurrency(hold.getCurrency());
        response.setStatus(hold.getStatus());
        response.setReason(hold.getReason());
        response.setExpiresAt(hold.getExpiresAt());
        response.setCreatedAt(hold.getCreatedAt());
        // Note: AccountHold doesn't have releasedAt field - status indicates if released
        response.setReleasedAt(null);

        return response;
    }
}
