package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.EarlySettlementResponse;
import com.openfinova.banking.loan.entity.EarlySettlement;

/**
 * Mapper for converting between EarlySettlement entities and DTOs.
 */
public class EarlySettlementMapper {

    private EarlySettlementMapper() {
        // Utility class
    }

    public static EarlySettlementResponse toResponse(EarlySettlement settlement) {
        if (settlement == null) {
            return null;
        }

        EarlySettlementResponse response = new EarlySettlementResponse();
        response.setId(settlement.getId());
        response.setQuoteReference(settlement.getQuoteReference());
        response.setLoanAccountId(settlement.getLoanAccount().getId());
        response.setQuoteDate(settlement.getQuoteDate());
        response.setValidUntil(settlement.getValidUntil());
        response.setOutstandingPrincipal(settlement.getOutstandingPrincipal());
        response.setOutstandingInterest(settlement.getOutstandingInterest());
        response.setOutstandingFees(settlement.getOutstandingFees());
        response.setRebateAmount(settlement.getRebateAmount());
        response.setPenaltyAmount(settlement.getPenaltyAmount());
        response.setSettlementAmount(settlement.getSettlementAmount());
        response.setCurrency(settlement.getCurrency());
        response.setCalculationMethod(settlement.getCalculationMethod());
        response.setStatus(settlement.getStatus());
        response.setSettledDate(settlement.getSettledDate());
        response.setPaymentReference(settlement.getPaymentReference());
        response.setApprovedDate(settlement.getApprovedDate());
        response.setApprovedBy(settlement.getApprovedBy());
        response.setRejectedDate(settlement.getRejectedDate());
        response.setRejectedBy(settlement.getRejectedBy());
        response.setRejectionReason(settlement.getRejectionReason());
        response.setCancelledDate(settlement.getCancelledDate());
        response.setCancelledBy(settlement.getCancelledBy());
        response.setCancellationReason(settlement.getCancellationReason());
        response.setRemarks(settlement.getRemarks());
        response.setCreatedAt(settlement.getCreatedAt());
        response.setUpdatedAt(settlement.getUpdatedAt());

        return response;
    }
}
