package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.LoanDisbursementResponse;
import com.openfinova.banking.loan.entity.LoanDisbursement;

/**
 * Mapper for converting between LoanDisbursement entities and DTOs.
 */
public class LoanDisbursementMapper {

    private LoanDisbursementMapper() {
        // Utility class
    }

    public static LoanDisbursementResponse toResponse(LoanDisbursement disbursement) {
        if (disbursement == null) {
            return null;
        }

        LoanDisbursementResponse response = new LoanDisbursementResponse();
        response.setId(disbursement.getId());
        response.setDisbursementReference(disbursement.getDisbursementReference());
        response.setLoanAccountId(disbursement.getLoanAccount().getId());
        response.setDisbursementDate(disbursement.getDisbursementDate());
        response.setDisbursementAmount(disbursement.getDisbursementAmount());
        response.setCurrency(disbursement.getCurrency());
        response.setDisbursementMethod(disbursement.getDisbursementMethod());
        response.setBeneficiaryAccountNumber(disbursement.getBeneficiaryAccountNumber());
        response.setBeneficiaryName(disbursement.getBeneficiaryName());
        response.setTransactionReference(disbursement.getTransactionReference());
        response.setStatus(disbursement.getStatus());
        response.setRemarks(disbursement.getRemarks());
        response.setCreatedAt(disbursement.getCreatedAt());
        response.setUpdatedAt(disbursement.getUpdatedAt());

        return response;
    }
}
