package com.openfinova.banking.gl.mapper;

import java.util.List;

import org.springframework.stereotype.Component;

import com.openfinova.banking.gl.dto.RevaluationDetailResponse;
import com.openfinova.banking.gl.dto.RevaluationRunResponse;
import com.openfinova.banking.gl.entity.GLRevaluationDetail;
import com.openfinova.banking.gl.entity.GLRevaluationRun;

@Component
public class RevaluationMapper {

    public RevaluationRunResponse toRunResponse(GLRevaluationRun run) {
        if (run == null) {
            return null;
        }
        RevaluationRunResponse response = new RevaluationRunResponse();
        response.setId(run.getId());
        response.setRevaluationDate(run.getRevaluationDate());
        response.setExecutedAt(run.getExecutedAt());
        response.setExecutedBy(run.getExecutedBy());
        response.setAccountsProcessed(run.getAccountsProcessed());
        response.setAccountsRevalued(run.getAccountsRevalued());
        response.setAccountsFailed(run.getAccountsFailed());
        response.setTotalAdjustment(run.getTotalAdjustment());
        response.setBaseCurrency(run.getBaseCurrency());
        response.setTriggerType(run.getTriggerType());
        response.setCorrelationId(run.getCorrelationId());
        response.setNotes(run.getNotes());
        return response;
    }

    public List<RevaluationRunResponse> toRunResponseList(List<GLRevaluationRun> runs) {
        if (runs == null) {
            return List.of();
        }
        return runs.stream().map(this::toRunResponse).toList();
    }

    public RevaluationDetailResponse toDetailResponse(GLRevaluationDetail detail) {
        if (detail == null) {
            return null;
        }
        RevaluationDetailResponse response = new RevaluationDetailResponse();
        response.setId(detail.getId());
        response.setRevaluationRunId(detail.getRevaluationRun() != null ? detail.getRevaluationRun().getId() : null);
        response.setAccountId(detail.getGlAccount() != null ? detail.getGlAccount().getId() : null);
        response.setAccountCurrency(detail.getAccountCurrency());
        response.setAccountBalance(detail.getAccountBalance());
        response.setOldExchangeRate(detail.getOldExchangeRate());
        response.setNewExchangeRate(detail.getNewExchangeRate());
        response.setOldBaseValue(detail.getOldBaseValue());
        response.setNewBaseValue(detail.getNewBaseValue());
        response.setUnrealizedGainLoss(detail.getUnrealizedGainLoss());
        response.setJournalTransactionId(
                detail.getJournalTransaction() != null ? detail.getJournalTransaction().getId() : null);
        return response;
    }

    public List<RevaluationDetailResponse> toDetailResponseList(List<GLRevaluationDetail> details) {
        if (details == null) {
            return List.of();
        }
        return details.stream().map(this::toDetailResponse).toList();
    }
}
