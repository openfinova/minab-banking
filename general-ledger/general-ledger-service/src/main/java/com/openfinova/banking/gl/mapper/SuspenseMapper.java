package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.dto.SuspenseAgingBucketDTO;
import com.openfinova.banking.gl.dto.SuspenseEscalationResponse;
import com.openfinova.banking.gl.dto.SuspenseItemResponse;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.SuspenseEscalation;
import com.openfinova.banking.gl.entity.SuspenseItem;
import com.openfinova.banking.setup.api.DateTimeService;

import org.springframework.stereotype.Component;

/**
 * Mapper for converting suspense entities to DTOs.
 * Used by controllers in the service module.
 */
@Component
public class SuspenseMapper {

    private final DateTimeService dateTimeService;

    public SuspenseMapper(DateTimeService dateTimeService) {
        this.dateTimeService = dateTimeService;
    }

    /**
     * Convert SuspenseItem entity to SuspenseItemResponse DTO.
     */
    public SuspenseItemResponse toSuspenseItemResponse(SuspenseItem item) {
        if (item == null) {
            return null;
        }

        SuspenseItemResponse response = new SuspenseItemResponse();
        response.setId(item.getId());

        if (item.getGlTransaction() != null) {
            response.setGlTransactionId(item.getGlTransaction().getId());
            response.setTransactionReference(item.getGlTransaction().getReferenceId());
        }

        response.setAmount(item.getAmount());
        response.setCurrency(item.getCurrency());
        response.setStatus(item.getStatus());
        response.setReasonCode(item.getReasonCode());
        response.setDescription(item.getDescription());
        response.setSourceSystem(item.getSourceSystem());
        response.setExternalReference(item.getExternalReference());
        response.setPostingDate(item.getPostingDate());
        var today = dateTimeService.today();
        response.setAgeDays(item.getAgeDays(today));
        response.setAgingBracket(item.getAgingBracket(today));
        response.setAssignedTo(item.getAssignedTo());
        response.setInvestigationNotes(item.getInvestigationNotes());

        if (item.getTargetAccount() != null) {
            GLAccount targetAccount = item.getTargetAccount();
            response.setTargetAccountId(targetAccount.getId());
            response.setTargetAccountNumber(targetAccount.getCode());
            response.setTargetAccountName(targetAccount.getName());
        }

        response.setClearedDate(item.getClearedDate());
        response.setClearedBy(item.getClearedBy());

        if (item.getClearingTransaction() != null) {
            response.setClearingTransactionId(item.getClearingTransaction().getId());
        }

        response.setRequiresAMLReview(item.requiresAMLReview());
        response.setCreatedAt(item.getCreatedAt());
        response.setUpdatedAt(item.getUpdatedAt());
        response.setCreatedBy(item.getCreatedBy());

        return response;
    }

    public SuspenseEscalationResponse toEscalationResponse(SuspenseEscalation escalation) {
        if (escalation == null) {
            return null;
        }
        SuspenseEscalationResponse response = new SuspenseEscalationResponse();
        response.setId(escalation.getId());
        response.setSuspenseItemId(escalation.getSuspenseItem() != null ? escalation.getSuspenseItem().getId() : null);
        response.setEscalationLevel(escalation.getEscalationLevel());
        response.setEscalatedDate(escalation.getEscalatedDate());
        response.setAssignedTo(escalation.getAssignedTo());
        response.setDueDate(escalation.getDueDate());
        response.setIsResolved(escalation.getIsResolved());
        response.setResolvedDate(escalation.getResolvedDate());
        response.setResolvedBy(escalation.getResolvedBy());
        response.setEscalationNotes(escalation.getEscalationNotes());
        response.setResolutionNotes(escalation.getResolutionNotes());
        response.setSlaBreached(escalation.getSlaBreached());
        response.setCreatedAt(escalation.getCreatedAt());
        response.setUpdatedAt(escalation.getUpdatedAt());
        response.setCreatedBy(escalation.getCreatedBy());
        return response;
    }

    public java.util.List<SuspenseEscalationResponse> toEscalationResponseList(
            java.util.List<SuspenseEscalation> escalations) {
        if (escalations == null) {
            return java.util.List.of();
        }
        return escalations.stream().map(this::toEscalationResponse).toList();
    }

    /**
     * Convert SuspenseAgingBucket to DTO.
     * Helper for aging report generation.
     */
    public SuspenseAgingBucketDTO toAgingBucketDTO(com.openfinova.banking.gl.api.entity.AgingBracket bracket,
            Integer count, java.math.BigDecimal total, String currency) {
        return new SuspenseAgingBucketDTO(bracket, count, total, currency);
    }
}
