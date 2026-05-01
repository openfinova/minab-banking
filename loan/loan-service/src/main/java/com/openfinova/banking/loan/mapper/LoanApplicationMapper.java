package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.LoanApplicationRequest;
import com.openfinova.banking.loan.api.dto.LoanApplicationResponse;
import com.openfinova.banking.loan.entity.LoanApplication;

/**
 * Mapper for converting between LoanApplication entities and DTOs.
 */
public class LoanApplicationMapper {

    private LoanApplicationMapper() {
        // Utility class
    }

    public static LoanApplicationResponse toResponse(LoanApplication application) {
        if (application == null) {
            return null;
        }

        LoanApplicationResponse response = new LoanApplicationResponse();
        response.setId(application.getId());
        response.setApplicationNumber(application.getApplicationNumber());
        response.setCustomerId(application.getCustomerId());
        response.setProductId(application.getProductId());
        response.setRequestedAmount(application.getRequestedAmount());
        response.setRequestedTenorMonths(application.getRequestedTenorMonths());
        response.setCurrency(application.getCurrency());
        response.setStatus(application.getStatus());
        response.setPurpose(application.getPurpose());
        response.setMonthlyIncome(application.getMonthlyIncome());
        response.setExistingObligations(application.getExistingObligations());
        response.setCreditScore(application.getCreditScore());
        response.setRiskRating(application.getRiskRating());
        response.setApprovedInterestRate(application.getApprovedInterestRate());
        response.setApprovedAmount(application.getApprovedAmount());
        response.setApprovedTenorMonths(application.getApprovedTenorMonths());
        response.setApprovalDate(application.getApprovalDate());
        response.setApprovedBy(application.getApprovedBy());
        response.setRejectionDate(application.getRejectionDate());
        response.setRejectionReason(application.getRejectionReason());
        response.setRejectedBy(application.getRejectedBy());
        response.setGuarantorsRequired(application.getGuarantorsRequired());
        response.setUnderwriterId(application.getUnderwriterId());
        response.setUnderwriterAssignedBy(application.getUnderwriterAssignedBy());
        response.setUnderwriterAssignedAt(application.getUnderwriterAssignedAt());
        response.setRemarks(application.getRemarks());
        response.setCreatedAt(application.getCreatedAt());
        response.setUpdatedAt(application.getUpdatedAt());

        return response;
    }

    public static LoanApplication toEntity(LoanApplicationRequest request) {
        if (request == null) {
            return null;
        }

        LoanApplication application = new LoanApplication();
        application.setCustomerId(request.getCustomerId());
        application.setProductId(request.getProductId());
        application.setRequestedAmount(request.getRequestedAmount());
        application.setRequestedTenorMonths(request.getRequestedTenorMonths());
        application.setCurrency(request.getCurrency());
        application.setPurpose(request.getPurpose());
        application.setMonthlyIncome(request.getMonthlyIncome());
        application.setExistingObligations(request.getExistingObligations());
        application.setRemarks(request.getRemarks());

        return application;
    }
}
