package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.LoanAccountResponse;
import com.openfinova.banking.loan.entity.LoanAccount;

/**
 * Mapper for converting between LoanAccount entities and DTOs.
 */
public class LoanAccountMapper {

    private LoanAccountMapper() {
        // Utility class
    }

    /**
     * Converts a LoanAccount entity to a response DTO.
     */
    public static LoanAccountResponse toResponse(LoanAccount account) {
        if (account == null) {
            return null;
        }

        LoanAccountResponse response = new LoanAccountResponse();
        response.setId(account.getId());
        response.setLoanAccountNumber(account.getLoanAccountNumber());
        response.setApplicationId(account.getApplicationId());
        response.setCustomerId(account.getCustomerId());
        response.setProductId(account.getProductId());
        response.setPrincipalAmount(account.getPrincipalAmount());
        response.setOutstandingPrincipal(account.getOutstandingPrincipal());
        response.setOutstandingInterest(account.getOutstandingInterest());
        response.setOutstandingFees(account.getOutstandingFees());
        response.setOutstandingPenalties(account.getOutstandingPenalties());
        response.setTotalOutstanding(account.getTotalOutstanding());
        response.setTenorMonths(account.getTenorMonths());
        response.setInterestRate(account.getInterestRate());
        response.setInterestCalculationMethod(account.getInterestCalculationMethod());
        response.setRepaymentFrequency(account.getRepaymentFrequency());
        response.setAmortizationType(account.getAmortizationType());
        response.setCurrency(account.getCurrency());
        response.setStatus(account.getStatus());
        response.setDisbursementDate(account.getDisbursementDate());
        response.setMaturityDate(account.getMaturityDate());
        response.setFirstPaymentDate(account.getFirstPaymentDate());
        response.setLastPaymentDate(account.getLastPaymentDate());
        response.setClosedDate(account.getClosedDate());
        response.setTotalPaid(account.getTotalPaid());
        response.setDaysPastDue(account.getDaysPastDue());
        response.setDelinquencyBucket(
                account.getDelinquencyBucket() != null ? account.getDelinquencyBucket().name() : null);
        response.setIsRestructured(account.getIsRestructured());
        response.setRestructuredDate(account.getRestructuredDate());
        response.setIsTopUp(account.getIsTopUp());
        response.setOriginalLoanId(account.getOriginalLoanId());
        response.setRemarks(account.getRemarks());
        response.setCreatedAt(account.getCreatedAt());
        response.setUpdatedAt(account.getUpdatedAt());

        return response;
    }
}
