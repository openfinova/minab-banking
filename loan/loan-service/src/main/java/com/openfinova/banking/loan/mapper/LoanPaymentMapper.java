package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.LoanPaymentResponse;
import com.openfinova.banking.loan.api.dto.PaymentAllocationResponse;
import com.openfinova.banking.loan.dto.PaymentAllocation;
import com.openfinova.banking.loan.entity.LoanPayment;

/**
 * Mapper for converting between LoanPayment entities and DTOs.
 */
public class LoanPaymentMapper {

    private LoanPaymentMapper() {
        // Utility class
    }

    /**
     * Converts a LoanPayment entity to a response DTO.
     */
    public static LoanPaymentResponse toResponse(LoanPayment payment) {
        if (payment == null) {
            return null;
        }

        LoanPaymentResponse response = new LoanPaymentResponse();
        response.setId(payment.getId());
        response.setPaymentReference(payment.getPaymentReference());
        response.setLoanAccountId(payment.getLoanAccount().getId());
        response.setPaymentDate(payment.getPaymentDate());
        response.setPaymentAmount(payment.getPaymentAmount());
        response.setPrincipalPaid(payment.getPrincipalPaid());
        response.setInterestPaid(payment.getInterestPaid());
        response.setFeesPaid(payment.getFeesPaid());
        response.setPenaltiesPaid(payment.getPenaltiesPaid());
        response.setCurrency(payment.getCurrency());
        response.setPaymentType(payment.getPaymentType());
        response.setPaymentMethod(payment.getPaymentMethod());
        response.setTransactionReference(payment.getTransactionReference());
        response.setIsReversed(payment.getIsReversed());
        response.setReversedAt(payment.getReversedAt());
        response.setReversalReason(payment.getReversalReason());
        response.setRemarks(payment.getRemarks());
        response.setCreatedAt(payment.getCreatedAt());
        response.setUpdatedAt(payment.getUpdatedAt());

        return response;
    }

    /**
     * Converts a PaymentAllocation to a response DTO.
     */
    public static PaymentAllocationResponse toAllocationResponse(PaymentAllocation allocation) {
        if (allocation == null) {
            return null;
        }

        PaymentAllocationResponse response = new PaymentAllocationResponse();
        response.setTotalAmount(allocation.getTotalAmount());
        response.setPrincipalAmount(allocation.getPrincipalAmount());
        response.setInterestAmount(allocation.getInterestAmount());
        response.setFeesAmount(allocation.getFeesAmount());
        response.setPenaltiesAmount(allocation.getPenaltiesAmount());
        response.setExcessAmount(allocation.getExcessAmount());

        return response;
    }
}
