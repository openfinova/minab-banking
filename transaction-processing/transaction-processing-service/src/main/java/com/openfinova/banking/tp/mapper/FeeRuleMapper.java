package com.openfinova.banking.tp.mapper;

import com.openfinova.banking.tp.api.dto.CreateFeeRuleRequest;
import com.openfinova.banking.tp.api.dto.FeeRuleResponse;
import com.openfinova.banking.tp.api.dto.UpdateFeeRuleRequest;
import com.openfinova.banking.tp.entity.FeeRule;

import org.springframework.stereotype.Component;

@Component
public class FeeRuleMapper {

    public FeeRuleResponse toResponse(FeeRule rule) {
        if (rule == null) {
            return null;
        }

        FeeRuleResponse response = new FeeRuleResponse();
        response.setId(rule.getId());
        response.setTransactionType(rule.getTransactionType());
        response.setCustomerTier(rule.getCustomerTier());
        response.setFeeType(rule.getFeeType());
        response.setFixedAmount(rule.getFixedAmount());
        response.setPercentageRate(rule.getPercentageRate());
        response.setMinFee(rule.getMinimumFee());
        response.setMaxFee(rule.getMaximumFee());
        response.setIsActive(rule.getIsActive());

        return response;
    }

    public FeeRule toEntity(CreateFeeRuleRequest request) {
        if (request == null) {
            return null;
        }

        FeeRule rule = new FeeRule();
        rule.setTransactionType(request.getTransactionType());
        rule.setCustomerTier(request.getCustomerTier());
        rule.setFeeType(request.getFeeType());
        rule.setFixedAmount(request.getFixedAmount());
        rule.setPercentageRate(request.getPercentageRate());
        rule.setMinimumFee(request.getMinFee());
        rule.setMaximumFee(request.getMaxFee());

        return rule;
    }

    public FeeRule toEntity(UpdateFeeRuleRequest request) {
        if (request == null) {
            return null;
        }

        FeeRule rule = new FeeRule();
        rule.setFixedAmount(request.getFixedAmount());
        rule.setPercentageRate(request.getPercentageRate());
        rule.setMinimumFee(request.getMinFee());
        rule.setMaximumFee(request.getMaxFee());
        rule.setIsActive(request.getIsActive());

        return rule;
    }
}
