package com.openfinova.banking.tp.mapper;

import org.springframework.stereotype.Component;

import com.openfinova.banking.setup.api.DateTimeService;
import com.openfinova.banking.tp.api.dto.CreateFeeWaiverRequest;
import com.openfinova.banking.tp.api.dto.FeeWaiverResponse;
import com.openfinova.banking.tp.entity.FeeWaiver;

@Component
public class FeeWaiverMapper {

    private final DateTimeService dateTimeService;

    public FeeWaiverMapper(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    /**
     * Maps create request to a persisted-ready entity (defaults, audit actor).
     */
    public FeeWaiver toEntity(CreateFeeWaiverRequest request, String actor) {
        if (request == null) {
            return null;
        }

        FeeWaiver waiver = new FeeWaiver();
        waiver.setAccountId(request.getCustomerId());

        String name = firstNonBlank(request.getWaiverName(), request.getReason());
        if (name != null) {
            waiver.setWaiverName(name);
        }

        if (request.getDescription() != null) {
            String d = request.getDescription().trim();
            if (!d.isEmpty()) {
                waiver.setDescription(d);
            }
        }

        waiver.setTransactionType(request.getTransactionType());
        waiver.setCustomerTier(request.getCustomerTier());

        String campaign = trimToNull(request.getCampaignCode());
        if (campaign != null) {
            waiver.setCampaignCode(campaign);
        }

        if (request.getIsActive() != null) {
            waiver.setIsActive(request.getIsActive());
        }
        if (request.getEffectiveFrom() != null) {
            waiver.setEffectiveFrom(request.getEffectiveFrom());
        }
        if (request.getEffectiveTo() != null) {
            waiver.setEffectiveTo(request.getEffectiveTo());
        }
        if (request.getMaxUsageCount() != null) {
            waiver.setMaxUsageCount(request.getMaxUsageCount());
        }
        if (request.getIsGlobal() != null) {
            waiver.setIsGlobal(request.getIsGlobal());
        }
        if (request.getConditions() != null) {
            waiver.setConditions(request.getConditions());
        }
        if (request.getMetadata() != null) {
            waiver.setMetadata(request.getMetadata());
        }

        if (waiver.getEffectiveFrom() == null) {
            waiver.setEffectiveFrom(dateTimeService.now());
        }
        if (waiver.getWaiverName() == null || waiver.getWaiverName().isBlank()) {
            waiver.setWaiverName("Fee waiver");
        }
        waiver.setCreatedBy(actor);
        waiver.setUpdatedBy(actor);
        return waiver;
    }

    public FeeWaiverResponse toResponse(FeeWaiver w) {
        if (w == null) {
            return null;
        }

        FeeWaiverResponse r = new FeeWaiverResponse();
        r.setId(w.getId());
        r.setCustomerId(w.getAccountId());
        r.setWaiverName(w.getWaiverName());
        r.setReason(w.getWaiverName());
        r.setDescription(w.getDescription());
        r.setTransactionType(w.getTransactionType() != null ? w.getTransactionType().name() : null);
        r.setCustomerTier(w.getCustomerTier() != null ? w.getCustomerTier().name() : null);
        r.setCampaignCode(w.getCampaignCode());
        r.setIsActive(w.getIsActive());
        r.setEffectiveFrom(w.getEffectiveFrom());
        r.setEffectiveTo(w.getEffectiveTo());
        r.setMaxUsageCount(w.getMaxUsageCount());
        r.setIsGlobal(w.getIsGlobal());
        r.setConditions(w.getConditions());
        r.setMetadata(w.getMetadata());
        return r;
    }

    private static String trimToNull(String s) {
        if (s == null) {
            return null;
        }
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.trim().isEmpty()) {
            return a.trim();
        }
        if (b != null && !b.trim().isEmpty()) {
            return b.trim();
        }
        return null;
    }
}
