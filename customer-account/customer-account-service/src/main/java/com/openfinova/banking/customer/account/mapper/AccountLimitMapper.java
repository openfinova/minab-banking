package com.openfinova.banking.customer.account.mapper;

import com.openfinova.banking.customer.account.api.dto.AccountLimitResponse;
import com.openfinova.banking.customer.account.entity.AccountLimit;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link AccountLimit} entities and {@link AccountLimitResponse} DTOs.
 */
@Component
public class AccountLimitMapper {

    public AccountLimitResponse toResponse(AccountLimit limit) {
        if (limit == null) {
            return null;
        }

        AccountLimitResponse response = new AccountLimitResponse();
        response.setId(limit.getId());
        response.setAccountId(limit.getCustomerAccount().getId());
        response.setLimitType(limit.getLimitType());
        response.setLimitPeriod(limit.getLimitPeriod());
        response.setMaxAmount(limit.getMaxAmount());
        response.setMaxCount(limit.getMaxCount());
        response.setCreatedAt(limit.getCreatedAt());
        response.setCreatedBy(limit.getCreatedBy());

        return response;
    }
}
