package com.openfinova.banking.customer.account.mapper;

import java.time.LocalDateTime;

import com.openfinova.banking.customer.account.api.dto.AccountRelationshipResponse;
import com.openfinova.banking.customer.account.entity.AccountRelationship;
import org.springframework.stereotype.Component;

/**
 * Maps between {@link AccountRelationship} entities and {@link AccountRelationshipResponse} DTOs.
 */
@Component
public class AccountRelationshipMapper {

    public AccountRelationshipResponse toResponse(AccountRelationship relationship, LocalDateTime evaluatedAt) {
        if (relationship == null) {
            return null;
        }

        AccountRelationshipResponse response = new AccountRelationshipResponse();
        response.setId(relationship.getId());
        response.setAccountId(relationship.getCustomerAccount().getId());
        response.setUserProfileId(relationship.getUserProfileId());
        response.setRelationshipType(relationship.getRelationshipType());
        response.setPermissions(relationship.getPermissions());
        response.setPercentageOwnership(relationship.getPercentageOwnership());
        response.setIsBeneficiary(relationship.getIsBeneficiary());
        response.setBeneficiaryPercentage(relationship.getBeneficiaryPercentage());
        // Map status to isActive boolean
        response.setIsActive(relationship.isEffective(evaluatedAt));
        response.setCreatedAt(relationship.getCreatedAt());
        response.setCreatedBy(relationship.getCreatedBy());

        return response;
    }
}
