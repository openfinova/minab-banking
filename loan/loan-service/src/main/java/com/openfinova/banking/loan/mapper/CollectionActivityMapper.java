package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.CollectionActivityResponse;
import com.openfinova.banking.loan.entity.CollectionActivity;

/**
 * Mapper for converting between CollectionActivity entities and DTOs.
 */
public class CollectionActivityMapper {

    private CollectionActivityMapper() {
        // Utility class
    }

    public static CollectionActivityResponse toResponse(CollectionActivity activity) {
        if (activity == null) {
            return null;
        }

        CollectionActivityResponse response = new CollectionActivityResponse();
        response.setId(activity.getId());
        response.setLoanAccountId(activity.getLoanAccount().getId());
        response.setActivityDate(activity.getActivityDate());
        response.setActivityType(activity.getActivityType());
        response.setNotes(activity.getNotes());
        response.setAssignedTo(activity.getAssignedTo());
        response.setFollowUpDate(activity.getFollowUpDate());
        response.setStatus(activity.getStatus());
        response.setCreatedAt(activity.getCreatedAt());
        response.setUpdatedAt(activity.getUpdatedAt());

        return response;
    }
}
