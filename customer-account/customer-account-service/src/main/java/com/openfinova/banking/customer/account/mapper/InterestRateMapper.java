package com.openfinova.banking.customer.account.mapper;

import com.openfinova.banking.customer.account.api.dto.InterestRateResponse;
import com.openfinova.banking.customer.account.api.entity.InterestRateType;
import com.openfinova.banking.customer.account.entity.InterestRate;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link InterestRate} entities and {@link InterestRateResponse} DTOs.
 */
@Component
public class InterestRateMapper {

    public InterestRateResponse toResponse(InterestRate rate) {
        if (rate == null) {
            return null;
        }

        InterestRateResponse response = new InterestRateResponse();
        response.setId(rate.getId());
        response.setAccountId(rate.getCustomerAccount().getId());
        response.setRateType(InterestRateType.valueOf(rate.getRateType().name()));
        response.setAnnualPercentageRate(rate.getAnnualPercentageRate());
        response.setEffectiveFrom(rate.getEffectiveFrom());
        response.setEffectiveUntil(rate.getEffectiveUntil());

        return response;
    }
}
