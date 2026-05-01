package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.LoanRestructuringResponse;
import com.openfinova.banking.loan.entity.LoanRestructuring;

/**
 * Mapper for converting between LoanRestructuring entities and DTOs.
 */
public class LoanRestructuringMapper {

    private LoanRestructuringMapper() {
        // Utility class
    }

    public static LoanRestructuringResponse toResponse(LoanRestructuring restructuring) {
        if (restructuring == null) {
            return null;
        }

        LoanRestructuringResponse response = new LoanRestructuringResponse();
        response.setId(restructuring.getId());
        response.setLoanAccountId(restructuring.getLoanAccount().getId());
        response.setRestructuringDate(restructuring.getRestructuringDate());
        response.setRestructuringType(restructuring.getRestructuringType());
        response.setOldPrincipalBalance(restructuring.getOldPrincipalBalance());
        response.setNewPrincipalBalance(restructuring.getNewPrincipalBalance());
        response.setOldInterestRate(restructuring.getOldInterestRate());
        response.setNewInterestRate(restructuring.getNewInterestRate());
        response.setOldTenorMonths(restructuring.getOldTenorMonths());
        response.setNewTenorMonths(restructuring.getNewTenorMonths());
        response.setReason(restructuring.getReason());
        response.setApprovedBy(restructuring.getApprovedBy());
        response.setCreatedAt(restructuring.getCreatedAt());

        return response;
    }
}
