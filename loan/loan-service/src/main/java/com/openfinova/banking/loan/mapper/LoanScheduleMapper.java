package com.openfinova.banking.loan.mapper;

import com.openfinova.banking.loan.api.dto.LoanScheduleResponse;
import com.openfinova.banking.loan.entity.LoanSchedule;

/**
 * Mapper for converting between LoanSchedule entities and DTOs.
 */
public class LoanScheduleMapper {

    private LoanScheduleMapper() {
        // Utility class
    }

    public static LoanScheduleResponse toResponse(LoanSchedule schedule) {
        if (schedule == null) {
            return null;
        }

        LoanScheduleResponse response = new LoanScheduleResponse();
        response.setId(schedule.getId());
        response.setLoanAccountId(schedule.getLoanAccount().getId());
        response.setInstallmentNumber(schedule.getInstallmentNumber());
        response.setDueDate(schedule.getDueDate());
        response.setPrincipalDue(schedule.getPrincipalDue());
        response.setInterestDue(schedule.getInterestDue());
        response.setTotalDue(schedule.getTotalDue());
        response.setPrincipalPaid(schedule.getPrincipalPaid());
        response.setInterestPaid(schedule.getInterestPaid());
        response.setFeesPaid(schedule.getFeesPaid());
        response.setPenaltiesPaid(schedule.getPenaltiesPaid());
        response.setOutstandingBalance(schedule.getOutstandingBalance());
        response.setStatus(schedule.getStatus());
        response.setPaidDate(schedule.getPaidDate());
        response.setIsOverdue(schedule.getIsOverdue());
        response.setDaysPastDue(schedule.getDaysPastDue());
        response.setCreatedAt(schedule.getCreatedAt());
        response.setUpdatedAt(schedule.getUpdatedAt());

        return response;
    }
}
