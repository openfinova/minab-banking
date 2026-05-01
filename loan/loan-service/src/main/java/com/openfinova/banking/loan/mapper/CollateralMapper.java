package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.CollateralResponse;
import com.openfinova.banking.loan.entity.Collateral;

/**
 * Mapper for converting between Collateral entities and DTOs.
 */
public class CollateralMapper {

    private CollateralMapper() {
        // Utility class
    }

    public static CollateralResponse toResponse(Collateral collateral) {
        if (collateral == null) {
            return null;
        }

        CollateralResponse response = new CollateralResponse();
        response.setId(collateral.getId());
        response.setCollateralReference(collateral.getCollateralReference());
        response.setLoanAccountId(collateral.getLoanAccount().getId());
        response.setCollateralType(collateral.getCollateralType());
        response.setDescription(collateral.getDescription());
        response.setValuationAmount(collateral.getValuationAmount());
        response.setCurrency(collateral.getCurrency());
        response.setValuationDate(collateral.getValuationDate());
        response.setValuedBy(collateral.getValuedBy());
        response.setStatus(collateral.getStatus());
        response.setLocation(collateral.getLocation());
        response.setRegistrationNumber(collateral.getRegistrationNumber());
        response.setInsuranceExpiryDate(collateral.getInsuranceExpiryDate());
        response.setInsurancePolicyNumber(collateral.getInsurancePolicyNumber());
        response.setReleaseDate(collateral.getReleaseDate());
        response.setRemarks(collateral.getRemarks());
        response.setCreatedAt(collateral.getCreatedAt());
        response.setUpdatedAt(collateral.getUpdatedAt());

        return response;
    }
}
