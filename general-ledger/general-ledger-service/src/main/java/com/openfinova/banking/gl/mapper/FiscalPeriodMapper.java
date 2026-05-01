package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.api.dto.CreateFiscalPeriodRequest;
import com.openfinova.banking.gl.api.dto.FiscalPeriodResponse;
import com.openfinova.banking.gl.entity.FiscalPeriod;
import org.springframework.stereotype.Component;

import java.time.ZoneOffset;

@Component
public class FiscalPeriodMapper {

    /**
     * Maps a {@link CreateFiscalPeriodRequest} to a new (transient) {@link FiscalPeriod} entity.
     * Server-managed fields (id, status, createdAt, etc.) are NOT set here; the service owns them.
     */
    public FiscalPeriod toEntity(CreateFiscalPeriodRequest request) {
        return new FiscalPeriod(
                request.getName(),
                request.getFiscalYear(),
                request.getPeriodNumber(),
                request.getStartDate(),
                request.getEndDate());
    }

    public FiscalPeriodResponse toResponse(FiscalPeriod period) {
        if (period == null) {
            return null;
        }

        FiscalPeriodResponse response = new FiscalPeriodResponse();
        response.setId(period.getId());
        response.setName(period.getName());
        response.setStartDate(period.getStartDate());
        response.setEndDate(period.getEndDate());
        response.setStatus(period.getStatus());
        response.setClosedBy(period.getClosedBy());
        response.setFiscalYear(period.getFiscalYear());
        response.setPeriodNumber(period.getPeriodNumber());

        // Convert LocalDateTime to Instant
        if (period.getClosedAt() != null) {
            response.setClosedAt(period.getClosedAt().toInstant(ZoneOffset.UTC));
        }
        if (period.getCreatedAt() != null) {
            response.setCreatedAt(period.getCreatedAt().toInstant(ZoneOffset.UTC));
        }

        return response;
    }
}
