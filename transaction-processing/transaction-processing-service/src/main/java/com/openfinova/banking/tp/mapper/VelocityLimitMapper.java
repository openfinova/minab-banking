package com.openfinova.banking.tp.mapper;

import com.openfinova.banking.tp.api.dto.CreateVelocityLimitRequest;
import com.openfinova.banking.tp.api.dto.UpdateVelocityLimitRequest;
import com.openfinova.banking.tp.api.dto.VelocityLimitResponse;
import com.openfinova.banking.tp.entity.VelocityLimit;

import org.springframework.stereotype.Component;

@Component
public class VelocityLimitMapper {

    public VelocityLimitResponse toResponse(VelocityLimit limit) {
        if (limit == null) {
            return null;
        }

        VelocityLimitResponse response = new VelocityLimitResponse();
        response.setId(limit.getId());
        response.setAccountId(limit.getAccountId());
        response.setTransactionType(limit.getTransactionType());
        response.setPeriod(limit.getVelocityLimitPeriod());
        response.setMaxAmount(limit.getMaxAmount());
        response.setMaxCount(limit.getMaxCount());
        response.setCurrentAmount(limit.getCurrentAmount());
        response.setCurrentCount(limit.getCurrentCount());
        response.setPeriodStart(limit.getPeriodStart());
        response.setPeriodEnd(limit.getPeriodEnd());
        response.setCreatedAt(limit.getCreatedAt());

        return response;
    }

    public VelocityLimit toEntity(CreateVelocityLimitRequest request) {
        if (request == null) {
            return null;
        }

        VelocityLimit limit = new VelocityLimit();
        limit.setAccountId(request.getAccountId());
        limit.setTransactionType(request.getTransactionType());
        limit.setVelocityLimitPeriod(request.getPeriod());
        limit.setMaxAmount(request.getMaxAmount());
        limit.setMaxCount(request.getMaxCount());

        return limit;
    }

    public VelocityLimit toEntity(UpdateVelocityLimitRequest request) {
        if (request == null) {
            return null;
        }

        VelocityLimit limit = new VelocityLimit();
        limit.setMaxAmount(request.getMaxAmount());
        limit.setMaxCount(request.getMaxCount());

        return limit;
    }
}
