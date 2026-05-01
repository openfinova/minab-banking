package com.openfinova.banking.gl.mapper;

import com.openfinova.banking.gl.dto.SuspenseAgingBucketDTO;
import com.openfinova.banking.gl.dto.SuspenseItemResponse;
import com.openfinova.banking.gl.entity.GLAccount;
import com.openfinova.banking.gl.entity.SuspenseItem;
import org.springframework.stereotype.Component;

/**
 * Mapper for converting suspense entities to DTOs.
 * Used by controllers in the service module.
 */
@Component
public class SuspenseMapper {

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
        response.setAgeDays(item.getAgeDays());
        response.setAgingBracket(item.getAgingBracket());
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

    /**
     * Convert SuspenseAgingBucket to DTO.
     * Helper for aging report generation.
     */
    public SuspenseAgingBucketDTO toAgingBucketDTO(com.openfinova.banking.gl.api.entity.AgingBracket bracket,
            Integer count, java.math.BigDecimal total, String currency) {
        return new SuspenseAgingBucketDTO(bracket, count, total, currency);
    }
}
