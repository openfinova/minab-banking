package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.GuarantorResponse;
import com.openfinova.banking.loan.entity.Guarantor;

/**
 * Mapper for converting between Guarantor entities and DTOs.
 */
public class GuarantorMapper {

    private GuarantorMapper() {
        // Utility class
    }

    public static GuarantorResponse toResponse(Guarantor guarantor) {
        if (guarantor == null) {
            return null;
        }

        GuarantorResponse response = new GuarantorResponse();
        response.setId(guarantor.getId());
        response.setLoanAccountId(guarantor.getLoanAccount() != null ? guarantor.getLoanAccount().getId() : null);
        response.setLoanApplicationId(
                guarantor.getLoanApplication() != null ? guarantor.getLoanApplication().getId() : null);
        response.setCustomerId(guarantor.getCustomerId());
        response.setGuarantorType(guarantor.getGuarantorType());
        response.setGuaranteedAmount(guarantor.getGuaranteedAmount());
        response.setGuaranteePercentage(guarantor.getGuaranteePercentage());
        response.setStatus(guarantor.getStatus());
        response.setRemarks(guarantor.getRemarks());
        response.setVerifiedDate(guarantor.getVerifiedDate());
        response.setVerifiedBy(guarantor.getVerifiedBy());
        response.setReleasedDate(guarantor.getReleasedDate());
        response.setReleasedBy(guarantor.getReleasedBy());
        response.setRemovedDate(guarantor.getRemovedDate());
        response.setRemovedBy(guarantor.getRemovedBy());
        response.setRemovalReason(guarantor.getRemovalReason());
        response.setCreatedAt(guarantor.getCreatedAt());
        response.setUpdatedAt(guarantor.getUpdatedAt());

        return response;
    }
}
